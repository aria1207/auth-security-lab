# auth-security-lab

Security-focused Spring Boot authentication lab.

Features:
- POST /login returns accessToken + refreshToken
- POST /refresh issues a new accessToken and rotates refreshToken (old refreshToken becomes invalid)
- POST /logout revokes refreshToken on server side
- GET /me is a protected endpoint (requires Authorization: Bearer <accessToken>)

Security focus:
- Refresh token is stored in database with expiry and revocation state
- Refresh token replay should fail after rotation or logout
- Auth flow is verified by MockMvc integration tests

Tech:
- Java 17
- Spring Boot
- JDBC + H2
- MockMvc tests

Run:
./mvnw spring-boot:run

Test:
./mvnw test

Disclaimer:
For educational and security research purposes only.
