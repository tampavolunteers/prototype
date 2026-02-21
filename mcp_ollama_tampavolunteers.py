#!/usr/bin/env python3
"""
MCP server: delegates Spring MVC boilerplate generation to a local Ollama model.
Tailored for the Tampa Volunteers (Spring Boot + PostgreSQL + Vite) project.

Setup:
    pip install mcp ollama
    ollama pull qwen3:30b

Register in ~/.claude.json or .mcp.json:
    {
      "mcpServers": {
        "ollama-tampavolunteers": {
          "command": "python",
          "args": ["/path/to/mcp_ollama_tampavolunteers.py"]
        }
      }
    }
"""

import asyncio
import json
from mcp.server import Server
from mcp.server.stdio import stdio_server
from mcp.types import Tool, TextContent
import ollama

# ---------------------------------------------------------------------------
# Config
# ---------------------------------------------------------------------------

DEFAULT_MODEL = "qwen3:30b"

SYSTEM_PROMPT = """You are a senior Java/Spring Boot developer working on a volunteer
management platform called Tampa Volunteers. The stack is:
- Spring Boot 3.x with Spring MVC
- Spring Security + JWT authentication
- Spring Data JPA / Hibernate
- PostgreSQL (via Flyway migrations)
- Maven
- Package root: org.tampavolunteers

Output only the requested code. No markdown fences, no explanation unless asked.
Follow standard Spring Boot conventions:
- @RestController for controllers
- @Service for services
- @Repository for repositories
- Constructor injection (no @Autowired on fields)
- ResponseEntity<T> return types on controllers
- Record-based DTOs where appropriate
- Standard exception handling via @ControllerAdvice
"""

# ---------------------------------------------------------------------------
# Server setup
# ---------------------------------------------------------------------------

server = Server("ollama-tampavolunteers")


def call_ollama(prompt: str, model: str = DEFAULT_MODEL, thinking: bool = False) -> str:
    """Call Ollama synchronously and return the response text."""
    messages = [
        {"role": "system", "content": SYSTEM_PROMPT},
        {"role": "user", "content": prompt},
    ]
    # Qwen3 supports /nothink prefix to disable chain-of-thought and speed things up
    if not thinking:
        messages[-1]["content"] = "/nothink\n\n" + messages[-1]["content"]

    response = ollama.chat(model=model, messages=messages)
    return response["message"]["content"].strip()


# ---------------------------------------------------------------------------
# Tools
# ---------------------------------------------------------------------------

