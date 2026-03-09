# Requirements: Local File Organization Feature

**Date:** 2026-03-02
**Status:** FROZEN
**Version:** 1.0
**Phase:** 1 - Requirement Design Complete

---

## 1. Problem Statement

Local file systems accumulate unorganized files over time, particularly in directories like Downloads, Desktop, and project folders. Users spend significant time manually organizing files, leading to lost productivity, inconsistent structures, and difficulty finding files.

**Target Users:** Technical users (developers, power users) comfortable with CLI who want automated file organization.

---

## 2. User Personas

### Primary Persona: "The Busy Professional" (Technical)
- **Role:** Software Developer / Content Creator
- **Comfort Level:** Comfortable with CLI, prefers keyboard-driven workflows
- **Pain Points:**
  - Downloads folder becomes chaotic within days
  - Screenshots mixed with important documents
  - Time wasted manually organizing files
- **Goals:**
  - Quick organization with minimal effort
  - Predictable folder structures
  - Preview before committing changes

### Secondary Persona: "The Power User"
- **Role:** System Administrator / Advanced User
- **Comfort Level:** Expert CLI user, scripts regularly
- **Pain Points:**
  - Needs custom organization rules
  - Wants to integrate into existing workflows
  - Requires detailed logging and auditing
- **Goals:**
  - Fully customizable rule engine
  - Scriptable interface
  - Integration with automation tools

### Note: GUI Version
A future version (v2.0) will include a GUI for non-technical users ("Casual User" persona).

---

## 3. Goals

### MVP Goals (v1.0)
- [ ] Automatically organize files by extension/type
- [ ] Support user-defined organization rules (JSON config)
- [ ] Provide dry-run mode to preview changes
- [ ] Include comprehensive operation logging
- [ ] Handle file conflicts (auto-rename strategy)
- [ ] Cross-platform support (macOS, Linux) - Windows in v1.2

### v1.1 Goals
- [ ] Undo functionality (log-based)
- [ ] Advanced rule options (YAML support)
- [ ] File type detection by MIME type
- [ ] Empty directory cleanup

### v2.0 Goals (Future)
- [ ] GUI interface for non-technical users
- [ ] AI-powered content categorization
- [ ] Cloud storage integration
- [ ] Duplicate file detection

---

## 4. Functional Requirements (MVP)

### FR-1: File Type Detection (Must Have)
| ID | Description | MVP |
|----|-------------|-----|
| FR-1.1 | Detect file type by extension | ✅ |
| FR-1.2 | Support custom file type definitions | ✅ |

### FR-2: Organization Rules (Must Have)
| ID | Description | MVP |
|----|-------------|-----|
| FR-2.1 | Built-in default rules for 50+ common file types | ✅ |
| FR-2.2 | User-defined rules via JSON configuration | ✅ |
| FR-2.3 | Regex pattern matching for filenames | ✅ |
| FR-2.4 | Rule priority/precedence handling | ✅ |
| FR-2.5 | Client prefix detection (e.g., `lulu-*`, `kering-*`) | ✅ |
| FR-2.6 | Smart pattern matching (invoices, screenshots, diagrams) | ✅ |
| FR-2.7 | Multiple organization styles (numbered_workflow, extension_based) | ✅ |

### FR-3: Operations (Must Have)
| ID | Description | MVP |
|----|-------------|-----|
| FR-3.1 | Move files to organized folders | ✅ |
| FR-3.2 | Dry-run mode (preview only) | ✅ |
| FR-3.3 | Batch process multiple directories | ✅ |
| FR-3.4 | Recursive directory scanning | ✅ |

### FR-4: Safety & Validation (Must Have)
| ID | Description | MVP |
|----|-------------|-----|
| FR-4.1 | Dry-run mode (preview only) | ✅ |
| FR-4.2 | Operation logging (JSON log file) | ✅ |
| FR-4.3 | Conflict resolution (auto-rename) | ✅ |
| FR-4.4 | File integrity verification (post-move check) | ✅ |

