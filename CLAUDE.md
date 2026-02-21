# Tampa Volunteers — Claude Code Guidelines

## Project
Spring Boot 3.x / Spring MVC backend + Vite/React frontend + PostgreSQL.
Package root: `org.tampavolunteers`

## Local LLM Delegation (MCP: ollama-tampavolunteers)

You have access to a local Ollama model (qwen3:30b) via MCP tools.
**Prefer these tools for boilerplate-heavy or repetitive generation tasks**
to reduce latency and API usage. Use Claude's own reasoning for architecture
decisions, debugging, and anything requiring project-wide context.

### Use `generate_mvc_layer` when:
- Scaffolding a new entity (e.g. adding a `Notification` or `TeamMembership` resource)
- Generating any subset of: Entity, DTO, Repository, Service, Controller

### Use `generate_flyway_migration` when:
- Adding new tables, columns, indexes, or constraints
- Renaming or dropping schema elements

### Use `generate_test_scaffold` when:
- Starting a new test class for a service or controller
- Generating Mockito stubs for a set of methods

### Use `generate_frontend_service` when:
- Adding a new Axios service module for a backend resource
- Generating TypeScript types from a DTO description

### Use `ask_local_llm` when:
- Generating Javadoc or inline comments for a block of code
- Drafting SQL queries or JPQL
- Transforming one code pattern to another (e.g. converting field injection to constructor injection across a file)
- Any other low-stakes, self-contained generation task

### Always use Claude directly for:
- Architecture decisions and trade-off analysis
- Security review (JWT config, @PreAuthorize, CORS)
- Debugging failures with stack traces
- Cross-cutting refactors that need full project context
- Anything involving the `.clinerules` / project context itself
