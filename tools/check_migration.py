#!/usr/bin/env python3
"""Run the newest Room migration against real SQLite and check what it produced.

A broken migration does not fail a build. It compiles, ships, and crashes on launch — on a
device that is holding the only copy of the log the app exists to keep. Room's own
`MigrationTestHelper` catches this, but it is an instrumented test and needs a device or
emulator, which this project's CI does not have.

This does the part that is checkable without Android:

  * builds the previous schema from the entity sources at the base revision,
  * puts representative rows in it,
  * runs the migration's SQL, read straight out of Migrations.kt so it cannot drift from the
    code that will actually run,
  * compares the result with the schema Room will derive from the current entities — column
    names, affinities, nullability, primary keys, declared defaults and indices,
  * checks the rows arrived the way the migration's comments claim.

What it does not check: that the committed entities at the base revision are what is on any
particular phone, and anything Room decides at runtime beyond schema identity. It is a floor,
not a substitute for running the thing on a device.

Usage:
    tools/check_migration.py [base-ref]

`base-ref` defaults to the merge base with origin/main, which on a pull request is the schema
the migration will actually be upgrading from. Exits 0 when the versions match and there is
nothing to check.
"""

import os
import pathlib
import re
import sqlite3
import subprocess
import sys

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
import roomschema  # noqa: E402

REPO = pathlib.Path(__file__).resolve().parent.parent
PKG = "app/src/main/java/com/tom/fourhourbody"
ENTITY_DIR = f"{PKG}/data/entity"
MIGRATIONS = REPO / PKG / "data/db/Migrations.kt"
DATABASE = REPO / PKG / "data/db/AppDatabase.kt"

failures: list[str] = []


def fail(msg: str) -> None:
    failures.append(msg)
    print(f"  FAIL  {msg}")


def ok(msg: str) -> None:
    print(f"  ok    {msg}")


def git(*args: str) -> str:
    return subprocess.run(
        ["git", *args], cwd=REPO, capture_output=True, text=True, check=True
    ).stdout


def resolves(ref: str) -> bool:
    try:
        git("rev-parse", "--verify", f"{ref}^{{commit}}")
        return True
    except subprocess.CalledProcessError:
        return False


def base_ref(argv: list[str]) -> str:
    """The revision whose schema this migration upgrades from.

    An explicit argument wins, then MIGRATION_BASE_SHA — CI sets that from the event, which
    names the base commit exactly instead of inferring it. The remaining candidates are
    guesses for a local run, in descending order of how likely they are to be right.
    """
    if len(argv) > 1:
        return argv[1]

    from_ci = os.environ.get("MIGRATION_BASE_SHA", "").strip()
    if from_ci and resolves(from_ci):
        return from_ci

    for candidate in ("origin/main", "main"):
        if not resolves(candidate):
            continue
        try:
            return git("merge-base", "HEAD", candidate).strip()
        except subprocess.CalledProcessError:
            continue
    return "HEAD~1"


def schema_version(text: str) -> int:
    m = re.search(r"version\s*=\s*(\d+)", text)
    if not m:
        sys.exit("could not read the schema version from AppDatabase.kt")
    return int(m.group(1))


def entity_sources(ref: str | None) -> list[str]:
    if ref is None:
        return [
            (REPO / n).read_text()
            for n in git("ls-files", f"{ENTITY_DIR}/").split()
        ]
    names = git("ls-tree", "--name-only", ref, f"{ENTITY_DIR}/").split()
    return [git("show", f"{ref}:{n}") for n in names]


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
    """Representative rows in whichever of these tables the base schema actually has.

    Written defensively: this script is meant to keep working for the *next* migration too,
    and a seed statement that fails because a column has since been renamed should not look
    like a migration bug. Anything that will not insert is reported and skipped.
    """
    rows = [
        ("runs", "INSERT INTO runs (runNumber, startDate, restDaysAtStart) VALUES (1, 20000, 2)"),
        ("sessions", "INSERT INTO sessions (date, runId, completed, stalled) VALUES (20000, 1, 1, 0)"),
        ("sessions", "INSERT INTO sessions (date, runId, completed, stalled) VALUES (20007, 1, 1, 1)"),
        ("sleep_logs", """
            INSERT INTO sleep_logs (date, roomTempOk, socksUsed, darkness, noScreensBeforeBed,
                wineWithinLimit, coldExposureBeforeBed, consistentWakeTime, qualityRating)
            VALUES (20000, 1, 0, 1, 1, 1, 0, 1, 4)"""),
        ("cold_exposure_logs",
         "INSERT INTO cold_exposure_logs (date, type, durationSec, notes) VALUES (20000, 'SHOWER', 120, 'brisk')"),
        ("frequency_setting",
         "INSERT INTO frequency_setting (id, currentRestDaysBetweenSessions) VALUES (1, 3)"),
    ]
    for table, sql in rows:
        if table not in tables:
            continue
        try:
            conn.execute(sql)
        except sqlite3.Error as e:
            print(f"  note  could not seed {table}: {e}")

    # The pre-TUL exercise log shape, which is what the 5 -> 6 data carry-over acts on.
    if "exercise_logs" in tables and "reps" in {c.name for c in tables["exercise_logs"].columns}:
        conn.executemany(
            """INSERT INTO exercise_logs
               (sessionId, exerciseName, equipment, weightKg, reps, targetReps, tempo, restSecActual)
               VALUES (?,?,?,?,?,?,?,?)""",
            [
                (1, "Leg press", "Machine", 100.0, 10, 10, "5/5", 0),
                (1, "Chest press", "Machine", 40.0, 7, 7, "5/5", 180),
                (2, "Leg press", "Machine", 110.0, 6, 10, "5/5", 0),
            ],
        )
    if "exercise_configs" in tables:
        cols = {c.name for c in tables["exercise_configs"].columns}
        if "targetReps" in cols:
            conn.execute(
                """INSERT INTO exercise_configs
                   (slotName, exerciseName, equipment, targetReps, isActive, orderIndex)
                   VALUES ('Legs', 'Leg press', 'Machine', 10, 1, 0)"""
            )
    conn.commit()


