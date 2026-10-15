# jiit-core

Rust client core for the JIIT web kiosk (**CampusLynx**). Handles the request
crypto, session, and API envelope. No Android dependencies — fully testable on
the host. See `../SPEC.md` for the full reverse-engineering write-up.

## Layout

| File | Purpose |
|---|---|
| `src/crypto.rs` | AES-128-CBC body encryption + `LocalName` nonce (date-derived key) |
| `src/session.rs` | Session/auth fields sent with most requests |
| `src/models.rs` | `ApiEnvelope`, `LoginResponse`, `MenuNode` |
| `src/client.rs` | Async `JiitClient` (encrypted POST, plain POST, GET) |

The Android/Compose layer (via UniFFI) wraps this crate; keep all logic here so
it stays unit-testable without an emulator.

## Build & test

```bash
cd jiit-core
cargo test          # crypto tests use real captured traffic from 2026-10-15
cargo build --release
```

The crypto tests decrypt actual captured request bodies and headers — if they
pass, the scheme is implemented correctly.

## Usage sketch

```rust
use jiit_core::{JiitClient, Session, LoginResponse};
use serde_json::json;

// After login, populate a Session from the LoginResponse, then:
let client = JiitClient::new(session)?;

// Encrypted POST: build the plain JSON, the client encrypts it.
let mut payload = client.session().auth_fields();
payload["enrollmentNo"] = json!(client.session().enrollmentno);
let env: jiit_core::ApiEnvelope<serde_json::Value> =
    client.post_encrypted("/feedbackformcontroller/getCreadits", &payload).await?;

// Menu: response is an array of nodes — render the app menu from this.
let menu: jiit_core::ApiEnvelope<Vec<jiit_core::MenuNode>> =
    client.post_encrypted("/clxuser/getmenulist", &client.session().auth_fields()).await?;
```

## Gotchas

- **Cert**: the portal uses an internal CA. The client currently sets
  `danger_accept_invalid_certs(true)`. For a real release, ship the JIIT root CA.
- **Date**: the AES key derives from the **local** (IST) date and rotates at
  midnight. A stale body fails with a padding error, not a clean 401.
- **`LocalName`**: must be fresh per request; never cache it.
- **Payments**: don't reimplement the gateways — open the returned URL in the
  system browser. Read-only screens are safe to fully replicate.

## Status

Scaffold. Crypto and envelope handling are implemented and tested against live
traffic. Endpoint coverage is partial — `post_encrypted` is generic, so adding a
feature means building its payload and deserializing the response. See the
endpoint catalogue in `../SPEC.md` §7.
