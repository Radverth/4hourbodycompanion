#!/usr/bin/env python3
"""Run the newest Room migration against real SQLite and check what it produced.

A broken migration does not fail a build. It compiles, ships, and crashes on launch — on a
device holding the only copy of the log the app exists to keep. Room's own
`MigrationTestHelper` catches this, but it is an instrumented test and needs a device or
emulator, which this project's CI does not have.

This does the part that is checkable without Android:

  * builds the previous version's database from `app/schemas/<N-1>.json`, using the exact
    `CREATE TABLE` statements Room itself recorded there — not a reconstruction,
  * puts representative rows in it,
  * runs the new migration's SQL, read straight out of `Migrations.kt` so it cannot drift
    from the code that will actually run,
  * compares the result against what Room will expect at version N: against `<N>.json` when
    KSP has produced it, and otherwise against the entity sources,
  * checks the rows arrived the way the migration's comments claim.

Run it after a Gradle task that invokes KSP (`testDebugUnitTest` will do) and it compares
Room's own output on both sides, which is as close as this gets to the real thing. Run it
standalone and the target side falls back to parsing Kotlin, which it says.

Usage:
    tools/check_migration.py
"""

import pathlib
import re
import sqlite3
import sys

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
import roomschema  # noqa: E402

REPO = pathlib.Path(__file__).resolve().parent.parent
PKG = REPO / "app/src/main/java/com/tom/fourhourbody"
SCHEMA_DIR = REPO / "app/schemas/com.tom.fourhourbody.data.db.AppDatabase"
MIGRATIONS = PKG / "data/db/Migrations.kt"
DATABASE = PKG / "data/db/AppDatabase.kt"
ENTITY_DIR = PKG / "data/entity"

failures: list[str] = []


def fail(msg: str) -> None:
    failures.append(msg)
    print(f"  FAIL  {msg}")


def ok(msg: str) -> None:
    print(f"  ok    {msg}")


def target_version() -> int:
    m = re.search(r"version\s*=\s*(\d+)", DATABASE.read_text())
    if not m:
        sys.exit("could not read the schema version from AppDatabase.kt")
    return int(m.group(1))


def migration_statements(text: str, frm: int, to: int) -> list[str]:
    """The execSQL arguments of one migration object, in order."""
    anchor = re.search(
        rf"val MIGRATION_{frm}_{to}\s*=\s*object\s*:\s*Migration\({frm},\s*{to}\)", text
    )
    if not anchor:
        sys.exit(f"MIGRATION_{frm}_{to} not found in Migrations.kt")
    rest = text[anchor.end():]
    nxt = re.search(r"\nval [A-Z_]+", rest)
    block = rest[: nxt.start()] if nxt else rest

    out = []
    for m in re.finditer(r"execSQL\(", block):
        i, depth = m.end(), 1
        while depth:
            if block[i] == "(":
                depth += 1
            elif block[i] == ")":
                depth -= 1
                if depth == 0:
                    break
            i += 1
        arg = block[m.end():i].strip()
        tri = re.match(r'"""(.*?)"""', arg, re.S)
        if tri:
            out.append(tri.group(1).strip())
            continue
        single = re.match(r'"((?:[^"\\]|\\.)*)"\s*$', arg, re.S)
        if single:
            out.append(single.group(1).encode().decode("unicode_escape"))
            continue
        sys.exit(f"could not read an execSQL argument: {arg[:80]}")
    return out


def registered_migrations(text: str) -> set[tuple[int, int]]:
    """The version pairs actually listed in ALL_MIGRATIONS."""
    m = re.search(r"val ALL_MIGRATIONS[^=]*=\s*arrayOf\((.*?)\)", text, re.S)
    if not m:
        return set()
    return {
        (int(a), int(b))
        for a, b in re.findall(r"MIGRATION_(\d+)_(\d+)", m.group(1))
    }


def live_schema(conn: sqlite3.Connection) -> dict:
    tables = {}
    for (name,) in conn.execute(
        "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%'"
    ):
        tables[name] = {
            row[1]: (row[2].upper(), row[3], row[5])
            for row in conn.execute(f"PRAGMA table_info(`{name}`)")
        }
    return tables


def live_indices(conn: sqlite3.Connection, table: str) -> set:
    out = set()
    for row in conn.execute(f"PRAGMA index_list(`{table}`)"):
        idx_name, unique, origin = row[1], row[2], row[3]
        if origin != "c":  # skip the ones SQLite creates for UNIQUE/PK itself
            continue
        cols = tuple(r[2] for r in conn.execute(f"PRAGMA index_info(`{idx_name}`)"))
        out.add((cols, bool(unique)))
    return out


