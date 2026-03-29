## Banking-Specific Guidelines
### Money Handling
* All monetary values use `BigDecimal` — NEVER use `float` or `double`.
* Rounding: `HALF_UP` to 2 decimal places.
* Currency: Always `MYR` — validate on all endpoints.

### Audit Trail
* Every financial transaction creates a JournalEntry (double-entry).
* AuditLog entity records who, what, when, where for all operations.
* Audit logs are immutable — append-only, no updates or deletes.

### Security
* PINs: Hardware-level encryption via HSM. DUKPT PIN blocks. Never decrypted outside HSM.
* PAN: Masked in all responses and logs (first 6, last 4 digits).
* MyKad: Encrypted at rest (AES-256). Never in plaintext logs.
* TLS 1.2+ for all external traffic.
* mTLS for internal service-to-service communication.

### Geofencing
* Transactions allowed only within 100m of registered Merchant GPS coordinate.
* If GPS unavailable: reject transaction with `ERR_GPS_UNAVAILABLE`.

### Velocity Checks
* Limit transactions per MyKad per day to prevent smurfing.
* Configurable via VelocityRule entity.