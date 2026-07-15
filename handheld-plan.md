# Connect HandheldPi to the WMS over WiFi — integration plan

Status (2026-07-15): **steps 1–8 implemented and verified.** WMS gate green
with the new fixture (33/33 IT, `mvnw verify`); HandheldPi suite green (100
tests); Stage 2 loopback e2e passed 44/44 checks with clean SQL
reconciliation — see
`docs/evidence/2026-07-15-hht-loopback-integration.md` and the new pass in
`docs/executed-test-report.md`. Changes are uncommitted working trees in both
repositories pending owner review.

**Remaining: step 9 (Stage 3, owner-executed — needs the physical GamePi20
and the LAN):**

1. On the WMS host: start the dev instance, confirm local health, then apply
   the scoped firewall rule per `docs/runbook-linux.md` §3 (or the Windows
   §3); PostgreSQL stays loopback-only.
2. Print `LOC:`/`ART:` labels from the WMS label endpoints and an operator
   badge with `HandheldPi/scripts/make_badge.py picker02`.
3. On HHT-001: set `/etc/hht/hht.toml` → `[device] id = "HHT-PI-01"`,
   `[wms] backend = "http"`, `base_url = "http://<wms-host-ip>:8080"`;
   restart `hht.service`; `curl http://<wms-host-ip>:8080/actuator/health`
   from the Pi first (runbook §4 — note the 2.4 GHz-only radio).
4. Run: full happy path, one offline pick (walk out of WiFi range), one
   replay-rejection (admin blocks the task from the dashboard mid-offline),
   badge login. Capture PNG screens + `var/hht.jsonl`; record evidence in
   both repos (WMS: new evidence file closing the runbook §3–4 gap;
   HandheldPi: TEST_REPORT from the template).

Scope spans this repository (one dev-fixture migration + evidence) and the
sibling `../HandheldPi` repository (client adaptation, the bulk of the work).

## Context

The warehouse-management PoC (Spring Boot, `/api/v1` REST) treats the HHT as "a separate LAN REST client" — but no real client has ever connected; the contract is exercised only by integration tests, and the runbooks' LAN firewall steps are documented but never evidenced. The sibling repo `../HandheldPi` (Pi Zero 2 W + GamePi20 + Camera Module 3, Python 3.11) is a substantially implemented handheld picking terminal whose own `PLAN.md` names this exact step as its open frontier: "Phase 3 against the real Spring Boot API."

The blocker: the Pi was coded against a **placeholder contract** (`../HandheldPi/API.md`, marked "ASSUMED — v0") that diverges from the WMS's implemented v1:

| Aspect | Pi assumed (v0) | WMS actual (v1) |
| --- | --- | --- |
| Login | `{deviceId, method: badge/pin}` | `{username, password, deviceCode}` → opaque bearer token; device must pre-exist |
| Task shape | `taskId` string, `qtyRequested`, `article.code` | numeric `id`, `state`, `orderNumber`/`lineNumber`/`taskSequence`, `location.code`, `article.sku`+`description`, `quantity` |
| Scans | local validation; server call = optional telemetry | **server-authoritative transitions** `{qrValue}`, required before confirm, replay-safe (`replayed: true`) |
| Confirm | rich body + `Idempotency-Key` header; any 409 = success | `{confirmationId, quantity}`; retry → 200 with original result; **409 codes are real failures** |
| Errors | `{error, message}` | RFC 9457 problem+json with `code`, `correlationId` |
| Health / logout | `GET /api/v1/health`; no logout | `GET /actuator/health` (tokenless); `POST /auth/logout` |
| Quantity | short picks allowed | exact quantity only (`422 QUANTITY_MISMATCH`) — confirmed WMS invariant |

Goal: adapt the HandheldPi client to v1, connect it over WiFi/LAN, verify and evidence end-to-end in both repos. **WMS workflow invariants stay untouched; the WMS code change is one dev-fixture migration.**

## Owner decisions (2026-07-15)

1. **Login = badge + numeric PIN.** Badge QR encodes the username (`OP:<username>`, a device-side convention the WMS never sees); the PIN-pad entry IS the account password → demo pickers get numeric passwords. WMS auth API unchanged; a new append-only dev-fixture migration adds a numeric-password picker.
2. **Offline = Level 2 store-and-forward.** Claiming always requires connectivity. Once claimed, scans validate locally (against the task payload — the exact server rule) and the ordered sequence scan-location → scan-article → confirm queues offline, replaying FIFO on reconnect via v1 replay-safety/idempotency. The server stays authoritative. A rejected replay (e.g. admin blocked the task meanwhile) → explicit terminal `SYNC_FAILED` "see supervisor" state with failed ops parked as dead-letter for audit; recovery = existing WMS admin block/resume + stock adjustment.
3. **No short-pick submission.** The quantity screen becomes a local count check; a mismatch → `DISCREPANCY` prompt (call supervisor), never confirms.

