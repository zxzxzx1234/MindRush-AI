# Contributing to MindRush AI

Thanks for contributing! This guide describes how we work on MindRush AI as a team.

## 1. Branching model

- `main` is always green and deployable.
- Feature work goes on branches named `<author>/<short-slug>` (e.g. `Insiderfyr/mds-compliance`).
- Bug fixes go on branches named `fix/<issue-number>-<slug>`.

## 2. Commit messages

We loosely follow [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>(<scope>): <subject>

<body>
```

Types we use: `feat`, `fix`, `docs`, `chore`, `refactor`, `test`, `ci`.

Examples:
- `feat(ai): add OllamaClient with availability check`
- `fix(game): guard against division by zero in metrics`
- `docs(diagrams): add component architecture diagram`

## 3. Pull requests

- Open a PR against `main` as soon as you have a meaningful first commit.
- Reference the issue you are closing: `Closes #N`.
- Use the PR template (`.github/pull_request_template.md`).
- CI must be green before merging.
- Prefer small PRs (≤ ~400 lines diff) over large ones.

## 4. Local development

Build:
```
./gradlew assembleDebug
```

Run unit tests:
```
./gradlew testDebugUnitTest
```

Run lint:
```
./gradlew lintDebug
```

## 5. AI tools

Using AI assistants (Copilot, Cursor, ChatGPT, Claude, etc.) is encouraged --
this project is part of a course where we are explicitly graded on AI use.
However:

1. **Read what the AI generates** before committing it.
2. **Run the build and tests locally** -- AI suggestions sometimes hallucinate APIs.
3. **Don't paste secrets** (API keys, tokens) into AI tools.
4. Document non-trivial AI usage in `docs/AI_USAGE.md`.

## 6. Code style

- Kotlin official style (the default in Android Studio).
- Public classes and non-trivial functions get a KDoc comment.
- Avoid platform-specific imports outside the `app/src/main/` Android sources.

## 7. Reporting bugs

Open a GitHub issue with the bug-report template (`.github/ISSUE_TEMPLATE/bug_report.md`).
Add the issue to `docs/BUGS.md` once it has a fix PR.
