# Research Report: JSON to CSV Converter

**Date:** 2026-03-01
**Phase:** 1.2 - Domain Research
**Agent:** Explore Agent

---

## Executive Summary

JSON to CSV conversion is a well-established pattern with multiple mature libraries available. Key considerations include handling nested structures, array expansion strategies, and proper CSV encoding.

---

## Key Findings

### 1. Common Conversion Patterns

| Pattern | Description | Use Case |
|---------|-------------|----------|
| **Simple 1:1 Mapping** | Direct field to column mapping | Flat JSON structures |
| **Dot Notation Flattening** | `user.address.city` → column | Nested objects |
| **Underscore Flattening** | `user_address_city` → column | Database-friendly naming |

### 2. Nested JSON Handling

**Recursive Flattening Approach:**
```python
def flatten_json(y):
    out = {}
    def flatten(x, name=''):
        if type(x) is dict:
            for a in x:
                flatten(x[a], name + a + '.')
        else:
            out[name[:-1]] = x
    flatten(y)
    return out
```

**Column Naming Strategies:**
- Dot notation: `user.address.city`
- JSONPath: `$..book[?(@.author == 'Tolkin')].title`
- Custom separators: `_` or `/`

### 3. Array Handling Strategies

| Strategy | Description | Example Output |
|----------|-------------|----------------|
| **Comma-Separated** | Join array values with commas | `"red,blue,green"` |
| **Row Explosion** | Create multiple rows | One array element per row |
| **Column Expansion** | Create numbered columns | `tags_0`, `tags_1`, `tags_2` |

### 4. Popular Libraries

**Python:**
- **pandas** - Most popular, simple API
- **json_normalize** - Handles nested structures
- **csvkit** - CLI tool `in2csv`

**JavaScript/Node.js:**
- **papaparse** - Most popular, browser + Node
- **json-2-csv** - Specialized for JSON→CSV
- **fast-csv** - Streaming support

### 5. Edge Cases to Handle

| Edge Case | Recommended Strategy |
|-----------|---------------------|
| Missing Fields | Fill with empty string or null |
| Type Conversions | Preserve types or convert to strings |
| Special Characters | Proper CSV escaping |
| Encoding Issues | UTF-8 with BOM for Excel |
| Large Files | Streaming/chunked processing |
| Mixed Schemas | Union of all fields across records |

### 6. CLI Tool Patterns

**Recommended Interface:**
```bash
json2csv input.json --output output.csv
json2csv input.json \
  --flatten-nested \
  --explode-arrays \
  --delimiter "," \
  --encoding utf-8
```

**Essential Features:**
- Multiple input files
- Output to stdout or file
- Field selection/renaming
- Custom transformations
- Schema validation

---

## Recommendations

1. **Language**: Python (pandas) or JavaScript (papaparse) - both have mature libraries
2. **Default Behavior**: Flatten nested objects with dot notation
3. **Array Strategy**: Provide configurable options (default: comma-separated)
4. **Error Handling**: Validate JSON before conversion, report errors clearly
5. **Performance**: Support streaming for large files (>100MB)

---

## Sources

- [pandas documentation](https://pandas.pydata.org/pandas-docs/stable/reference/api/pandas.read_json.html)
- [papaparse documentation](https://www.papaparse.com/docs)
- [json-2-csv npm package](https://www.npmjs.com/package/json-2-csv)
- [csvkit documentation](https://csvkit.readthedocs.io/en/latest/)
