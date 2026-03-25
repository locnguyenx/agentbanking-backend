# BDD Specification: Product Configuration

**Version:** 1.1
**Date:** 2026-03-20
**Status:** Draft
**Module:** Product Configuration (`com.banking.product`)
**BRD Reference:** `docs/superpowers/brds/2026-03-20-product-configuration.md`

Each BDD scenario is tagged with exactly one `@US` (User Story) and one `@FR` (Functional Requirement) for atomic traceability.

---

## 1. Product CRUD & Versioning

```gherkin
Feature: Product Creation and Versioning

  @US-1 @FR-1.1
  Scenario: Create a new product as draft
    Given a Product Manager "alice" is authenticated
    When alice creates a product with code "BIZ-CURRENT", name "Business Current Account", family "ACCOUNT"
    Then the product is created with status "DRAFT" and version 1

  @US-1 @FR-1.3
  Scenario: Product code is immutable
    Given a product "BIZ-CURRENT" exists at version 1 with status "DRAFT"
    When alice attempts to change the product code to "BIZ-CURR-NEW"
    Then the update is rejected with error "Product code cannot be changed"

  @US-1 @FR-1.4
  Scenario: Duplicate product code is rejected
    Given a product "BIZ-CURRENT" exists
    When alice creates a product with code "BIZ-CURRENT"
    Then the creation is rejected with error "Product code already exists"

  @US-6 @FR-2.3
  Scenario: Edit a draft product
    Given a product "BIZ-CURRENT" exists at version 1 with status "DRAFT"
    When alice updates the product name to "Business Current Plus"
    Then version 1 is updated in place with the new name

  @US-6 @FR-2.2
  Scenario: Edit an active product creates a new version
    Given a product "BIZ-CURRENT" exists at version 1 with status "ACTIVE"
    When alice updates the product description
    Then version 2 is created with status "DRAFT"
    And version 1 remains with status "ACTIVE"

  @US-6 @FR-2.3
  Scenario: Cannot edit a pending-approval version
    Given a product "BIZ-CURRENT" exists at version 1 with status "PENDING_APPROVAL"
    When alice attempts to update version 1
    Then the update is rejected with error "Only DRAFT versions can be edited"

  @US-6 @FR-2.3
  Scenario: Cannot edit an approved version
    Given a product "BIZ-CURRENT" exists at version 1 with status "APPROVED"
    When alice attempts to update version 1
    Then the update is rejected with error "Only DRAFT versions can be edited"

  @US-6 @FR-2.3
  Scenario: Cannot edit a retired version
    Given a product "BIZ-CURRENT" exists at version 1 with status "RETIRED"
    When alice attempts to update version 1
    Then the update is rejected with error "Only DRAFT versions can be edited"

  @US-11 @FR-6.2
  Scenario: List products filtered by family
    Given products exist across families "ACCOUNT", "PAYMENT", "TRADE_FINANCE"
    When alice lists products filtered by family "ACCOUNT"
    Then only ACCOUNT family products are returned

  @US-11 @FR-6.2
  Scenario: List products filtered by status
    Given products exist with statuses "DRAFT", "ACTIVE", "RETIRED"
    When alice lists products filtered by status "ACTIVE"
    Then only products with ACTIVE status are returned

  @US-11 @FR-6.2
  Scenario: List products filtered by customer segment
    Given products exist targeting different customer segments
    When alice lists products filtered by customer segment "SME"
    Then only products available to SME customers are returned

  @US-11 @FR-6.2
  Scenario: List products with combined filters
    Given products exist across families, statuses, and segments
    When alice lists products filtered by family "ACCOUNT", status "ACTIVE", segment "CORPORATE"
    Then only ACCOUNT family products with ACTIVE status for CORPORATE are returned

  @US-5 @FR-2.1
  Scenario: View product version history
    Given a product "BIZ-CURRENT" has versions 1 "SUPERSEDED", 2 "SUPERSEDED", 3 "ACTIVE"
    When alice views the version history of "BIZ-CURRENT"
    Then all 3 versions are returned in chronological order with their statuses

  @US-8 @FR-2.1
  Scenario: Compare two product versions
    Given a product "BIZ-CURRENT" has version 1 "ACTIVE" and version 2 "PENDING_APPROVAL"
    When bob compares version 1 and version 2
    Then the differences in features, pricing, and segments are displayed
```

---

