# Airemore API Add-on (for the Kotlin field app)

This `api/` folder is a **pure add-on** — copy it into your existing
`airemore_system/` folder (same level as `config.php`). Nothing in your
current site is modified or deleted.

## 1. Install

1. Copy this `api/` folder into your live `airemore_system/` directory, so
   you end up with `airemore_system/api/...` next to `airemore_system/config.php`.
2. In phpMyAdmin, run `api/migration_api.sql` against your live database
   (adds `api_tokens` table + `client_uuid` columns; nothing existing is
   touched).
3. **Important — verify table names first.** Your live database has
   evolved past `schema.sql`: PM tables are lowercase (`pm_forms`,
   `pm_units`, …) but Repair and Installation tables are capitalized
   (`Repair_forms`, `Repair_units`, `Repair_photos`, `Repair_personnel`,
   `Installation_forms`, `Installation_personnel`, `Installation_units`).
   That's what `pm_save.php` / `repair_save.php` / `installation_save.php`
   assume, based on your current `*/save.php` files. If your DB differs
   (e.g. `lower_case_table_names` is set differently on your host, or
   you've renamed something since), open each `*_save.php` and fix the
   table names — the SQL is otherwise complete and ready to run.
4. Test the deploy: visit `https://your-domain.com/airemore_system/api/lookups.php`
   in a browser — you should get `{"success":false,"message":"Missing or
   malformed Authorization header."}` (that's correct — it means the file
   is reachable and PHP is running).

## 2. Try a login from the command line

```bash
curl -X POST https://your-domain.com/airemore_system/api/auth_login.php \
  -H "Content-Type: application/json" \
  -d '{"username":"staff","password":"password123","device_label":"test"}'
```

You should get back `{"success":true,"token":"…","user":{…}}`. Use that
token for every other call:

```bash
curl https://your-domain.com/airemore_system/api/lookups.php \
  -H "Authorization: Bearer PASTE_TOKEN_HERE"
```

## 3. Endpoints

| Endpoint | Method | Purpose |
|---|---|---|
| `auth_login.php` | POST | `{username, password, device_label}` → token |
| `auth_logout.php` | POST | Revokes the current token |
| `lookups.php` | GET | Companies, brand/type/capacity lists, both checklists, finding-option pick-lists — cache this in the app on login |
| `companies_add.php` | POST | Quick-add a company (idempotent by name) |
| `pm_save.php` | POST multipart | Create a PM record (idempotent via `client_uuid`) |
| `pm_list.php` | GET | Staff's own PM records (admin sees all) |
| `repair_save.php` | POST multipart | Create a Repair/Checkup record |
| `repair_list.php` | GET | Own Repair records |
| `installation_save.php` | POST multipart | Create an Installation record |
| `installation_list.php` | GET | Own Installation records |

## 4. Deliberately out of scope (v1)

To ship something staff can actually use this week instead of chasing
100% field parity, the API/app **do not** cover these admin-configured
extras — finish these on the web dashboard if a specific job needs them:

- SM Store PM checklist variant (`pm/sm_store_form.php`)
- Certification of Accomplishment (COA) blocks on PM/Repair/Installation
- Installation "Start-Up Certificates" sub-form (28 fields per unit)
- Free-form "particulars" rows on PM/Repair
- Editing a record after it's synced (app is create-only; corrections
  happen on the web, same as admin already does today)

Everything staff fill out **in the field** day-to-day — company, date,
personnel, AC units with before/after readings, the 17-item PM checklist
(or 14-item Repair one), before/after photos, findings/A.F.I./
recommendation/action-taken/A.L.I., and the customer signature — is fully
covered.

## 5. Security notes

- Tokens never expire automatically (matches your existing 30-day web
  session philosophy) but can be revoked any time by deleting the row in
  `api_tokens` or wiring up an admin "Revoke device" button later.
- All endpoints use the same PDO prepared-statement pattern as your
  existing code — no raw string concatenation into SQL anywhere.
- CORS is wide open (`Access-Control-Allow-Origin: *`) since only the
  Android app calls this API with a bearer token, not a browser session —
  tighten it if you ever add a web-based API consumer.
