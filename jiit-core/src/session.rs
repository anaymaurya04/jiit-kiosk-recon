use serde_json::json;

/// Everything the portal stores after login and re-sends on most calls.
///
/// Mirrors the web app's `localStorage` keys (see SPEC.md §5c). Keep this in one
/// place — the auth fields are sprinkled into nearly every request body.
#[derive(Clone, Debug, Default)]
pub struct Session {
    pub token: Option<String>,
    pub userid: Option<String>,
    pub memberid: Option<String>,
    pub membertype: Option<String>,
    pub instituteid: Option<String>,
    pub institute_name: Option<String>,
    pub enrollmentno: Option<String>,
    pub name: Option<String>,
    pub username: Option<String>,
    pub clientid: Option<String>,
    pub companyid: Option<String>,
    /// Timestamp string used by `/token/refreshTokenRequest`.
    pub token_date: Option<String>,
}

impl Session {
    /// Build a session from the login response fields.
    pub fn from_login(r: &crate::models::LoginResponse) -> Self {
        Self {
            token: Some(r.token.clone()),
            userid: Some(r.userid.clone()),
            memberid: Some(r.memberid.clone()),
            membertype: Some(r.membertype.clone()),
            // The SSO response puts the institute id in `value`.
            instituteid: Some(r.value.clone()),
            institute_name: Some(r.label.clone()),
            enrollmentno: Some(r.enrollmentno.clone()),
            name: Some(r.name.clone()),
            username: Some(r.username.clone()),
            clientid: Some(r.clientid.clone()),
            companyid: None,
            token_date: Some(chrono::Local::now().to_string()),
        }
    }

    pub fn is_authed(&self) -> bool {
        self.token.as_deref().is_some_and(|t| !t.is_empty())
    }

    /// The identity fields most feature endpoints expect alongside the token.
    ///
    /// Not every endpoint takes every field — merge this into the endpoint's own
    /// payload and drop what it doesn't use.
    pub fn auth_fields(&self) -> serde_json::Value {
        json!({
            "token": self.token,
            "memberid": self.memberid,
            "membertype": self.membertype,
            "instituteid": self.instituteid,
            "userid": self.userid,
        })
    }
}