## Part A — HandheldPi changes (the bulk)

Keep the existing seam: events → pure `PickingStateMachine` → `WmsClient` port + `OfflineQueue` + `Flusher`. Change the port's *vocabulary* to v1 and generalize the queue payload; event loop, script runner, display pipeline untouched.

### A1. `src/hht/wms/models.py` — v1 domain objects

- `Session`: `token`, `username`, `role`, `expires_at`, `device_code` (+ `display_name` property so `screens.py` keeps working).
- `Article`: `sku`, `description` (drop `code`).
- `Task`: `id: int`, `state`, `order_number`, `line_number`, `task_sequence`, `location_code`, `article`, `quantity`, `assigned_at`; helpers `expected_location_qr` = `f"LOC:{location_code}"`, `expected_article_qr` = `f"ART:{article.sku}"` — the single place local-validation rules live.
- Replace `Confirmation` with **`QueuedOp`**: `op_key` (unique: `"{task_id}:scan_location"` / `"{task_id}:scan_article"` / the `confirmation_id`), `kind` (`SCAN_LOCATION|SCAN_ARTICLE|CONFIRM`), `task_id`, `payload: dict`, `created_at`, JSON round-trip.
- Typed outcomes: `ScanOutcome(state, replayed)`, `ConfirmOutcome(state, confirmed_quantity, order_state)`.

### A2. `src/hht/wms/base.py` — port + error taxonomy

New ABC: `login(username, password)`, `logout()` (best-effort, never raises), `next_task() -> Task | None` (204 → None), `scan_location(task_id, qr_value)`, `scan_article(...)`, `confirm(task_id, confirmation_id, quantity)`, `ping()` (`/actuator/health`). Errors: keep `WmsError`/`WmsUnavailable`/`WmsRejected`; add `code: str` to `WmsRejected`; add `WmsAuthError(WmsRejected)` for 401 `INVALID_TOKEN`/`TOKEN_EXPIRED`/`TOKEN_REVOKED` (= re-login required; the flusher pauses, never dead-letters).

### A3. `src/hht/wms/http_client.py` — full v1 rewrite

- `_request()`: parse problem+json (`code`, `title`/`detail`, `correlationId`); 401 auth codes → `WmsAuthError`; other 4xx → `WmsRejected(msg, status, code)`; 5xx/timeout/ConnectionError → `WmsUnavailable`. Send `X-Correlation-Id: uuid4()` per request and log it. Never log the token.
- `login`: `POST /api/v1/auth/login` `{username, password, deviceCode: cfg.device.id}`; set bearer header on the `requests.Session`. `logout`: `POST /api/v1/auth/logout`, clear header, swallow errors.
- `next_task`: `GET /api/v1/hht/tasks/next` (no query param — device binding is in the token).
- `scan_location`/`scan_article`: `{"qrValue": <verbatim scanned payload>}`; parse `state` + `replayed`.
- `confirm`: `{"confirmationId", "quantity"}`. **No** `Idempotency-Key` header, **no** 409-means-success shortcut — every 409 propagates as `WmsRejected` (a retry with the same UUID returns 200).
- `ping`: `GET {base}/actuator/health` (outside `/api/v1`, tokenless).

### A4. `src/hht/wms/offline_queue.py` — ordered op sequences + dead-letter

- New `operations` table (`op_key` UNIQUE, `task_id`, `kind`, `payload`, `created_at`, `attempts`, `last_error`, `status` `pending|sent|dead`, `sent_at`), versioned via `PRAGMA user_version = 2`.
- **Live-device migration:** deployed unit HHT-001 has a real `/var/lib/hht/queue.db` with the legacy `confirmations` table — on open with `user_version == 0`, mark unsent legacy rows `dead` (`'v0 payload — not deliverable to v1 API'`), keep the table as audit, create `operations`. Unit-test with a hand-built legacy DB.
- API: `enqueue` (INSERT OR IGNORE on `op_key`), `pending_count`, `dead_count`, `dead_ops`, `acknowledge_dead`, `clear_all`.
- `flush(client) -> FlushResult(sent, dead, failed_task_id, failed_code, auth_required)`: FIFO by id, dispatch on kind. `WmsUnavailable` → stop, keep pending. `WmsAuthError` → stop, keep pending, `auth_required=True`. `WmsRejected` → **poison handling**: mark op `dead`, cascade-dead every later pending op with the same `task_id`, record the code, stop. Success (incl. `replayed` and idempotent confirm-retry 200) → `sent`.