### FR-5: Configuration (Must Have)
| ID | Description | MVP |
|----|-------------|-----|
| FR-5.1 | JSON configuration support | ✅ |
| FR-5.2 | Command-line argument overrides | ✅ |
| FR-5.3 | Configuration file validation | ✅ |
| FR-5.4 | Example configurations included | ✅ |

### FR-6: Safety Safeguards (Must Have - From Critic)
| ID | Description | MVP |
|----|-------------|-----|
| FR-6.1 | Protected directory blocklist | ✅ |
| FR-6.2 | Pre-flight permission check | ✅ |
| FR-6.3 | Empty directory cleanup option | ✅ |

### FR-7: Edge Case Handling (Must Have - From Critic)
| ID | Description | MVP |
|----|-------------|-----|
| FR-7.1 | Handle files without extensions | ✅ |
| FR-7.2 | Handle same-filename collisions | ✅ |
| FR-7.3 | Handle files in use (skip with warning) | ✅ |
| FR-7.4 | Handle long paths (pre-check) | ✅ |

### FR-8: User Interface (Must Have)
| ID | Description | MVP |
|----|-------------|-----|
| FR-8.1 | Clear command syntax | ✅ |
| FR-8.2 | Progress indication | ✅ |
| FR-8.3 | Colored output for better UX | ✅ |
| FR-8.4 | Verbose/quiet modes | ✅ |

### FR-9: Configuration (Should Have - v1.1)
| ID | Description | MVP |
|----|-------------|-----|
| FR-9.1 | YAML configuration support | ❌ v1.1 |
| FR-9.2 | Conditional rules based on file size/date | ❌ v1.1 |

### FR-10: Safety (Should Have - v1.1)
| ID | Description | MVP |
|----|-------------|-----|
| FR-10.1 | Undo functionality | ❌ v1.1 |
| FR-10.2 | Backup creation before moves | ❌ v1.1 |

---

## 5. Non-Functional Requirements (MVP)

### NFR-1: Performance
| Requirement | Metric |
|-------------|--------|
| Small directories (<100 files) | < 1 second |
| Medium directories (100-500 files) | < 5 seconds (local SSD) |
| Large directories (>500 files) | < 30 seconds |
| Memory usage | < 100MB typical |

### NFR-2: Compatibility
| Requirement | Specification |
|-------------|----------------|
| Python version | 3.9+ |
| Operating systems | macOS, Linux (Windows in v1.2) |
| Dependencies | Minimal (stdlib preferred) |

### NFR-3: Reliability
| Requirement | Specification |
|-------------|----------------|
| Error handling | Graceful, actionable messages |
| Data safety | No data loss in normal operation |
| Recovery | Operation log for troubleshooting |

### NFR-4: Usability
| Requirement | Specification |
|-------------|----------------|
| Learning curve | < 15 minutes for basic use |
| Documentation | README with examples |
| Help system | Built-in `--help` with examples |

### NFR-5: Maintainability
| Requirement | Specification |
|-------------|----------------|
| Code structure | Modular, extensible |
| Test coverage | > 60% for core logic (MVP) |
| API design | Clear interfaces for future plugins |

---

## 6. Default Organization Structure

### 6.1 Recommended Structure (Numbered Priority System)

Based on real-world usage patterns from technical professionals, this structure uses **numbered prefixes** for consistent ordering and **workflow-oriented categories**:

