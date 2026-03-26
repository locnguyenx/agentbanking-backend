# Findings during request code review
- Core hexagonal architecture is violated — domain contains JPA annotations and services use EntityManager directly