## 2. Product Lifecycle (State Machine)

```gherkin
Feature: Product Lifecycle Management

  @US-4 @FR-7.1
  Scenario: Submit product for approval
    Given a product "BIZ-CURRENT" exists at version 1 with status "DRAFT"
    When alice submits the product for approval
    Then version 1 status changes to "PENDING_APPROVAL"

  @US-7 @FR-3.2
  Scenario: Approve a pending product
    Given a product "BIZ-CURRENT" exists at version 1 with status "PENDING_APPROVAL"
    When approver "bob" approves the product
    Then version 1 status changes to "APPROVED"

  @US-7 @FR-3.2
  Scenario: Reject a pending product
    Given a product "BIZ-CURRENT" exists at version 1 with status "PENDING_APPROVAL"
    When bob rejects the product with comment "Missing fee schedule"
    Then version 1 status changes to "DRAFT"

  @US-7 @FR-3.3
  Scenario: Rejection requires a comment
    Given a product "BIZ-CURRENT" exists at version 1 with status "PENDING_APPROVAL"
    When bob rejects the product without a comment
    Then the rejection is rejected with error "Rejection comment is required"

  @US-9 @FR-3.4
  Scenario: Activate an approved product
    Given a product "BIZ-CURRENT" exists at version 1 with status "APPROVED"
    When admin "charlie" activates the product
    Then version 1 status changes to "ACTIVE"

  @US-9 @FR-3.5
  Scenario: Activating new version supersedes previous
    Given a product "BIZ-CURRENT" with version 1 "ACTIVE" and version 2 "APPROVED"
    When charlie activates version 2
    Then version 2 status changes to "ACTIVE"
    And version 1 status changes to "SUPERSEDED"

  @US-9 @FR-3.7
  Scenario: Cannot activate a non-approved version
    Given a product "BIZ-CURRENT" exists at version 1 with status "DRAFT"
    When charlie attempts to activate version 1
    Then the activation is rejected with error "Only APPROVED versions can be activated"

  @US-10 @FR-3.6
  Scenario: Retire an active product version
    Given a product "BIZ-CURRENT" exists at version 1 with status "ACTIVE"
    And no live contracts reference version 1
    When charlie retires version 1
    Then version 1 status changes to "RETIRED"

  @US-10 @FR-3.6
  Scenario: Cannot retire a product version with live contracts
    Given a product "BIZ-CURRENT" exists at version 1 with status "ACTIVE"
    And 5 active accounts reference version 1
    When charlie attempts to retire version 1
    Then the retirement is rejected with error "Cannot retire product version with active contracts"
```

---

## 3. Maker-Checker Separation

```gherkin
Feature: Maker-Checker Enforcement

  @US-4 @FR-7.3
  Scenario: Same user cannot submit and approve
    Given a product "BIZ-CURRENT" exists at version 1 with status "PENDING_APPROVAL"
    And alice submitted the product for approval
    When alice attempts to approve the product
    Then the approval is rejected with error "Maker cannot approve their own product"

  @US-7 @FR-7.2
  Scenario: Different user can approve
    Given a product "BIZ-CURRENT" exists at version 1 with status "PENDING_APPROVAL"
    And alice submitted the product for approval
    When bob approves the product
    Then version 1 status changes to "APPROVED"
```

---

## 4. Feature Toggles

```gherkin
Feature: Product Feature Toggles

  @US-2 @FR-4.1
  Scenario: Add features to a draft product version
    Given a product "BIZ-CURRENT" exists at version 1 with status "DRAFT"
    When alice adds features:
      | key                        | value |
      | account.overdraft_enabled  | true  |
      | account.cheque_book        | false |
      | account.standing_orders    | true  |
    Then version 1 has 3 features configured

  @US-2 @FR-4.1
  Scenario: Toggle a feature value
    Given a product "BIZ-CURRENT" version 1 has feature "account.cheque_book" set to "false"
    And version 1 status is "DRAFT"
    When alice updates feature "account.cheque_book" to "true"
    Then feature "account.cheque_book" is "true"

  @US-2 @FR-4.3
  Scenario: Invalid feature prefix is rejected
    Given a product "BIZ-CURRENT" with family "ACCOUNT"
    When alice attempts to add feature "payment.instant_transfer" to the product
    Then the update is rejected with error "Feature prefix must match product family"

  @US-2 @FR-2.3
  Scenario: Cannot modify features on non-draft version
    Given a product "BIZ-CURRENT" exists at version 1 with status "ACTIVE"
    When alice attempts to add a feature to version 1
    Then the update is rejected with error "Only DRAFT versions can be edited"

  @US-6 @FR-2.5
  Scenario: Features are preserved across versions
    Given a product "BIZ-CURRENT" version 1 is "ACTIVE" with 3 features
    When alice edits the product creating version 2 as "DRAFT"
    Then version 2 has the same 3 features copied from version 1
```

