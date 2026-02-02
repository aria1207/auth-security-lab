# Fix plan

## Fix for F-01 (refresh rotation race)
- Enforce atomic refresh token consumption (single conditional update + row count check)
- Optional: refresh token family / reuse detection to revoke all descendants on reuse

## Fix for F-02 (BOLA/IDOR)
Option A (simpler demo):
- Users can only read/update their own user ID (match JWT `sub`)

Option B (more realistic):
- Add role claim (ADMIN/USER)
- `GET /users` admin-only
- `GET/PUT /users/{id}` allowed for self or admin

This repo will implement one of the above options and include regression tests.
