## API Contract Enforcement

**OpenAPI 3.0 Specification** is the single source of truth for all REST APIs.

### Rules
- **External API:** All backend REST endpoints exposed via Gateway MUST be documented in `docs/api/openapi.yaml`
- **Internal API:** Each service's internal endpoints documented in `<service-root>/docs/openapi-internal.yaml`
- **Frontend API clients and TypeScript types** MUST be generated from `openapi.yaml`
- **No manual hand-written API mocks** — use generated mocks from OpenAPI spec
- **CI validation**: Run `openapi-generator-cli validate` and diff check