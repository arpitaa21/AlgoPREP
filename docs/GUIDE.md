# Placement Prep Portal: Build and Interview Guide

Read this next to the code. Every file path below exists in the repository.

---------------------------------------------------------------------

## 0. The plan in one page

| Phase | What you build | Where it lives |
|---|---|---|
| 1 Setup | Maven project, MySQL config, Vite app | `backend/pom.xml`, `application.properties`, `frontend/package.json` |
| 2 Entities + repositories | 20 JPA entities, 16 repositories | `entity/`, `repository/` |
| 3 Auth | Register, login, BCrypt, JWT, filter, roles | `security/`, `config/SecurityConfig`, `AuthService` |
| 4 Question bank | CRUD, filter by topic/difficulty/type/keyword, pagination | `QuestionService`, `QuestionSpecifications` |
| 5 Study tools | Bookmarks, solved tracking, revision lists | `BookmarkService`, `SolvedService`, `RevisionListService` |
| 6 Timed tests | Start, resume, server-side deadline, auto-score | `TestService` |
| 7 Coding records + stats | Submissions, JPQL + native SQL analytics, streak | `SubmissionService`, `StatsService` |
| 8 Company roadmaps | Company, ordered steps, link to topic | `CompanyService` |
| 9 Admin | Dashboard numbers, content CRUD, users | `AdminService`, `Admin*Controller` |
| 10 Recommender | Weak topics from wrong answers, targeted practice | `WeaknessCalculator`, `RecommenderService` |
| 11 Frontend | Auth flow, browser, timer, dashboard, admin | `frontend/src/` |
| 12 Quality | Unit, integration, security tests, smoke script | `src/test/`, `scripts/smoke-test.sh` |

Build order matters: entities first, because every later layer depends on them. Then security, because it
shapes every controller. Then features in order of dependency (questions, then tests, then the recommender
that reads test results).

---------------------------------------------------------------------

## 1. Architecture

```
 BROWSER                                  SERVER (Spring Boot, port 8080)                          DATABASE
┌──────────────────┐   HTTPS + JSON    ┌───────────────────────────────────────────────────┐     ┌──────────┐
│ React (Vite)     │ ───────────────►  │ SECURITY LAYER (filter chain)                     │     │          │
│ pages/components │  Authorization:   │  CORS ► JwtAuthFilter ► role rules (/api/admin    │     │  MySQL   │
│ axios + context  │  Bearer <JWT>     │  = ADMIN) ► 401 / 403 JSON handlers               │     │  8       │
└──────────────────┘                   │ ─────────────────────────────────────────────────  │     │          │
        ▲                              │ CONTROLLER layer   (HTTP only: parse, validate,   │     │ 18       │
        │  JSON (DTO records)          │                     call a service, return DTO)   │     │ tables   │
        │                              │ ─────────────────────────────────────────────────  │     │          │
        └───────────────────────────── │ SERVICE layer      (business rules, @Transactional│     │          │
                                       │                     scoring, timer, recommender)  │     │          │
                                       │ ─────────────────────────────────────────────────  │     │          │
                                       │ REPOSITORY layer   (Spring Data JPA interfaces,   │ ──► │          │
                                       │                     JPQL, native SQL, Specs)      │ ◄── │          │
                                       │ ─────────────────────────────────────────────────  │     │          │
                                       │ ENTITY layer       (JPA / Hibernate mappings)     │     └──────────┘
                                       │ GlobalExceptionHandler: every error -> one JSON   │
                                       └───────────────────────────────────────────────────┘
```

Rules that keep it clean: controllers never touch repositories; entities never leave the service layer
(DTO records go out); the security layer runs before any controller code.

### Backend packages (`com.placementportal`)

