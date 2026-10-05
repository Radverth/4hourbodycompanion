"""Room schema descriptions, from two sources.

The preferred source is the JSON Room itself exports under `app/schemas/`. That file is
authoritative: it carries the exact `CREATE TABLE` statement Room uses, the affinity and
nullability of every column, the declared defaults, and the indices. Nothing is inferred.

The fallback parses the Kotlin `@Entity` sources, for the version being *added* — its JSON
does not exist until KSP has run, which needs the Android toolchain. The fallback is only as
good as its type mapping, so anything using it says so.
"""

import json
import re


class Column:
    def __init__(self, name, affinity, notnull, pk, default):
        self.name = name
        self.affinity = affinity
        self.notnull = notnull
        self.pk = pk           # 0 = not part of the primary key, else 1-based position
        self.default = default  # the SQL literal, when one is declared

    def __repr__(self):
        return (f"{self.name} {self.affinity}"
                f"{' NOT NULL' if self.notnull else ''}"
                f"{' PK' if self.pk else ''}"
                f"{f' DEFAULT {self.default}' if self.default else ''}")


class Table:
    def __init__(self, name):
        self.name = name
        self.columns = []
        self.indices = []       # (columns tuple, unique)
        self.autoincrement = False
        self.create_sql_literal = None   # Room's own statement, when we have it
        self.index_sql_literals = []

    def column(self, name):
        return next((c for c in self.columns if c.name == name), None)

    def create_sql(self):
        if self.create_sql_literal:
            return self.create_sql_literal
        parts = []
        pk_cols = [c for c in self.columns if c.pk]
        single_autoinc = self.autoincrement and len(pk_cols) == 1
        for c in self.columns:
            piece = f"`{c.name}` {c.affinity}"
            if single_autoinc and c.pk:
                piece += " PRIMARY KEY AUTOINCREMENT"
            if c.notnull:
                piece += " NOT NULL"
            if c.default is not None:
                piece += f" DEFAULT {c.default}"
            parts.append(piece)
        if pk_cols and not single_autoinc:
            ordered = sorted(pk_cols, key=lambda c: c.pk)
            parts.append("PRIMARY KEY(" + ", ".join(f"`{c.name}`" for c in ordered) + ")")
        return f"CREATE TABLE `{self.name}` (" + ", ".join(parts) + ")"

    def index_sql(self):
        if self.index_sql_literals:
            return list(self.index_sql_literals)
        out = []
        for cols, unique in self.indices:
            name = "index_" + self.name + "_" + "_".join(cols)
            kind = "UNIQUE INDEX" if unique else "INDEX"
            cols_sql = ", ".join(f"`{c}`" for c in cols)
            out.append(f"CREATE {kind} IF NOT EXISTS `{name}` ON `{self.name}` ({cols_sql})")
        return out


# ---------------------------------------------------------------------------
# Room's exported JSON — authoritative
# ---------------------------------------------------------------------------

def from_json(path):
    """{table name: Table} from a Room schema JSON file."""
    data = json.loads(open(path).read())
    db = data["database"]
    out = {}
    for entity in db["entities"]:
        table = Table(entity["tableName"])
        pk = entity.get("primaryKey") or {}
        pk_names = pk.get("columnNames", [])
        table.autoincrement = bool(pk.get("autoGenerate"))
        for field in entity["fields"]:
            name = field["columnName"]
            table.columns.append(Column(
                name=name,
                affinity=field["affinity"].upper(),
                notnull=bool(field.get("notNull")),
                pk=(pk_names.index(name) + 1) if name in pk_names else 0,
                default=field.get("defaultValue"),
            ))
        for index in entity.get("indices", []):
            table.indices.append((tuple(index["columnNames"]), bool(index.get("unique"))))
            sql = index.get("createSql")
            if sql:
                table.index_sql_literals.append(sql.replace("${TABLE_NAME}", table.name))
        create = entity.get("createSql")
        if create:
            table.create_sql_literal = create.replace("${TABLE_NAME}", table.name)
        out[table.name] = table
    return out


def json_version(path):
    return json.loads(open(path).read())["database"]["version"]


# ---------------------------------------------------------------------------
# Kotlin @Entity sources — fallback, for a version whose JSON does not exist yet
# ---------------------------------------------------------------------------

