# JIIT Web Kiosk (CampusLynx) — Reverse-Engineered API Spec

Target: `https://webportal.jiit.ac.in/jiitwebkiosk/`
Recon date: 2026-10-15. Verified against live authenticated traffic.

> This document contains **no personal data**. All values from a real session are redacted.

---

## 1. Architecture

The kiosk URL is a **launcher only** (Angular, minified `main.js`). It routes students to a
completely separate app, **CampusLynx**, which is the real product.

| Piece | URL | Notes |
|---|---|---|
| Launcher | `https://webportal.jiit.ac.in/jiitwebkiosk/` | Just two buttons: Student/Parent, Employee. Ignore it. |
| **Student app** | `https://webportal.jiit.ac.in:6011/studentportal/` | Angular + PrimeNG + Bootstrap. This is what you clone. |
| **Student API** | `https://webportal.jiit.ac.in:6011/StudentPortalAPI` | All data calls. Same origin as app. |
| **SSO API** | `https://webportal.jiit.ac.in:6013/CLXSSOAPI` | Google Sign-In token exchange only. |
| Employee app | `http://webportal.jiit.ac.in:6010/employeeportal` | Out of scope (staff). |

Certs are an **internal CA** — a normal browser/Rust client must either trust the JIIT root CA
or disable verification. Chrome rejected it (`ERR_CERT_AUTHORITY_INVALID`) until it was accepted.
Your Rust client needs `danger_accept_invalid_certs(true)` (or ship the JIIT root CA).

Server: `WildFly/10` / Undertow. `x-application-context: CampusLynxPortal:JIITPROD:6011`.

---

## 2. Transport & envelope

### Requests
`POST <apiBase><path>` with `Content-Type: application/json`.
`apiBase` = `/StudentPortalAPI` (baked into the bundle as `environment.apiBase`).

**Headers on every authenticated call:**
```
Content-Type: application/json
Authorization: Bearer <JWT>          # the token from login
LocalName:  <AES blob, base64>       # see §4 — anti-tamper nonce, MUST be reproduced
```

**Body is an AES blob, not plain JSON** (see §3/§4). One class of endpoint
(`feedbackformresponse*`) skips encryption and sends plain JSON.

### Response
Every response uses the same envelope:
```json
{
  "status": { "responseStatus": "Success", "errors": null, "identifier": null },
  "response": { ... }
}
```
- Success check: `status.responseStatus == "Success"`.
- Errors arrive as `status.errors[]` (array of strings) OR `error.status.errors[0]` on failure.
- Some responses are `response: { ... }` (object), some are `response: [ ... ]` (array,
  e.g. `getmenulist`). Handle both.

---

## 3. Crypto: request body encryption (AES-256-CBC)

Every non-trivial POST body is encrypted client-side with a **date-derived key**.

```
key   = "qa8y" + datePart + "ty1pn"     # 16 bytes → AES-128 in practice, written as 256
iv    = "dcek9wb8frty1pnm"              # 16 bytes, CONSTANT
mode  = CBC
pad   = PKCS7
out   = Base64( AES( UTF8(jsonString) ) )
```

`datePart` is built from the **device-local date** (7 chars):
```
d  = day-of-month  zero-padded to 2     e.g. "15"
m  = month         zero-padded to 2     e.g. "10"
y  = last 2 digits of year              e.g. "26"
w  = weekday number, JS getDay()        0=Sun .. 6=Sat   e.g. 4

datePart = d[0] + m[0] + y[0] + str(w) + d[1] + m[1] + y[1]
         = "1" + "1" + "2" +  "4"  + "5" + "0" + "6"   → "1124506"
```
So for 2026-10-15 (a Thursday): `key = "qa8y1124506ty1pn"`, `iv = "dcek9wb8frty1pnm"`.

**Verified:** decrypting the captured `getmenulist` body with this key yields
`{"userid":"...","instituteid":"...","membertype":"S"}`. Wrong weekday → PKCS7 padding failure.

> ⚠️ Key changes at **local midnight**. A token/body encrypted yesterday fails today. The server
> presumably computes the same key from the server date, so client and server dates must agree
> (both in IST). In Rust, use the device local date exactly as above.

