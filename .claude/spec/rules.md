# Project Rules

## Git Conventions
- Use conventional commit format: `<type>: <description>`
- Commit types: `feat`, `fix`, `chore`, `docs`, `style`, `refactor`, `perf`, `test`, `ci`
- Co-author commits when using Claude Code: `Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>`

## Repository Management
- Use GitHub CLI (`gh`) for repository operations
- Create repositories with `--add-readme` flag for initial setup
- Use `--public` or `--private` flags to set visibility

## Python Project Setup
- Always include a Python `.gitignore` with standard exclusions:
  - `__pycache__/`, `*.pyc`, `*.pyo`
  - Virtual environments: `venv/`, `.venv/`, `env/`
  - IDE directories: `.vscode/`, `.idea/`
  - OS files: `.DS_Store`, `Thumbs.db`
  - Distribution artifacts: `build/`, `dist/`, `*.egg-info/`

## Project Structure
```
.
├── .gitignore          # Version control exclusions
├── README.md           # Project documentation
├── CLAUDE.md           # Claude Code working memory
└── .claude/
    ├── session/        # Phase summaries
    └── spec/           # Project specifications and rules
```

## Documentation
- Keep `README.md` focused on user-facing information
- Use `CLAUDE.md` for AI agent context and working patterns
- Update documentation incrementally—never rewrite entirely
