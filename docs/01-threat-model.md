# Threat model

## Assets
- User accounts and user data
- Access tokens (JWT)
- Refresh tokens (long-lived session capability)

## Adversary assumptions
- Network-level attacker is out of scope; assume attacker can send arbitrary HTTP requests.
- Token theft is possible (e.g., XSS, malware, leaked logs) and must be contained by rotation and revocation.

## Key security goals
- Enforce authentication on protected endpoints
- Prevent refresh token replay and session duplication
- Enforce object-level authorization (users cannot read/modify other users)