```
config/      SecurityConfig (filter chain, CORS, BCrypt), AppConfig (Clock bean), DataInitializer (seed data)
controller/  AuthController, QuestionController, BookmarkController, SolvedController, RevisionListController,
             TestController, SubmissionController, StatsController, RecommendationController, CompanyController,
             TopicController, Admin{,Topic,Question,Test,Company}Controller
dto/         request/response records grouped by module (AuthDtos, QuestionDtos, TestDtos, ...)
entity/      User, Role, Topic, Question, QuestionOption, TestSet, TestAttempt, Answer, CodingSubmission,
             Bookmark, SolvedQuestion, RevisionList, Company, CompanyRoadmap, UserTopicStat + enums
exception/   ResourceNotFound, BadRequest, Conflict, TestExpired, GlobalExceptionHandler
repository/  one interface per aggregate + QuestionSpecifications
security/    JwtService, JwtAuthFilter, UserPrincipal, CustomUserDetailsService, RestAuthHandlers
service/     business logic + WeaknessCalculator, StreakCalculator (pure logic classes)
```

### Frontend folders (`frontend/src`)

```
pages/       one file per screen (Login, Dashboard, Questions, TakeTest, ...); pages/admin/ for admin screens
components/  reusable pieces: Navbar, ProtectedRoute, QuestionTable, Pagination, DifficultyBadge, Loading, ErrorAlert
services/    api.js (axios instance + interceptors), endpoints.js (every backend call as a function)
context/     AuthContext (current user, login, logout, isAdmin)
utils/       format.js
App.jsx      the route table;  main.jsx  mounts Router + AuthProvider
```

---------------------------------------------------------------------

## 2. Data model

```
 roles ◄──┐
          │ N:M (user_roles)
        users ──1:N── test_attempts ──1:N── answers ──N:1── questions ──1:N── question_options
          │                │                                  │   ▲                  ▲
          │                └─N:1── test_sets ──N:M(test_set_questions)──┘   │        │ (answers.selected_option_id)
          ├──1:N── coding_submissions ──N:1── questions        │            │
          ├──1:N── bookmarks ──N:1── questions                 │            │
          ├──1:N── solved_questions ──N:1── questions          questions ──N:1── topics
          ├──1:N── revision_lists ──N:M(revision_list_questions)── questions         ▲
          └──1:N── user_topic_stats ──N:1── topics ──────────────────────────────────┤
 companies ──1:N── company_roadmaps ──N:1(optional)── topics ────────────────────────┘
```

| Relationship | Type | Why this shape |
|---|---|---|
| User ↔ Role | many-to-many (`user_roles`) | A user can hold several roles (the seeded admin is ADMIN + STUDENT). Roles are data, so adding MENTOR needs no schema change. |
| Question → Topic | many-to-one | Every question has exactly one topic; topic filtering and per-topic stats rely on this. |
| Question → QuestionOption | one-to-many, cascade + orphanRemoval | Options have no life without their question. `ON DELETE CASCADE` mirrors it in SQL. |
| TestSet ↔ Question | many-to-many (`test_set_questions`) | One question can be reused across tests. The join table needs no extra columns, so plain `@ManyToMany`. |
| User → TestAttempt → Answer | one-to-many chain | An attempt is one sitting; answers belong to it. Unique `(attempt_id, question_id)` blocks duplicates. |
| Bookmark, SolvedQuestion | **join entities** (not `@ManyToMany`) | The relationship itself carries data (`created_at`, `solved_at`) and needs a unique constraint `(user_id, question_id)` for idempotency. |
| RevisionList ↔ Question | many-to-many | A named list holds many questions, a question can sit in many lists. Pure join, no extra columns. |
| UserTopicStat | one row per (user, topic) | **Denormalised counters** (`attempted`, `correct`). The recommender reads a handful of rows instead of re-scanning every historical answer. |
| Company → CompanyRoadmap | one-to-many, cascade | Steps are ordered (`step_order`), owned by the company, and optionally point to a Topic so the UI can jump to practice. |

Full DDL: `db/schema.sql` (18 tables, FKs, unique keys, one composite index on `questions(topic_id, difficulty)`).
A script check confirmed every column the entities map exists in the DDL.

Design notes worth saying out loud:
* `Answer.correct` is **stored**, not recomputed, so a past result never changes if an admin edits the options later.
* `TestAttempt.deadline` is stored, so the timer is a fact in the database, not a number the browser reports.
* DSA problems are not hosted. The portal stores a link and your own approach notes, and you log results.
  Running untrusted code safely needs a sandbox and is out of scope (see limitations).

