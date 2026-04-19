# KI-003: Token Audit & Pruning
[CLAUDE.md](../../CLAUDE.md)

**Scope:** Comiqueta, BipSale, global GEMINI.md.

## Changes Applied

### Both projects
- Removed redundant `## Git` from `RULES.md`.
- Removed dead `[Index]` links.
- Deleted historical `KI-002_CONFIG_CONSOLIDATION.md`.
- Updated `CLAUDE.md` to match `RULES.md`.
- Deleted stale `.agent/` files/dirs.

### BipSale-specific
- Removed "Agent approval" from `RULES.md` (handled by GEMINI.md/Claude).
- Moved `## Business Flows` → `ARCHITECTURE.md` (`## Domain Flows`).

### Global
- Updated `GEMINI.md` to point to `conductor/WORKFLOWS.md`.
- Removed redundant `## Operação Densa` (covered by `CODE-DENSE`).
- Removed boilerplate from `CLAUDE.md`.

## Final State
| Project | Files | ~Tokens |
|---------|------:|--------:|
| Comiqueta | 6 | ~1,761 |
| BipSale | 5 | ~1,555 |
| GEMINI.md | 1 | ~316 |

Working set (excluding KIs):
| Project | ~Tokens |
|---------|--------:|
| Comiqueta | ~954 |
| BipSale | ~1,057 |

**Status:** Completed — 2026-03-21
