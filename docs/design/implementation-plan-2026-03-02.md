# Auth Service MVP - Implementation Plan (READY)

**Date:** 2026-03-02
**Status:** READY FOR BUILD
**Version:** 1.0

---

## Overview

This implementation plan follows TDD-first methodology with realistic time estimates. Tasks are organized into 4 phases over 8 weeks.

**Key Adjustments from Review:**
- Time estimates increased by 50% for complex tasks
- Added OAuth app approval buffer (mock OAuth2 for dev)
- Test strategy: H2 for unit tests, TestContainers for integration
- Added Flyway for database migrations
- Group commits by feature (not per task)

---

## Phase 1: Core Authentication (Weeks 1-2)

### Week 1: Project Setup

| Task | Time | Commit | Description |
|------|------|--------|-------------|
| 1.1.1 | 5m | `feat(build): initialize Spring Boot project` | Create pom.xml with dependencies |
| 1.1.2 | 3m | `feat(config): create application.yml` | Base configuration |
| 1.1.3 | 2m | `feat(app): create main application class` | AuthServiceApplication |
| 1.1.4 | 10m | `feat(build): add Flyway migration` | Create database schema |
| 1.1.5 | 5m | `test(build): verify database connection` | TestContainers test |

### Week 1: Entities & Repositories

| Task | Time | Commit | Description |
|------|------|--------|-------------|
| 1.2.1 | 10m | `feat(model): create User entity` | Entity with test |
| 1.2.2 | 10m | `feat(model): create RefreshToken entity` | Entity with test |
| 1.2.3 | 10m | `feat(model): create TokenBlacklist entity` | Entity with test |
| 1.2.4 | 10m | `feat(repo): create UserRepository` | Repository interface |
| 1.2.5 | 10m | `feat(repo): create RefreshTokenRepository` | Repository interface |
| 1.2.6 | 10m | `feat(repo): create TokenBlacklistRepository` | Repository interface |
| 1.2.7 | 15m | `test(repo): integration tests for repos` | TestContainers |

### Week 2: Security & JWT

| Task | Time | Commit | Description |
|------|------|--------|-------------|
| 1.3.1 | 20m | `feat(security): create JwtTokenProvider` | RS256 with key generation |
| 1.3.2 | 15m | `test(security): JWT generation tests` | Unit tests |
| 1.3.3 | 15m | `feat(security): create JWKS endpoint` | Public key endpoint |
| 1.3.4 | 10m | `test(security): JWKS endpoint test` | Verify response format |
| 1.3.5 | 15m | `feat(security): create SecurityConfig` | Filter chain |
| 1.3.6 | 10m | `test(security): security config test` | Test endpoint permissions |
| 1.3.7 | 10m | `feat(dto): create request DTOs` | LoginRequest, etc. |
| 1.3.8 | 10m | `feat(dto): create response DTOs` | AuthResponse |
| 1.3.9 | 20m | `feat(service): create AuthService` | Login logic |
| 1.3.10 | 15m | `test(service): AuthService tests` | Unit tests with mocks |
| 1.3.11 | 10m | `feat(controller): create AuthController` | Login endpoint |
| 1.3.12 | 10m | `test(controller): AuthController tests` | @WebMvcTest |
| 1.3.13 | 20m | `feat(service): implement TokenService` | Refresh token logic |
| 1.3.14 | 15m | `test(service): TokenService tests` | Rotation tests |
| 1.3.15 | 10m | `feat(controller): add refresh endpoint` | POST /refresh |
| 1.3.16 | 10m | `feat(controller): add logout endpoint` | POST /logout |
| 1.3.17 | 15m | `test(e2e): login/refresh/logout flow` | Integration test |

**Week 1-2 Total:** ~5 hours (realistic)

---

## Phase 2: OAuth2 Integration (Weeks 3-4)

### Week 3: OAuth2 Setup

