# auth-security-lab  
**JWT Authentication & Refresh Token Security Lab**

> A hands-on authentication security laboratory focusing on JWT, refresh token rotation, logout revocation, and attack-aware test coverage.

---

## 🎯 Project Goal

This project is a **security-oriented authentication lab**, designed from the perspective of **offensive & defensive security** rather than pure application development.

The main goal is to **build, verify, and reason about a secure JWT-based authentication flow**, including:

- Short-lived access tokens
- Long-lived refresh tokens
- Refresh token rotation
- Token revocation on logout
- Automated tests that simulate real attack scenarios

This lab is intended to support:
- Penetration testing practice
- Secure backend design learning
- Security-focused portfolio demonstration

---

## 🧠 Threat Model & Security Assumptions

This lab explicitly considers common authentication threats:

- Stolen refresh token reuse
- Replay attacks on refresh endpoint
- Session fixation
- Improper logout handling
- Missing server-side refresh token state

**Design principle:**  
> _Refresh tokens are stateful, access tokens are stateless._

---

## 🔐 Authentication Design

### Access Token (JWT)
- Short-lived
- Stateless
- Used for API authorization
- Contains `userId` and `username`
- Signed server-side

### Refresh Token
- Random UUID
- Stored server-side in database
- Has expiration time
- Can be revoked
- Rotated on every refresh

---

## 🔁 Refresh Token Rotation (Key Security Feature)

On every `/refresh` request:

1. Validate refresh token:
   - Exists
   - Not expired
   - Not revoked
2. Revoke the old refresh token
3. Issue:
   - New access token
   - New refresh token
4. Store the new refresh token

This ensures:
- **Stolen refresh tokens cannot be reused**
- Replay attacks fail immediately

---

## 🚪 Logout Semantics

Logout is **server-side enforced**, not client-only.

On `/logout`:
- The provided refresh token is revoked in database
- Any further refresh attempts with that token return `401`

---

## 🧪 Security Test Coverage (MockMvc)

This project includes **end-to-end security tests**, not just unit tests.

### Covered scenarios:
- ✅ Login returns both access & refresh tokens
- ✅ Refresh rotates refresh token
- ❌ Old refresh token reuse fails (401)
- ✅ Logout revokes refresh token
- ❌ Refresh after logout fails
- ❌ Missing refresh token returns 400

These tests model **real attacker behavior**, not just happy paths.

---

## 🛠 Tech Stack

- Java 17
- Spring Boot
- JWT (custom service)
- H2 database (local, file-based)
- MockMvc (integration testing)
- Maven

---

## 📂 Project Structure
