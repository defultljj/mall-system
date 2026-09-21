# Capability Map: Mall Completion

| Module id | Responsibility | Depends on |
|---|---|---|
| product-lifecycle | SPU/SKU status, draft recovery, SKU stock entry | existing product publishing |
| inventory | Stock balance, warning and immutable stock flow | product-lifecycle, order |
| marketing | Activities and product discount configuration | product-lifecycle |
| membership | Member level, balance, points and status | order |
| advertisement | Timed placement, sorting and on/off status | existing upload infrastructure |
| analytics | Behavior, sales, review and service metrics from supplied samples | order, supplied datasets |
| access-control | Role-to-module grants enforced by backend and reflected in navigation | existing roles and sessions |

Build order: product-lifecycle → inventory → marketing, membership, advertisement → analytics → access-control.

Course-project boundary: use Spring Boot, MySQL, Redis and the existing native frontend. HDFS/Hive, payment gateways, carrier APIs and a separate Vue application are intentionally replaced by deterministic local aggregation and simulated business transitions.
