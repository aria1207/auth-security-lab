# Methodology

The assessment follows a practical API pentest workflow:

1. Baseline functional verification (login/refresh/logout)
2. Authentication enforcement checks (protected endpoints reject missing/invalid tokens)
3. Concurrency testing for refresh-token rotation (race / TOCTOU)
4. Authorization testing for BOLA/IDOR (read/write access across user IDs)
5. Evidence capture via curl, saved JSON responses, and scripts for reproducibility

All testing was performed locally against this lab application.
