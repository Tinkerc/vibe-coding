# Implementation Plan Review Report

**Date:** 2026-03-02
**Reviewed:** implementation-plan-draft-2026-03-02.md
**Reviewer:** Solution Critic

---

## Critical Review

### Hidden Assumptions

- **Assumption:** Each task takes exactly 2-5 minutes
  - **Risk:** Debugging, dependency issues, environment setup can take longer
  - **If false:** Timeline slips, team loses confidence in plan

- **Assumption:** TDD test-first approach works for all code
  - **Risk:** Security configuration, Spring Boot auto-configuration may not be easily testable first
  - **If false:** Developers skip TDD, code quality suffers

- **Assumption:** OAuth2 provider credentials (Google, GitHub) are available during Week 3
  - **Risk:** OAuth app approval may take days/weeks
  - **If false:** Cannot test OAuth2 integration, timeline blocked

- **Assumption:** Database is always available for integration tests
  - **Risk:** TestContainers requires Docker. CI/CD environment may not support Docker
  - **If false:** Tests fail in CI, deployment blocked

- **Assumption:** Key generation on startup works in all environments
  - **Risk:** File system permissions, read-only file systems
  - **If false:** Service fails to start

### Unrealistic Parts

- **Problem:** "130 tasks over 8 weeks" = ~16 tasks/week = ~3 tasks/day
  - **Why unrealistic:** Doesn't account for meetings, bugs, refactoring, learning curve
  - **Reality:** Plan for 2 tasks/day maximum, buffer for unexpected issues

- **Problem:** Task 1.3.1 (JwtTokenProvider) estimated at 5 minutes
  - **Why unrealistic:** RSA key generation, file I/O, exception handling takes time
  - **Reality:** 15-30 minutes more realistic for complete implementation

- **Problem:** "Commit after each task"
  - **Why unrealistic:** Creates too many small commits. Git history becomes noisy
  - **Reality:** Commit after feature completion (2-3 related tasks)

- **Problem:** TestContainers for all integration tests
  - **Why unrealistic:** Slow startup time (seconds per test). Developers will skip tests
  - **Reality:** Use H2 in-memory for unit tests, TestContainers only for critical paths

### Logical Issues

- **Issue:** Phase 1 Week 2 has no token refresh or logout implementation
  - **Explanation:** Login creates access token but no refresh token. Users must re-login every 30 minutes
  - **Where logic breaks:** Incomplete MVP, poor UX

- **Issue:** OAuth2 handler not specified in implementation plan
  - **Explanation:** Task 2.1.1 creates config but no success handler or user provisioning
  - **Where logic breaks:** OAuth2 login doesn't work end-to-end

- **Issue:** No migration scripts specified
  - **Explanation:** `ddl-auto: update` used in dev, but no Flyway/Liquibase for production
  - **Where logic breaks:** Production deployment has no schema versioning

- **Issue:** JWKS endpoint not in implementation plan
  - **Explanation:** Design specifies JWKS endpoint but implementation tasks skip it
  - **Where logic breaks:** API Gateway cannot fetch public key

### Failure Scenarios

1. **OAuth App Approval Delay**
   - **Cause:** Google/GitHub app review takes longer than expected
   - **Early signals:** Cannot obtain client credentials by Week 3
   - **Impact:** OAuth2 testing blocked, Phase 2 delayed
   - **Missing:** Mock OAuth2 for development, buffer time for approval

2. **Key Permission Issues in Docker**
   - **Cause:** Container runs as non-root user, cannot write to `/opt/auth-service/keys`
   - **Early signals:** "Permission denied" errors on startup
   - **Impact:** Service fails to start
   - **Missing:** Pre-create keys in Dockerfile, or use volume with correct permissions

3. **TestContainers Slow Test Suite**
   - **Cause:** PostgreSQL container startup on every test
   - **Early signals:** Tests take 5+ minutes, developers stop running them
   - **Impact:** Code quality degrades, bugs slip through
   - **Missing:** Test categorization (unit vs integration), shared database for tests

4. **JWT Key Loss During Deployment**
   - **Cause:** New deployment overwrites keys, no volume persistence
   - **Early signals:** All JWTs invalid after deployment
   - **Impact:** All users logged out, service disruption
   - **Missing:** Key backup/restore procedure, volume mount verification

### Hidden Costs

- **Time Cost:** OAuth app setup (Google Console, GitHub Developer Settings) - 1-2 hours
- **Operational Cost:** Key backup procedures, disaster recovery testing
- **Development Cost:** Learning curve for Spring Security 6.x (breaking changes from 5.x)
- **Testing Cost:** OAuth2 integration testing requires real callbacks or complex mocking
- **Maintenance Cost:** Key rotation requires coordination, no rollback specified

### Edge Cases

- **Case:** Developer's machine doesn't have Docker
  - **Why fails:** TestContainers requires Docker
  - **Missing:** Alternative testing strategy (H2, mock repos)

- **Case:** Running tests on CI without Docker-in-Docker
  - **Why fails:** TestContainers fails, build breaks
  - **Missing:** CI-specific test configuration

- **Case:** Developer changes OAuth2 redirect URI locally
  - **Why fails:** OAuth callback returns 400 (redirect URI mismatch)
  - **Missing:** Local OAuth configuration guide

---

## Reliability Upgrade

### Key Fixes

1. **Realistic Time Estimates:** Double estimates for complex tasks (JWT, OAuth2)
2. **OAuth Buffer:** Plan for OAuth app approval delay (mock for dev)
3. **Test Strategy:** Use H2 for fast unit tests, TestContainers for integration only
4. **Migration Scripts:** Add Flyway/Liquibase for production schema management
5. **Complete Phase 1:** Add refresh/logout to Week 2
6. **Key Persistence:** Add volume mount verification test
7. **Batch Commits:** Group related tasks, commit after feature completion
8. **CI Testing:** Add separate CI test profile without TestContainers

### Improved Plan

See [implementation-plan-2026-03-02.md](./implementation-plan-2026-03-02.md) for the final approved plan.

---

## Summary

| Aspect | Finding |
|--------|---------|
| Time Estimates | Too optimistic, add 50% buffer |
| Task Granularity | Good, but group commits by feature |
| OAuth2 | Missing app approval time |
| Testing | TestContainers overkill for unit tests |
| Deployment | Key persistence needs verification |

**Recommendation:** Address time estimates and OAuth2 buffer before starting implementation.

---

**Review Status:** COMPLETE
**Next Step:** Step 2.8 - Plan Freeze
