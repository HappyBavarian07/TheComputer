# Commit Message Guide

This repository uses a Conventional Commits style format.

## Standard format

```text
type(scope)!: short summary

Optional body with more detail.

Optional footer(s)
```

Example:

```text
refactor(roadmaps): improve roadmap structure and add submodule roadmaps
```

---

## Parts of a commit message

### 1. Type

The `type` describes what kind of change was made.

Common types:

* `feat` — new feature
* `fix` — bug fix
* `refactor` — code change without changing behavior
* `docs` — documentation only
* `chore` — maintenance, cleanup, tooling
* `test` — tests added or changed
* `perf` — performance improvement
* `build` — build system or dependency changes
* `ci` — CI/CD changes
* `style` — formatting or style-only changes

Use the most accurate type possible.

---

### 2. Scope

The `scope` describes what part of the project was changed.

Examples:

* `roadmaps`
* `api`
* `ui`
* `config`
* `training`
* `docs`
* `build`

Scopes are optional, but recommended when the change clearly belongs to one area.

Examples:

```text
feat(ui): add search bar
fix(api): handle invalid input
refactor(roadmaps): split roadmap files
```

---

### 3. Breaking change marker

Use `!` when the commit introduces a breaking change.

Examples:

```text
feat(api)!: remove deprecated endpoint
refactor(config)!: change configuration format
```

This means old code, old configs, or old integrations may stop working.

---

### 4. Summary

The summary is the short description after the colon.

Rules:

* Keep it short.
* Use imperative mood.
* Do not end with a period.
* Prefer lowercase unless a name or acronym requires capitalization.
* Aim for one line.

Good:

```text
fix(auth): reject expired tokens
refactor(core): simplify reward calculation
```

Bad:

```text
Fixed authentication tokens.
Refactored a lot of things!!!
```

---

## When to add a body

Add a body when the change is large, non-obvious, or needs context.

Use the body to explain:

* what changed
* why it changed
* any tradeoffs
* any follow-up work
* anything reviewers should know

Example:

```text
refactor(roadmaps): improve roadmap structure and add submodule roadmaps

The roadmap files were reorganized to make navigation easier.
Submodule-specific roadmaps now live in separate sections so they can be updated independently.
```

---

## When to add a footer

Use footers for metadata and important notes.

Common footers:

* `BREAKING CHANGE: ...`
* `Fixes #123`
* `Closes #123`
* `Refs #123`

Example:

```text
feat(api): add export endpoint

BREAKING CHANGE: exported JSON now includes nested metadata fields.
Fixes #42
```

---

## Breaking changes

There are two valid ways to mark breaking changes:

### Option 1: `!` in the header

```text
feat(api)!: change response format
```

### Option 2: `BREAKING CHANGE:` footer

```text
feat(api): change response format

BREAKING CHANGE: the response now uses a new schema.
```

Use the footer when you need to explain the impact in more detail.

---

## Recommended rules

* Keep the first line under about 72 characters.
* One commit should do one logical thing.
* Do not mix unrelated changes in one commit.
* Use present tense.
* Keep the message readable by humans and tools.
* Use `body` only when needed, not for every commit.
* Be consistent across the whole repository.

---

## Character usage

### `!`

Use it only for breaking changes in the header.

### `&`

This has no special meaning. It may be used normally in the summary, but avoid cluttering commit messages with symbols.

### `#`

Usually used in footers like issue references, for example `Fixes #12`.

### `:`

Separates the header parts from the summary:

```text
type(scope): summary
```

---

## Good examples

```text
feat(roadmaps): add submodule roadmap files
fix(parser): handle empty input
refactor(training): split reward logic into smaller modules
docs(readme): improve setup instructions
chore(deps): update build dependencies
```

With body:

```text
refactor(roadmaps): improve roadmap structure and add submodule roadmaps

The roadmap layout was split into smaller files to make maintenance easier.
This also prepares the project for future submodule-specific updates.
```

Breaking change:

```text
feat(config)!: replace legacy config format

The old YAML layout is no longer supported.
Users must migrate to the new structure before updating.
```

---

## Bad examples

```text
update stuff
```

```text
fixed bug
```

```text
refactor(roadmaps): improved roadmap and added submodule roadmaps!!!
```

```text
Feat(Roadmaps): Add Stuff
```

Problems:

* too vague
* wrong tense
* inconsistent capitalization
* unnecessary punctuation
* missing structure

---

## Suggested project standard

Use this format:

```text
type(scope): summary

body

footer
```

Use `!` only when the change breaks compatibility.

Use a body for bigger commits.

Use footers for issue references and breaking changes.

---

## Minimal template

```text
type(scope): summary
```

## Extended template

```text
type(scope)!: summary

More detailed explanation of the change.

BREAKING CHANGE: explanation here
Fixes #123
```