---------------------------------------------------------------------

## 3. Build phases: what to look at, pitfalls, how to test

### Phase 1: Setup
* Backend: Spring Initializr equivalent is `backend/pom.xml` (Web, Data JPA, Security, Validation, MySQL, JJWT, H2 + test starters).
* Config is in `application.properties`; every secret/URL has an environment override.
* **Pitfall: `open-in-view`**. It is turned off, so lazy loading outside a transaction fails loudly instead of silently
  firing queries while JSON is written. The fix is the habit used everywhere: map entity to DTO inside a `@Transactional` service method.
* Frontend: `npm create vite` equivalent is `frontend/package.json` + `vite.config.js` (dev server on 5173, the origin CORS allows).

Verify: `docker compose up -d`, `mvn spring-boot:run`, then `curl localhost:8080/api/topics` should answer 401 (the app is up and locked).

### Phase 2: Entities and repositories
* All relationships are `LAZY` except `User.roles` (tiny, needed on every request).
* **Pitfall: N+1.** Three defences are used: `@EntityGraph` on the paged question search, `join fetch` in detail queries,
  and `hibernate.default_batch_fetch_size=50` as a safety net. Bookmark/solved flags for a whole page come from two `IN (...)` queries, not two per row.
* **Pitfall: fetching two `List` collections** in one query breaks Hibernate, so detail queries fetch only `options`.

### Phase 3: Authentication and authorization
* `register` always creates a STUDENT. A test proves a client cannot inject `"roles":["ADMIN"]`.
* Passwords: **BCrypt**, slow and salted. Max length 72 because BCrypt ignores bytes beyond 72.
* `JwtService` signs `{sub: email, roles, iat, exp}` and `parseSignedClaims` verifies signature **and** expiry in one call.
* `JwtAuthFilter` reads `Authorization: Bearer ...`; invalid, tampered or expired tokens just leave the request unauthenticated, and the entry point answers **401**. Wrong role gets **403**.
* Login errors are deliberately vague ("Invalid email or password") so attackers cannot discover which emails exist.
* **Pitfall: CORS.** `SecurityConfig.corsConfigurationSource` allows only the configured origin and answers the browser's OPTIONS pre-flight.
* **Pitfall: JWT expiry.** The axios response interceptor clears the token and redirects to `/login?expired=1` on any 401, and `AuthContext` re-validates a stored token via `/auth/me` on page load.
* **Pitfall: CSRF.** Disabled on purpose: tokens travel in a header, not a cookie, so a hostile site cannot make the browser attach them.

```bash
curl -s -X POST localhost:8080/api/auth/register -H 'Content-Type: application/json' \
     -d '{"name":"Asha","email":"asha@x.com","password":"Password1!"}'
# 201 {"token":"eyJ...","tokenType":"Bearer","expiresInMs":3600000,"user":{"id":3,"name":"Asha","email":"asha@x.com","roles":["STUDENT"]}}
curl -s localhost:8080/api/auth/me -H "Authorization: Bearer $TOKEN"
```

### Phase 4: Question bank
`GET /api/questions?topicId=1&difficulty=EASY&type=DSA&keyword=sum&page=0&size=10`
* Dynamic filters use **JPA Specifications**: only filters that were sent become `WHERE` clauses.
* Page size is capped at 50 so a client cannot request the whole table.
* Student responses never contain `correct`; MCQ explanations are withheld from the detail view (they would reveal the answer).
* Admin create/update validates "MCQ needs 2 to 6 options and exactly one correct". Updates edit options **in place** so old answers keep valid foreign keys.

### Phase 5: Bookmarks, solved, revision lists
* `POST /api/bookmarks/{questionId}` is **idempotent** (second call is a no-op 204); the unique constraint guards the race.
* Revision lists are looked up by `(id AND userId)`, so another user's list simply looks "not found" (404), which avoids leaking that it exists.

