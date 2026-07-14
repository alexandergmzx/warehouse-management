# Incident record template

**Status:** Phase 9 deliverable. Copy this template for each real incident;
retain the filled-in copy under `docs/evidence/` (or the operational
incident log the owner designates) alongside its supporting log/SQL excerpts.
Compilation or "it works now" is not a resolution — this record with
retained evidence is.

---

## Incident `<INC-YYYY-MM-DD-NN>`

| Field | Value |
|---|---|
| Detected at (UTC) | |
| Reported by | |
| Severity | Critical / High / Medium / Low |
| Build/configuration identifier | `<git short hash>` `+` environment (dev/preprod) |
| Status | Open / Diagnosed / Recovered / Closed |

### Summary

One or two sentences: what was observed, by whom, and its operational
impact (e.g., "picker could not confirm task X; blocked one HHT device for
12 minutes").

### Identifiers

Fill in whichever apply — these are the join keys across logs, SQL, and the
audit ledger:

| Identifier | Value |
|---|---|
| Correlation ID(s) | |
| Order number | |
| Task ID / task number | |
| User / device | |
| Article SKU | |
| Location code | |
| Stock movement ID | |

### Timeline

| Time (UTC) | Event | Source |
|---|---|---|
| | | e.g. HHT operator report, log line, SQL query |

### Diagnosis

Attach or paste the actual evidence, not a description of it:

- Relevant `docs/log-analysis-guide.md` query output (structured log lines,
  particularly any `business rule violation` entries for the correlation
  ID/time window).
- Relevant `docs/sql-diagnostics.md` query output (stuck-task check, ledger
  reconciliation, or full order trace, as applicable).
- Screenshots only if the dashboard or HHT UI itself is part of the
  evidence.

### Root cause

What actually caused it — a wrong scan, a genuine defect, a stuck task past
the threshold, a stock/ledger discrepancy, a configuration problem, etc. If
unknown at the time of initial recording, say so explicitly and update this
section once known; do not leave it blank silently.

### Recovery action taken

State exactly what was done, through which interface:

- Administrative block/resume (`POST /admin/tasks/{id}/block` or
  `.../resume`, ADR 0004) — cite the resulting `task_transition` row.
- Stock adjustment (`POST /admin/stock/adjustments`) — cite the resulting
  `stock_movement` row.
- A code/configuration fix — cite the commit/PR and, if it changed the
  schema, the new forward migration (never an edit to an applied one).

**Never** record "corrected via direct SQL `UPDATE`" as a recovery action —
that path bypasses the audit ledger and violates the project's stock/task
integrity guarantees (CLAUDE.md). If direct SQL was used to *inspect* state,
that is fine and expected; only *state changes* must go through an audited
endpoint or a new migration.

### Follow-up

- [ ] Root cause addressed (code fix, configuration fix, or confirmed
      operator/process issue with no code change needed).
- [ ] Evidence retained under `docs/evidence/` or the designated incident
      log.
- [ ] Traceability/functional-test-specification updated if this incident
      revealed a gap in test coverage.

### Closed by / closed at (UTC)