```
{source_directory}/
├── 📁 01-Projects-by-Client/        (Client-specific work)
│   ├── 📂 {ClientName}/
│   │   ├── 📄 *.pdf, *.md, *.docx
│   │   ├── 🖼️ *.png, *.jpg
│   │   └── 📊 *.xlsx, *.pptx
│   └── ... (other clients)
│
├── 📁 02-Internal-Systems/          (Internal projects & systems)
│   ├── 📂 {SystemName}/
│   │   ├── 📄 Architecture docs
│   │   ├── 🖼️ Diagrams
│   │   └── 📊 Reports
│   └── ... (other systems)
│
├── 📁 03-Documents-General/         (General documentation)
│   ├── 📂 HR-Admin/                 (HR, admin, invoices)
│   │   ├── 🧾 Invoices/             (*发票*.pdf, *invoice*.pdf)
│   │   └── 📋 Forms/                (*.pdf, *.docx)
│   ├── 📂 Learning-Materials/       (Tutorials, courses)
│   │   ├── 📚 PDFs/                 (*.pdf)
│   │   └── 📝 Markdown/             (*.md)
│   ├── 📂 Presentations/            (*.pptx, *.key)
│   └── 📂 Technical-Docs/           (*.md, *.txt, *.rst)
│
├── 📁 04-Images-Screenshots/        (Visual content)
│   ├── 📂 Diagrams-General/         (Architecture, flowcharts)
│   │   └── 🎨 *.png, *.jpg, *.svg
│   ├── 📂 Photos/                   (Personal photos)
│   │   └── 📸 *.jpg, *.jpeg, *.heic
│   └── 📂 Screenshots/              (Screen captures)
│       └── 📸 screenshot*, screen*, *.png
│
├── 📁 05-Installers-Archives/       (Software & archives)
│   ├── 📦 Archives-ZIP/             (*.zip)
│   ├── 📦 Archives-TAR/             (*.tar, *.tar.gz, *.tgz)
│   ├── 📦 Archives-RAR/             (*.rar, *.7z)
│   └── ⚙️ Installers/               (*.dmg, *.pkg, *.exe, *.app)
│
├── 📁 06-Code-Data-Files/           (Development & data)
│   ├── 📂 Code-Snippets/            (*.py, *.js, *.ts, *.go, *.rs)
│   ├── 📂 Config-Files/             (*.json, *.yaml, *.yml, *.toml)
│   ├── 📂 JSON-CSV-Data/            (*.json, *.csv, *.xml)
│   └── 📂 Postman-Collections/      (*.postman_collection.json)
│
├── 📁 07-To-Review/                 (Pending review inbox)
│   └── 📥 All unsorted files        (temporary holding area)
│
└── 📂 Other/                        (Uncategorized files)
    └── 📦 Files not matching rules
```

### 6.2 Key Design Principles

1. **Numbered Prefixes (01-07)**
   - Ensures consistent ordering across all file managers
   - Priority-based: most important categories first
   - Easy to extend (08, 09, etc.)

2. **Workflow-Oriented Categories**
   - **01-Projects-by-Client**: Active client work (highest priority)
   - **02-Internal-Systems**: Internal projects
   - **03-Documents-General**: Reference materials
   - **04-Images-Screenshots**: Visual content
   - **05-Installers-Archives**: Software & archives
   - **06-Code-Data-Files**: Development files
   - **07-To-Review**: Inbox for new files

3. **Client-Based Organization**
   - Files with client prefixes (e.g., `lulu-*.pdf`, `kering-*.xlsx`)
   - Automatically routed to `01-Projects-by-Client/{ClientName}/`
   - Supports multiple file types per client

4. **Smart Pattern Matching**
   - **Client prefix**: `lulu-*`, `kering-*`, `private-*` → Client folders
   - **Invoice pattern**: `*发票*.pdf`, `*invoice*.pdf` → HR-Admin/Invoices
   - **Screenshot pattern**: `screenshot*`, `screen*` → Screenshots
   - **Architecture pattern**: `*architecture*.png`, `*diagram*.png` → Diagrams-General

### 6.3 Alternative: Simple Extension-Based Structure

For users preferring traditional extension-based organization:

