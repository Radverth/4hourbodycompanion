# Big Five Companion

A single Android app for one thing: running *Body by Science*'s Big Five workout and keeping
the log that the protocol's progression and frequency rules actually depend on.

Fully on-device: no backend, no login, no network calls.

## The protocol

Five machine exercises — leg press, pulldown, seated row, chest press, overhead press — each
taken to one set of positive muscular failure. Nothing else. The book's own case for this is
that a single, properly loaded set inroads the muscle as effectively as several, and that the
rest of the work is in recovering from it, not repeating it.

| What | How |
| --- | --- |
| A set | One set per exercise, to positive failure — not a rep count, a genuine inability to move the weight further |
| Cadence | A guide, not a rule: roughly 10 seconds up, 10 seconds down, as slow as you can manage without the movement turning into stops and starts |
| The real measure | Time under load (TUL), timed on a stopwatch from the first rep to failure |
| Progression | A set that runs past 90 seconds earns a 5% weight increase next session — the resistance was too light to reach failure in a meaningful window |
| A plateau | An exercise that fails to beat its own last time under load, at the same or less weight, closes the run and buys another rest day |
| Frequency | Rule-driven, starting at once every seven days and growing as plateaus accumulate — not a fixed schedule |

Progress and body measurements (weight, waist, hip, photos) are tracked alongside training,
since the book's own case for the protocol includes what it does for body composition — but
this app makes no claims about diet.

## Protocol rules worth knowing

These live in `domain/`, not in the UI, and are covered by unit tests:

- **A set ends on positive failure**, not on a rep count. The book's whole case for timing the
  set instead of counting reps is that a slower final rep near failure is expected, not a
  fault — counting reps would misrepresent exactly the part of the set doing the most.
- **90 seconds is the ceiling.** A set still going at 90 seconds means the weight was too
  light to reach genuine failure in a meaningful window, and earns a 5% bump next time.
- **A plateau is not a failure state.** It is the protocol's own signal that the gap between
  sessions is too short: an exercise that cannot beat its last time under load at the same or
  lower weight means recovery, not effort, is now the limit.
- **A plateau on any exercise closes the run** and adds one rest day to every session that
  follows — but nothing in the book supports cutting a session short over it, so every
  exercise in a session always runs; only what happens *after* changes.
- **The weight step is +5%**, rounded up to the nearest 0.5 kg so the step is never quietly
  undercut. The book's own figure is "5 to 10 percent"; this app applies the conservative end.
- **Frequency starts at once every seven days** and grows by one rest day per plateau — not a
  fixed Mon/Thu/Sat split, and not capped, since the book is explicit that trainees who keep
  progressing eventually need ten days, then more.

## Project layout

```
app/src/main/java/com/tom/fourhourbody/
  data/          entities, DAOs, repositories, the single Room database, seed data
  domain/        protocol rules — progression, scheduling, plateau detection
  notifications/ alarms, channels, the daily WorkManager refresh, boot handling
  ui/training/   the guided session, exercise config, run/session history
  ui/dashboard/  Today — one hero card, nothing else competing for attention
  ui/deck/       the character sheet: exercise loadout, attributes, milestones
  ui/progress/   weight/waist/hip measurements and progress photos
  util/          monotonic timers, formatting, photo storage
app/src/test/    unit tests for the protocol rules
```

## Things the implementation is deliberate about

- **The stopwatch uses `SystemClock.elapsedRealtime`**, read fresh on every tick, so time
  under load stays accurate with the screen off. A Handler wall-clock loop drifts and deep
  sleep stops it dead.
- **The working set is a stopwatch, not a rep counter.** Counting reps would need a fixed
  cadence to convert into a time, and the book is explicit that the cadence slows near
  failure — so the set is timed directly instead.
- **Every exercise in a session always runs.** Nothing in the book supports stopping a session
  early over one exercise's result; only the frequency for the *next* block changes.
- **Room migrations from the first schema change onward** — schemas are exported, and there is
  no destructive fallback. The log history is the point of the app, including the sessions
  logged under the app's previous protocol.
- **The dashboard is all local Flow queries.** It is the screen opened most often.

## Building

Open in Android Studio (Ladybug or newer) and run, or:

```
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

Requires the Android SDK (compileSdk 35, minSdk 26) and JDK 17.

## Out of scope

No backend or login. No nutrition, sleep, cold exposure or creatine tracking — those were
protocols from a different book, and this app is scoped to what *Body by Science* actually
covers: the Big Five and the recovery it depends on. No conditioning work (kettlebells,
intervals, ab circuits) folded into the session — the book keeps those separate from the
strength protocol, and so does this app. No calorie or macro tracking. No wearable or
health-platform integration.
