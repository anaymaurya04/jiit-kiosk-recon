use crate::crypto;
use crate::models::ApiEnvelope;
use crate::session::Session;
use reqwest::header::{HeaderMap, HeaderValue, AUTHORIZATION, CONTENT_TYPE};
use reqwest::{Client, Response};
use serde::de::DeserializeOwned;
use serde_json::Value;
use std::time::Duration;

/// Portal origin (student app + API share this host).
pub const DEFAULT_BASE: &str = "https://webportal.jiit.ac.in:6011";
/// Baked into the bundle as `environment.apiBase`.
pub const API_BASE: &str = "/StudentPortalAPI";

pub struct JiitClient {
    http: Client,
    base: String,
    session: Session,
}

impl JiitClient {
    pub fn new(session: Session) -> Result<Self, ClientError> {
        let http = Client::builder()
            // The portal uses an internal CA a normal build can't verify. For a
            // safer release, ship the JIIT root CA PEM via `add_root_certificate`
            // and drop this flag.
            .danger_accept_invalid_certs(true)
            .cookie_store(true)
            .timeout(Duration::from_secs(30))
            .build()?;
        Ok(Self {
            http,
            base: DEFAULT_BASE.into(),
            session,
        })
    }

    /// Point the client at a different origin (e.g. employee portal on :6010).
    pub fn with_base(mut self, base: impl Into<String>) -> Self {
        self.base = base.into();
        self
    }

    pub fn session(&self) -> &Session {
        &self.session
    }

    pub fn session_mut(&mut self) -> &mut Session {
        &mut self.session
    }

    fn url(&self, path: &str) -> String {
        format!("{}{}{}", self.base, API_BASE, path)
    }

    /// Auth + nonce headers. A fresh `LocalName` is minted per call.
    fn auth_headers(&self) -> Result<HeaderMap, ClientError> {
        let mut h = HeaderMap::new();
        h.insert(CONTENT_TYPE, HeaderValue::from_static("application/json"));
        if let Some(token) = self.session.token() {
            h.insert(
                AUTHORIZATION,
                HeaderValue::from_str(&format!("Bearer {token}"))?,
            );
        }
        h.insert("localname", HeaderValue::from_str(&crypto::local_name_now())?);
        Ok(h)
    }

    /// Encrypted POST — the normal path for every feature endpoint.
    ///
    /// `payload` is the endpoint's plain JSON (merge `session.auth_fields()`);
    /// it gets encrypted before it leaves the device.
    pub async fn post_encrypted<T: DeserializeOwned>(
        &self,
        path: &str,
        payload: &Value,
    ) -> Result<ApiEnvelope<T>, ClientError> {
        let body = crypto::encrypt_body_now(&payload.to_string());
        let res = self
            .http
            .post(self.url(path))
            .headers(self.auth_headers()?)
            .body(body)
            .send()
            .await?;
        parse(res).await
    }

    /// Plain-JSON POST — only the `feedbackformresponse*` family skips encryption.
    pub async fn post_plain<T: DeserializeOwned>(
        &self,
        path: &str,
        payload: &Value,
    ) -> Result<ApiEnvelope<T>, ClientError> {
        let res = self
            .http
            .post(self.url(path))
            .headers(self.auth_headers()?)
            .json(payload)
            .send()
            .await?;
        parse(res).await
    }

    /// Unauthenticated GET (captcha, marquee, logo, product version).
    pub async fn get<T: DeserializeOwned>(&self, path: &str) -> Result<ApiEnvelope<T>, ClientError> {
        let res = self.http.get(self.url(path)).send().await?;
        parse(res).await
    }
}

async fn parse<T: DeserializeOwned>(res: Response) -> Result<ApiEnvelope<T>, ClientError> {
    let http_status = res.status();
    let text = res.text().await?;

    if http_status.as_u16() == 401 {
        return Err(ClientError::Unauthorized);
    }

    let env: ApiEnvelope<T> =
        serde_json::from_str(&text).map_err(|_| ClientError::Decode)?;

    if !env.status.is_success() {
        return Err(ClientError::Api(env.status.errors.unwrap_or_default()));
    }
    Ok(env)
}

#[derive(Debug, thiserror::Error)]
pub enum ClientError {
    #[error("HTTP transport: {0}")]
    Transport(#[from] reqwest::Error),
    #[error("invalid header value: {0}")]
    Header(#[from] reqwest::header::InvalidHeaderValue),
    #[error("session expired (HTTP 401)")]
    Unauthorized,
    #[error("could not decode response")]
    Decode,
    #[error("API error: {0:?}")]
    Api(Vec<String>),
    #[error("crypto: {0}")]
    Crypto(#[from] crate::crypto::CryptoError),
}