### Phase 6: Timed test engine (the interesting one)
```
start:  find IN_PROGRESS attempt -> resume it | else create attempt with deadline = now + duration
submit: load attempt by (id, userId) -> reject if not IN_PROGRESS (409)
        -> if now > deadline + grace(10 s): mark EXPIRED, reject (409), score nothing
        -> else score on the SERVER, store Answers, update UserTopicStat, set SUBMITTED
```
* Client sends only `(questionId, optionId)`. It never says whether an answer is right.
* The page timer is a display; refresh-safe because `secondsRemaining` comes from the server on every `start` call.
* **Pitfall: rollback.** Marking EXPIRED then throwing would normally roll the status back, so `@Transactional(noRollbackFor = TestExpiredException.class)` keeps it.
* Skipped questions count as attempted-but-wrong for the weak-topic stats.
* `Clock` is injected so tests can say "11 seconds after the deadline" in one line.

### Phase 7: Coding submissions and statistics
* `POST /api/submissions` logs language, code, status, minutes. A `SOLVED` submission also marks the question solved.
* `StatsService` combines JPQL (`group by difficulty`), one **native SQL** query (`LEFT JOIN` + `COUNT(DISTINCT ...)` for per-topic progress), and Java for the 30-day activity chart and streak.
* Streak logic is a pure class (`StreakCalculator`): a streak stays alive if you were active yesterday.

### Phase 8-9: Company roadmaps and admin
Students read; admins write (`/api/admin/**` is `hasRole("ADMIN")` in one place, plus tests proving 403 for students).
Deleting content that students already used returns a clean **409** (foreign-key violation translated), instead of a stack trace.

### Phase 10: The recommender

```
accuracy      = correct / attempted
confidence    = min(attempted, 10) / 10            (few answers => less sure)
weaknessScore = (1 - accuracy) * confidence        (0 strong ... 1 very weak, confidently)

RULE 1  weak  <=>  attempted >= 3  AND  accuracy < 50 %
RULE 2  rank weak topics by weaknessScore, highest first, take the top 3
RULE 3  per weak topic: preferred difficulty = EASY if accuracy < 25 % else MEDIUM
RULE 4  suggest unsolved, not-yet-answered-correctly questions, closest difficulty to the preferred one first
```
Worked example: Arrays 2 correct of 10 -> accuracy 0.20, weak, score 0.8, difficulty EASY.
Graphs 0 of 2 -> too few attempts, ignored (not enough evidence). Practising an MCQ (`POST /api/questions/{id}/practice`) updates the same stats, so improvement is reflected.

Every recommendation carries a human-readable `reason` ("You answered 2 of 10 correctly in Arrays (20% accuracy)").

### Phase 11: Frontend
* `AuthContext` holds the user; `ProtectedRoute` redirects anonymous users (and non-admins from admin pages).
  **UI guards are convenience only; the backend enforces security.**
* `TakeTest.jsx` counts down against a fixed end time (immune to background-tab drift), auto-submits at zero using the latest answers, guards against double submit with a ref, and keeps answers in `sessionStorage` so a refresh does not lose them.

---------------------------------------------------------------------

## 4. REST API reference

Base `http://localhost:8080/api`. "Auth" = JWT required. Errors are always
`{"timestamp","status","error","message","fieldErrors":{...}|null}`.

