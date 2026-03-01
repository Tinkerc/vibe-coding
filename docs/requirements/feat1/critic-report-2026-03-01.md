# Critical Review Report: JSON to CSV Converter Requirements

**Date:** 2026-03-01
**Reviewer:** Solution Critic
**Document Reviewed:** requirement-expanded-2026-03-01.md

---

## Critical Review

### Hidden Assumptions

| # | Assumption | Risk | If False |
|---|------------|-------|----------|
| 1 | All JSON files have a consistent structure | HIGH | Union of fields approach may create columns with mostly empty values; CSV becomes unusable |
| 2 | Input files fit on disk (even with streaming) | MEDIUM | 10GB file still needs disk space; may exceed available storage |
| 3 | User knows what "flatten" or "row explosion" means | MEDIUM | Poor UX; users will make wrong choices and get unexpected output |
| 4 | JSON is always valid or should be rejected | MEDIUM | Real-world JSON often has minor issues; strict rejection may be too harsh |
| 5 | Single-threaded processing is sufficient | LOW | Large files on multi-core systems will be slower than necessary |

### Unrealistic Parts

| # | Problem | Why Unrealistic | Reality |
|---|---------|-----------------|----------|
| 1 | "Support files up to 10GB" | No clear definition of success criteria | What about 11GB? 100GB? Need to define actual limits or state "unbounded with streaming" |
| 2 | "Convert 10MB JSON in < 5 seconds" | Depends heavily on nesting depth and array sizes | A deeply nested 10MB file could take much longer |
| 3 | "Minimal dependencies" vs "full feature set" | Pandas is a heavy dependency (~100MB+) | Either accept heavy deps or reduce feature scope |
| 4 | "Single-file deployment" | Conflicts with modular code quality requirements | Single file = hard to test; choose one |
| 5 | Streaming + field selection | Cannot know all fields upfront without reading entire file | Must make two passes or accept partial field knowledge |

### Logical Issues

| # | Issue | Explanation |
|---|-------|-------------|
| 1 | Priority mismatch | Array handling has P1 priority but is complex; nested flattening is P0 but simpler |
| 2 | NDJSON format | Not clearly defined if user passes single file with multiple JSON objects |
| 3 | Error recovery contradiction | FR-004.3 says "skip invalid records" but FR-004.5 says "exit with non-zero on error" - which is it? |
| 4 | Scope contradiction | "Future enhancement: configuration file" but FR-003.4 mentions "via configuration" |

### Failure Scenarios

#### 1. **Schema Drift Collapse**
- **Cause:** Input JSON files have varying schemas with 100+ different fields across files
- **Early signals:** CSV has many empty columns, headers change between runs
- **Impact:** Unusable CSV with 200 columns, mostly empty; Excel crashes

#### 2. **Memory Explosion on "Small" Files**
- **Cause:** A 5MB JSON file contains deeply nested arrays that expand to millions of rows
- **Early signals:** Process hangs, memory usage spikes
- **Impact:** OOM kill, data loss

#### 3. **Character Encoding Disaster**
- **Cause:** Input JSON claims UTF-8 but has mixed encoding (e.g., Latin-1 characters)
- **Early signals:** Garbled output, Unicode replacement characters (�)
- **Impact:** Data corruption, silent data loss

