//! CampusLynx (JIIT web kiosk) client core.
//!
//! Reverse-engineered from the Angular bundle at
//! `https://webportal.jiit.ac.in:6011/studentportal/` and verified against live
//! traffic on 2026-10-15. See `../SPEC.md` for the full write-up.
//!
//! This crate is deliberately free of Android bindings so the crypto and API
//! logic stay testable on the host. The Compose UI layer (UniFFI) wraps this.

pub mod client;
pub mod crypto;
pub mod models;
pub mod session;

pub use client::{JiitClient, ClientError, API_BASE, DEFAULT_BASE};
pub use crypto::{decrypt_body, encrypt_body, encrypt_body_now, local_name, local_name_now, CryptoError};
pub use models::{ApiEnvelope, LoginResponse, MenuNode, Status};
pub use session::Session;