---

## 5. Tiered Pricing

```gherkin
Feature: Product Pricing Configuration

  @US-3 @FR-5.2
  Scenario: Configure flat fee
    Given a product "BIZ-CURRENT" exists at version 1 with status "DRAFT"
    When alice adds a fee entry with type "MONTHLY_MAINTENANCE", method "FLAT", amount "25.00", currency "USD"
    Then version 1 has 1 fee entry with flat amount 25.00 USD

  @US-3 @FR-5.2
  Scenario: Configure percentage-based fee
    Given a product "INTL-WIRE" exists at version 1 with status "DRAFT"
    When alice adds a fee entry with type "TRANSFER_FEE", method "PERCENTAGE", rate "0.15", currency "USD"
    Then version 1 has 1 fee entry with percentage rate 0.15%

  @US-3 @FR-5.3
  Scenario: Configure tiered volume pricing
    Given a product "BIZ-CURRENT" exists at version 1 with status "DRAFT"
    When alice adds a fee entry with type "TRANSACTION_FEE", method "TIERED_VOLUME", currency "USD" and tiers:
      | from | to  | rate |
      | 0    | 100 | 0.00 |
      | 101  | 500 | 0.50 |
      | 501  |     | 0.25 |
    Then version 1 has 1 fee entry with 3 tiers

  @US-3 @FR-5.4
  Scenario: Add multiple fee entries to a product
    Given a product "BIZ-CURRENT" exists at version 1 with status "DRAFT"
    When alice adds fee entries:
      | type               | method | amount | currency |
      | MONTHLY_MAINTENANCE| FLAT   | 25.00  | USD      |
      | TRANSACTION_FEE    | TIERED |        | USD      |
      | OVERDRAFT_FEE      | FLAT   | 35.00  | USD      |
    Then version 1 has 3 fee entries

  @US-3 @FR-2.3
  Scenario: Cannot modify pricing on non-draft version
    Given a product "BIZ-CURRENT" exists at version 1 with status "ACTIVE"
    When alice attempts to add a fee entry to version 1
    Then the update is rejected with error "Only DRAFT versions can be edited"

  @US-6 @FR-2.5
  Scenario: Fee entries are preserved across versions
    Given a product "BIZ-CURRENT" version 1 is "ACTIVE" with 3 fee entries
    When alice edits the product creating version 2 as "DRAFT"
    Then version 2 has the same 3 fee entries copied from version 1
```

---

## 6. Customer Segment Targeting

```gherkin
Feature: Customer Segment Assignment

  @US-1 @FR-6.1
  Scenario: Assign customer segments to a product
    Given a product "BIZ-CURRENT" exists at version 1 with status "DRAFT"
    When alice assigns customer types "CORPORATE, SME" to version 1
    Then version 1 is available to customer types "CORPORATE" and "SME"

  @US-11 @FR-6.2
  Scenario: Query products by customer type
    Given products exist with segments:
      | product        | segments          |
      | BIZ-CURRENT    | CORPORATE, SME    |
      | PERSONAL-SAVER | INDIVIDUAL        |
      | GOVT-ACCOUNT   | CORPORATE         |
    When the system queries products for customer type "CORPORATE"
    Then products "BIZ-CURRENT" and "GOVT-ACCOUNT" are returned

  @US-11 @FR-8.2
  Scenario: Query active products only by segment
    Given a product "BIZ-CURRENT" version 1 is "ACTIVE" for "CORPORATE"
    And a product "BIZ-SAVINGS" version 1 is "DRAFT" for "CORPORATE"
    When the system queries active products for customer type "CORPORATE"
    Then only "BIZ-CURRENT" is returned
```

---

## 7. Downstream Integration

