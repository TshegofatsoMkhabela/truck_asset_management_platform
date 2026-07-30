# TAMP Demo Guide — Swagger UI Click-Through

A non-developer can present the full 8-step journey (register → post load/truck → match →
accept → receipt → track → rate → admin view) using only the browser and Swagger UI —
no terminal, no curl.

**Open:** http://localhost:8080/swagger-ui/index.html

**Seeded demo accounts** (password for all three: `TampDemo2026!`):

| Role | Email | User ID |
|---|---|---|
| Freight Owner | `owner@tamp.example` | `00000000-0000-7000-8000-000000000001` |
| Transporter | `transporter@tamp.example` | `00000000-0000-7000-8000-000000000002` |
| Admin | `admin@tamp.example` | `00000000-0000-7000-8000-000000000003` |

Once you log in as one of these and paste the token into **Authorize**, every request below
picks up that identity automatically: `ownerId`, `transporterId`, `actorId`, `raterId`, and
`adminId` can all be left out of the example body entirely — just delete that line. This
works because every controller now overrides those fields with the logged-in caller's own id
whenever a real token is presented; you only need to type an id by hand for a request made
with no Authorize token active at all.

---

## Step 1 — Register

Expand `POST /auth/register`, click **Try it out**, use:

```json
{
  "fullName": "Demo Presenter",
  "email": "demo.presenter@tamp.example",
  "password": "DemoPass2026!",
  "role": "FREIGHT_OWNER"
}
```

Execute → **201**. This proves live registration works; the rest of the journey below
uses the seeded `owner@tamp.example` account instead, since it already has an eligible
load to match against.

## Step 2 — Login (get a token)

Expand `POST /auth/login`:

```json
{ "email": "owner@tamp.example", "password": "TampDemo2026!" }
```

Execute → **200**, body contains `token`. Copy the token value.

Click the **Authorize** button (top right, padlock icon) → paste `Bearer <token>` →
**Authorize** → **Close**. Re-authorize with a different login later in the flow to switch
identity (Swagger only holds one bearer token at a time).

## Step 3 — Post a load

`POST /loads`, body with `ownerId` deleted (the owner is whoever you logged in as in Step 2):

```json
{
  "originCity": "Cape Town",
  "destinationCity": "Bloemfontein",
  "cargoType": "REFRIGERATED",
  "weightKg": 8000,
  "volumeM3": 18,
  "pickupWindowStart": "2026-08-01T08:00:00Z",
  "pickupWindowEnd": "2026-08-03T08:00:00Z"
}
```

Execute → **201**, `ownerId` in the response matches the logged-in owner automatically. Note
the returned `id` as `<loadId>`.

Then re-authorize as `transporter@tamp.example` (repeat Step 2) and post a matching truck via
`POST /trucks`, `transporterId` deleted the same way:

```json
{
  "vehicleType": "REFRIGERATED",
  "capacityKg": 10000,
  "capacityM3": 25,
  "currentCity": "Cape Town",
  "availableFrom": "2026-07-31T00:00:00Z",
  "availableUntil": "2026-08-04T00:00:00Z"
}
```

## Step 4 — Get a match

Re-authorize as `owner@tamp.example`. Expand `POST /loads/{loadId}/matches`, `loadId` =
`<loadId>` from Step 3, body:

```json
{}
```

`requestedBy` isn't needed once logged in. Execute → **200**, body lists the eligible truck
with its score and reasons. Note the returned `matchId`.

## Step 5 — Accept the match

`POST /matches/{matchId}/decision`, `matchId` = `<matchId>` from Step 4, `actorId` deleted:

```json
{ "decision": "ACCEPTED" }
```

Execute → **200**.

## Step 6 — Fetch the receipt

Expand `GET /matches/{matchId}/receipt`, same `<matchId>`. Execute → **200**, shows a
generated `contractId` starting `TAMP-`.

## Step 7 — Track the trip

Expand `POST /matches/{matchId}/tracking`, same `<matchId>`:

```json
{ "status": "IN_TRANSIT", "latitude": -28.748, "longitude": 24.763 }
```

Execute → **201**. Then `GET /matches/{matchId}/tracking` → **200**, shows the event
list oldest-first.

## Step 8 — Rate the counterparty

Re-authorize as `transporter@tamp.example`. `POST /matches/{matchId}/ratings`, `raterId`
query param left blank (it's derived from the token), `rateeId` still filled in since it
names the *other* party, not the caller:

`rateeId=00000000-0000-7000-8000-000000000001`

```json
{ "score": 5, "comment": "Great service, on time." }
```

Execute → **201**.

## Step 9 — Admin view

Re-authorize as `admin@tamp.example`. `adminId` can be left blank on all three calls below,
it's derived from the token:

- `GET /admin/metrics` → **200**, platform counts.
- `GET /admin/users` → **200**, every user with compliance status.
- `GET /admin/disputes` → **200**, the seeded open dispute.
- `GET /admin/audit-logs` → **200**, the full audit trail, including the actions just performed above.

---

## Known gap to mention if asked

Login/JWT (`/auth/login`) is real and issues a genuine signed token, and every step above now
uses that token's identity in place of retyping ids by hand. But this is additive, not
enforcement: no business endpoint above *requires* a token, so an unauthenticated caller can
still supply `ownerId`/`actorId`/`adminId`/etc. directly and get through exactly as before.
Tracked in [`known-limitations.md`](known-limitations.md) as the largest open item from #9's
original scope: real role-based access control (rejecting requests with no valid token,
enforcing the token's role) was never wired in, only this convenience layer on top of it.