def check_data(conn: sqlite3.Connection, frm: int, to: int) -> None:
    """Assertions specific to one migration's data carry-over.

    Keyed by version so the generic schema comparison above keeps working for later
    migrations without anyone having to remember to come back and prune this.
    """
    if (frm, to) != (5, 6):
        print("  note  no data assertions recorded for this migration — schema only")
        return

    rows = conn.execute(
        "SELECT exerciseName, tulSeconds, reps, tulDerived, repCadenceSec, "
        "targetTulMinSec, targetTulMaxSec FROM exercise_logs ORDER BY id"
    ).fetchall()
    if len(rows) != 3:
        fail(f"exercise_logs has {len(rows)} rows, expected 3 — logged sets were lost")
    bad = False
    for name, tul, reps, derived, cadence, lo, hi in rows:
        if tul != reps * 10:
            fail(f"{name}: tulSeconds {tul} is not reps*10 ({reps * 10})")
            bad = True
        if derived != 1:
            fail(f"{name}: tulDerived is {derived}, so an inferred TUL would read as measured")
            bad = True
        if (cadence, lo, hi) != ("10/10", 60, 90):
            fail(f"{name}: carried-over defaults wrong: {cadence} {lo}-{hi}")
            bad = True
    if rows and not bad:
        ok("old rep counts became a marked, derived TUL with the rep count kept beside it")

    cfg = conn.execute("SELECT COUNT(*) FROM exercise_configs").fetchone()[0]
    if cfg != 0:
        fail(f"exercise_configs has {cfg} rows; the migration leaves it for the seeder")
    else:
        ok("exercise_configs emptied for the seeder to refill with the Big Five")

    rest, cutting = conn.execute(
        "SELECT currentRestDaysBetweenSessions, cuttingPhaseActive FROM frequency_setting"
    ).fetchone()
    if rest != 7:
        fail(f"rest days are {rest}, expected the new protocol's floor of 7")
    elif cutting != 0:
        fail("cutting phase defaulted on")
    else:
        ok("rest days raised to the protocol's floor of 7, cutting phase off")

    kinds = conn.execute("SELECT DISTINCT kind FROM sessions").fetchall()
    if kinds and kinds != [("STANDARD",)]:
        fail(f"existing sessions did not default to STANDARD: {kinds}")
    else:
        ok("existing sessions defaulted to STANDARD")

    cold = conn.execute("SELECT type, durationSec FROM cold_exposure_logs").fetchall()
    if cold != [("SHOWER", 120)]:
        fail(f"cold exposure row changed: {cold}")
    else:
        ok("cold exposure row intact with durationSec now nullable")
    conn.execute(
        "INSERT INTO cold_exposure_logs (date, type, durationSec) "
        "VALUES (20001, 'COLD_WATER_DRINK', NULL)"
    )
    ok("a cold-water row with no duration inserts")

    sleep = conn.execute("SELECT hoursSlept, qualityRating FROM sleep_logs").fetchone()
    if sleep != (None, 4):
        fail(f"sleep row: {sleep}")
    else:
        ok("hours slept added as null on existing nights, rating untouched")


def main() -> int:
    ref = base_ref(sys.argv)
    to = schema_version(DATABASE.read_text())
    try:
        frm = schema_version(git("show", f"{ref}:{PKG}/data/db/AppDatabase.kt"))
    except subprocess.CalledProcessError:
        print(f"could not read AppDatabase.kt at {ref} — skipping")
        return 0

    if frm == to:
        print(f"schema version unchanged at {to} — no migration to check")
        return 0
    if to != frm + 1:
        print(
            f"schema went {frm} -> {to} in one change. This checks a single step; "
            f"run it once per migration with an explicit base ref."
        )
        return 1

    print(f"== migration {frm} -> {to} (base {ref[:12]}) ==\n")

    before = roomschema.parse_files(entity_sources(ref))
    after = roomschema.parse_files(entity_sources(None))
    print(f"v{frm}: {len(before)} tables    v{to}: {len(after)} tables\n")

    conn = sqlite3.connect(":memory:")
    for table in before.values():
        conn.execute(table.create_sql())
        for sql in table.index_sql():
            conn.execute(sql)
    seed(conn, before)

    statements = migration_statements(MIGRATIONS.read_text(), frm, to)
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
        ok(f"all {len(after)} tables match the entity declarations")

    print("\n-- declared defaults --")
    # Room rejects a database at startup when an entity declares a column default the table
    # does not carry. It is invisible in a diff and fatal on launch.
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
    print(f"migration {frm} -> {to} is sound against real SQLite")
    return 0


if __name__ == "__main__":
    sys.exit(main())