#### 4. **Circular Reference Death Loop**
- **Cause:** JSON contains circular references (shouldn't exist but does in wild)
- **Early signals:** Stack overflow, infinite loop
- **Impact:** Process hangs or crashes

#### 5. **Comma in Value Without Escaping**
- **Cause:** Custom transformation code produces CSV without proper escaping
- **Early signals:** CSV columns misaligned when opened
- **Impact:** Silent data corruption, misaligned columns

### Hidden Costs

| Cost Type | Description |
|-----------|-------------|
| **Maintenance** | Supporting three array modes triples the testing matrix |
| **Documentation** | Each mode needs examples, edge cases, troubleshooting |
| **Cognitive** | Users must understand three different array concepts before using basic feature |
| **Testing** | Testing all combinations of (flatten × array-mode × delimiter × encoding) = 20+ combinations |
| **Dependency Hell** | Pandas requires numpy; version conflicts can break installation |

### Edge Cases

| # | Case | Why It Fails |
|---|------|--------------|
| 1 | Empty JSON array `[]` | What CSV to output? Empty file or file with just headers? |
| 2 | JSON with only one field (single key-value) | Still outputs CSV - is this useful? |
| 3 | Array with 10,000 elements in "column expansion" mode | Creates 10,000 columns - unusable |
| 4 | Key name contains the delimiter character | E.g., key is `user,name` - creates ambiguous output |
| 5 | Null vs empty string vs missing field | Three different states but CSV may not distinguish |
| 6 | Date formats in JSON | ISO, timestamp, string - all need different handling |
| 7 | Floating point precision | JSON `0.1 + 0.2` becomes CSV `0.30000000000000004` |

### Overconfidence Detected

| Statement | Issue | Realistic Version |
|-----------|-------|-------------------|
| "Support files up to 10GB" | Arbitrary limit | "Support streaming for arbitrarily large files" |
| "Convert 10MB JSON in < 5 seconds" | Hardware dependent | "Typical 10MB file converts in < 5 seconds on modern hardware" |
| "Proper CSV escaping" | Assumes perfect implementation | "RFC 4180 compliant CSV escaping" |
| "UTF-8 encoding support" | Complex in practice | "UTF-8 encoding with BOM option for Excel" |
| "All data is accessible in a single CSV row" | Not true for exploded arrays | "All data is accessible across related rows" |

---

## Reliability Upgrade

### Key Fixes

1. **Clarify Schema Handling**
   - Add explicit requirement: behavior when schemas vary between files
   - Add `--schema-strict` flag to fail on field mismatches
   - Default: use union of fields, warn if >50% empty columns

2. **Fix Array Priority**
   - Make row explosion P2 (complex, edge case)
   - Keep comma-separated as P0 (simple, common case)
   - Remove column expansion or make it P3 (rarely useful)

3. **Resolve Error Handling Contradiction**
   - Default: strict mode (exit on first error)
   - Add `--continue-on-error` flag for batch processing
   - Always exit non-zero if ANY error occurred

4. **Add Resource Limits**
   - Add `--max-rows` limit to prevent accidental explosion
   - Add `--max-columns` limit (default: 1000)
   - Add progress indicator for files > 1MB

5. **Simplify Language Decision**
   - **Recommend Python** for data focus
   - Use `csv` module from stdlib for basic version
   - Optional pandas dependency for advanced features

6. **Clarify Scope**
   - Remove "via configuration" from FR-003.4 (out of scope for v1)
   - Move config file to explicit future enhancement

7. **Add Critical Requirements**
   - FR-006: Dry-run mode (`--dry-run`) to preview output
   - FR-007: Statistics output (row count, column count, processing time)
   - FR-008: Validate output CSV can be re-parsed

### Improved Solution

See the consolidated requirement document for the updated specifications incorporating these fixes.

---

## Recommendations

### Must Fix Before Phase 2
1. ✅ Resolve error handling contradiction
2. ✅ Clarify schema variation behavior
3. ✅ Decide Python vs JavaScript
4. ✅ Simplify array modes (remove or de-prioritize column expansion)

### Should Fix
1. Add resource limits (max rows, max columns)
2. Add dry-run mode
3. Clarify performance expectations (hardware dependent)

### Can Defer
1. Configuration file support
2. Custom transformation functions
3. Parallel processing

---

## Summary

The requirements are **generally sound** but have some contradictions and ambiguities that should be resolved before moving to design phase. The main concerns are:

1. **Error handling strategy** needs clarification
2. **Array mode priorities** seem misaligned with complexity
3. **Schema variation** is not adequately addressed
4. **Resource limits** are missing

**Overall Assessment:** **Conditionally Approved** - address "Must Fix" items before Phase 2.
