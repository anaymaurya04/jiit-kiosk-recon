//! Request-body encryption and the per-request `LocalName` nonce.
//!
//! The Angular app encrypts every POST body with AES-128-CBC using a key derived
//! from the **current local date**, and sends a fresh `LocalName` header built
//! the same way. Verified by decrypting captured live traffic (tests below).
//!
//! The key and IV are public — they ship in the JS bundle. This is obfuscation,
//! not real transport security; TLS is what actually protects the channel. Don't
//! treat it as a secret, and don't oversell it in the app.

use aes::cipher::{block_padding::Pkcs7, BlockDecryptMut, BlockEncryptMut, KeyIvInit};
use aes::Aes128;
use base64::{engine::general_purpose::STANDARD as B64, Engine as _};
use chrono::{Datelike, Local, NaiveDate};
use rand::Rng;

type Aes128CbcEnc = cbc::Encryptor<Aes128>;
type Aes128CbcDec = cbc::Decryptor<Aes128>;

/// Fixed IV from the bundle (`tokenFromUIIV`).
const IV: &[u8; 16] = b"dcek9wb8frty1pnm";

/// Exactly the alphabet used by the Angular interceptor — note the missing `Y`
/// and `j`, and the odd `WXTZ` tail. Copy it verbatim; the server may validate
/// the nonce shape.
const NONCE_ALPHABET: &[u8] = b"0123456789ABCDEFGHIJKLMNOPQRSTUVWXTZabcdefghiklmnopqrstuvwxyz";

/// Mirror of the JS date string:
/// `d[0] m[0] y[0] weekday d[1] m[1] y[1]`, with day/month zero-padded to 2 and
/// the year cut to its last 2 digits. `weekday` is JS `getDay()` (0 = Sunday).
///
/// Example: 2026-10-15 (Thursday) → `"1124506"`.
pub fn date_part(date: NaiveDate) -> String {
    let d = format!("{:02}", date.day());
    let m = format!("{:02}", date.month());
    let y = format!("{:02}", date.year() % 100);
    let w = date.weekday().num_days_from_sunday(); // 0 = Sunday, matches JS
    let ch = |s: &str, i: usize| s.chars().nth(i).expect("padded to 2 chars");
    format!(
        "{}{}{}{}{}{}{}",
        ch(&d, 0),
        ch(&m, 0),
        ch(&y, 0),
        w,
        ch(&d, 1),
        ch(&m, 1),
        ch(&y, 1)
    )
}

/// `qa8y` + datePart + `ty1pn` → exactly 16 bytes.
fn key_for(date: NaiveDate) -> [u8; 16] {
    let mut k = [0u8; 16];
    k[..4].copy_from_slice(b"qa8y");
    k[4..11].copy_from_slice(date_part(date).as_bytes());
    k[11..].copy_from_slice(b"ty1pn");
    k
}

/// Encrypt a JSON string into the base64 blob the API expects as the POST body.
pub fn encrypt_body(plaintext: &str, date: NaiveDate) -> String {
    let ct = Aes128CbcEnc::new(key_for(date).into(), IV.into())
        .encrypt_padded_vec_mut::<Pkcs7>(plaintext.as_bytes());
    B64.encode(ct)
}

/// [`encrypt_body`] using the current **local** date (what the browser does).
pub fn encrypt_body_now(plaintext: &str) -> String {
    encrypt_body(plaintext, Local::now().date_naive())
}

/// Decrypt a base64 blob captured from the wire.
///
/// The API never sends encrypted responses — this exists for tests and debugging.
pub fn decrypt_body(ciphertext_b64: &str, date: NaiveDate) -> Result<Vec<u8>, CryptoError> {
    let ct = B64.decode(ciphertext_b64).map_err(|_| CryptoError::Base64)?;
    Aes128CbcDec::new(key_for(date).into(), IV.into())
        .decrypt_padded_vec_mut::<Pkcs7>(&ct)
        .map_err(|_| CryptoError::Padding)
}

