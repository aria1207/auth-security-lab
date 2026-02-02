# Findings

## F-01 Refresh Token Rotation Race Condition (TOCTOU) → Session Forking
**Severity:** High  
**Endpoint:** `POST /refresh`

### Summary
Concurrent refresh requests using the same refresh token succeeded multiple times, minting multiple distinct refresh tokens. This breaks the single-use guarantee of refresh token rotation under concurrency.

### Steps to reproduce
1. `POST /login` to obtain refresh token `R1`
2. Send 10 concurrent `POST /refresh` requests with the same `R1`
3. Observe multiple `200 OK` responses and multiple distinct `refreshToken` values in responses

### Evidence
- `evidence/refresh_*.json` (multiple refresh responses produced from the same `R1`)
- Additional validation showed multiple newly minted refresh tokens can be used successfully (session fork continues), until each is consumed once.

### Impact
- Session duplication after token theft: attacker can clone multiple valid refresh chains.
- Rotation is ineffective under concurrency; revocation/audit becomes harder.

### Likely root cause
Non-atomic “validate then revoke” flow (classic TOCTOU). Multiple requests can validate the same token before it is marked revoked/used.

### Recommendation
- Make refresh consumption atomic (single SQL update with conditional WHERE clause + check affected rows)
- Consider refresh token “family” / reuse detection (if an old token is presented, revoke the entire family)

---

## F-02 Broken Object Level Authorization (BOLA/IDOR) – Read/Write
**Severity:** High (write access)  
**Endpoints:** `GET /users`, `GET /users/{id}`, `PUT /users/{id}`

### Summary
Any authenticated user can enumerate users and read/modify other users by changing the `{id}` path parameter. Object-level authorization is missing.

### Steps to reproduce
1. Login to obtain `accessToken`
2. `GET /users` → returns multiple users
3. `GET /users/2` → returns another user's record
4. `PUT /users/2` with a new username/password → modifies another user

### Impact
- Account integrity compromise (attacker can modify other users)
- User enumeration and privacy exposure

### Recommendation
- Enforce object-level authorization (self-only or self-or-admin)
- Add regression tests verifying cross-user access returns `403 Forbidden`