| Method | Path | Role | Purpose / body → response |
|---|---|---|---|
| POST | /auth/register | public | `{name,email,password}` → 201 `{token,tokenType,expiresInMs,user}` (always STUDENT; 409 if email taken) |
| POST | /auth/login | public | `{email,password}` → 200 same shape; 401 on bad credentials |
| GET | /auth/me | any | → `{id,name,email,roles}` |
| GET | /topics | any | → `[{id,name,description}]` |
| GET | /questions | any | query `topicId,difficulty,type,keyword,page,size` → `{content:[{id,title,difficulty,type,topicId,topicName,bookmarked,solved}],page,size,totalElements,totalPages}` |
| GET | /questions/{id} | any | → detail with `options:[{id,text}]` (no answers) |
| POST | /questions/{id}/practice | any | `{optionId}` → `{correct,correctOptionId,explanation}`; updates weak-topic stats |
| GET / POST / DELETE | /bookmarks[/{questionId}] | any | list / add (idempotent, 204) / remove (204) |
| GET / POST / DELETE | /solved[/{questionId}] | any | list / mark (204) / unmark (204) |
| GET / POST | /revision-lists | any | list `[{id,name,questionCount}]` / create `{name}` → 201 (409 duplicate name) |
| GET / DELETE | /revision-lists/{id} | owner | detail with questions / delete |
| POST / DELETE | /revision-lists/{id}/questions/{qid} | owner | add / remove → updated detail |
| GET | /tests | any | active tests `[{id,title,description,durationMinutes,questionCount}]` |
| POST | /tests/{id}/start | any | → `{attemptId,deadline,secondsRemaining,questions:[{id,title,options:[{id,text}]}]}` (resumes an open attempt) |
| POST | /attempts/{id}/submit | owner | `{answers:[{questionId,optionId|null}]}` → `{summary:{score,totalQuestions,percentage,status},review:[...]}`; 409 if closed or late |
| GET | /attempts | any | own history |
| GET | /attempts/{id} | owner | summary + review (review only after SUBMITTED); 404 for other users |
| POST | /submissions | any | `{questionId,language,code,status,timeTakenMinutes}` → 201 (DSA only) |
| GET | /submissions?questionId= | any | own submissions |
| GET | /stats/me | any | solved counts by difficulty, per-topic progress, tests taken, average %, 30-day activity, streak |
| GET | /recommendations?limit= | any | `[{questionId,title,difficulty,topicName,reason}]` |
| GET | /recommendations/weak-topics | any | `[{topicId,topicName,attempted,correct,accuracy,weaknessScore}]` |
| GET | /companies, /companies/{id} | any | summaries / detail with ordered roadmap |
| GET | /admin/dashboard | ADMIN | totals, averages, questions by difficulty/type/topic |
| GET | /admin/users | ADMIN | all users with roles |
| POST / PUT / DELETE | /admin/topics[/{id}] | ADMIN | create / rename / delete (409 if in use) |
| GET / POST / PUT / DELETE | /admin/questions[/{id}] | ADMIN | full CRUD; responses include `correct` flags |
| GET / POST / PUT / DELETE | /admin/tests[/{id}] | ADMIN | `{title,description,durationMinutes,active,questionIds}` (MCQs only) |
| POST / PUT / DELETE | /admin/companies[/{id}] | ADMIN | company CRUD |
| POST | /admin/companies/{id}/steps | ADMIN | `{stepOrder,title,description,topicId?,resourceUrl?}` |
| PUT / DELETE | /admin/roadmap-steps/{id} | ADMIN | edit / remove a step |

A script check matched every URL the frontend calls to a real controller mapping (49 of 49).

---------------------------------------------------------------------

## 5. Workflows

**Registration / login**
```
React form ─POST /auth/register─► validate (@Valid) ─► email unique? ─► BCrypt hash ─► save User(role STUDENT)
          ◄─ 201 {token,user} ── JwtService.sign(email, roles, exp)
React stores token (localStorage) ─► axios adds "Authorization: Bearer" to every call
Next request ─► JwtAuthFilter verifies signature+expiry ─► SecurityContext ─► controller
Token expired ─► 401 ─► interceptor clears token ─► /login?expired=1
```

**Timed test**
```
Start    POST /tests/{id}/start ─► resume or create attempt (deadline stored) ─► questions WITHOUT answers
Answer   client keeps {questionId: optionId} in memory + sessionStorage (no server call per click)
Submit   POST /attempts/{id}/submit
           ├─ not yours / not found ......... 404
           ├─ already closed ................ 409
           ├─ now > deadline + 10 s ......... mark EXPIRED, 409, nothing scored
           └─ else: score server-side ─► save Answer rows ─► UserTopicStat.attempted++/correct++ per topic
                    ─► status SUBMITTED ─► return score + review
Then     GET /recommendations ─► WeaknessCalculator ranks weak topics ─► unsolved questions + reasons
```

**Solving a coding question**
```
Open question ─► solve on LeetCode (external link) ─► POST /submissions {SOLVED, Java, 25 min}
             ─► CodingSubmission saved ─► SolvedQuestion created if new ─► stats + streak + dashboard update
```