### A5. `src/hht/flusher.py` + `src/hht/events.py`

Consume `FlushResult`: post `QueueDepthEvent` (unchanged); new `SyncFailedEvent(task_id, code)` on rejection; new `AuthRequiredEvent()` — then pause flushing until re-login (resume on `kick()` after login). Online probe unchanged.

### A6. `src/hht/state_machine.py` — workflow changes

- New states: `SYNC_FAILED`, `DISCREPANCY`.
- **Login:** `_in_login_badge` — `OP:<username>` scan stores `badge_username` → `LOGIN_PIN`; `_in_login_pin` A-press calls `wms.login(badge_username, pin)`; B → back to badge. Map login codes to banners (`INVALID_CREDENTIALS`, `DEVICE_NOT_REGISTERED`/`DEVICE_INACTIVE`, `USER_INACTIVE`, `DEVICE_ASSIGNMENT_CONFLICT` → "Device busy — see supervisor"). PIN entry is fixed-length (`pin_digits`, `state_machine.py:70`) — the demo password must be exactly `pin_length` digits.
- **`_fetch_task`:** guard — refuse to fetch while `queue_depth > 0` ("Sync pending — wait"); v1 returns the *current active* task, which would otherwise hand back the still-unsynced pick. `TASK_ASSIGNMENT_CONFLICT` → retry once. Map returned `task.state` → screen (`ASSIGNED` → GOTO_LOCATION, `LOCATION_CONFIRMED` → SCAN_ARTICLE, `ARTICLE_CONFIRMED` → SET_QUANTITY) — resume after reboot/re-login.
- **Scans:** replace `_report_scan` telemetry with `_submit_scan(kind)`: (1) local pre-check against `expected_*_qr` — wrong → banner, stay; (2) match → call server: success/replayed → advance; `WmsUnavailable` → mark offline, enqueue op, kick flusher, advance (Level 2); `WmsAuthError` → session-expired flow; other `WmsRejected` (e.g. admin blocked → `INVALID_TASK_STATE`) → banner, drop task, → IDLE. Remove the bare-EAN `RAW` acceptance branch; always send the scanned payload verbatim as `qrValue`.
- **Quantity:** UP no longer capped at requested; A: `qty == task.quantity` → `_confirm_pick()`, else → `DISCREPANCY` (B → recount; no confirm path). `_confirm_pick` enqueues `QueuedOp(kind=CONFIRM, op_key=confirmation_id=uuid4())` + kicks flusher (confirm-always-queued pattern retained → pending/synced indication free). Delete the `allow_short_pick` branch (`state_machine.py:161`).
- **`SyncFailedEvent`** → clear task, → `SYNC_FAILED` (shows task id + code + "see supervisor"; ack → `acknowledge_dead()` → IDLE). **`AuthRequiredEvent`/401** → clear session (keep the queue!), "Session expired", → `LOGIN_BADGE`; after re-login kick the flusher.
- **`_logout`:** blocked while `pending_count() > 0` ("Sync pending — cannot log out") — logout would revoke the token the replay needs; otherwise call `wms.logout()` then local logout.

### A7. `src/hht/ui/screens.py`

New `_body_discrepancy`, `_body_sync_failed` + hints; `_body_confirmed` shows SYNCED vs SYNCING/stored-offline from `queue_depth`; `_body_login_pin` shows `badge_username`; field renames (`qty_requested` → `quantity`, `article.sku`).

### A8. `src/hht/wms/mock_client.py` — mirror v1

Tiny in-memory v1 server: login against new `[mock] operators` format `"username:Display Name:password"`; server-side task states with FIFO claim + current-active-task return; scans/confirm enforce order (`INVALID_TASK_STATE`), correctness (`WRONG_LOCATION`/`WRONG_ARTICLE`), replay-safety, confirm idempotency by ID (`CONFIRMATION_ID_REUSED` on payload mismatch), `QUANTITY_MISMATCH`. Failure injection: existing `offline`; new `block_current_task()` (replay-rejection scenario) and `expire_token()`.

### A9. `src/hht/script_runner.py` — DSL extensions

`wms block_task`, `wms expire_token`; `expect_dead <n>`; `flush` also feeds resulting `SyncFailedEvent`/`AuthRequiredEvent` into the state machine (deterministic, single-threaded).

### A10. Config