```
{source_directory}/
├── 📁 Documents/
│   ├── 📄 PDFs/                    (*.pdf)
│   ├── 📝 Markdown/                (*.md)
│   ├── 📊 Spreadsheets/            (*.xlsx, *.csv)
│   └── 📑 Presentations/           (*.pptx, *.key)
├── 🖼️ Images/
│   ├── 📸 Screenshots/             (screenshot*, screen*)
│   ├── 🏞️ Photos/                  (*.jpg, *.jpeg, *.png, *.heic)
│   └── 🎨 Graphics/                (*.svg, *.ai, *.psd)
├── 🎬 Videos/                      (*.mp4, *.mov, *.mkv, *.avi)
├── 🎵 Audio/                       (*.mp3, *.flac, *.m4a, *.aac)
├── 📦 Archives/                    (*.zip, *.tar*, *.rar, *.7z)
├── 💻 Code/                        (*.py, *.js, *.ts, *.go, *.rs, *.java)
├── ⚙️ Installers/                  (*.dmg, *.pkg, *.exe, *.app)
└── 📂 Other/                       (uncategorized)
```

### 6.4 Configuration Selection

Users can choose organization style via configuration:

```json
{
  "organization_style": "numbered_workflow",  // or "extension_based"
  "client_prefixes": ["lulu", "kering", "private", "bby"],
  "invoice_patterns": ["*发票*.pdf", "*invoice*.pdf"]
}
```

---

## 7. Use Cases (MVP)

### UC-1: Organize Downloads Folder (Basic)
**Actor:** Busy Professional
**Precondition:** Downloads folder has 200+ mixed files with client prefixes
**Main Flow:**
1. User runs command: `organizer organize ~/Downloads --dry-run`
2. Tool shows preview:
   - "Will move 187 files into 7 main folders"
   - "Client files: lulu-* → 01-Projects-by-Client/Lululemon (45 files)"
   - "Invoices: *发票*.pdf → 03-Documents-General/HR-Admin/Invoices (5 files)"
   - "Architecture diagrams: *architecture*.png → 04-Images-Screenshots/Diagrams-General (12 files)"
3. User reviews summary and approves
4. User runs: `organizer organize ~/Downloads`
5. Tool creates folders and moves files
6. Tool shows completion summary with statistics
**Postcondition:** Downloads folder is organized, no data loss

### UC-2: Custom Rules for Project Files
**Actor:** Power User
**Precondition:** Project directory has mixed file types needing custom organization
**Main Flow:**
1. User creates `project-rules.json` with custom patterns
2. User runs: `organizer organize ~/project --config project-rules.json --dry-run`
3. Tool validates rules and shows preview
4. User runs: `organizer organize ~/project --config project-rules.json`
5. Tool generates operation log: `project-organize-20260302.json`
**Postcondition:** Files organized per custom rules, log saved

### UC-3: Batch Process Multiple Directories
**Actor:** Busy Professional
**Precondition:** Multiple directories need organization
**Main Flow:**
1. User runs: `organizer organize ~/Downloads ~/Desktop --batch`
2. Tool processes each directory with combined progress
3. Tool shows combined summary
**Postcondition:** All directories organized

### UC-4: Client-Based Organization (New)
**Actor:** Consultant / Freelancer
**Precondition:** Downloads folder has files with client prefixes (lulu-*, kering-*, etc.)
**Main Flow:**
1. User runs: `organizer organize ~/Downloads --style numbered_workflow`
2. Tool detects client prefixes and creates client folders
3. Files are organized by client first, then by type within client folders
4. Tool shows: "Organized 45 Lululemon files, 23 Kering files, 12 Private files"
**Postcondition:** Client files are grouped together for easy access

---

## 8. Scope Boundaries

### Confirmed In Scope (MVP)
- CLI-based file organization tool
- Python 3.9+ implementation
- Extension-based file type detection
- JSON configuration
- Dry-run mode
- Operation logging
- Auto-rename conflict resolution
- macOS and Linux support

### Out of Scope (Phase 1)
- GUI application → v2.0
- Windows support → v1.2
- Cloud storage integration → v2.0
- File content analysis/OCR → Future
- Network file system operations → Future
- Undo functionality → v1.1
- YAML configuration → v1.1
- Backup creation → v1.1

---

## 9. Technical Constraints

- Must use Python 3.9 or higher
- Must work without system administrator privileges
- Must handle paths with spaces and special characters
- Must respect file permissions
- Must not modify file contents
- Must be reversible via operation log (future v1.1)

