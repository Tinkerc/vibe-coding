# Requirement Specification: JSON to CSV Converter

**Date:** 2026-03-01
**Status:** ✅ FROZEN
**Version:** 1.0
**Phase:** 1.5 - Consolidated & Frozen

---

## 📋 Document Status

**This document is FROZEN.** Changes require explicit user approval and a formal change request process.

---

## 1. Executive Summary

Create a Python-based command-line tool that converts JSON files to CSV format with support for:
- Nested object flattening
- Configurable array handling
- Multiple file processing
- Proper error handling and validation
- Resource limits for safety

**Technology Choice:** **Python 3.9+** (standard library `csv`, `json` modules; optional `pandas` for advanced features)

---

## 2. User Stories

| ID | Story | Priority | Acceptance Criteria |
|----|-------|----------|---------------------|
| US-001 | Convert JSON file to CSV | P0 | Single file converts to valid CSV with headers |
| US-002 | Flatten nested JSON objects | P0 | Nested fields use dot notation (e.g., `user.address.city`) |
| US-003 | Handle arrays in JSON | P1 | Supports comma-separated and row explosion modes |
| US-004 | Batch process multiple files | P1 | Accepts glob patterns, processes each file independently |
| US-005 | Process large files safely | P0 | Supports streaming mode with configurable limits |

---

## 3. Functional Requirements

### FR-001: Input Formats

| ID | Requirement | Priority | Notes |
|----|-------------|----------|-------|
| FR-001.1 | Single JSON file input | P0 | Default mode |
| FR-001.2 | Multiple JSON files (glob patterns) | P1 | `*.json`, `data/*.json` |
| FR-001.3 | JSON arrays | P0 | Top-level array of objects |
| FR-001.4 | NDJSON (JSON Lines) | P1 | One JSON object per line |
| FR-001.5 | Read from stdin | P1 | For pipe operations: `cat data.json \| json2csv` |

### FR-002: Output Formats

| ID | Requirement | Priority | Notes |
|----|-------------|----------|-------|
| FR-002.1 | Write to CSV file | P0 | `--output` flag |
| FR-002.2 | Write to stdout | P1 | For pipe operations |
| FR-002.3 | UTF-8 with optional BOM | P1 | `--bom` flag for Excel compatibility |
| FR-002.4 | Configurable delimiter | P2 | Comma (default), tab, semicolon, pipe |

### FR-003: Data Transformation

| ID | Requirement | Priority | Notes |
|----|-------------|----------|-------|
| FR-003.1 | Flatten nested objects | P0 | Dot notation: `user.address.city` |
| FR-003.2 | Configurable flattening separator | P2 | `--separator` flag (default: `.`) |
| FR-003.3 | Array: comma-separated mode | P1 | `--array-mode=joined` (default) |
| FR-003.4 | Array: row explosion mode | P1 | `--array-mode=explode` |
| FR-003.5 | Field selection via `--fields` | P2 | Comma-separated list of fields to include |

### FR-004: Error Handling

| ID | Requirement | Priority | Notes |
|----|-------------|----------|-------|
| FR-004.1 | Validate JSON syntax | P0 | Fail fast on invalid JSON |
| FR-004.2 | Report line number of errors | P0 | Include file path and line |
| FR-004.3 | Strict mode (default) | P0 | Exit on first error |
| FR-004.4 | Continue mode | P1 | `--continue-on-error` flag |
| FR-004.5 | Exit status codes | P0 | 0=success, 1=error, 2=partial success |

### FR-005: Resource Limits

| ID | Requirement | Priority | Notes |
|----|-------------|----------|-------|
| FR-005.1 | Maximum row limit | P0 | `--max-rows` (default: 1,000,000) |
| FR-005.2 | Maximum column limit | P0 | `--max-columns` (default: 1000) |
| FR-005.3 | Warning on approaching limits | P1 | Log warning at 80% of limit |

### FR-006: CLI Interface