def seed(conn: sqlite3.Connection, tables: dict) -> None:
    """Representative rows, in whichever of these tables the previous schema has.

    Written to keep working for the next migration too, so a seed statement that no longer
    applies is reported and skipped rather than looking like a migration bug.
    """
    rows = [
        ("runs", "INSERT INTO runs (runNumber, startDate, restDaysAtStart) VALUES (1, 20000, 6)"),
        ("sessions",
         "INSERT INTO sessions (date, runId, completed, stalled, plateauedOnExercise) "
         "VALUES (20000, 1, 1, 0, NULL)"),
        ("sessions",
         "INSERT INTO sessions (date, runId, completed, stalled, plateauedOnExercise) "
         "VALUES (20007, 1, 1, 1, 'Leg press')"),
        ("exercise_logs",
         "INSERT INTO exercise_logs (sessionId, exerciseName, equipment, weightKg, tulSec, reps, restSecActual) "
         "VALUES (1, 'Leg press', 'Machine', 100.0, 95, 0, 0)"),
        ("exercise_logs",
         "INSERT INTO exercise_logs (sessionId, exerciseName, equipment, weightKg, tulSec, reps, restSecActual) "
         "VALUES (2, 'Leg press', 'Machine', 105.0, 71, 0, 45)"),
        ("exercise_configs",
         "INSERT INTO exercise_configs (slotName, exerciseName, equipment, isActive, orderIndex) "
         "VALUES ('Legs', 'Leg press', 'Machine', 1, 0)"),
        ("frequency_setting",
         "INSERT INTO frequency_setting (id, currentRestDaysBetweenSessions) VALUES (1, 7)"),
        ("settings", None),   # built from the schema: it has NOT NULL columns with no default
        ("measurements", "INSERT INTO measurements (date, weightKg) VALUES (20000, 82.5)"),
    ]
    for table, sql in rows:
        if table not in tables:
            continue
        statement = sql or _generic_insert(tables[table])
        try:
            conn.execute(statement)
        except sqlite3.Error as e:
            print(f"  note  could not seed {table}: {e}")
    conn.commit()


def _generic_insert(table) -> str:
    """One row filling every column the schema requires.

    Used where a table has NOT NULL columns with no default and listing them by hand would go
    stale the moment one is added. The values are placeholders — these rows exist to be
    carried through a migration, not to be realistic.
    """
    cols, values = [], []
    for col in table.columns:
        if not col.notnull and not col.pk:
            continue
        cols.append(f"`{col.name}`")
        if col.pk and table.autoincrement:
            values.append("NULL")
        elif col.pk:
            values.append("1")
        elif col.affinity == "TEXT":
            values.append("'x'")
        elif col.affinity == "REAL":
            values.append("1.0")
        else:
            values.append("1")
    return f"INSERT INTO `{table.name}` ({', '.join(cols)}) VALUES ({', '.join(values)})"


def check_data(conn: sqlite3.Connection, frm: int, to: int) -> None:
    """Assertions about one migration's effect on existing rows.

    Keyed by version so this keeps working for later migrations without anyone having to
    remember to prune it.
    """
    if (frm, to) != (6, 7):
        print("  note  no data assertions recorded for this migration — schema only")
        return

    kinds = conn.execute("SELECT DISTINCT kind FROM sessions").fetchall()
    if kinds and kinds != [("STANDARD",)]:
        fail(f"existing sessions did not default to STANDARD: {kinds}")
    else:
        ok("existing sessions read back as standard sessions, which is what they were")

    rows = conn.execute(
        "SELECT exerciseName, weightKg, tulSec, position FROM exercise_logs ORDER BY id"
    ).fetchall()
    if len(rows) != 2:
        fail(f"exercise_logs has {len(rows)} rows, expected 2 — logged sets were lost")
    elif any(r[3] is not None for r in rows):
        fail(f"position was invented on existing rows: {rows}")
    else:
        ok("logged sets intact, with position null rather than guessed")

    rest, cutting = conn.execute(
        "SELECT currentRestDaysBetweenSessions, cuttingPhaseActive FROM frequency_setting"
    ).fetchone()
    if rest != 7:
        fail(f"rest days changed from 7 to {rest} — this migration should not touch them")
    elif cutting != 0:
        fail("cutting phase defaulted on")
    else:
        ok("rest days untouched, cutting phase off by default")

    big_three = conn.execute("SELECT bigThreeOnly FROM settings").fetchone()
    if big_three and big_three[0] != 0:
        fail("Big Three toggle defaulted on")
    else:
        ok("Big Three toggle off by default")

    conn.execute(
        "INSERT INTO sticking_point_logs (sessionId, date, exerciseName, technique) "
        "VALUES (2, 20007, 'Leg press', 'REST_PAUSE')"
    )
    conn.execute(
        "INSERT INTO sticking_point_logs (sessionId, date, exerciseName, technique) "
        "VALUES (NULL, 20008, 'Leg press', 'NEGATIVE_ONLY')"
    )
    ok("sticking-point rows insert, with and without a session")