---

## 4. Crypto: `LocalName` header (per-request nonce)

The Angular HTTP interceptor runs on every request:

1. Pick 9 random chars from alphabet
   `0123456789ABCDEFGHIJKLMNOPQRSTUVWXTZabcdefghiklmnopqrstuvwxyz`
   (note: no `s`, no `u` — copy it exactly).
2. Split: `head` = first 4 chars, `tail` = last 5 chars.
3. Build the same `datePart` as §3.
4. `LocalName = Base64( AES_CBC_PKCS7( key, iv, head + datePart + tail ) )`

The plaintext is always **16 chars** (4 + 7 + 5), so the ciphertext is always 32 bytes → 44-char
base64 ending in `=`. Example observed: plaintext `VZnv1124506eD230` → header
`QKM0Nb0fYVJBmR+qDGy+PMOJ731xjUxEE/DXq+W1wyk=`.

**Your Rust client must generate a fresh `LocalName` for every request** using the current date.
Reuse across midnight will break.

> The server likely validates that the nonce decrypts with the current date's key — that's the
> anti-tamper/"request freshness" mechanism. It is NOT a secret; just replicate it.

---

## 5. Auth flows

### 5a. Google Sign-In (what was used in this session) — easiest to replicate
1. App loads Google Identity Services button (client id
   `395311773821-85mh9upf35k1q4nc1rmhncmnhmc8v2ed.apps.googleusercontent.com`).
2. On success, POST to SSO API:
   `https://webportal.jiit.ac.in:6013/CLXSSOAPI/token/generate-token-google-signin`
   ```json
   { "googleToken": "<Google ID token JWT>", "modulename": "studentportal" }
   ```
   Header: `authorization: Bearer` (literal), `origin: https://webportal.jiit.ac.in:6011`.
3. Response gives the session token + identity. **Server also sets an `SJWT` HttpOnly cookie**
   (30 min) — carry cookies if you hit the SSO host.

Response shape:
```json
{ "status": {...}, "response": {
  "clientid": "JIIT", "Username": "...", "name": "...", "usertype": "S", "membertype": "S",
  "enrollmentno": "...", "label": "Jaypee Institute of Information Technology Sec-128",
  "userid": "USID...", "value": "11IN1902J000003", "memberid": "N1ST...",
  "token": "<JWT>"
}}
```
The returned `token` is the JWT used as `Authorization: Bearer` afterwards.
Note `value` here is the **instituteid**.

### 5b. Password login (two-step, encrypted)
1. `GET  /token/getcaptcha` → `{ response: { captcha: { image: "<base64 jpeg>", ... } } }`
   Display the image; user solves it. (Bundle also embeds a reCAPTCHA site key
   `6LfUnEQeAAAAANF6_fhGFqzntLqlOaHcV1ABPDgO` — a Google reCAPTCHA v3 key.)
2. Build JSON, **AES-encrypt it** (§3), POST to `/token/pretoken-check`:
   ```json
   { "username": "...", "usertype": "S"|"P", "captcha": { ... } }
   ```
   (Parent logins prefix username with `P`.) Response contains `random`, `otppwd`.
3. If `otppwd == "PWD"` → password path; if `"otp"` → OTP path.
4. Password path: AES-encrypt `{username, usertype, password, ...}`, POST
   `/token/generatewebtoken` → returns `response.regdata` with `token`, `clientid`,
   `companyid`, `enrollmentno`, `membertype`, `userid`, `name`, `institutelist[]`,
   `expiredpassword`. If multiple institutes, user picks one and you store its `value`.

### 5c. Session bootstrap (after any login)
Store these keys (the web app reads them for every call — your Rust app needs the same model):
`Token`, `clientid`, `userid`, `name`, `membertype`, `enrollmentno`, `Username`,
`instituteid`, `institutename`, `memberid`, `companyid`, `tokendate`, `Today_DATE`.

