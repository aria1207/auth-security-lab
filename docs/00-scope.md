# Scope

This repository documents a security assessment of a Spring Boot authentication lab implementing:

- JWT login
- Refresh token storage (DB)
- Refresh token rotation
- Logout (refresh token revocation)
- MockMvc integration tests

## In scope
- HTTP API endpoints under this app
- Authentication and authorization logic
- Refresh-token rotation correctness under concurrency

## Out of scope
- Infrastructure/cloud configuration
- Frontend security
- Third-party identity providers

## Environment
- Local deployment (H2 database)
- Single instance
