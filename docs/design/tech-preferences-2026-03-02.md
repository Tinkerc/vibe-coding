# Technical Preferences - Auth Service

**Date:** 2026-03-02
**Status:** COLLECTED
**Project:** Auth Service

---

## User Preferences (From Q&A)

| Dimension | Choice | Rationale |
|-----------|--------|-----------|
| **Backend Framework** | Java Spring Boot | User selected |
| **Project Scale** | MVP (1-2 months) | Core features, quick launch |
| **Deployment** | Local deployment | Dev/VM environment |

---

## From Requirements Document

| Dimension | Choice | Source |
|-----------|--------|--------|
| **Database** | PostgreSQL 15+ | requirement-2026-03-02.md |
| **Auth Protocol** | OAuth2 + OIDC | requirement-2026-03-02.md |
| **Token Format** | JWT (RS256) | requirement-2026-03-02.md |

---

## Technology Stack Summary

```
┌─────────────────────────────────────────────────────────────┐
│                    Auth Service Tech Stack                   │
├─────────────────────────────────────────────────────────────┤
│  Language:        Java 17+                                   │
│  Framework:       Spring Boot 3.x                            │
│  Security:        Spring Security 6.x                        │
│  Database:        PostgreSQL 15+                             │
│  ORM:             Spring Data JPA / Hibernate                │
│  Auth Protocol:   OAuth2 + OIDC                             │
│  Token Format:    JWT (RS256)                               │
│  Deployment:      Local (embedded Tomcat)                    │
│  Build Tool:      Maven/Gradle (TBD)                        │
│  Testing:         JUnit 5, Mockito                          │
└─────────────────────────────────────────────────────────────┘
```

---

## MVP Scope (1-2 Months)

### Must Have (Phase 1)
- User login (username/password)
- JWT token generation and validation
- Refresh token with rotation
- Logout functionality
- Basic OAuth2/OIDC (Google, GitHub)
- Rate limiting
- Basic monitoring

### Should Have (Phase 2 - if time permits)
- Password reset flow
- Multi-device support
- Token blacklist

### Nice to Have (Phase 3 - Future)
- 2FA/MFA
- Additional OAuth providers
- Session management UI

---

## Design Considerations

### Spring Boot Advantages for This Project
- **Spring Security:** Mature OAuth2/OIDC support
- **Spring Data JPA:** PostgreSQL integration
- **Embedded Server:** No external server needed for local deployment
- **Ecosystem:** Rich libraries for JWT, rate limiting, monitoring

### Local Deployment Implications
- No external service discovery needed
- Single JVM deployment
- In-memory rate limiting (Guava RateLimiter)
- Embedded PostgreSQL or Docker Compose for database

---

## Open Questions (Deferred to Architecture Research)

1. **Build Tool:** Maven vs Gradle?
2. **Java Version:** Java 17 LTS or Java 21 LTS?
3. **Testing Strategy:** Integration test framework (TestContainers)?
4. **Rate Limiting:** Local (Guava) or Redis-based?
5. **Key Management:** File-based or HSM (for local)?
6. **API Documentation:** OpenAPI/Swagger integration?
7. **Logging Framework:** Logback vs Log4j2?
8. **Package Structure:** Monolith or modular structure?

---

## Next Step: Step 2.1 - Architecture Research

Research topics:
- Spring Boot 3.x + Spring Security 6.x best practices
- OAuth2/OIDC implementation patterns
- PostgreSQL schema design for auth
- JWT signing key management
- Rate limiting strategies for Spring Boot
- Testing strategies for Spring Security