AFFINITY = {
    "Long": "INTEGER",
    "Int": "INTEGER",
    "Boolean": "INTEGER",
    "Double": "REAL",
    "Float": "REAL",
    "String": "TEXT",
    # Converters: LocalDate -> epoch day (Long), DayOfWeek -> Int, Set<DayOfWeek> -> String.
    "LocalDate": "INTEGER",
    "DayOfWeek": "INTEGER",
    "Set<DayOfWeek>": "TEXT",
}

# Every enum in the project is stored by name as TEXT.
ENUMS = {"RunEnd", "SessionKind", "StickingPointTechnique"}


def _split_params(body):
    """Split a constructor parameter list on commas at depth zero."""
    out, depth, cur, in_str = [], 0, "", False
    i = 0
    while i < len(body):
        ch = body[i]
        if in_str:
            cur += ch
            if ch == '"' and body[i - 1] != "\\":
                in_str = False
        elif ch == '"':
            cur += ch
            in_str = True
        elif ch in "([<":
            depth += 1
            cur += ch
        elif ch in ")]>":
            depth -= 1
            cur += ch
        elif ch == "," and depth == 0:
            out.append(cur)
            cur = ""
        else:
            cur += ch
        i += 1
    if cur.strip():
        out.append(cur)
    return out


def _balanced(src, start):
    """The text between the paren at `start` and its match."""
    depth = 0
    for i in range(start, len(src)):
        if src[i] == "(":
            depth += 1
        elif src[i] == ")":
            depth -= 1
            if depth == 0:
                return src[start + 1:i], i
    raise ValueError("unbalanced parentheses")


def _indices(entity_args):
    out = []
    m = re.search(r"indices\s*=\s*\[(.*?)\]\s*\)", entity_args, re.S)
    if not m:
        return out
    for idx in re.finditer(r"Index\((.*?)\)", m.group(1), re.S):
        args = idx.group(1)
        cols = re.findall(r'"([^"]+)"', args)
        if cols:
            out.append((tuple(cols), "unique = true" in args))
    return out


def parse(src):
    """Every @Entity in one Kotlin source file."""
    tables = []
    for m in re.finditer(r"@Entity\(", src):
        entity_args, end = _balanced(src, m.end() - 1)
        name_m = re.search(r'tableName\s*=\s*"([^"]+)"', entity_args)
        if not name_m:
            continue
        table = Table(name_m.group(1))
        table.indices = _indices(src[m.end() - 1:end + 1])

        cls = re.search(r"data class \w+\s*\(", src[end:])
        body, _ = _balanced(src, end + cls.end() - 1)

        pk_position = 0
        for raw in _split_params(body):
            param = raw.strip()
            if not param:
                continue
            is_pk = "@PrimaryKey" in param
            autogen = "autoGenerate = true" in param
            override_name, default = None, None
            col_info = re.search(r"@ColumnInfo\((.*?)\)", param, re.S)
            if col_info:
                nm = re.search(r'name\s*=\s*"([^"]+)"', col_info.group(1))
                if nm:
                    override_name = nm.group(1)
                dv = re.search(r'defaultValue\s*=\s*"(.*?)"\s*(?:,|$)', col_info.group(1), re.S)
                if dv:
                    default = dv.group(1)
            decl = re.search(r"\bval\s+(\w+)\s*:\s*([^=]+)", param)
            if not decl:
                continue
            field, ktype = decl.group(1), decl.group(2).strip()
            nullable = ktype.endswith("?")
            base = ktype.rstrip("?").strip()
            affinity = AFFINITY.get(base) or ("TEXT" if base in ENUMS else None)
            if affinity is None:
                raise ValueError(f"{table.name}.{field}: unmapped type {base!r}")
            if is_pk:
                pk_position += 1
            table.columns.append(Column(
                name=override_name or field,
                affinity=affinity,
                notnull=not nullable,
                pk=pk_position if is_pk else 0,
                default=default,
            ))
            if autogen:
                table.autoincrement = True
        tables.append(table)
    return tables


def from_sources(sources):
    """{table name: Table} across several Kotlin sources."""
    out = {}
    for src in sources:
        for t in parse(src):
            out[t.name] = t
    return out
