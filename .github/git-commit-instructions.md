# Commit instructions

Keep commits small, searchable and in one pattern.

## Format

```
<type>(scope): <short summary>
```

Use the imperative mood, no trailing full stop, summary under about 72 characters.

### Types

* `feat` - new feature
* `fix` - bug fix
* `refactor` - restructure without behaviour change
* `style` - formatting only
* `docs` - documentation only
* `test` - adding or updating tests
* `build` - build system, dependencies, CI/CD
* `chore` - maintenance
* `perf` - performance improvement

### Scope

Optional but recommended: the package or area, such as `bot`, `walker`, `bank`, `agentserver`,
`ui`, `build`.

## One commit, one purpose

Do not mix a fix with a refactor, or code with unrelated docs. A release commit that bumps
`projectx.version` / `projectx.pluginapi.version` in `gradle.properties` stands on its own.
