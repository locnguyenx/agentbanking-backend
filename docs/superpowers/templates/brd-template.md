# Business Requirements Document: Product Configuration

**Version:** 1.0
**Date:** 2026-03-20
**Status:** Draft
**Module:** Product Configuration (`com.banking.product`)

---

## 1. Business Goals & Success Criteria

### Business Goals

1. Replace hardcoded product references (`productId: Long`) with a proper configurable product catalog
2. Enable bank admins to launch and modify banking products without code deployments
3. Support multiple product families (Accounts, Payments, Trade Finance) from a single configuration module
4. Enforce maker-checker approval for audit compliance
5. Allow targeted product offerings to different customer segments (Corporate, SME, Individual)
6. Support complex tiered pricing common in corporate banking

### Success Criteria

- Admin can create, approve, and activate a new product in < 10 minutes
- Product changes require no code deployment
- All product lifecycle changes are audit-logged with maker/checker attribution
- Downstream modules can query product config via a clean read interface
- Existing Account functionality continues working (backward-compatible migration)

---

## 2. User Roles & Stories

### Roles

| Role | Responsibilities |
|------|-----------------|
| **Product Manager (Maker)** | Creates/edits products, defines features & pricing, submits for approval |
| **Product Approver (Checker)** | Reviews pending products, approves or rejects with comments |
| **Bank Admin** | Activates approved products, retires products, assigns customer segments |

### User Stories

#### Product Manager (Maker)

- **US-1** As a Product Manager, I can create a new product with name, code, family, description, and target customer segments
- **US-2** As a Product Manager, I can define features and toggles for a product (e.g., "overdraft enabled", "cheque book available")
- **US-3** As a Product Manager, I can configure tiered pricing rules with volume-based fee schedules
- **US-4** As a Product Manager, I can submit a draft product for approval
- **US-5** As a Product Manager, I can view the version history of a product
- **US-6** As a Product Manager, when I edit a retired or active product, the system creates a new draft version (original remains unchanged)

#### Product Approver (Checker)

- **US-7** As a Product Approver, I can review product details and approve or reject with comments
- **US-8** As a Product Approver, I can compare two versions of a product before approving

#### Bank Admin

- **US-9** As a Bank Admin, I can activate an approved product to make it available for use
- **US-10** As a Bank Admin, I can retire an active product (blocked if live contracts exist)
- **US-11** As a Bank Admin, I can view all products filtered by family, status, or customer segment
- **US-12** As a Bank Admin, I can see which version of a product is active vs. superseded

#### Downstream Modules

- **US-13** As the Account module, when opening an account, I link it to a product. The system internally binds it to the active version of that product at time of opening
- **US-14** As a bank user viewing an account, I see the product name (e.g., "Business Current Account") — the version is internal plumbing
- **US-15** As the system, I can retrieve the exact product version tied to any contract/transaction to apply the correct features and pricing
- **US-16** As the system, if a product version is superseded, I can still resolve its features and pricing for existing contracts
- **US-17** As the Payment module, I can link a payment transaction to the product version that governed its fee calculation
- **US-18** As the Trade Finance module, I can link an LC or guarantee to the product version that was active when the contract was originated
- **US-19** As a compliance/audit user, I can trace which product version governed a historical transaction

### User Story to Functional Requirement Mapping

| User Story | Functional Requirements |
|------------|------------------------|
| US-1 | FR-1.1, FR-1.2, FR-1.3, FR-1.4, FR-6.1 |
| US-2 | FR-4.1, FR-4.2, FR-4.3 |
| US-3 | FR-5.1, FR-5.2, FR-5.3, FR-5.4 |
| US-4 | FR-3.1, FR-7.1 |
| US-5 | FR-2.1 |
| US-6 | FR-2.2, FR-2.5 |
| US-7 | FR-3.2, FR-3.3, FR-7.2 |
| US-8 | FR-2.1 |
| US-9 | FR-3.4, FR-3.5, FR-3.7 |
| US-10 | FR-3.6 |
| US-11 | FR-6.2 |
| US-12 | FR-2.1 |
| US-13 | FR-2.4, FR-8.4 |
| US-14 | FR-8.4 |
| US-15 | FR-8.1, FR-8.3 |
| US-16 | FR-8.3, FR-8.5 |
| US-17 | FR-8.1 |
| US-18 | FR-8.1 |
| US-19 | FR-7.4, FR-8.3 |

---

## 3. Functional Requirements

### FR-1: Product Catalog

- **FR-1.1** Products have: code (unique, immutable), name, description, family (ACCOUNT, PAYMENT, TRADE_FINANCE), status, and target customer types
- **FR-1.2** Each product belongs to exactly one family
- **FR-1.3** Product codes are immutable once created
- **FR-1.4** Product codes are globally unique

### FR-2: Product Versioning

- **FR-2.1** Each product maintains a version history (v1, v2, v3...)
- **FR-2.2** Editing a non-Draft product creates a new Draft version; existing versions remain untouched
- **FR-2.3** Only Draft versions are editable (features, pricing, segments)
- **FR-2.4** Contracts/transactions bind to the specific version that was Active at time of origination
- **FR-2.5** Features and pricing are copied to new draft versions when editing