```gherkin
Feature: Downstream Module Product Access

  @US-13 @FR-8.4
  Scenario: Account opening binds to active product version
    Given a product "BIZ-CURRENT" version 1 is "ACTIVE"
    When an account is opened for product "BIZ-CURRENT"
    Then the account is linked to product version 1

  @US-14 @FR-8.4
  Scenario: Account displays product name not version
    Given an account is linked to product "BIZ-CURRENT" version 1
    When a bank user views the account
    Then the account displays product name "Business Current Account"

  @US-15 @FR-8.1
  Scenario: Query active product by code for business use
    Given a product "BIZ-CURRENT" version 1 is "ACTIVE"
    When the Account module queries product by code "BIZ-CURRENT"
    Then version 1 details are returned

  @US-15 @FR-8.3
  Scenario: Retrieve product version for an existing account
    Given an account is linked to product "BIZ-CURRENT" version 1
    And version 1 has been superseded by version 2
    When the system resolves the product for the account
    Then version 1 features and pricing are returned (not version 2)

  @US-15 @FR-8.1
  Scenario: Query product by code and version
    Given a product "BIZ-CURRENT" has versions 1 "SUPERSEDED" and 2 "ACTIVE"
    When the system queries product "BIZ-CURRENT" version 1
    Then version 1 details are returned even though it is superseded

  @US-16 @FR-8.5
  Scenario: Superseded version remains queryable
    Given a product "BIZ-CURRENT" version 1 is "SUPERSEDED"
    When the system queries version 1 by versionId
    Then version 1 features and pricing are returned
    And version 1 status is "SUPERSEDED"

  @US-17 @FR-8.1
  Scenario: Payment module queries product by family
    Given products exist across families "ACCOUNT", "PAYMENT", "TRADE_FINANCE"
    When the Payment module queries products for family "PAYMENT"
    Then only PAYMENT family products are returned

  @US-18 @FR-8.2
  Scenario: Trade Finance module queries active products by family
    Given TRADE_FINANCE products exist with statuses "ACTIVE", "DRAFT", "RETIRED"
    When the Trade Finance module queries active products for family "TRADE_FINANCE"
    Then only TRADE_FINANCE products with status "ACTIVE" are returned
```

---

## 8. Product Families

```gherkin
Feature: Multi-Family Product Support

  @US-1 @FR-1.2
  Scenario: Create an Account family product
    Given a Product Manager "alice" is authenticated
    When alice creates a product with code "BIZ-CURRENT", family "ACCOUNT"
    Then the product is created with family "ACCOUNT"

  @US-1 @FR-1.2
  Scenario: Create a Payment family product
    Given a Product Manager "alice" is authenticated
    When alice creates a product with code "DOM-WIRE", family "PAYMENT"
    Then the product is created with family "PAYMENT"

  @US-1 @FR-1.2
  Scenario: Create a Trade Finance family product
    Given a Product Manager "alice" is authenticated
    When alice creates a product with code "IMPORT-LC", family "TRADE_FINANCE"
    Then the product is created with family "TRADE_FINANCE"

  @US-1 @FR-1.2
  Scenario: Invalid family is rejected
    Given a Product Manager "alice" is authenticated
    When alice creates a product with family "INSURANCE"
    Then the creation is rejected with error "Invalid product family"

  @US-2 @FR-4.2
  Scenario: Features are namespaced per family
    Given a product "BIZ-CURRENT" with family "ACCOUNT"
    When alice adds features to the product
    Then feature keys use "account." prefix (e.g., "account.overdraft_enabled")
```

---

## 9. Audit Trail

```gherkin
Feature: Product Audit Trail

  @US-19 @FR-7.4
  Scenario: Product creation is audited
    Given a Product Manager "alice" is authenticated
    When alice creates a product "BIZ-CURRENT"
    Then an audit record is created with action "CREATE", actor "alice", timestamp

  @US-19 @FR-7.4
  Scenario: Status transitions are audited
    Given a product "BIZ-CURRENT" version 1 with status "DRAFT"
    When alice submits for approval
    Then an audit record is created with action "SUBMIT", from "DRAFT", to "PENDING_APPROVAL", actor "alice"

  @US-19 @FR-7.4
  Scenario: Approval is audited with maker-checker attribution
    Given a product "BIZ-CURRENT" version 1 with status "PENDING_APPROVAL"
    And alice is the maker (creator)
    When bob approves the product
    Then an audit record is created with action "APPROVE", actor "bob", maker "alice"

  @US-7 @FR-3.3
  Scenario: Rejection is audited with comment
    Given a product "BIZ-CURRENT" version 1 with status "PENDING_APPROVAL"
    When bob rejects with comment "Missing fee schedule"
    Then an audit record is created with action "REJECT", actor "bob", comment "Missing fee schedule"

  @US-19 @NFR-2.3
  Scenario: Audit records are immutable
    Given an audit record exists for product "BIZ-CURRENT"
    When any user attempts to modify or delete the audit record
    Then the operation is rejected
```

