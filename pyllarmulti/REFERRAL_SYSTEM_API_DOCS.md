# Referral System — API & UI Documentation

*For iOS implementation reference. All endpoints require the standard Pyllar auth headers.*

---

## Standard Request Headers

Every authenticated API call must include:

```
Authorization: Bearer <jwt_token>
X-User-Id: <user_uuid>
X-App-Version: <app_version>
X-App-Name: pyllar
```

---

## Overview

The referral system lets existing users share a unique invite code. When a referred user completes their **first investment**, both parties earn a reward. The feature is gated per-user via `referralEnabled` — the card/screen is hidden until the backend enables it for that account.

**Attribution strategy:**
- Primary: AppsFlyer OneLink — the share URL embeds the referral code in `af_sub1`
- Fallback: Play Store Install Referrer URL param `referral_code`
- First-write-wins: once a user's referral attribution is set it is never overwritten
- Post-investment block: attribution is rejected if the user has already invested

---

## API Endpoints

### 1. Dashboard — includes `referralEnabled` flag

```
GET /api/dashboardv2/{userId}
```

Use this to decide whether to show the "Refer & Earn" card on the home/dashboard screen.

**Response (relevant fields):**
```json
{
  "data": {
    "userName": "Priya",
    "portfolioSummary": { ... },
    "kycDetails": { ... },
    "currentInvestments": [ ... ],
    "kycStatus": "COMPLETE",
    "showAll": false,
    "referralEnabled": true
  }
}
```

| Field | Type | Description |
|---|---|---|
| `referralEnabled` | `Boolean` | Whether to show the Refer & Earn card and referral screen for this user. Default `false`. |

---

### 2. Profile — includes `referralEnabled` and `referredByCode`

```
POST /api/profile/details
Content-Type: application/json

{
  "userId": "<user_uuid>"
}
```

**Response (relevant fields):**
```json
{
  "data": {
    "name": "Priya Sharma",
    "email": "priya@example.com",
    "phoneNumber": "+91XXXXXXXXXX",
    "dob": "1990-01-15",
    "gender": "F",
    "referralEnabled": true,
    "referredByCode": "PYLLAR4A"
  }
}
```

| Field | Type | Description |
|---|---|---|
| `referralEnabled` | `Boolean` | Same flag as in the dashboard response. |
| `referredByCode` | `String?` | The referral code of the person who referred this user. `null` if organic (not referred). Show a "Joined via invite" tile on the profile screen when non-null. |

---

### 3. My Referral Code + Share URL

```
GET /api/referral/my-code
X-User-Id: <user_uuid>
```

Lightweight endpoint — returns everything needed to render the referral code chip and populate all share actions. Call this when the referral screen opens.

**Response:**
```json
{
  "data": {
    "referralCode": "PYLLAR4A",
    "shareUrl": "https://pyllar.onelink.me/JV5P/4obn7t7z?pid=referral&campaign=DEFAULT&af_sub1=PYLLAR4A",
    "shareMessage": "Join me on Pyllar and start your investment journey! Use my code PYLLAR4A and we both earn rewards when you make your first investment. https://pyllar.onelink.me/...",
    "referralEnabled": true
  }
}
```

| Field | Type | Description |
|---|---|---|
| `referralCode` | `String` | User's unique 8-character invite code. |
| `shareUrl` | `String` | Ready-to-use AppsFlyer OneLink URL. Pass this directly to the iOS share sheet and WhatsApp. Do not construct URLs client-side. |
| `shareMessage` | `String` | Pre-composed share text including the OneLink URL. Use as the share sheet body text. |
| `referralEnabled` | `Boolean` | If `false`, show a "coming soon" placeholder instead of the referral UI. |

**Note:** `shareUrl` falls back to `https://pyllar.in/refer?code=PYLLAR4A` when the OneLink base URL is not configured.

---

### 4. My Referral Stats

```
GET /api/referral/my-stats
X-User-Id: <user_uuid>
```

Heavier endpoint — returns reward totals and referral counts. Call in parallel with `/my-code` so the stats section can load independently.

**Response:**
```json
{
  "data": {
    "totalReferrals": 5,
    "convertedReferrals": 2,
    "pendingRewardPaise": 10000,
    "creditedRewardPaise": 5000
  }
}
```

| Field | Type | Description |
|---|---|---|
| `totalReferrals` | `Long` | Total users who signed up using this user's code (regardless of investment status). |
| `convertedReferrals` | `Long` | Users who signed up AND completed their first investment (reward-qualifying). |
| `pendingRewardPaise` | `Long` | Rewards earned but not yet credited to the user, in paise (₹1 = 100 paise). |
| `creditedRewardPaise` | `Long` | Total rewards already credited, in paise. |

---

### 5. Signup — referral attribution

```
POST /api/auth/register  (or equivalent signup endpoint)
```

Include these optional fields in the signup request body when a referral code was captured (via AppsFlyer `af_sub1` or Install Referrer `referral_code` param):

```json
{
  "phoneNumber": "+91XXXXXXXXXX",
  "fullName": "...",
  "referralCode": "PYLLAR4A",
  "campaignCode": "DEFAULT"
}
```

