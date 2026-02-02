# Regression tests

This section documents tests that prove fixes are effective.

## For F-01
- A concurrency test that issues N refresh requests in parallel using the same refresh token
- Expected: exactly 1 request succeeds (200), the rest fail (401)

## For F-02
- User A requesting `GET /users/{id_of_B}` returns 403
- User A requesting `PUT /users/{id_of_B}` returns 403
- User A requesting their own resource returns 200