- `config.py`: remove `WorkflowCfg.allow_short_pick`; new `[mock] operators` format.
- `config/hht.toml.example`: `[device] id = "HHT-PI-01"` (comment: must match a registered WMS `device_code`, uppercase); `[wms] base_url = "http://<wms-host-ip>:8080"`; `pin_length` comment (= picker's numeric password length); delete `allow_short_pick`.
- New `config/dev-http.toml` for loopback e2e: `backend = "http"`, `base_url = "http://localhost:8080"`, `[device] id = "HHT-DEV-01"`, keyboard/console backends.
- No new `[wms]` keys: `/api/v1` and `/actuator/health` are contract constants.

### A11. Docs (HandheldPi)

- `API.md`: replace the v0 placeholder — canonical source = `warehouse-management/API.md`; document the subset used, an error-code → device-behavior table, the offline replay/dead-letter protocol, the badge convention `OP:<username>`.
- `PLAN.md` Phase 3: rewrite exit criteria to this plan (becomes the evidence checklist); fix the stale `FETCHING` state in the diagram.
- `README.md`: badge+PIN login, exact-quantity check, sync-failed path.
- `docs/TEST_SPECIFICATION.md`: revise TC-020..025 (badge+PIN); retire TC-034 (bare EAN) and TC-036 (short pick); repurpose TC-037 → discrepancy; add TC-045 replay rejection → SYNC_FAILED + dead-letter, TC-046 token expiry mid-replay, TC-047 claim guard, TC-048 mid-state resume, TC-049 logout guard.

### A12. `scripts/make_badge.py` (new)

Dev-side badge QR generator using `qrcode` as a **dev-only** dependency (`dev = ["pytest>=7", "qrcode>=7"]`): `python scripts/make_badge.py picker02 -o badge-picker02.png` renders `OP:picker02` + caption. Chosen over extending WMS label endpoints — badges are a device-side convention; adding them to the evidenced v1 admin surface would be new server scope for a demo artifact.

## Part B — WMS changes (minimal)

### B1. New `src/main/resources/db/devdata/V1_2__seed_hht_demo_picker.sql`

Append-only; never edit applied V1_1.

- `picker02` with a 4-digit numeric password (e.g. `2468`), Argon2id PHC hash, role `PICKER`; header comment in V1_1's style (dev-only demo credentials + badge convention). Not `0000` (indistinguishable from an untouched PIN pad).
- Second device row `HHT-DEV-01` ("Developer loopback HHT") — lets loopback dev and the physical `HHT-PI-01` stay logged in simultaneously and enables a two-device `DEVICE_ASSIGNMENT_CONFLICT` demo.
- Hash generated via the evidenced Phase 7 Step 0 pattern: throwaway JUnit test invoking the app's `Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8()` encoder, output captured, test deleted.
- Verify: `env -u SPRING_PROFILES_ACTIVE ./mvnw -B verify`; if `FlywayMigrationIT` enumerates seeded users in its argon2-format assertion, extend it additively.
- Update the demo-credentials line in `README.md`. **Nothing else server-side.**

## Part C — Network / deployment (OS-neutral)

1. The WMS host (Windows workstation or Mint desktop) runs profile `dev` (fixtures only load there); plain HTTP on the trusted LAN per ADR 0005 (the HTTPS requirement applies to preprod). Start per runbook §2; confirm `curl http://localhost:8080/actuator/health` **before** opening the firewall.
2. Scoped firewall rule per runbook §3 — the first real exercise of these documented steps. Mint: `sudo ufw allow from <lan-subnet>/24 to any port 8080 proto tcp comment "WMS API (LAN)"`; Windows: the runbook's scoped `New-NetFirewallRule`. PostgreSQL stays loopback-only.
3. Reachability per runbook §4 from the Pi over WiFi: `curl http://<server-lan-ip>:8080/actuator/health` (Pi Zero 2 W is 2.4 GHz-only — note the AP band in evidence).
4. Pi `/etc/hht/hht.toml`: `[device] id = "HHT-PI-01"`; `[wms] backend = "http"`, `base_url = "http://<server-lan-ip>:8080"`, `timeout_s = 5.0`, `retry_interval_s = 30.0`; restart `hht.service`. Rollback: runbook §6.3 (remove the firewall rule).

## Part D — Verification & evidence (staged)

HTTP-test approach: a small in-repo fake v1 server (`tests/fake_wms.py`, threaded stdlib `http.server`, ~150 lines), not `requests-mock` — zero new deps (HandheldPi's budget is deliberately apt-only), exercises the real socket path (timeouts, connection-refused → `WmsUnavailable`), and doubles as a scriptable stand-in for canned problem+json / 401 sequences.

### Stage 1 — automated tests (HandheldPi)

New `tests/test_http_client.py` (login mapping + token header, logout, problem+json → `WmsRejected.code`, 401 → `WmsAuthError`, 204 → None, `X-Correlation-Id`, confirm-409 propagation, connection-refused/timeout, health URL); updated `test_offline_queue.py` (op ordering, dead-letter cascade, acknowledge, legacy-schema migration), `test_state_machine.py`, `test_mock_wms.py`; scripted tests: updated `happy_path.txt`, `wrong_scans.txt` (no bare EAN), `offline_pick.txt` (2 scan ops + 1 confirm queued → drain to 0); new `sync_failed.txt` (offline pick → `wms block_task` → online → flush → `expect_state SYNC_FAILED`, `expect_dead 3` → ack → IDLE), `discrepancy.txt`, `token_expiry.txt`. All via `pytest`.

### Stage 2 — loopback e2e

`python -m hht -c config/dev-http.toml` against the real WMS dev profile, keyboard scans: (1) happy path `OP:picker02` + PIN → seeded task → `LOC:A-01-01` → `ART:ART-001` → exact qty → CONFIRMED synced; verify stock/movement via dashboard/`docs/sql-diagnostics.md`; (2) wrong-scan 409 banners; (3) drain all tasks → 204 → NO_TASK; (4) admin blocks the assigned task mid-pick → scan → task dropped; two-login `DEVICE_ASSIGNMENT_CONFLICT`; (5) idempotent confirm retry (kill WMS between enqueue and flush; restart; one movement only); (6) offline drain (stop WMS mid-task, finish the pick, restart, watch FIFO replay); (7) replay rejection (complete the pick while WMS is down, restart, admin-block before flush → SYNC_FAILED + dead rows; recover via dashboard resume); (8) token expiry (`WMS_AUTH_TOKEN_TTL=PT1M` → re-login → drain); (9) quantity mismatch → DISCREPANCY, no confirm on the wire (check by correlation id); (10) claim + logout guards while the queue is pending.

### Stage 3 — LAN e2e on hardware

Print labels from the WMS (`/admin/labels/locations/A-01-01/pdf`, `/admin/labels/articles/ART-001/pdf`) + the badge from `make_badge.py`; full happy path, one offline pick (walk out of WiFi range), one replay-rejection on the GamePi20 against the LAN host; the first real exercise of runbook §3–4.

### Evidence

- warehouse-management: new `docs/evidence/2026-07-XX-hht-lan-integration.md` under a build identifier (git HEAD + toolchain): V1_2 `mvnw verify` run, firewall apply/rollback output, health checks from the Pi, correlated server-log excerpts (correlation IDs from the device log), SQL reconciliation; append an execution pass to `docs/executed-test-report.md` (runbook §3–4 now evidenced).
- HandheldPi: tick `PLAN.md` Phase 3 boxes with dates; fill `docs/TEST_REPORT` from the template (PNG screen captures via the `image` display backend + `var/hht.jsonl` excerpts).

## Part E — Sequencing & risks

| # | Step | Depends on | Size |
| --- | --- | --- | --- |
| 1 | WMS V1_2 migration + hash + `mvnw verify` + README line | — | S |
| 2 | Pi contract layer: `models.py`, `base.py`, `http_client.py`, `API.md` | — | M |
| 3 | `tests/fake_wms.py` + `test_http_client.py` | 2 | M |
| 4 | `offline_queue.py` v2 + legacy migration + `flusher.py` + `events.py` | 2 | M |
| 5 | `state_machine.py` + `screens.py` + config | 2, 4 | M–L |
| 6 | `mock_client.py` v1 + script DSL + scripts + TEST_SPECIFICATION | 5 | M |
| 7 | `make_badge.py` + `dev-http.toml` + pyproject dev-dep | — | S |
| 8 | Loopback e2e (Stage 2) + fixes + evidence | 1–7 | M |
| 9 | LAN e2e on HHT-001 + firewall + labels + evidence in both repos | 8 | M |

Steps 1, 2/3, and 7 are parallelizable. One WMS commit (step 1); HandheldPi commits per step; evidence commits per stage.

Key risks (verified in code):

- Fixed-length PIN entry → the demo password must be exactly `pin_length` (4) digits; never `0000`.
- Deployed HHT-001 has a live legacy queue DB → the `user_version` migration path is mandatory; legacy rows are archived dead, never replayed.
- Without the `WmsAuthError` pause, the flusher would dead-letter valid work on token expiry (8 h TTL); logout-with-pending-queue must stay blocked.
- Keep `Task.id` an `int` end-to-end (URLs, `op_key`, script assertions).
- Don't couple mock fixtures to WMS seed data; scripts pump `flush` before asserting synced indication.
