---
description: Generates a conventional commit message in English based on git stage or last commit.
---

1. Check for staged changes: `git diff --cached --name-only`
2. If stage is empty, check last commit changes: `git log -1 --name-only`
3. Generate the commit text in English using the identified files/changes.
4. Output must follow the Conventional Commits pattern.