---

## 10. Protected Directories (Blocklist)

The tool will refuse to organize these directories unless explicitly forced:

```
/                      (root)
~/                     (home root)
/bin, /sbin, /usr/bin, /usr/sbin   (system binaries)
/etc, /var                          (system config)
/System, /Library                  (macOS system)
~/Library                          (user library)
/Applications                      (macOS apps)
```

Users can override with `--force` flag (requires confirmation).

---

## 11. Configuration Format

### JSON Configuration Example

#### Example 1: Numbered Workflow Style (Recommended)

```json
{
  "organization_style": "numbered_workflow",
  "client_prefixes": ["lulu", "kering", "private", "bby"],

  "rules": [
    {
      "name": "Client Files - Lululemon",
      "patterns": ["lulu-*"],
      "destination": "01-Projects-by-Client/Lululemon",
      "priority": 100
    },
    {
      "name": "Client Files - Kering",
      "patterns": ["kering-*"],
      "destination": "01-Projects-by-Client/Kering",
      "priority": 100
    },
    {
      "name": "Client Files - Private",
      "patterns": ["private-*"],
      "destination": "01-Projects-by-Client/Private",
      "priority": 100
    },
    {
      "name": "Invoices",
      "patterns": ["*发票*.pdf", "*invoice*.pdf"],
      "destination": "03-Documents-General/HR-Admin/Invoices",
      "priority": 90
    },
    {
      "name": "Architecture Diagrams",
      "patterns": ["*architecture*.png", "*diagram*.png", "*flow*.png"],
      "destination": "04-Images-Screenshots/Diagrams-General",
      "priority": 85
    },
    {
      "name": "Screenshots",
      "patterns": ["screenshot*", "screen*", "Screenshot*"],
      "destination": "04-Images-Screenshots/Screenshots",
      "priority": 80
    },
    {
      "name": "PDFs",
      "patterns": ["*.pdf"],
      "destination": "03-Documents-General/Learning-Materials/PDFs",
      "priority": 50
    },
    {
      "name": "Markdown",
      "patterns": ["*.md"],
      "destination": "03-Documents-General/Technical-Docs",
      "priority": 50
    },
    {
      "name": "Presentations",
      "patterns": ["*.pptx", "*.key"],
      "destination": "03-Documents-General/Presentations",
      "priority": 50
    },
    {
      "name": "Spreadsheets",
      "patterns": ["*.xlsx", "*.csv"],
      "destination": "06-Code-Data-Files/JSON-CSV-Data",
      "priority": 50
    },
    {
      "name": "Postman Collections",
      "patterns": ["*.postman_collection.json"],
      "destination": "06-Code-Data-Files/Postman-Collections",
      "priority": 60
    },
    {
      "name": "Installers",
      "patterns": ["*.dmg", "*.pkg", "*.exe"],
      "destination": "05-Installers-Archives/Installers",
      "priority": 50
    },
    {
      "name": "Archives",
      "patterns": ["*.zip", "*.tar*", "*.rar", "*.7z"],
      "destination": "05-Installers-Archives/Archives-ZIP",
      "priority": 50
    }
  ],

  "options": {
    "conflict_resolution": "rename",
    "create_empty_folders": true,
    "cleanup_empty_dirs": false,
    "default_destination": "07-To-Review"
  }
}
```

#### Example 2: Simple Extension-Based Style

```json
{
  "organization_style": "extension_based",

  "rules": [
    {
      "name": "PDFs",
      "patterns": ["*.pdf"],
      "destination": "Documents/PDFs"
    },
    {
      "name": "Screenshots",
      "patterns": ["screenshot*", "screen*"],
      "destination": "Images/Screenshots"
    },
    {
      "name": "Python Code",
      "patterns": ["*.py"],
      "destination": "Code/Python"
    }
  ],

  "options": {
    "conflict_resolution": "rename",
    "create_empty_folders": true,
    "cleanup_empty_dirs": false
  }
}
```

---