**Admin adds a question**
```
Admin form ─POST /admin/questions─► role check (ADMIN) ─► @Valid ─► MCQ rules (2-6 options, exactly 1 correct)
          ─► topic exists? ─► save Question (+options via cascade) ─► 201 with correct flags
Students immediately see it in GET /questions (without the answer flag)
```

---------------------------------------------------------------------

## 6. Interview preparation

### One request, end to end (say this in 60 seconds)
"A click in React calls a function in `endpoints.js`. Axios adds the JWT header. Spring's filter chain runs first:
CORS, then my `JwtAuthFilter` verifies the signature and expiry and loads the user. Role rules decide 401 or 403.
The controller validates the DTO and calls a service. The service runs inside a transaction, uses repositories
(Spring Data generates the SQL through Hibernate), applies business rules, and returns a DTO. Jackson writes JSON,
axios resolves the promise, and React state re-renders. Errors from anywhere become one JSON shape through a
`@RestControllerAdvice`."

### Key decisions and trade-offs
* **JWT vs sessions.** JWT: stateless, any server instance can verify it, no shared session store, simple for a SPA. Cost: you cannot revoke a token before it expires (mitigations: short expiry, refresh tokens, a deny-list). Sessions revoke instantly but need sticky sessions or a shared store and CSRF protection.
* **Where to keep the token.** `localStorage` is simple but readable by injected JavaScript (XSS). An `HttpOnly` cookie resists XSS but needs CSRF protection. I chose the header approach for simplicity and documented the trade-off.
* **Lazy by default.** Eager loading fetches data you did not ask for and multiplies queries. I fetch explicitly per use case (`join fetch`, entity graphs) and keep batch fetching as a net.
* **Server-side timer.** The browser clock can be changed or paused, so the deadline is computed and stored by the server and every submission is checked against it, plus a small grace period for network latency.
* **Denormalised `user_topic_stats`.** One read-optimised table for the recommender instead of aggregating all answers each time; the cost is keeping it updated in the same transaction as the answers.
* **Why rule-based before ML.** There is no training data yet, rules are explainable ("you got 2 of 10 right in Arrays"), testable with plain unit tests, and give a baseline to beat later. ML needs data volume, evaluation, and monitoring that a new product lacks.
* **DTO records, not entities, in the API.** Prevents accidental data leaks (like `correct`), avoids lazy-loading surprises during serialisation, and lets the DB and API evolve separately.
* **Specifications for filters.** Type-safe dynamic `WHERE` without 16 repository method variants.

### Likely questions (model answers)
1. **Why BCrypt?** Adaptive and salted: each hash embeds a random salt and a cost factor, so identical passwords hash differently and brute force is slow. MD5/SHA are fast, which is the wrong property for passwords.
2. **What does `@Transactional` do and why `readOnly`?** It wraps the method in one DB transaction (commit on success, rollback on a runtime exception). `readOnly` lets Hibernate skip dirty checking and signals the driver; it also documents intent.
3. **What is the N+1 problem and how did you avoid it?** One query for a list, then one per row for a lazy association. I used entity graphs, `join fetch`, a batch size, and `IN (...)` lookups for per-user flags.
4. **401 vs 403?** 401: not authenticated (no or bad token). 403: authenticated but not allowed (student calling an admin endpoint).
5. **How do you stop a student reading someone else's attempt?** The repository method is `findByIdAndUserId`, so ownership is part of the query; it returns 404 rather than 403 so existence is not revealed.
6. **How could a user cheat the timer?** Changing the browser clock only changes the display. Submission is validated against the stored deadline on the server; answers carry no correctness flag.
7. **What if the same user starts a test twice?** The open attempt is resumed with the remaining time, so refresh does not reset the timer.
8. **Why is `Bookmark` an entity and not `@ManyToMany`?** It carries a timestamp, and I want a unique constraint plus an idempotent API.
9. **How do you handle concurrent bookmark clicks?** Unique `(user_id, question_id)` in the database is the real guard; the pre-check is a friendly shortcut.
10. **Explain the weakness score.** Accuracy alone treats 0/1 and 0/10 equally; multiplying `(1 - accuracy)` by a confidence term (attempts capped at 10) ranks well-evidenced weakness first.
11. **Why CSRF off?** No cookie authentication, so the browser never auto-attaches credentials. If I moved the JWT into a cookie I would turn CSRF protection back on.
12. **What does `ddl-auto=update` do and why not in production?** It lets Hibernate change the schema from entities. In production use `none`/`validate` with reviewed migration scripts (Flyway/Liquibase).
13. **How would you test the timer?** Inject a `Clock`; the unit test fixes it to deadline + 11 s and asserts a `TestExpiredException`, no answers saved, status EXPIRED.

