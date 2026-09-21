# Spec: Unified Mall Demo Data and Navigation

## Objective
Replace disconnected test rows with a realistic, internally consistent mall dataset and make every visible workflow use it. Reorganize the navigation around daily mall operations without removing existing capabilities.

## Tech Stack
Spring Boot 2.6, MyBatis-Plus/JdbcTemplate, MySQL 8, Redis, native HTML/CSS/JavaScript and ECharts.

## Commands
- Backup: `mysqldump -uroot -p --single-transaction --result-file=db/backup.sql mall_sys`
- Import: `mysql -uroot -p --default-character-set=utf8mb4 mall_sys < db/mall_sys_full_demo.sql`
- Test: `.\.tools\apache-maven-3.9.11\bin\mvn.cmd -s .\.tools\maven-settings.xml test`
- Package: `.\.tools\apache-maven-3.9.11\bin\mvn.cmd -s .\.tools\maven-settings.xml package`

## Project Structure
- `db/mall_sys_full_demo.sql`: repeatable full demonstration dataset
- `src/main/java/com/gec`: transactional linkage and live statistics
- `src/main/resources/static`: reordered navigation and operational views
- `src/test/java/com/gec`: state-transition and reconciliation checks

## Data Conventions
- `tbl_user` contains backend employees; `tbl_member` contains shoppers.
- Customer id `Uxxxxx` is identical in members, orders, behavior, reviews and service sessions.
- `tbl_sales_order.product_id` stores the SKU code so orders, inventory, behavior and reviews share one product key.
- Money uses two decimals. Timestamps form valid business timelines and stay within 2026.
- Dashboard and analysis values are queried from business tables; no chart literal is a business metric.

## Testing Strategy
Reconcile product ids, stock balances, member spend, points, reviews and order timelines with SQL assertions. Run Java tests, authenticated endpoint smoke tests and navigation/browser checks.

## Boundaries
- Always: back up before replacement; keep identifiers consistent; preserve admin login; reject negative stock and illegal state transitions.
- Ask first: external services, new frameworks or production personal data.
- Never: use real personal information, silently drop the backup, or retain unrelated analytics samples.

## Success Criteria
- Every menu page has coherent records and at least one meaningful state/action.
- Every SKU in an order exists in product and inventory data.
- Completed-order totals reconcile with member spend and sales analysis.
- Stock and points mutations create flows and remain consistent after legal order transitions.
- Reviews reference completed orders; behavior/service records reference known members and products.
- Dashboard and all four analysis tabs derive from the same transactional dataset.
- Sidebar exposes all pages through workflow-oriented collapsible groups and defaults to the dashboard.

## Open Questions
None. The user approved employee/member separation, full coherent replacement and the proposed navigation structure.