## 12. Command-Line Interface

### Basic Commands

```bash
# Preview organization (dry-run) - Recommended first step
organizer organize ~/Downloads --dry-run

# Organize with default rules (numbered_workflow style)
organizer organize ~/Downloads

# Organize with specific style
organizer organize ~/Downloads --style numbered_workflow
organizer organize ~/Downloads --style extension_based

# Organize with custom config
organizer organize ~/project --config rules.json

# Batch multiple directories
organizer organize ~/Downloads ~/Desktop --batch

# Verbose output (show detailed file movements)
organizer organize ~/Downloads --verbose

# Quiet mode (only show summary)
organizer organize ~/Downloads --quiet

# Analyze directory structure and show statistics
organizer analyze ~/Downloads

# Show client-based statistics
organizer analyze ~/Downloads --by-client
```

### Help

```bash
organizer --help
organizer organize --help
organizer analyze --help
```

---

## 13. Success Criteria (MVP)

The feature will be considered successful when:

| Criterion | Target | Notes |
|-----------|--------|-------|
| Categorization accuracy | 85-90% | Remaining to "Other" |
| Operations complete without errors | 95%+ | Expected 5% edge cases |
| Data loss in normal operation | 0 incidents | Excludes user error |
| Performance (500 small files) | < 5 seconds | On local SSD |
| Test coverage | 60-70% | Core logic focus |
| Supported file types | 50+ extensions | Default rules |

---

## 14. Known Limitations

1. **Extension-based detection:** Files without extensions may be misclassified
2. **No content analysis:** File content is not examined
3. **Same-name collisions:** Auto-renamed (file.txt → file_1.txt)
4. **Undo not available in MVP:** Requires v1.1
5. **Windows support:** Requires additional development (v1.2)
6. **Large files:** No size-based filtering in MVP

---

## 15. Risks & Mitigation

| Risk | Impact | Probability | Mitigation |
|------|--------|-------------|------------|
| User organizes wrong directory | High | Medium | Protected blocklist, dry-run default |
| Files in use fail to move | Medium | High | Detect and skip with warning |
| Permission errors | Medium | Medium | Pre-flight check, clear errors |
| Filename collisions | Low | High | Auto-rename strategy |
| Cross-platform path issues | Medium | Medium | Use pathlib, extensive testing |

---

## 16. Development Timeline

| Milestone | Duration | Target |
|-----------|----------|--------|
| MVP (CLI + basic rules) | 2-3 weeks | v1.0 |
| Advanced rules + undo | +1 week | v1.1 |
| Windows support | +1 week | v1.2 |
| GUI interface | +4-6 weeks | v2.0 |

---

## 17. Dependencies

### Required (Python Stdlib)
- `pathlib` - Path operations
- `shutil` - File operations
- `json` - Configuration
- `logging` - Operation logging
- `argparse` - CLI parsing

### Optional (External)
- `rich` - Colored output, progress bars (recommended)
- `click` - Alternative CLI framework (optional)

---

## 18. Deliverables

### Code
- `organizer/` - Main package
- `organizer/cli.py` - Command-line interface
- `organizer/rules.py` - Rule engine
- `organizer/organizer.py` - Core organization logic
- `organizer/detector.py` - File type detection
- `tests/` - Test suite

### Documentation
- `README.md` - User guide
- `CONFIG.md` - Configuration guide
- `CHANGELOG.md` - Version history

### Configuration
- `default-rules.json` - Built-in rules
- `config.example.json` - Example user config

---

## 19. Approval

| Role | Name | Status | Date |
|------|------|--------|------|
| Product Owner | [User] | ⏳ Pending | |
| Technical Lead | [Claude] | ✅ Complete | 2026-03-02 |

---

## 20. Next Phase

Upon user approval, proceed to **Phase 2: Technical Design**

This will include:
- Architecture research
- Solution design
- Implementation planning

---

**Requirements Status:** FROZEN
**Version:** 1.0
**Date:** 2026-03-02
**Ready for User Approval:** ✅