Then immediately call, in order:
1. `/clxuser/getmenulist` → full nav tree (§6)
2. `/feedbackformcontroller/getCreadits` → body `{"enrollmentNo":"<enroll>"}` → dashboard header
3. `/studentbankdetails/getstudentbankinfo` → bank + address + contact (heavily PII)
4. `/feedbackformcontroller/getDataForFeedback`
5. `/onlinefeepayment/getmyactivefeeevents` → fee status + `studentInfo`

### 5d. Token expiry
On HTTP **401**, the app POSTs `/token/refreshTokenRequest` with
`{username, tokendate}`. JWT lifetime observed: 2 hours (7200s). Handle refresh or re-login.

---

## 6. Navigation model (`/clxuser/getmenulist`)

Request body (encrypted): `{token, memberid, membertype, instituteid}`.
Response: `response[0].children` — a group containing "collapse" sections, each with "item" leaves.

Menu id/title → route url (all under `student/`):

- **Feed Back** → `FeedBackFormComponent`
- **Student Information** → `studentsPersonalInfo`, `medicalformforstudent`,
  `studentdocumentupload`, `studentPhotoUpdate`, `updateapaarid`, `phdattendance`
- **Academic Detail** → `servicerequest`, `collectionofpendingpayments`, `feepayslip`,
  `noduesstatus`, `studentfeeledger`, `feedemandreport`, `onlinefeepayments`,
  `hostelallocationdetails`
- **Registration** → `studentcourseregistration`, `subjectadddroprequest`,
  `subjectadddropstatus`, `supplimentarysubjectregistrationmodify`,
  `summersubjectregistrationmodify`, `gipsubjectregistration`, `studentsubjectchoiceprint`,
  `registrationslip`, `supplimentarysubjectchoiceprint`, `studentminorstreamselection`,
  `studentminorstream`, `minorsubjectregistrationdetail`
- **Class and Attendance** → `myclassattendance`, `studentregisteredfaculty`
- **Examination Detail** → `myexaminationschedule`, `eventsubjectmarks`, `studentgradecard`,
  `studentresult`, `provisioncertificateacceptance`, `minorsubjecttranscriptprint`
- **MOOC Subjects** → `moocsubjectpreference`, `moocsubjectstatus`
- **Certification Course** → `certcourse`, `certsubjectregistration`,
  `certsubjectregistrationdetail`, `certsubjecttranscriptprint`

**Build the app menu from this API response, not hardcoded** — the university can change it
server-side. That's why the web app looks different for different students/roles.

---

## 7. Endpoint catalogue (by feature)

Base = `/StudentPortalAPI`. All POST bodies AES-encrypted (§3) unless noted.
Standard auth payload sprinkled into bodies: `token`, `memberid`, `membertype`,
`instituteid`, `userid` (read from session, §5c).

### Dashboard / home
- `/feedbackformcontroller/getCreadits` — plain JSON `{"enrollmentNo":"..."}`
- `/feedbackformcontroller/getDataForFeedback`
- `/onlinefeepayment/getmyactivefeeevents`
- `/studentbankdetails/getstudentbankinfo`
- `/token/marqeelist` (GET) — news ticker text
- `/token/getlogoClientWise` (GET) → `"JIIT" | "JUIT" | "JUET"` picks branded background
- `/token/productversion` (GET)

### Attendance
- `/StudentClassAttendance/getstudentInforegistrationforattendence`
- `/StudentClassAttendance/getstudentattendancedetail`
- `/StudentClassAttendance/getstudentsubjectpersentage`
- `/StudentClassAttendance/getpreviousstudentsubjectpersentage`

### Exams & results
- `/studentgradecard/getregistrationList`, `/studentgradecard/getstudentinfo`
- `/studentgradecard/showstudentgradecard`
- `/studentgradecard/printgradecard/<10 path params>` (GET → PDF blob)
- `/studentsexamview/getstudent-exammarks`, `printstudent-exammarks`, `printstudent-exammarksJIIT`
- `/studentsttattview/getstudent-examschedule`
- `/studentcommonsontroller/getstudentexamevents`, `getsemestercode-exammarks`