| Flag | Description | Default | Priority |
|------|-------------|---------|----------|
| `-i, --input` | Input file(s) | Required | P0 |
| `-o, --output` | Output file | stdout | P0 |
| `--flatten` | Enable nested flattening | true | P0 |
| `--array-mode` | joined\|explode | joined | P1 |
| `--separator` | Flattening separator | . | P2 |
| `--delimiter` | CSV delimiter | , | P2 |
| `--bom` | Add BOM for Excel | false | P1 |
| `--max-rows` | Maximum output rows | 1M | P0 |
| `--max-columns` | Maximum output columns | 1000 | P0 |
| `--continue-on-error` | Continue after errors | false | P1 |
| `--dry-run` | Preview without writing | false | P1 |
| `--stats` | Show processing statistics | false | P1 |
| `--no-header` | Exclude CSV header row | false | P2 |
| `--help` | Show help message | - | P0 |
| `--version` | Show version | - | P0 |

---

## 4. Non-Functional Requirements

### NFR-001: Performance

| ID | Requirement | Metric |
|----|-------------|--------|
| NFR-001.1 | 10MB typical file conversion | < 5 seconds (modern CPU) |
| NFR-001.2 | Streaming support | Constant memory usage |
| NFR-001.3 | File size limit | Unbounded (with streaming) |

### NFR-002: Compatibility

| ID | Requirement |
|----|-------------|
| NFR-002.1 | Python 3.9+ |
| NFR-002.2 | Linux, macOS, Windows |
| NFR-002.3 | CSV compatible with Excel, Google Sheets, LibreOffice |
| NFR-002.4 | RFC 4180 CSV format compliance |

### NFR-003: Usability

| ID | Requirement |
|----|-------------|
| NFR-003.1 | Clear error messages with file and line info |
| NFR-003.2 | Progress bar for files > 1MB |
| NFR-003.3 | Example usage in `--help` |
| NFR-003.4 | Man page available |

### NFR-004: Code Quality

| ID | Requirement |
|----|-------------|
| NFR-004.1 | Unit tests with > 80% coverage |
| NFR-004.2 | Type hints using `typing` module |
| NFR-004.3 | Black formatter configured |
| NFR-004.4 | Ruff linter configured |
| NFR-004.5 | CI/CD with GitHub Actions |

---

## 5. Schema Handling Strategy

### Schema Variation Behavior

| Situation | Default Behavior | User Override |
|-----------|------------------|---------------|
| Fields vary between records | Union of all fields | `--schema-strict` to fail |
| Mixed data types in same field | Convert to string | Warning logged |
| Missing fields | Empty cell in CSV | - |

### Schema Limits

- **Warning**: If > 50% of columns are mostly empty (> 80% empty)
- **Hard limit**: `--max-columns` (default: 1000)
- **Recommendation**: Use `--fields` to select specific columns for wide schemas

---

## 6. Error Handling Strategy

### Default Mode (Strict)

```
Invalid JSON encountered → Report error → Exit with code 1 → No output file created
```

### Continue Mode

```
Invalid JSON encountered → Log warning → Skip record → Process remaining → Exit with code 2
```

### Error Exit Codes

| Code | Meaning | Output |
|------|---------|--------|
| 0 | Success | CSV file created |
| 1 | Error (strict mode) | No output file |
| 2 | Partial success | Output file with warnings logged |

---

## 7. Scope and Boundaries

### ✅ In Scope (v1.0)

- Single and multiple JSON file conversion
- Nested object flattening with dot notation
- Two array handling modes: joined, explode
- CLI tool with standard flags
- Basic error handling and validation
- UTF-8 encoding with optional BOM
- RFC 4180 CSV compliance
- Resource limits (max rows, max columns)
- Dry-run mode
- Statistics output

### ❌ Out of Scope

- CSV to JSON conversion (separate feature)
- GUI interface
- Real-time/streaming API data
- Database connections
- Cloud storage integration
- Configuration files
- Custom transformation functions
- Column expansion array mode (deferred)
- Parallel processing

### 🔄 Future Enhancements

