"""Derive the SQLite schema Room will expect from a set of Kotlin @Entity sources.

Room validates a migrated database by reading each table back and comparing it, column by
column, with what the entities declare. This reproduces the entity side of that comparison so
a migration can be checked against the real entity definitions instead of against a
hand-copied idea of them.

Only the primary constructor is read. Anything in the class body is a computed property and
has no column.
"""

import re

# Kotlin type -> SQLite affinity, after the project's TypeConverters have had their say.
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
ENUMS = {
    "RunEnd", "DietMode", "StretchRoutine", "StretchMode", "ColdExposureType",
    "SessionKind", "PlateauTechnique", "Pillar",
}


class Column:
    def __init__(self, name, affinity, notnull, pk, default):
        self.name = name
        self.affinity = affinity
        self.notnull = notnull
        self.pk = pk           # 0 = not part of the primary key, else 1-based position
        self.default = default  # only what @ColumnInfo(defaultValue=...) declares

    def __repr__(self):
        return (f"{self.name} {self.affinity}"
                f"{' NOT NULL' if self.notnull else ''}"
                f"{' PK' if self.pk else ''}"
                f"{f' DEFAULT {self.default}' if self.default else ''}")


class Table:
    def __init__(self, name):
        self.name = name
        self.columns = []
        self.indices = []     # (columns tuple, unique)
        self.autoincrement = False

    def column(self, name):
        return next((c for c in self.columns if c.name == name), None)

    def create_sql(self):
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
        out = []
        for cols, unique in self.indices:
            name = ("index_" + self.name + "_" + "_".join(cols))
            kind = "UNIQUE INDEX" if unique else "INDEX"
            cols_sql = ", ".join(f"`{c}`" for c in cols)
            out.append(f"CREATE {kind} IF NOT EXISTS `{name}` ON `{self.name}` ({cols_sql})")
        return out


def _split_params(body):
    """Split a constructor parameter list on commas that are at depth zero."""
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


def _constructor_body(src, start):
    """Everything between the data class's opening paren and its match."""
    depth = 0
    for i in range(start, len(src)):
        if src[i] == "(":
            depth += 1
        elif src[i] == ")":
            depth -= 1
            if depth == 0:
                return src[start + 1:i]
    raise ValueError("unbalanced constructor")


def _indices(entity_args):
    """Parse the indices = [...] argument of @Entity."""
    out = []
    m = re.search(r"indices\s*=\s*\[(.*?)\]\s*\)", entity_args, re.S)
    if not m:
        return out
    inner = m.group(1)
    for idx in re.finditer(r"Index\((.*?)\)", inner, re.S):
        args = idx.group(1)
        unique = "unique = true" in args
        cols = re.findall(r'"([^"]+)"', args)
        if cols:
            out.append((tuple(cols), unique))
    return out


def parse(src):
    """Every @Entity in one Kotlin source file."""
    tables = []
    for m in re.finditer(r"@Entity\(", src):
        head_start = m.end() - 1
        depth, end = 0, None
        for i in range(head_start, len(src)):
            if src[i] == "(":
                depth += 1
            elif src[i] == ")":
                depth -= 1
                if depth == 0:
                    end = i
                    break
        entity_args = src[head_start:end + 1]
        name_m = re.search(r'tableName\s*=\s*"([^"]+)"', entity_args)
        if not name_m:
            continue
        table = Table(name_m.group(1))
        table.indices = _indices(entity_args)

        cls = re.search(r"data class \w+\s*\(", src[end:])
        body = _constructor_body(src[end:], end + cls.end() - 1 - end + (end - end))
        # The slice above is relative to src[end:]; redo it plainly to avoid confusion.
        abs_paren = end + cls.end() - 1
        body = _constructor_body(src, abs_paren)

        pk_position = 0
        for raw in _split_params(body):
            param = raw.strip()
            if not param:
                continue
            is_pk = "@PrimaryKey" in param
            autogen = "autoGenerate = true" in param
            col_info = re.search(r"@ColumnInfo\((.*?)\)", param, re.S)
            override_name = None
            default = None
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


def parse_files(sources):
    """{table name: Table} across several Kotlin sources."""
    out = {}
    for src in sources:
        for t in parse(src):
            out[t.name] = t
    return out
