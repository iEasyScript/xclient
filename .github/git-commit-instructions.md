Keep it strict and boring. You want commits that are searchable, small, and enforce a pattern.

---

# Commit instructions for Vue/Nuxt 3 projects

## 1. Use Conventional Commits

Format:

```
<type>(scope): <short summary>
```

### Allowed types

* `feat` → new feature
* `fix` → bug fix
* `refactor` → code restructure without behavior change
* `style` → formatting, CSS, code style (no logic change)
* `docs` → documentation only
* `test` → adding or updating tests
* `build` → build system, dependencies, CI/CD
* `chore` → maintenance tasks (configs, scripts, no app code)
* `perf` → performance improvements

### Scope

Optional, but recommended. Use folder or domain names:
`auth`, `api`, `store`, `ui`, `layout`, `router`, `config`

---

## 2. Summary line

* Imperative present tense: “add X”, not “added X”.
* ≤ 72 characters.
* No trailing period.

---

## 3. Description (optional body)

* Explain **what** and **why**, not **how**.
* Wrap at 80 chars.
* Separate paragraphs with blank lines.

---

## 4. Examples

```
feat(auth): add token refresh to api client
fix(ui): correct sidebar collapse state on mobile
refactor(store): move user preferences into pinia
style: apply prettier formatting
docs: add usage section for useApiClient composable
test(api): add retry logic tests for error handling
build: upgrade nuxt to 3.14 and update lockfile
chore: remove unused eslint-disable rules
perf(router): lazy-load admin routes
```

---

## 5. Rules

* One logical change per commit. Don’t mix unrelated edits.
* Keep commits small; large rewrites → split by feature.
* Don’t commit build outputs (`.nuxt`, `dist`).
* Run lint/test before committing. Use a pre-commit hook if possible.
* Reference issues/tickets in body, not summary:

  ```
  fix(api): handle 401 on refresh token

  Closes #123
  ```

---
