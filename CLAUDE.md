# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

`vibe-coding` is a learning repository for exploring GitHub workflows, git operations, and development best practices.

## Repository Information

| Property | Value |
|----------|-------|
| **Remote** | `https://github.com/Tinkerc/vibe-coding.git` |
| **Current Branch** | `main` |
| **HEAD** | `72dcbe7` |
| **User** | Tinkerc <chenruoyun@126.com> |

## Related Repositories

- **vibe-coding-testing**: https://github.com/Tinkerc/vibe-coding-testing
  - Test repository for vibe coding experiments
  - Cloned to: `~/work/code/learning/github/vibe-coding-testing`

## Git Workflow

Follow conventional commit format:
```
<type>[optional scope]: <description>
```

**Types:** `feat`, `fix`, `docs`, `style`, `refactor`, `perf`, `test`, `chore`, `ci`

**Examples:**
- `feat(auth): add OAuth2 login support`
- `fix(api): handle null response from user endpoint`
- `docs(readme): update installation instructions`

## Project Structure

```
.
├── .claude/
│   ├── session/        # Phase summaries from session-clear workflow
│   └── spec/           # Project rules and specifications
├── CLAUDE.md           # This file - AI working memory
└── README.md           # User-facing documentation
```

## Development Tools

- **GitHub CLI**: Used for repository management (gh)
- **Session Clear**: Use `/session-clear` to consolidate work into structured documentation
