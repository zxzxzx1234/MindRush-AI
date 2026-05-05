# Known Bugs & Resolutions

This file lists the bugs that have been identified during development of MindRush AI, their resolution status, and the PR / commit that fixes them. It is a lightweight changelog focused on defects (the rubric item B.5).

For new bugs, please open a GitHub issue using the bug report template (`.github/ISSUE_TEMPLATE/bug_report.md`).

---

## Open

| ID | Title | Severity | Notes |
|---|---|---|---|
| BUG-005 | Best score not reset when changing user (single-device, multi-player) | Low | Edge case, low priority. |

---

## Resolved

### BUG-001 -- Game crashes when LLM endpoint is unreachable
- **Severity**: High
- **Reported in**: internal testing, mid-development
- **Symptom**: Toggling Ollama off mid-game caused an unhandled `IOException`, crashing `MainActivity`.
- **Root cause**: `OllamaClient.complete()` did not catch network errors; the exception propagated to the agent and from there to the UI thread.
- **Fix**: Added `isAvailable()` check + try/catch in each `LLMClient` implementation; agents now fall back to the local heuristic when the client reports unavailable.
- **Status**: ✅ Resolved
- **Tracking**: see PR for compliance branch (`Insiderfyr/mds-compliance`).

### BUG-002 -- DifficultyAdjusterAI returns NaN when no rounds played yet
- **Severity**: Medium
- **Symptom**: First round had undefined difficulty because success rate was computed as `correct / total` with `total == 0`.
- **Root cause**: Division by zero in metrics aggregation.
- **Fix**: Added a guard returning `DifficultyLevel.EASY` when no metrics are available yet.
- **Status**: ✅ Resolved

### BUG-003 -- SequenceGeneratorAI sometimes outputs duplicate consecutive tokens that the UI cannot render distinctly
- **Severity**: Medium
- **Symptom**: Two consecutive identical buttons in the displayed sequence look like a single, longer flash.
- **Root cause**: The LLM-generated sequence had no constraint against repeats; the UI renders each step with the same animation duration so consecutive duplicates collapse visually.
- **Fix**: Added a small inter-step pause and a validator that re-rolls the second token if it equals the first.
- **Status**: ✅ Resolved

### BUG-004 -- Local LLM returns malformed JSON, breaking parsing
- **Severity**: High
- **Symptom**: `SequenceGeneratorAI` threw `JsonSyntaxException` on small Ollama models.
- **Root cause**: Models like `phi3:mini` occasionally prefix output with prose ("Sure, here is the sequence: ...") and break strict JSON parsing.
- **Fix**: Implemented tolerant parsing: extract the first `[...]` substring, fall back to heuristic if extraction fails.
- **Status**: ✅ Resolved

---

## How a bug is processed

1. Open a GitHub issue using the bug report template.
2. Reproduce locally; confirm severity.
3. Create a `fix/<issue-number>-<slug>` branch.
4. Submit a PR that references the issue (`Closes #N`).
5. After review and CI green, merge into `main`.
6. Add an entry above to the **Resolved** table.