### Fees & payments (payment gateways: Razorpay, Paynimo/ICICI, Cashfree, PayU)
- `/onlinefeepayment/loadmyfeedetail`, `studentfeedetail`, `paymentmode`, `paynow`,
  `icicipaymentgateway`
- `/studentfeeledger/loadfeesummary`, `loadfeeheadwisedetails`, `fetchfeetransaction`,
  `getfeewisedetaildata`
- `/feepayslipcontroller/getdueamountdetails`, `getreport`, `getsemestercodelist`
- `/feereceipt/getreport`
- `/collectionpendingpayments/getpendingpaymentsdata`, `paynow`, `paymentmode`,
  `getfeereceiptreport`
- `/noduesstatus/getfeestatus`, `getactivites`, `showform`, `selfapproval`
- `/feedemandreportcontroller/generatereportforpdf`
- `/payment/getpaymentlinkforpayu`, `getpaymentlinkforNonAcademicspayu`, `emi-plans`
- `/commonpaymentgateway/cashfreegateway`, `cashfreegatewayNonAcademic`
- `/rtgsneftentrycontroller/loadGrid`, `saveRequest`, `getBankName`, `getFeePayemtFor`

### Course registration / add-drop
- `/courseregistration/getstudentlov-courseregistration`
- `/courseregistrationjiit/getcourseregistrationinstructions`,
  `getstudentlovValidate-courseregistrationjiit1`, `savechoicesubjects-courseregistration`,
  `lockandfreeze-courseregistration`, `printstudentcourseregistrationreport/`
- `/addDropSubjectRequest/adddropsubjectSubjectList`, `gridData`, `saveAddSubjectRequest`,
  `beforsaveAddSubjectRequest`, `beforFrrezeAddSubjectRequest`, `frrezedAddSubjectRequest`,
  `deleteAdddropSubjectChoice`, `getStudentInfo`
- `/adddropsubjectstatus/getsemestercodelist`, `getsubjectstatus`
- `/supplimentoryregistrationmodify/*`, `/summerregistrationmodify/*`, `/gipregistration/*`
- `/studentchoiceprint/getsubjectpreference`, `getsemestercodelist`, `printsubjectpreference/`
- `/registrationslip/getStudentInfo`, `getStudentRegistrationInfo`, `studentRegistrationSlip`

### Minor / MOOC / certification
- `/studentminorstream/getminoregdetail`, `getsubjects`, `saveminorstreamrequest`,
  `saveminorsubjectregirequest`, `lockminorsubjectregirequest`,
  `getstudentlov-supplimentoryregistration`
- `/moocsubjectpreference/*` (getgriddata, getMoocSubjectList, getAgencyList, freezeSubjects...)
- `/moocsubjectstatus/getsemestercodelist`, `getsubjectstatus`
- `/studentcertificationcourseregistration/*`

### Personal / documents / services
- `/studentpersinfo/getstudent-personalinformation`, `updatestudent-photo`,
  `checkphotouploadevent`
- `/studentdocumentupload/save`, `getstudentdocumentinfo`, `getstudentdocumentpdf`,
  `getstudentdocumentphoto`, `freezestudentdocumentinfo`, `geteventswithdatecheck`
- `/studentbankdetails/getstudentbankinfo`, `savestudentbankinfo`
- `/studentinformation/getstudentmedicalinfo`, `savemedicalinfobystudent`
- `/apaarconfirmationcontroller/getStudentApaarIdData`, `saveApaarId`
- `/servicerequestbystudent/*` (savesevicerequestbystudent, getpendingservicerequestgrid,
  getapprovedrequestgrid, getservicecodelov, getservicecharge...)
- `/studentrequestcontroller/getRequestNo`, `fetchdetail`, `save`
- `/requestforapprovalcontroller/*`, `/requestforconferenceseminarcontroller/*`

### User / account
- `/clxuser/changepassword`, `getsecretquestionanswer`, `savesecretquestionanswer`
- `/student/changepassword`, `/student/secretquestion`
- `/token/studentpasswordrecover`, `/userdetailsrecoverypassword`, `/recoverypassword`
- `/clxuser/getmenulist`
- `/token/refreshTokenRequest`