def main() -> int:
    to = target_version()
    frm = to - 1

    previous_json = SCHEMA_DIR / f"{frm}.json"
    if not previous_json.exists():
        print(f"no exported schema at {previous_json.relative_to(REPO)} — nothing to check against")
        return 0

    migrations_text = MIGRATIONS.read_text()
    if (frm, to) not in registered_migrations(migrations_text):
        fail(f"MIGRATION_{frm}_{to} is not listed in ALL_MIGRATIONS, so Room will never run it")
        return 1

    print(f"== migration {frm} -> {to} ==\n")

    before = roomschema.from_json(previous_json)
    print(f"v{frm}: {len(before)} tables, from Room's own exported schema")

    target_json = SCHEMA_DIR / f"{to}.json"
    if target_json.exists():
        after = roomschema.from_json(target_json)
        source = f"Room's exported schema ({to}.json)"
        exact = True
    else:
        after = roomschema.from_sources(
            [p.read_text() for p in sorted(ENTITY_DIR.glob("*.kt"))]
        )
        source = "the Kotlin entity sources (no exported schema yet — run KSP for the exact one)"
        exact = False
    print(f"v{to}: {len(after)} tables, from {source}\n")

    conn = sqlite3.connect(":memory:")
    for table in before.values():
        conn.execute(table.create_sql())
        for sql in table.index_sql():
            conn.execute(sql)
    seed(conn, before)

    statements = migration_statements(migrations_text, frm, to)
    print(f"running {len(statements)} statements from Migrations.kt")
    for n, sql in enumerate(statements, 1):
        try:
            conn.execute(sql)
        except sqlite3.Error as e:
            fail(f"statement {n} failed: {e}\n        {sql.splitlines()[0][:100]}")
            print("\nmigration aborted — later checks would be meaningless")
            return 1
    conn.commit()
    ok("every statement executed")

    print("\n-- schema --")
    live = live_schema(conn)
    for name, expected in sorted(after.items()):
        if name not in live:
            fail(f"table `{name}` missing after migration")
            continue
        got = live[name]
        for col in expected.columns:
            if col.name not in got:
                fail(f"{name}.{col.name} missing")
                continue
            affinity, notnull, pk = got[col.name]
            if affinity != col.affinity:
                fail(f"{name}.{col.name} affinity {affinity} != expected {col.affinity}")
            if bool(notnull) != col.notnull:
                fail(f"{name}.{col.name} notnull {bool(notnull)} != expected {col.notnull}")
            if bool(pk) != bool(col.pk):
                fail(f"{name}.{col.name} primary-key {bool(pk)} != expected {bool(col.pk)}")
        extra = set(got) - {c.name for c in expected.columns}
        if extra:
            fail(f"{name} has columns no entity declares: {sorted(extra)}")
        missing_idx = set(expected.indices) - live_indices(conn, name)
        if missing_idx:
            fail(f"{name} missing indices {sorted(missing_idx)}")

    leftover = set(live) - set(after) - {"android_metadata", "room_master_table"}
    if leftover:
        fail(f"tables left behind that no entity claims: {sorted(leftover)}")
    if not failures:
        ok(f"all {len(after)} tables match what Room expects at version {to}")

    print("\n-- declared defaults --")
    # Room refuses to open a database when an entity declares a column default the table does
    # not carry. It is invisible in a diff and fatal on launch.
    checked, default_failures = 0, 0
    for name, expected in sorted(after.items()):
        if name not in live:
            continue
        ddl = conn.execute(
            "SELECT sql FROM sqlite_master WHERE type='table' AND name=?", (name,)
        ).fetchone()[0]
        for col in expected.columns:
            if col.default is None:
                continue
            checked += 1
            pattern = rf"`?{re.escape(col.name)}`?[^,]*?DEFAULT\s+{re.escape(col.default)}"
            if not re.search(pattern, ddl, re.I):
                fail(
                    f"{name}.{col.name} declares DEFAULT {col.default} but the table does "
                    f"not have it — Room refuses to open this database"
                )
                default_failures += 1
    if not checked:
        ok("no entity declares a column default")
    elif not default_failures:
        ok(f"{checked} declared default(s) present in the live schema")

    print("\n-- data --")
    check_data(conn, frm, to)

    print()
    if failures:
        print(f"FAILED: {len(failures)} problem(s)")
        return 1
    if not exact:
        print(f"migration {frm} -> {to} is sound against real SQLite")
        print("note: the target schema came from parsing Kotlin. Run this after a Gradle task")
        print("      that invokes KSP to compare against Room's own output instead.")
        return 0
    print(f"migration {frm} -> {to} is sound against real SQLite, both sides from Room's own schemas")
    return 0


if __name__ == "__main__":
    sys.exit(main())
