---
description: Generate a short, simple git commit message from current uncommitted changes
---

# Git Commit Message Generator

1. Run `git diff --cached` to see staged changes. If there are no staged changes, run `git diff` to see all unstaged changes.

// turbo
2. Run `git diff --cached --stat` and `git diff --stat` together to get a summary of what files changed.

3. Review the diff output and identify:
   - What files were modified
   - What was added or removed at a high level

4. Generate a short, simple commit message in the imperative mood (e.g. "Display active modules", "Fix FOV reset on death", "Add screenshot keybind"). Rules:
   - 3–5 words max
   - No period at the end
   - No prefix like "feat:" or "fix:" — keep it plain English
   - Capitalize the first word only
   - Reflect the most meaningful change if multiple files changed

5. Present the suggested commit message to the user and ask for confirmation or edits.

6. Once confirmed, run:
```
git add -A
git commit -m "<confirmed message>"
```