- Configuration file support (`--config`)
- Custom field renaming mappings
- Data validation rules
- Parallel batch processing
- Web API wrapper
- Plugin system

---

## 8. Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────┐
│                         json2csv CLI                                │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  ┌─────────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────┐ │
│  │  Input  │───▶│   Parser    │───▶│  Validator  │───▶│ Flatten │ │
│  │ Handler │    │  (json mod) │    │   (schema)  │    │ Engine  │ │
│  └─────────┘    └─────────────┘    └─────────────┘    └─────────┘ │
│       │                                                   │         │
│       │                   ┌─────────────┐                │         │
│       └───────────────────▶│   Config    │◀───────────────┘         │
│                           │  Manager    │                          │
│                           └─────────────┘                          │
│                                     │                               │
│                                     ▼                               │
│  ┌─────────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────┐ │
│  │ Output  │◀───│   Writer    │◀───│   Escape    │◀───│ Convert │ │
│  │ Handler │    │  (csv mod)  │    │  (RFC 4180) │    │  & Map  │ │
│  └─────────┘    └─────────────┘    └─────────────┘    └─────────┘ │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 9. Open Questions (Resolved)

| ID | Question | Decision | Rationale |
|----|----------|----------|-----------|
| Q-001 | Python or JavaScript? | **Python 3.9+** | Better data libraries, simpler packaging |
| Q-002 | Default array handling mode? | **joined** (comma-separated) | Most common use case |
| Q-003 | Column expansion mode? | **Deferred** | Rarely useful, adds complexity |

---

## 10. Exit Criteria

Phase 1 Complete ✅

- ✅ All user stories documented with acceptance criteria
- ✅ All functional requirements have priorities
- ✅ Scope boundaries clearly defined
- ✅ Critical review conducted
- ✅ All "Must Fix" items addressed
- ✅ Requirements frozen (STATUS: FROZEN)
- ⏳ User approval pending

---

## 11. Change Request Process

To modify frozen requirements:

1. Create a new markdown file: `requirement-change-{date}-{id}.md`
2. Document: current requirement, proposed change, rationale
3. Update this document with `CHANGE [date]` reference
4. Obtain user approval

---

## 12. Example Usage

```bash
# Basic conversion
json2csv --input data.json --output data.csv

# Flatten nested objects
json2csv -i api-response.json -o output.csv --flatten

# Handle arrays by exploding rows
json2csv -i data.json -o output.csv --array-mode explode

# Batch process with glob pattern
json2csv -i "logs/*.json" -o combined.csv

# Preview without writing
json2csv -i data.json --dry-run --stats

# With custom delimiter for European CSV
json2csv -i data.json -o output.csv --delimiter ";"

# Continue on errors for batch processing
json2csv -i "data/*.json" -o output.csv --continue-on-error --stats

# Excel-compatible output
json2csv -i data.json -o output.csv --bom
```

---

## 13. Glossary

| Term | Definition |
|------|------------|
| **Dot Notation** | Using `.` to separate nested keys (e.g., `user.address.city`) |
| **Flattening** | Converting nested JSON structure to flat key-value pairs |
| **NDJSON** | Newline-delimited JSON - one JSON object per line |
| **BOM** | Byte Order Mark - helps Excel detect UTF-8 encoding |
| **Row Explosion** | Creating multiple rows from array elements |
| **RFC 4180** | Standard format for CSV files |
| **Schema-Strict** | Fail when field sets vary between records |
| **Union of Fields** | Include all fields from all records in output |

---

## Appendix A: Critical Review Summary

See [critic-report-2026-03-01.md](./critic-report-2026-03-01.md) for full review.

**Items Addressed:**
- ✅ Error handling contradiction resolved
- ✅ Schema variation behavior defined
- ✅ Array mode priorities adjusted
- ✅ Resource limits added
- ✅ Performance expectations clarified

---

**Document Status:** ✅ **FROZEN**

**Ready for:** Phase 2 - Technical Design

**Requires:** User approval to proceed