---

## 10. Traceability Matrix

### User Story → BDD Scenario Coverage

| User Story | FR(s) | BDD Scenario(s) |
|------------|-------|-----------------|
| US-1 | FR-1.1, FR-1.3, FR-1.4, FR-6.1 | S1.1, S1.2, S1.3, S6.1, S8.1, S8.2, S8.3, S8.4, S8.5 |
| US-2 | FR-4.1, FR-4.2, FR-4.3 | S4.1, S4.2, S4.3, S4.4, S8.5 |
| US-3 | FR-5.2, FR-5.3, FR-5.4 | S5.1, S5.2, S5.3, S5.4, S5.5 |
| US-4 | FR-3.1, FR-7.1, FR-7.3 | S2.1, S3.1 |
| US-5 | FR-2.1 | S1.13 |
| US-6 | FR-2.2, FR-2.3, FR-2.5 | S1.4, S1.5, S1.6, S1.7, S1.8, S4.5, S5.6 |
| US-7 | FR-3.2, FR-3.3, FR-7.2 | S2.2, S2.3, S2.4, S3.2, S9.4 |
| US-8 | FR-2.1 | S1.14 |
| US-9 | FR-3.4, FR-3.5, FR-3.7 | S2.5, S2.6, S2.7 |
| US-10 | FR-3.6 | S2.8, S2.9 |
| US-11 | FR-6.2, FR-8.2 | S1.9, S1.10, S1.11, S1.12, S6.2, S6.3 |
| US-12 | FR-2.1 | S7.6 |
| US-13 | FR-2.4, FR-8.4 | S7.1 |
| US-14 | FR-8.4 | S7.2 |
| US-15 | FR-8.1, FR-8.3 | S7.3, S7.4, S7.5 |
| US-16 | FR-8.3, FR-8.5 | S7.6 |
| US-17 | FR-8.1 | S7.7 |
| US-18 | FR-8.1 | S7.8 |
| US-19 | FR-7.4, NFR-2.3 | S9.1, S9.2, S9.3, S9.5 |

### Requirement Coverage Summary

| Requirement | Covered By Scenario(s) |
|-------------|----------------------|
| FR-1.1 | S1.1 |
| FR-1.2 | S8.1, S8.2, S8.3, S8.4 |
| FR-1.3 | S1.2 |
| FR-1.4 | S1.3 |
| FR-2.1 | S1.13, S1.14 |
| FR-2.2 | S1.5 |
| FR-2.3 | S1.4, S1.6, S1.7, S1.8, S4.4, S5.5 |
| FR-2.4 | S7.1 |
| FR-2.5 | S4.5, S5.6 |
| FR-3.1 | S2.1 (state transition covered by submit action) |
| FR-3.2 | S2.2, S2.3 |
| FR-3.3 | S2.4, S9.4 |
| FR-3.4 | S2.5 |
| FR-3.5 | S2.6 |
| FR-3.6 | S2.8, S2.9 |
| FR-3.7 | S2.5, S2.7 |
| FR-4.1 | S4.1, S4.2 |
| FR-4.2 | S8.5 |
| FR-4.3 | S4.3 |
| FR-5.2 | S5.1, S5.2 |
| FR-5.3 | S5.3 |
| FR-5.4 | S5.4 |
| FR-6.1 | S6.1 |
| FR-6.2 | S1.9, S1.10, S1.11, S1.12, S6.2, S6.3 |
| FR-7.1 | S2.1 |
| FR-7.2 | S3.2 |
| FR-7.3 | S3.1 |
| FR-7.4 | S9.1, S9.2, S9.3 |
| FR-8.1 | S7.3, S7.5, S7.7, S7.8 |
| FR-8.2 | S6.3, S7.8 |
| FR-8.3 | S7.4, S7.5 |
| FR-8.4 | S7.1, S7.2 |
| FR-8.5 | S7.6 |
| NFR-2.3 | S9.5 |
