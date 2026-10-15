use serde::Deserialize;

/// Every response from the portal uses this envelope.
///
/// `response` is an object on most endpoints but an **array** on some (notably
/// `getmenulist`), so callers pick `T` accordingly — e.g. `serde_json::Value`
/// for exploration or `Vec<MenuNode>` for the menu.
#[derive(Debug, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ApiEnvelope<T> {
    pub status: Status,
    pub response: T,
}

#[derive(Debug, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct Status {
    pub response_status: String,
    #[serde(default)]
    pub errors: Option<Vec<String>>,
    #[serde(default)]
    pub identifier: Option<String>,
}

impl Status {
    pub fn is_success(&self) -> bool {
        self.response_status == "Success"
    }
}

/// Shape returned by the Google Sign-In exchange and the password login.
///
/// Only the fields observed in live traffic are declared; unknown fields are
/// ignored by serde, so this stays valid if the server adds more.
#[derive(Debug, Clone, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct LoginResponse {
    pub clientid: String,
    /// Capital `Username` on the wire.
    #[serde(rename = "Username")]
    pub username: String,
    pub name: String,
    pub usertype: String,
    pub membertype: String,
    pub enrollmentno: String,
    pub label: String,
    pub userid: String,
    /// Institute id (despite the generic name).
    pub value: String,
    pub memberid: String,
    pub token: String,
}

/// One node of the navigation tree from `/clxuser/getmenulist`.
///
/// Render the app menu from this rather than hardcoding — the university can
/// change it server-side per student or role.
#[derive(Debug, Clone, Deserialize)]
pub struct MenuNode {
    pub id: String,
    pub title: String,
    #[serde(rename = "type")]
    pub kind: String,
    #[serde(default)]
    pub icon: String,
    #[serde(default)]
    pub url: Option<String>,
    #[serde(default)]
    pub children: Vec<MenuNode>,
}

impl MenuNode {
    /// Leaf items ("item" nodes carry a `url`).
    pub fn is_page(&self) -> bool {
        self.kind == "item" && self.url.is_some()
    }
}