### FR-3: Product Lifecycle (State Machine)

```
DRAFT → PENDING_APPROVAL → APPROVED → ACTIVE → RETIRED
              ↓ (reject)
            DRAFT
```

- **FR-3.1** Maker creates/edits → submits to PENDING_APPROVAL
- **FR-3.2** Checker approves → APPROVED, or rejects → back to DRAFT
- **FR-3.3** Rejection requires a mandatory comment
- **FR-3.4** Admin activates → ACTIVE (only one version active per product at a time)
- **FR-3.5** Activating a new version supersedes the previous active version
- **FR-3.6** Admin retires → RETIRED (blocked if live contracts reference this version)
- **FR-3.7** Only APPROVED versions can be activated

### FR-4: Feature Toggles

- **FR-4.1** Each product version has a set of boolean/string features (e.g., `overdraft_enabled: true`, `cheque_book: false`)
- **FR-4.2** Feature keys are namespaced per family (e.g., `account.overdraft_enabled`)
- **FR-4.3** Feature prefix must match the product family

### FR-5: Tiered Pricing

- **FR-5.1** Each product version has a fee schedule with multiple fee entries
- **FR-5.2** Each fee entry has: fee type, currency, calculation method (FLAT, PERCENTAGE, TIERED_VOLUME), and tier definitions
- **FR-5.3** Tiered volume: define breakpoints (e.g., 0-100 free, 101-500 at $0.50, 501+ at $0.25)
- **FR-5.4** Multiple fee entries per product version

### FR-6: Customer Segment Targeting

- **FR-6.1** Each product version lists eligible customer types (CORPORATE, SME, INDIVIDUAL)
- **FR-6.2** Products can be queried by customer type
- **FR-6.3** Data model designed for future extensibility to rules-based eligibility

### FR-7: Maker-Checker

- **FR-7.1** Maker role: create, edit Draft products, submit for approval
- **FR-7.2** Checker role: approve or reject with mandatory comment on rejection
- **FR-7.3** Same user cannot both submit and approve a product
- **FR-7.4** Separate audit trail entries for each action

### FR-8: Downstream Integration

- **FR-8.1** Other modules query products via a read service: by code, by code+version, by family, by customer type
- **FR-8.2** Only ACTIVE products are returned for business use
- **FR-8.3** Historical lookups by versionId always work regardless of current status
- **FR-8.4** Account opening binds to the active product version at time of opening
- **FR-8.5** Superseded versions remain fully queryable for existing contracts

---

## 4. Entity Definitions

BDD scenarios reference these entities by name and field. All entities use UUID primary keys unless noted otherwise.

### FR-1: Product Catalog
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| productId | UUID | Yes | Unique agent identifier |
| productCode | String(20) | Yes | Human-readable agent code (e.g., "AGT-00123") |
| productName | String(200) | Yes | Registered business name |


### FR-2: Product Versioning
| Field | Type | Required | Description |
|-------|------|----------|-------------|

---

## 5. Non-Functional Requirements

### NFR-1: Performance

- Product lookup by code: < 50ms
- Product list with filters: < 200ms (paginated)
- System supports 1,000+ products across all families

### NFR-2: Audit & Compliance

- All product lifecycle changes logged with: actor, timestamp, action, before/after values
- Maker-checker separation enforced at API level (same user cannot both submit and approve)
- Audit records are immutable

### NFR-3: Data Integrity

- Product codes are globally unique
- Only one ACTIVE version per product at any time
- Retiring a product version is blocked if live contracts reference it
- Optimistic locking on product versions to prevent concurrent edits

### NFR-4: Backward Compatibility

- Existing `productId` references on `Account` entity must be migrated to point to the new Product table
- Existing demo data (hardcoded productId 1-4) must map to seeded product records
- No breaking changes to existing Account API contracts

### NFR-5: Extensibility

- New product families can be added without schema changes (family is an enum, features/pricing are generic)
- Customer eligibility model designed to evolve from type-based to rules-based without data migration

---

## 6. Constraints & Assumptions

### Constraints

- Must follow existing modular monolith architecture — no microservices
- Must use existing tech stack: Java 17, Spring Boot 3.2, PostgreSQL, Flyway, React 18, Ant Design, Redux Toolkit
- Must follow existing code conventions: entity patterns, service patterns, controller patterns, frontend patterns as documented in AGENTS.md
- Module package: `com.banking.product`
- Database migrations via Flyway (V2 or later)
- Maker-checker roles assumed to exist in a future auth module — for now, roles are passed as request parameters or headers

### Assumptions

- No external product catalog system (e.g., Temenos, Finacle) to integrate with — this is a built-in catalog
- Product families are a closed enum (ACCOUNT, PAYMENT, TRADE_FINANCE) for now; new families added via code change
- Currency support uses the existing `Currency` enum from the Account module (USD, EUR, GBP, SGD, JPY, CAD, AUD, CHF)
- Frontend admin UI is part of the same React app (new sidebar entry: "Products")
- Product data volume is modest (< 10,000 products including all versions) — no special partitioning needed
