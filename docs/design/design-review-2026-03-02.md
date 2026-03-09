# Design Review Report - Auth Service Solution

**Date:** 2026-03-02
**Reviewed:** tech-solution-draft-2026-03-02.md
**Reviewer:** Solution Critic

---

## Critical Review

### Hidden Assumptions

- **Assumption:** API Gateway has direct access to Auth Service's public key for JWT validation
  - **Risk:** No key distribution mechanism specified. How does API Gateway get the public key?
  - **If false:** API Gateway cannot validate JWTs locally, defeating stateless architecture

- **Assumption:** Database cleanup job `cleanup_expired_tokens()` runs automatically
  - **Risk:** No scheduling mechanism specified. Spring `@Scheduled` requires `@EnableScheduling`
  - **If false:** Database grows indefinitely with expired tokens, performance degrades

- **Assumption:** File-based key storage at `/opt/auth-service/keys` works in Docker
  - **Risk:** Docker containers are ephemeral. Keys lost on container restart unless volume mounted
  - **If false:** All JWTs become invalid after restart, complete service outage

- **Assumption:** OAuth2 providers (Google, GitHub) are always available
  - **Risk:** No fallback mechanism when OAuth provider is down
  - **If false:** Users cannot log in via OAuth, but password login still works (partial mitigation)

- **Assumption:** Device ID is reliable for refresh token tracking
  - **Risk:** User agent string can be spoofed, changed via browser updates, or same user across multiple browsers
  - **If false:** Multi-device support breaks, users logged out unexpectedly

- **Assumption:** Clock skew tolerance of ±30 seconds is sufficient
  - **Risk:** No NTP sync requirement specified for containers. Container time drift can exceed 30s
  - **If false:** Valid tokens rejected, users repeatedly logged out

- **Assumption:** Rate limiting based on IP address works for all clients
  - **Risk:** Users behind NAT/proxy share same IP. Mobile users change IPs frequently
  - **If false:** Legitimate users blocked, or attackers bypass via IP rotation

### Unrealistic Parts

- **Problem:** Rate limiting with in-memory Caffeine cache
  - **Why unrealistic:** Doesn't scale across multiple instances. Each instance has independent rate limit state
  - **Reality:** Distributed systems need Redis or similar for coordinated rate limiting

- **Problem:** "Local JWT validation at API Gateway" without specifying how public key is distributed
  - **Why unrealistic:** API Gateway is separate service. Needs mechanism to fetch/cache public keys
  - **Reality:** Need `/api/v1/auth/.well-known/jwks.json` endpoint or shared filesystem

- **Problem:** Docker key generation with `RUN chmod 700`
  - **Why unrealistic:** Keys generated at build time are baked into image. All instances share same keys
  - **Reality:** Keys must be generated at runtime or injected via secrets

- **Problem:** OAuth2 success handler returns JSON in callback
  - **Why unrealistic:** OAuth2 callback is browser redirect. JSON response not appropriate
  - **Reality:** Need to redirect to frontend with tokens in URL hash or POST-message

- **Problem:** 15-minute access token expiry
  - **Why unrealistic:** User experience suffers - re-authentication every 15 minutes
  - **Reality:** Balance security with UX. Consider 30-60 minutes with blacklist for logout

- **Problem:** "Generate keys on startup (or mount from volume)"
  - **Why unrealistic:** No key persistence strategy. Startup generates new keys = invalidates all existing tokens
  - **Reality:** Need key persistence across restarts or key rotation strategy

### Logical Issues

- **Issue:** Token blacklist requires database lookup on every validation
  - **Explanation:** Defeats purpose of stateless JWT validation. Every request needs DB check
  - **Where logic breaks:** Section 4.3 says "Local JWT validation" but blacklist requires DB call

- **Issue:** Refresh token rotation not fully specified
  - **Explanation:** "Every refresh" mentioned but no race condition handling for concurrent refresh requests
  - **Where logic breaks:** Two concurrent refreshes could both succeed, creating token leakage

- **Issue:** OAuth2 user account creation has no verification
  - **Explanation:** `findOrCreate` automatically creates accounts. Anyone with Google account can access system
  - **Where logic breaks:** No email verification, no allowlist/blocklist for domains

- **Issue:** Soft delete (`deleted_at`) but queries don't consistently filter
  - **Explanation:** Some queries use `WHERE deleted_at IS NULL`, others don't specify
  - **Where logic breaks:** Inconsistent soft delete handling could expose deleted accounts

- **Issue:** Password reset flow sends email but no email service specified
  - **Explanation:** `send confirmation email` mentioned but no SMTP config or service integration
  - **Where logic breaks:** Incomplete feature - can't actually send reset emails

- **Issue:** `CASCADE DELETE` on foreign keys
  - **Explanation:** Deleting user deletes all tokens, but what about audit trail?
  - **Where logic breaks:** Compliance requirements may need to retain auth logs even after account deletion

### Failure Scenarios

1. **Docker Container Restart**
   - **Cause:** Container crash, deployment, or restart
   - **Early signals:** All JWT validations fail after restart
   - **Impact:** Complete service outage. All users logged out. Existing access tokens invalid
   - **Missing:** Key persistence via volume mount or external secret store

2. **OAuth2 Provider Outage**
   - **Cause:** Google/GitHub API downtime
   - **Early signals:** OAuth2 login attempts timeout
   - **Impact:** New users cannot register via OAuth. Existing OAuth users can't login if password not set
   - **Missing:** Fallback to password login or cached provider status

