# Spec: Remaining Mall Modules

## Objective
Complete the remaining requirement modules as a runnable course-project baseline: inventory, activities, members, advertisements, product lifecycle controls, access control, and four analysis workspaces.

## Tech Stack
Spring Boot 2.6, MyBatis-Plus/JdbcTemplate, MySQL 8, Redis, native HTML/CSS/JavaScript and ECharts.

## Commands
- Build: `.\.tools\apache-maven-3.9.11\bin\mvn.cmd -s .\.tools\maven-settings.xml package`
- Test: `.\.tools\apache-maven-3.9.11\bin\mvn.cmd -s .\.tools\maven-settings.xml test`
- Run: `java -jar target\mall-sys-1.0.jar`

## Project Structure
- `src/main/java/com/gec`: controllers, services and business models
- `src/main/resources/static`: native admin interface
- `db/migration`: repeatable local database changes and sample data
- `src/test/java/com/gec`: business-rule regression tests

## Code Style
Reuse the existing `R` response, session authentication, compact controllers and native browser controls. Keep state transitions explicit and transactional.

## Testing Strategy
Unit-test non-trivial status/quantity rules; run the full Maven suite; verify seeded MySQL queries and authenticated HTTP endpoints.

## Boundaries
- Always: validate quantities, dates and state changes; paginate lists; preserve supplied sample data.
- Ask first: external services, new frameworks, destructive data replacement.
- Never: fake payment/carrier calls, embed secrets, bypass authentication.

## Success Criteria
- Every module has a visible menu, usable list/filter UI and backed API/data.
- Stock adjustments generate flows and cannot make stock negative.
- Activity/member/ad status changes are persisted and validated.
- Four analysis pages use supplied or business sample data rather than static chart literals.
- Existing login, product publishing, order management and tests remain functional.

## Open Questions
None. The user approved the recommended non-enterprise implementation; external infrastructure is out of scope.