| Task | Time | Commit | Description |
|------|------|--------|-------------|
| 2.1.1 | 10m | `feat(config): create OAuth2Config` | Provider configuration |
| 2.1.2 | 30m | `docs(oauth): setup OAuth apps` | **BUFFER: Get credentials** |
| 2.1.3 | 20m | `test(oauth): mock OAuth2 setup` | WireMock for dev |
| 2.1.4 | 20m | `feat(oauth): OAuth2SuccessHandler` | Redirect to frontend |
| 2.1.5 | 20m | `feat(service): OAuth2UserService` | User provisioning |
| 2.1.6 | 15m | `test(service): OAuth2 user tests` | Account linking |
| 2.1.7 | 20m | `test(e2e): OAuth2 flow test` | Full integration |

### Week 4: Rate Limiting & Cleanup

| Task | Time | Commit | Description |
|------|------|--------|-------------|
| 2.2.1 | 20m | `feat(security): create RateLimitFilter` | Bucket4j implementation |
| 2.2.2 | 15m | `test(security): rate limit tests` | Verify blocking |
| 2.2.3 | 15m | `feat(scheduled): TokenCleanupService` | @Scheduled task |
| 2.2.4 | 10m | `test(scheduled): cleanup tests` | Verify deletion |
| 2.2.5 | 15m | `feat(exception): GlobalExceptionHandler` | Error handling |
| 2.2.6 | 10m | `docs(api): add OpenAPI annotations` | Swagger docs |
| 2.2.7 | 15m | `test(e2e): rate limit integration` | Verify limits |

**Week 3-4 Total:** ~3.5 hours

---

## Phase 3: Advanced Features (Weeks 5-6)

### Week 5: Password Reset & Blacklist

| Task | Time | Commit | Description |
|------|------|--------|-------------|
| 3.1.1 | 20m | `feat(email): create EmailService` | SMTP config |
| 3.1.2 | 10m | `test(email): email service test` | Mock JavaMailSender |
| 3.1.3 | 20m | `feat(service): password reset flow` | Request + confirm |
| 3.1.4 | 15m | `test(service): password reset tests` | Token generation |
| 3.1.5 | 15m | `feat(security): token blacklist check` | In JWT filter |
| 3.1.6 | 10m | `test(security): blacklist tests` | Verify blocking |
| 3.1.7 | 15m | `feat(service): password change invalidation` | Revoke tokens |
| 3.1.8 | 15m | `test(service): multi-device overflow` | Revoke oldest |

### Week 6: Key Rotation & Monitoring

| Task | Time | Commit | Description |
|------|------|--------|-------------|
| 3.2.1 | 30m | `feat(security): key rotation service` | 90-day rotation |
| 3.2.2 | 20m | `test(security): key rotation tests` | Verify backward compat |
| 3.2.3 | 15m | `feat(monitoring): add Micrometer metrics` | Custom metrics |
| 3.2.4 | 10m | `test(monitoring): metrics endpoint test` | Verify exposure |
| 3.2.5 | 15m | `feat(actuator): health indicators` | DB, key store |
| 3.2.6 | 20m | `test(performance): load test` | Parallel refresh |

**Week 5-6 Total:** ~3.5 hours

---

## Phase 4: Deployment (Weeks 7-8)

### Week 7: Docker & Configuration

| Task | Time | Commit | Description |
|------|------|--------|-------------|
| 4.1.1 | 20m | `feat(docker): create Dockerfile` | Multi-stage build |
| 4.1.2 | 15m | `feat(docker): docker-compose.yml` | With volumes |
| 4.1.3 | 10m | `test(docker): verify key persistence` | Restart test |
| 4.1.4 | 15m | `feat(deploy): production config` | application-prod.yml |
| 4.1.5 | 15m | `docs(deploy): deployment guide` | Runbook |

### Week 8: Testing & Documentation