3. **Database Connection Pool Exhaustion**
   - **Cause:** High load, slow queries, connection leaks
   - **Early signals:** Rising connection count, increased latency
   - **Impact:** No new logins. Token refresh fails. Cascading failure
   - **Missing:** Circuit breaker, connection pool monitoring, graceful degradation

4. **Concurrent Token Refresh**
   - **Cause:** User has multiple tabs, all refresh simultaneously
   - **Early signals:** Multiple refresh tokens for same user in database
   - **Impact:** Only one refresh succeeds. Other tabs show auth errors
   - **Missing:** Optimistic locking or idempotency key for refresh requests

5. **Clock Skew Between Services**
   - **Cause:** Container time drift, NTP misconfiguration
   - **Early signals:** Tokens rejected as "expired" immediately after issuance
   - **Impact:** Users repeatedly logged out. Poor UX
   - **Missing:** NTP configuration in Docker, clock sync monitoring

6. **RSA Key Compromise**
   - **Cause:** Key leaked via logs, compromised server, insider threat
   - **Early signals:** Unexpected valid tokens for non-existent users
   - **Impact:** Attacker can forge tokens for any user. Complete system breach
   - **Missing:** Key compromise detection procedure, emergency revocation process

### Hidden Costs

- **Operational Cost:** Key management requires secure storage, access controls, audit trails. File-based storage insufficient for production
- **Development Cost:** Email service integration needed for password reset. SMTP or SendGrid/AWS SES
- **Testing Cost:** OAuth2 integration testing requires provider test accounts or mocking
- **Maintenance Cost:** Scheduled cleanup jobs need monitoring. What if job fails?
- **Cognitive Cost:** Debugging JWT issues requires understanding RS256, key rotation, clock skew
- **Infrastructure Cost:** Multi-instance deployment requires shared rate limiting state (Redis)
- **Opportunity Cost:** Stateful token blacklist negates JWT benefits. Consider shorter expiry instead

### Edge Cases

- **Case:** User changes password while logged in
  - **Why fails:** Existing access tokens remain valid until expiry (15 min)
  - **Missing:** Password change should invalidate all user's refresh tokens

- **Case:** User has 5 active devices, tries to login on 6th
  - **Why fails:** "Max 5 tokens" specified but no behavior defined for overflow
  - **Missing:** Oldest token revocation or error message

- **Case:** OAuth2 provider changes user's email
  - **Why fails:** Database has unique constraint on email. UPDATE fails
  - **Missing:** Email change handling, account merge logic

- **Case:** Refresh token expires during refresh request
  - **Why fails:** Race condition between expiry check and network latency
  - **Missing:** Grace period for tokens near expiry

- **Case:** Admin deletes user account, but user has active access token
  - **Why fails:** Access token valid for 15 more minutes. User can still access resources
  - **Missing:** Account deletion should add token to blacklist

- **Case:** Attacker steals refresh token, uses it from different IP
  - **Why fails:** No anomaly detection. Refresh succeeds from attacker's IP
  - **Missing:** IP-based anomaly detection or device fingerprinting

- **Case:** Database connection fails during token refresh
  - **Why fails:** Transaction partially committed. New token issued but old one not revoked
  - **Missing:** Transaction rollback handling, idempotent refresh

### Overconfidence

| Statement | Reality |
|-----------|----------|
| "Local JWT validation (No network call)" | Requires public key fetch from Auth Service on startup and periodically |
| "Rate limiting: 5 attempts per IP per minute" | Doesn't work behind NAT/proxy. Need user-based limiting |
| "Generate keys on startup" | Breaks existing tokens. Need key persistence |
| "OAuth2 callback returns JSON" | OAuth2 is browser redirect. Need frontend integration |
| "15-minute access token expiry" | Poor UX. Balance with security (30-60 min) |
| "Cleanup job: Delete expired tokens" | No scheduling specified. Job may never run |

---

## Reliability Upgrade

### Key Fixes

1. **Public Key Distribution:** Add JWKS endpoint for API Gateway to fetch public keys
2. **Key Persistence:** Volume mount for key storage, generate keys only if not exist
3. **OAuth2 Callback:** Redirect to frontend with tokens in URL hash, not JSON response
4. **Email Service:** Specify SMTP configuration or SendGrid integration for password reset
5. **Rate Limiting:** Document limitations, recommend Redis for distributed deployments
6. **Token Blacklist:** Clarify trade-off. Consider shorter expiry (30 min) instead of blacklist
7. **Clock Sync:** Add NTP configuration to Docker Compose
8. **Concurrent Refresh:** Add optimistic locking or idempotency key
9. **Password Change:** Invalidate all refresh tokens when password changes
10. **Multi-Device Overflow:** Specify revocation of oldest token when limit exceeded

### Improved Solution

See [tech-solution-2026-03-02.md](./tech-solution-2026-03-02.md) for the final approved solution addressing these issues.

---

## Summary of Critical Issues

| Severity | Count | Issues |
|----------|-------|--------|
| Critical | 3 | Key persistence, Public key distribution, OAuth2 callback |
| High | 5 | Database cleanup, Clock skew, Concurrent refresh, Rate limiting, Token blacklist |
| Medium | 8 | Email service, Soft delete consistency, Key compromise, Edge cases |
| Low | 4 | Documentation, Monitoring gaps |

**Recommendation:** Address Critical and High issues before proceeding to implementation. Medium and Low issues can be tracked as technical debt.

---

**Review Status:** COMPLETE
**Next Step:** Step 2.5 - Final Solution