/// Generate the `LocalName` header value: `head(4) + datePart(7) + tail(5)` = 16
/// chars, AES-encrypted and base64-encoded (always 44 chars ending in `=`).
///
/// A fresh value is required on **every** request — reuse across local midnight
/// fails because the key rotates with the date.
pub fn local_name(date: NaiveDate) -> String {
    let mut rng = rand::thread_rng();
    let head = rand_chars(&mut rng, 4);
    let tail = rand_chars(&mut rng, 5);
    encrypt_body(&format!("{}{}{}", head, date_part(date), tail), date)
}

/// [`local_name`] using the current **local** date.
pub fn local_name_now() -> String {
    local_name(Local::now().date_naive())
}

fn rand_chars(rng: &mut impl Rng, n: usize) -> String {
    (0..n)
        .map(|_| NONCE_ALPHABET[rng.gen_range(0..NONCE_ALPHABET.len())] as char)
        .collect()
}

#[derive(Debug, thiserror::Error, PartialEq, Eq)]
pub enum CryptoError {
    #[error("invalid base64")]
    Base64,
    #[error("PKCS7 padding mismatch — usually a wrong or date-rotated key")]
    Padding,
}

#[cfg(test)]
mod tests {
    use super::*;

    /// 2026-10-15 was a Thursday → JS `getDay()` == 4.
    fn day() -> NaiveDate {
        NaiveDate::from_ymd_opt(2026, 10, 15).unwrap()
    }

    /// Captured live `POST /clxuser/getmenulist` body.
    const CAPTURED_BODY: &str = "H7XoAW/M41ZQe1CnVxK3qPcohL3GC1WwDLM8f3ygAOuqK4GsFuDdXdS4FG1IkCYF1I8G7pnQlrqwx3Nc9SEn0bgQK0map+b0/5q720TnY24=";

    /// Captured live `LocalName` header from the same request.
    const CAPTURED_LOCALNAME: &str = "QKM0Nb0fYVJBmR+qDGy+PMOJ731xjUxEE/DXq+W1wyk=";

    #[test]
    fn date_part_matches_js() {
        assert_eq!(date_part(day()), "1124506");
    }

    #[test]
    fn key_is_the_captured_one() {
        assert_eq!(&key_for(day()), b"qa8y1124506ty1pn");
    }

    #[test]
    fn decrypts_live_request_body() {
        let pt = decrypt_body(CAPTURED_BODY, day()).unwrap();
        let v: serde_json::Value = serde_json::from_slice(&pt).unwrap();
        assert_eq!(v["membertype"], "S");
        assert_eq!(v["instituteid"], "11IN1902J000003");
    }

    #[test]
    fn decrypts_live_localname_header() {
        let pt = decrypt_body(CAPTURED_LOCALNAME, day()).unwrap();
        assert_eq!(pt.len(), 16, "plaintext must be 4+7+5 chars");
        assert_eq!(&pt[4..11], b"1124506", "date sits in the middle");
    }

    #[test]
    fn local_name_round_trips() {
        let header = local_name(day());
        assert!(header.len() == 44 && header.ends_with('='));
        let pt = decrypt_body(&header, day()).unwrap();
        assert_eq!(&pt[4..11], date_part(day()).as_bytes());
    }

    #[test]
    fn encrypt_round_trips() {
        let msg = r#"{"userid":"USID2507A0008049","instituteid":"11IN1902J000003","membertype":"S"}"#;
        let ct = encrypt_body(msg, day());
        assert_eq!(decrypt_body(&ct, day()).unwrap(), msg.as_bytes());
    }

    #[test]
    fn wrong_date_fails_with_padding_error() {
        let tomorrow = day().succ_opt().unwrap();
        assert_eq!(
            decrypt_body(CAPTURED_BODY, tomorrow),
            Err(CryptoError::Padding),
            "key rotates at local midnight"
        );
    }
}