| Task | Time | Commit | Description |
|------|------|--------|-------------|
| 4.2.1 | 20m | `test(e2e): full test suite` | All flows |
| 4.2.2 | 15m | `test(security): clock skew tests` | ±30s tolerance |
| 4.2.3 | 10m | `docs(api): API documentation` | OpenAPI spec |
| 4.2.4 | 15m | `docs(readme): README.md` | Setup instructions |
| 4.2.5 | 10m | `release: tag v1.0.0` | MVP release |

**Week 7-8 Total:** ~2 hours

---

## Total Time Estimate

| Phase | Hours | Buffer (20%) | Total |
|-------|-------|--------------|-------|
| Phase 1 | 5 | 1 | 6 hours |
| Phase 2 | 3.5 | 0.7 | 4.2 hours |
| Phase 3 | 3.5 | 0.7 | 4.2 hours |
| Phase 4 | 2 | 0.4 | 2.4 hours |
| **Total** | **14** | **2.8** | **16.8 hours** |

**Weekly Commitment:** ~2 hours/week for 8 weeks

---

## Testing Strategy

### Unit Tests (Fast)
- Framework: JUnit 5 + Mockito
- Database: H2 in-memory
- Coverage Goal: 80%+

### Integration Tests (Slow)
- Framework: TestContainers
- Database: PostgreSQL 15
- Run: CI only, or `mvn verify -P integration`

### Test Execution

```bash
# Unit tests only (fast)
mvn test

# Integration tests (slow)
mvn verify -P integration

# All tests
mvn verify
```

---

## Environment Setup

### Prerequisites
- Java 21 JDK
- Maven 3.9+
- Docker Desktop (for TestContainers)
- PostgreSQL 15+ (local dev)

### OAuth2 Setup (Week 3 Buffer)

**Google OAuth2:**
1. Go to https://console.cloud.google.com/
2. Create project, enable OAuth2
3. Add redirect URI: `http://localhost:8080/login/oauth2/code/google`
4. Copy Client ID/Secret to `.env`

**GitHub OAuth2:**
1. Go to https://github.com/settings/developers
2. New OAuth App
3. Authorization callback: `http://localhost:8080/login/oauth2/code/github`
4. Copy Client ID/Secret to `.env`

---

## Git Workflow

### Branch Strategy
```
main (protected)
  └── develop (integration)
      └── feature/* (per phase)
```

### Commit Conventions
```
feat(scope): description
fix(scope): description
test(scope): description
docs(scope): description
refactor(scope): description
```

### Merge Guidelines
- Feature branch → develop: After code review
- develop → main: After phase completion

---

## Risk Mitigation

| Risk | Mitigation | Timeline Impact |
|------|------------|-----------------|
| OAuth approval delay | Mock OAuth2 for dev | None |
| TestContainers slow | H2 for unit tests | None |
| Key persistence issues | Volume mount test | +1 day |
| Time estimate overrun | 20% buffer included | None |

---

## Success Criteria

### Phase 1 Complete
- [ ] User can login with username/password
- [ ] JWT token generation works
- [ ] Token refresh works
- [ ] Logout invalidates refresh token
- [ ] Unit tests pass (80%+ coverage)
- [ ] Integration tests pass

### Phase 2 Complete
- [ ] Google OAuth2 login works
- [ ] GitHub OAuth2 login works
- [ ] Rate limiting blocks excessive attempts
- [ ] Expired tokens cleaned up

### Phase 3 Complete
- [ ] Password reset sends email
- [ ] Token blacklist blocks revoked tokens
- [ ] Key rotation works without disruption

### Phase 4 Complete (MVP)
- [ ] Docker compose starts all services
- [ ] Keys persist across restarts
- [ ] All tests pass in CI
- [ ] API documentation complete
- [ ] Deployment guide complete

---

## Next Steps

1. **Review this plan** with team
2. **Set up OAuth apps** (do this early!)
3. **Create feature branch:** `git checkout -b feature/phase1-core-auth`
4. **Start Week 1, Task 1.1.1**

---

**Plan Status:** READY FOR BUILD
**Author:** Design Orchestrator
**Approved:** 2026-03-02