@server.list_tools()
async def list_tools() -> list[Tool]:
    return [
        Tool(
            name="generate_mvc_layer",
            description=(
                "Generate a full Spring MVC layer (Entity, DTO, Repository, Service, Controller) "
                "for a given domain entity in the Tampa Volunteers project. "
                "Use this to scaffold new features quickly without burning Claude API tokens."
            ),
            inputSchema={
                "type": "object",
                "properties": {
                    "entity_name": {
                        "type": "string",
                        "description": "PascalCase entity name, e.g. 'Opportunity', 'Registration'",
                    },
                    "fields": {
                        "type": "string",
                        "description": (
                            "Comma-separated field descriptions, e.g. "
                            "'id: Long, title: String, startDateTime: LocalDateTime, status: OpportunityStatus'"
                        ),
                    },
                    "layers": {
                        "type": "array",
                        "items": {"type": "string", "enum": ["entity", "dto", "repository", "service", "controller"]},
                        "description": "Which layers to generate. Defaults to all.",
                    },
                },
                "required": ["entity_name", "fields"],
            },
        ),
        Tool(
            name="generate_flyway_migration",
            description=(
                "Generate a Flyway SQL migration script for a schema change in the Tampa Volunteers database. "
                "Returns a versioned V__description.sql file content."
            ),
            inputSchema={
                "type": "object",
                "properties": {
                    "description": {
                        "type": "string",
                        "description": "What the migration does, e.g. 'add skills table and opportunity_skills join table'",
                    },
                    "version": {
                        "type": "string",
                        "description": "Flyway version string, e.g. '1.3'. Will become V1_3__<slug>.sql",
                    },
                    "details": {
                        "type": "string",
                        "description": "Additional schema details, constraints, indexes to include",
                    },
                },
                "required": ["description", "version"],
            },
        ),
        Tool(
            name="generate_test_scaffold",
            description=(
                "Generate JUnit 5 + Mockito test scaffolding for a Spring service or controller class. "
                "Produces a test class with common test cases stubbed out."
            ),
            inputSchema={
                "type": "object",
                "properties": {
                    "class_name": {
                        "type": "string",
                        "description": "The class to test, e.g. 'OpportunityService', 'RegistrationController'",
                    },
                    "methods": {
                        "type": "string",
                        "description": "Comma-separated method names to test, e.g. 'findAll, findById, create, update, delete'",
                    },
                    "notes": {
                        "type": "string",
                        "description": "Any special test scenarios to include, e.g. 'test that volunteers cannot create opportunities'",
                    },
                },
                "required": ["class_name", "methods"],
            },
        ),
        Tool(
            name="generate_frontend_service",
            description=(
                "Generate a TypeScript API service module (using Axios) and matching TypeScript types "
                "for a Tampa Volunteers backend endpoint. Targets the Vite/React frontend."
            ),
            inputSchema={
                "type": "object",
                "properties": {
                    "resource": {
                        "type": "string",
                        "description": "The API resource, e.g. 'opportunities', 'registrations'",
                    },
                    "endpoints": {
                        "type": "string",
                        "description": (
                            "Comma-separated endpoint descriptions, e.g. "
                            "'GET /opportunities, POST /opportunities, GET /opportunities/:id, DELETE /opportunities/:id'"
                        ),
                    },
                    "dto_fields": {
                        "type": "string",
                        "description": "Fields for the TypeScript interface, e.g. 'id: number, title: string, startDateTime: string'",
                    },
                },
                "required": ["resource", "endpoints"],
            },
        ),
        Tool(
            name="ask_local_llm",
            description=(
                "Send a freeform prompt to the local Ollama model. Use for one-off questions, "
                "code reviews of small snippets, or tasks that don't fit the other tools. "
                "Cheaper than using Claude for repetitive or low-stakes generation."
            ),
            inputSchema={
                "type": "object",
                "properties": {
                    "prompt": {"type": "string", "description": "The prompt to send"},
                    "thinking": {
                        "type": "boolean",
                        "description": "Enable Qwen3 chain-of-thought reasoning (slower but better for complex problems)",
                        "default": False,
                    },
                    "model": {
                        "type": "string",
                        "description": "Ollama model to use. Defaults to qwen3:30b",
                        "default": DEFAULT_MODEL,
                    },
                },
                "required": ["prompt"],
            },
        ),
    ]