### Known limitations and next steps
| Limitation | Improvement |
|---|---|
| No code execution; submissions are self-reported | Sandboxed judge (containers, resource limits) or integrate an external judge API |
| JWT cannot be revoked early | Short-lived access token + refresh token with rotation and a revocation table |
| Rule thresholds are hand-picked | Log outcomes, tune with data; later try spaced repetition or a learned model with the rules as baseline |
| Stats counters could drift if code paths forget to update them | Nightly reconciliation job; or compute from answers with a materialised view |
| No rate limiting on login | Bucket4j or a gateway limit; lockout / captcha after repeated failures |
| Schema managed by `ddl-auto` in dev | Flyway migrations |
| Single instance, no caching | Cache topics and question counts (Caffeine/Redis); read replica; paginate everything |
| Auto-save of in-progress test answers is client-side only | Periodic `PATCH` of draft answers so a device switch keeps progress |
| Admin passwords/seed defaults | Force password change on first admin login |

---------------------------------------------------------------------

## 7. Testing and quality

| Test class | Kind | What it proves |
|---|---|---|
| `WeaknessCalculatorTest` | unit, no Spring | threshold boundaries (exactly 50% is not weak, 2 attempts is not enough), confidence cap, ranking, difficulty rule |
| `StreakCalculatorTest` | unit | consecutive days, yesterday keeps streak alive, gap breaks it |
| `TestServiceTest` | unit, Mockito + fixed Clock | scoring, skipped counts as wrong, stat updates per topic, grace-period boundary, late submit rejected and EXPIRED, double submit 409, foreign option/question rejected, resume keeps remaining time, no correct flag in start response |
| `RecommenderServiceTest` | unit, Mockito | only weak topics, solved questions excluded, difficulty ordering, limit respected, empty when strong |
| `PortalIntegrationTest` | `@SpringBootTest` + MockMvc + H2 | real security chain: 401 without/garbage token, 403 for student on admin endpoints, role injection ignored on register, duplicate email 409, validation 400 with field errors, wrong password 401; full flow admin builds test -> student takes it -> weak topic -> recommendation -> practice marks solved; bookmark idempotency, solved + stats + streak, MCQ validation |
| `scripts/smoke-test.sh` | black-box against the running server | same guarantees through real HTTP and MySQL |

Run: `cd backend && mvn test` (needs internet once to download dependencies).

---------------------------------------------------------------------

## 8. Verification status (honest)

Verified from the project root on 2026-11-09:

| Check | Result |
|---|---|
| `cd backend && mvn test` | passed: 39 tests, 0 failures/errors (unit tests plus Spring Boot + MockMvc + H2 integration tests) |
| `cd frontend && npm ci && npm run build` | passed: Vite production bundle generated |
| Live database startup and `scripts/smoke-test.sh` | not run: Docker is not installed in this environment, so MySQL was unavailable |
| npm dependency audit | install reported 6 advisories (1 moderate, 5 high); review with `npm audit` before deployment and upgrade deliberately |

The automated integration tests exercise the real Spring security chain and JPA mappings on H2, but this does
not replace one final startup and smoke test against MySQL 8. Start MySQL using the compose command in the
README, start the backend and frontend, then run `bash scripts/smoke-test.sh` from the project root. In
production, replace the sample admin credentials and JWT secret, use HTTPS, and review npm audit findings.