| Field | Type | Description |
|---|---|---|
| `referralCode` | `String?` | The referrer's invite code. From AF attribution (`af_sub1` param) or Install Referrer `referral_code` URL param. |
| `campaignCode` | `String?` | Campaign slug from AF `campaign` param. Determines reward amounts. Defaults to `"DEFAULT"` if omitted or unknown. |

**Attribution rules (enforced server-side):**
- If `referralCode` doesn't exist in the system → silently ignored
- If the new user already has a `referredByCode` set → silently ignored (first-write-wins)
- If the new user has already made a successful investment or has an approved mandate → silently ignored (no retroactive attribution)

---

## Deep Links

The referral screen supports direct deep link navigation.

| Scheme | URL | Behaviour |
|---|---|---|
| HTTPS | `https://pyllar.in/refer` | Opens referral screen for existing users |
| Custom | `pyllar://refer` | Same as above |
| AppsFlyer OneLink | `https://pyllar.onelink.me/JV5P/4obn7t7z?pid=referral&campaign=X&af_sub1=CODE` | New users: Install + attribute. Existing users: opens referral screen. |

The referral code is embedded in the OneLink as `af_sub1`. AppsFlyer fires the conversion callback on first app open after install — extract `af_sub1` from the attribution data and send it in the signup request.

---

## UI Screens

### A. Dashboard / Home — Refer & Earn Card

**Show condition:** `dashboardResponse.referralEnabled == true`

**Card content:**
- Label: "REFER & EARN"
- Headline: "Invite friends, earn ₹100 each" (reward amount subject to active campaign)
- CTA button: "Invite" → navigates to referral screen

**Hide condition:** `referralEnabled == false` — card is not shown at all (no placeholder).

---

### B. Referral Screen

**Entry points:**
1. Dashboard Refer & Earn card tap
2. Deep link: `pyllar://refer` or `https://pyllar.in/refer`
3. Server-driven navigation after first investment (server returns `nextScreen: "referral"`)

**Load behaviour:** Call `/api/referral/my-code` and `/api/referral/my-stats` in parallel.

**If `referralEnabled == false`** (e.g. user navigated directly): Show a "Referrals coming soon — not available for your account yet" placeholder. Do not show referral UI.

**Screen layout:**

```
┌─────────────────────────────┐
│  ← Referral         How it  │
│                     works?  │
├─────────────────────────────┤
│                             │
│  [Wallet card]              │
│  Balance: ₹100 pending      │
│  Lifetime earned: ₹150      │
│                             │
├─────────────────────────────┤
│  YOUR INVITE CODE           │
│  Share your unique code and │
│  both earn when friend      │
│  invests for the first time │
│                             │
│  ┌─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─┐  │
│  │ YOUR CODE            │  │
│  │ PYLLAR-PYLLAR4A  [Copy]│ │
│  └─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─┘  │
│                             │
│  [WhatsApp]    [Share ↗]   │
│                             │
├─────────────────────────────┤
│  MY REFERRALS               │
│  5 invited · 2 invested     │
│  (list of referred users)   │
└─────────────────────────────┘
```

**Copy button:** Copies `shareUrl` (the OneLink URL) to clipboard.

**WhatsApp button:** Opens `https://api.whatsapp.com/send?text=<URL-encoded shareMessage>`.

**Share button:** Opens native iOS share sheet with `shareMessage` as the body text.

**Important:** Use `shareUrl` and `shareMessage` exactly as returned by the backend. Do not construct URLs client-side. This ensures the OneLink URL is used when configured, and any future URL changes (e.g. switching link provider) require zero iOS app changes.

---

### C. Profile Screen — "Joined via invite" tile

**Show condition:** `profileResponse.referredByCode` is non-null and non-empty.

**Placement:** Between "Personal Details" and "Manage Account" sections.

**Tile content:**
- Icon: person/invite icon
- Title: "Joined via invite"
- Subtitle: "You were referred by a friend"
- Not tappable (informational only)
- Do **not** display the raw `referredByCode` value (privacy)

---

## Reward Rules

| Rule | Detail |
|---|---|
| Reward trigger | Only on referred user's **first completed investment** — not on signup |
| Both parties rewarded | Referrer earns `referrerRewardPaise`, referred user earns `refereeRewardPaise` |
| Default reward | ₹50 each (5000 paise) — determined by active campaign |
| Campaign-based | Different campaigns can have different reward amounts and validity windows |
| Idempotent | Reward rows are only created once per referred user (DB unique constraint) |
| Campaign expiry | If campaign expired before the referred user invested, no reward is issued |
| Status flow | `PENDING` → `CREDITED` (manual process, not in-app) |

---

## Paise Conversion

All monetary values are returned in **paise** (1/100 of a rupee).

```
₹1 = 100 paise
₹50 = 5000 paise
₹100 = 10000 paise
```

Display formula: `amountInRupees = paise / 100`

---

## Error Handling

All endpoints return the standard Pyllar response envelope:

```json
{
  "status": "success" | "error",
  "message": "...",
  "data": { ... },
  "navigation": {
    "nextScreen": "referral" | "home" | ...
  }
}
```

If `referralEnabled` is `false` when `/api/referral/my-code` is called (user navigated directly without the flag being set), the endpoint still returns a valid referral code and URLs — the iOS app should check `referralEnabled` and show the placeholder instead of the full referral UI.