@server.call_tool()
async def call_tool(name: str, arguments: dict) -> list[TextContent]:

    if name == "generate_mvc_layer":
        entity = arguments["entity_name"]
        fields = arguments["fields"]
        layers = arguments.get("layers", ["entity", "dto", "repository", "service", "controller"])

        results = []
        for layer in layers:
            layer_prompts = {
                "entity": (
                    f"Generate a JPA @Entity class named {entity} with these fields: {fields}. "
                    "Include @Table, field validations, and a standard equals/hashCode based on id. "
                    "Use Lombok @Getter @Setter or Java records where appropriate."
                ),
                "dto": (
                    f"Generate request and response DTO records for {entity} with fields: {fields}. "
                    "Create {entity}Request (for create/update) and {entity}Response (for API output). "
                    "Use Java records."
                ),
                "repository": (
                    f"Generate a Spring Data JPA repository interface for {entity}. "
                    "Include common finder methods relevant to a volunteer management platform."
                ),
                "service": (
                    f"Generate a @Service class for {entity} with CRUD methods: findAll, findById, create, update, delete. "
                    "Use the repository and map between entity and DTO. Include proper exception handling."
                ),
                "controller": (
                    f"Generate a @RestController for {entity} at /api/{entity.lower()}s with "
                    "GET (list + by id), POST, PUT, DELETE endpoints. "
                    "Use ResponseEntity<T> and proper HTTP status codes. "
                    "Include @PreAuthorize annotations for role-based access."
                ),
            }
            prompt = layer_prompts[layer]
            code = await asyncio.to_thread(call_ollama, prompt)
            results.append(f"// === {layer.upper()}: {entity} ===\n\n{code}")

        return [TextContent(type="text", text="\n\n".join(results))]

    elif name == "generate_flyway_migration":
        description = arguments["description"]
        version = arguments["version"].replace(".", "_")
        details = arguments.get("details", "")

        slug = description.lower().replace(" ", "_")[:40]
        filename = f"V{version}__{slug}.sql"

        prompt = (
            f"Generate a Flyway SQL migration script for PostgreSQL.\n"
            f"Migration: {description}\n"
            f"{'Additional details: ' + details if details else ''}\n\n"
            f"Start with a comment: -- {filename}\n"
            f"Include IF NOT EXISTS guards where applicable. "
            f"Add appropriate indexes for foreign keys and frequently queried columns."
        )
        sql = await asyncio.to_thread(call_ollama, prompt)
        return [TextContent(type="text", text=f"-- Filename: {filename}\n\n{sql}")]

    elif name == "generate_test_scaffold":
        class_name = arguments["class_name"]
        methods = arguments["methods"]
        notes = arguments.get("notes", "")

        prompt = (
            f"Generate a JUnit 5 + Mockito test class for {class_name}.\n"
            f"Test these methods: {methods}\n"
            f"{'Special scenarios: ' + notes if notes else ''}\n\n"
            f"Use @ExtendWith(MockitoExtension.class). "
            f"Stub dependencies with @Mock. "
            f"Include happy path and common failure cases (not found, validation errors). "
            f"Use AssertJ assertions."
        )
        code = await asyncio.to_thread(call_ollama, prompt)
        return [TextContent(type="text", text=code)]

    elif name == "generate_frontend_service":
        resource = arguments["resource"]
        endpoints = arguments["endpoints"]
        dto_fields = arguments.get("dto_fields", "")

        prompt = (
            f"Generate a TypeScript Axios service module for the '{resource}' API resource.\n"
            f"Endpoints: {endpoints}\n"
            f"{'DTO fields: ' + dto_fields if dto_fields else ''}\n\n"
            f"Create:\n"
            f"1. TypeScript interfaces/types for the resource\n"
            f"2. A service object with async functions for each endpoint\n"
            f"3. Use a shared axios instance that reads VITE_API_BASE_URL from import.meta.env\n"
            f"4. Include JWT auth header via an axios interceptor reference (assume interceptor is set up elsewhere)\n"
            f"Export both the types and the service."
        )
        code = await asyncio.to_thread(call_ollama, prompt)
        return [TextContent(type="text", text=code)]

    elif name == "ask_local_llm":
        prompt = arguments["prompt"]
        thinking = arguments.get("thinking", False)
        model = arguments.get("model", DEFAULT_MODEL)
        result = await asyncio.to_thread(call_ollama, prompt, model, thinking)
        return [TextContent(type="text", text=result)]

    else:
        return [TextContent(type="text", text=f"Unknown tool: {name}")]


# ---------------------------------------------------------------------------
# Entry point
# ---------------------------------------------------------------------------

async def main():
    async with stdio_server() as (read_stream, write_stream):
        await server.run(read_stream, write_stream, server.create_initialization_options())


if __name__ == "__main__":
    asyncio.run(main())
