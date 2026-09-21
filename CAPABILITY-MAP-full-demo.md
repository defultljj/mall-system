# Capability Map: Unified Mall Demo

| Module id | Responsibility | Depends on |
|---|---|---|
| unified-master-data | One coherent catalog, staff, members, images and identifiers | — |
| transaction-linkage | Order state, stock, activity usage, member spend and points flows | unified-master-data |
| business-evidence | Behavior, reviews and service sessions tied to the same members, SKUs and orders | transaction-linkage |
| live-analytics | Dashboard and four analysis views derived only from business records | business-evidence |
| navigation-shell | Workflow-oriented, collapsible navigation preserving every existing page | live-analytics |
| verification | Cross-table reconciliation, API tests and browser smoke checks | all modules |

Build order: unified-master-data → transaction-linkage → business-evidence → live-analytics → navigation-shell → verification.

Boundary: this is a local course-project dataset. Payment, carrier and messaging events are deterministic simulations recorded in MySQL; no external providers are called.
