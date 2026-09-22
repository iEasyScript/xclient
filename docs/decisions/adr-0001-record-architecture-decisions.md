# ADR 0001: Record Architecture Decisions

- Status: Accepted (2026-02-08)

## Context
Contributors need a lightweight, traceable way to capture architectural choices across the RuneLite+ProjectX composite build without scattering rationale in PRs.

## Decision
Adopt Architecture Decision Records (ADRs) stored under `docs/decisions/` with sequential numbering, concise rationale, and links from `docs/INDEX.md`.

## Consequences
- New significant architectural choices are documented via ADRs.
- Reviewers expect ADR updates when behavior or structure changes at module/runtime boundaries.
- Past rationale is easy to reference for future Codex sessions and human maintainers.
