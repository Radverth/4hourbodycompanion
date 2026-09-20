# FourHourBody Companion

A single Android app for the set of *The 4-Hour Body* protocols actually in use — training,
nutrition, static stretches, sleep, cold exposure and creatine — with one adherence dashboard
across all of it.

Fully on-device: no backend, no login, no network calls.

## The six pillars

| Pillar | What it tracks |
| --- | --- |
| Training | Occam's Protocol sessions: 7+ reps to failure (10+ on leg press), 5s up / 5s down, exactly 3 minutes rest, rule-driven frequency |
| Static stretches | Three book-sourced routines — pre-kettlebell, rest-day mobility, desk reset — through one stretch engine |
| Nutrition | Slow-Carb / Hybrid rule compliance, meal tagging, cheat day with damage control |
| Sleep | Nightly checklist (temperature, darkness, screens, wine timing, cold exposure, wake time) plus an optional quality rating |
| Cold exposure | Shower / ice pack / pre-bed bath, timed or typed in |
| Creatine | 3.5 g twice daily across a 28-day cycle |

Every pillar is independently toggleable in Settings. Switching one off removes it from the
dashboard and cancels its reminders; nothing already logged is deleted.

## Protocol rules worth knowing

These live in `domain/`, not in the UI, and are covered by unit tests:

- **Failure target** is 7+ reps for everything except leg press, which is 10+.
- **A stall** is a miss of *more than one* rep. Exactly one rep short is not a stall.
- **A stall ends the session on the spot** — the remaining exercises are not run — and adds one
  rest day to every session that follows. Frequency starts at 2 rest days between sessions and
  grows from there; it is not a fixed Mon/Thu.
- **Progression** is +10 lb or +10%, whichever is greater, applied only when every exercise in
  the session hit its target. Rounded up to the nearest 0.5 kg so the step is never undercut.
- **A cheat day counts as adherent** when it is logged. It is part of the plan; damage control
  is a set of optional taps, not a requirement.
- **The wine check is one check**, combining count and timing, because the book's finding is
  about finishing 4+ hours before bed rather than the count alone.

## Project layout

```
app/src/main/java/com/tom/fourhourbody/
  data/          entities, DAOs, repositories, the single Room database, seed data
  domain/        protocol rules — progression, scheduling, creatine cycle, adherence
  notifications/ alarms, channels, the daily WorkManager refresh, boot handling
  ui/<pillar>/   Compose screens and their view models
  util/          monotonic timers, formatting, photo storage
app/src/main/assets/reference/   editable markdown reference content
app/src/test/                    unit tests for the protocol rules
```

## Things the implementation is deliberate about

- **Timers use `SystemClock.elapsedRealtime`**, read fresh on every tick, so the tempo guide and
  the Tabata timer stay accurate with the screen off. A Handler wall-clock loop drifts and deep
  sleep stops it dead.
- **Hold mode is calm** — a countdown with a single completion tone, no per-second beeping. Reps
  mode (Active Bridges) is a plain counter, not a timer wearing a counter's clothes.
- **One stretch engine.** The training session calls into it for glute activation and the
  pre-kettlebell hip flexor stretch; the standalone screens call the same code. Inline logs carry
  a session id, standalone logs do not, and that is the only difference.
- **Reference content is a bundled asset**, editable in-app (the edit is stored in app storage and
  can be reverted), so rules and the meal plan change without a rebuild.
- **Room migrations from the first schema change onward** — schemas are exported, and there is no
  destructive fallback. The log history is the point of the app.
- **The dashboard is all local Flow queries.** It is the screen opened most often.

## Building

Open in Android Studio (Ladybug or newer) and run, or:

```
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

Requires the Android SDK (compileSdk 35, minSdk 26) and JDK 17.

## Out of scope

No backend or login. No calorie/macro counting — nutrition tracking is rule compliance and meal
tagging, not a food database. Nothing from the book's sex/testosterone chapters, its sprinting,
polyphasic sleep, injection or powerlifting-record chapters. No wearable or health-platform
integration.