### Certificates / PHD / hostel / leave
- `/provisioncertificateacceptancecontroller/*`
- `/phdsubjectregistration/*`, `/phdsubjectregistrationReport/*`, `/phdstudentcontroller/save`
- `/myhostelallocationdetail/gethostelallocationdetail`
- `/leaveentryscholarshipcontroller/*`, `/scholareavestatuscontroller/*`
- `/publicconnector/*` (getleavebalance, getleaveentrygrid, getledgereportdata...)
- `/myinvigilationdutyschedule/*`, `/nodutyrequest/*`, `/teachingloadentrycontroller/*`
- `/workflowapproval/downloadpdf`
- `/transferfeereceipt/getreportfortransferin`, `gettransferindatainlist`
- `/feedbackformresponsebymailing/*` (the one set that uses **plain JSON**, not AES)

---

## 8. Payments — important caveat

Fee payment redirects to third-party gateways (Razorpay `checkout.razorpay.com`,
Paynimo `paynimo.com`, Cashfree, PayU). The portal calls
`/onlinefeepayment/icicipaymentgateway` etc. to get a payment URL, then hands off.

**Recommendation for the Rust app:** do NOT reimplement the gateways. Open the returned
payment URL in the system browser (Android Custom Tab / `Intent.ACTION_VIEW`), or fall back to
opening the original website's fee page. Reimplementing PCI-scoped checkout is out of scope and
risky. Read-only views (attendance, marks, fees, ledger) are safe to fully replicate.

---

## 9. Rust Android implementation notes

- **HTTP**: `reqwest` with `danger_accept_invalid_certs(true)` (internal CA) — or bundle the JIIT
  root CA PEM for a safer build.
- **Crypto**: `aes` + `cbc` + `pkcs7` (RustCrypto) + `base64`. One `encrypt_body(json) -> String`
  helper and one `local_name_header() -> String`, both taking the current local date. This is the
  whole crypto surface — ~40 lines.
- **Date**: must be **IST / device local**, matching §3. Test with the vectors in §3/§4.
- **Model**: mirror the `status/response` envelope with serde
  (`#[serde(rename_all = "camelCase")]`, and `response` as `serde_json::Value` since it's
  object-or-array depending on endpoint).
- **Session**: store `Token` + identity fields; expose a `refresh()` on 401.
- **Menu**: render from `/clxuser/getmenulist`, don't hardcode.
- **JWT** is HS256 with an unknown server secret — treat as opaque, never parse/forge.

### Suggested crate layout
```
jiit-core/     # no Android deps — crypto, api client, models, session  (unit-testable)
jiit-android/  # UniFFI bindings + Compose UI  OR  a Rust-native UI shell
```
Put everything in `jiit-core` so the crypto/API logic is testable on the host without an emulator.

### Test vectors (from live capture, 2026-10-15, weekday=4)
- key `qa8y1124506ty1pn`, iv `dcek9wb8frty1pnm`
- decrypt body `H7XoAW/M41ZQe1CnVxK3qPcohL3GC1WwDLM8f3ygAOuqK4GsFuDdXdS4FG1IkCYF1I8G7pnQlrqwx3Nc9SEn0bgQK0map+b0/5q720TnY24=`
  → `{"userid":"USID2507A0008049","instituteid":"11IN1902J000003","membertype":"S"}`
- decrypt LocalName `QKM0Nb0fYVJBmR+qDGy+PMOJ731xjUxEE/DXq+W1wyk=` → `VZnv1124506eD230`

---

## 10. Security / ethics notes for the app

- Credentials and JWTs are high-value; store in Android **EncryptedSharedPreferences** or the
  Keystore, never plaintext SharedPreferences.
- The AES key/IV are **public in the JS bundle** — this "encryption" is obfuscation, not real
  transport security. TLS is the actual protection. Don't oversell it.
- Handle the PII endpoints (bank account, Aadhaar, address, medical) with care: don't log, don't
  cache to backup.
- Respect the university's ToS. This spec is for a personal client; don't redistribute the app
  with embedded credentials or scrape at high rate.
