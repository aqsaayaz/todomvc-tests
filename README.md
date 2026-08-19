# TodoMVC Cross-Framework Test Suite

One TestNG suite, run **unchanged** against the React, Vue, and Angular
builds of [TodoMVC](https://todomvc.com), driven by a deterministic seeded
action generator and verified against an independent expected-state model.

## How it works (flow)

```
seed
  │
  ▼
ActionGenerator (java.util.Random(seed))
  │  produces List<Action> — deterministic, framework-agnostic
  ▼
CrossFrameworkTest.seededActionSequenceMatchesModel()
  │
  ▼
for each Action:
  BaseTodoTest.executeAction(action)
      │
      ├─► TodoPage   — drives the real app (Selenium, no fixed sleeps,
      │                 all waits are explicit/condition-based)
      │
      └─► TodoModel  — mutated independently, in parallel, from the SAME
                        action (never reads the DOM)
      │
      ▼
  assertStateMatches()
      compares: visible todo texts, completion states, active filter,
      remaining count — TodoPage (actual) vs TodoModel (expected)
      │
      ▼ on any mismatch or exception
  TestReporter.reportFailure()
      writes: seed, full action history (failing step marked), expected
      state, actual state, screenshot, trace → target/failure-reports/
```

Which framework is targeted is resolved once per TestNG `<test>` block
(`testng.xml`) via the `framework` parameter, read in
`BaseTodoTest.setUp()`. **`CrossFrameworkTest` itself contains no
framework-specific code** — that's what "runs unchanged" means concretely:
same class, same bytecode, same assertions, three different base URLs.

## Project layout

```
todomvc-tests/
├── pom.xml                 Maven build (Selenium 4, TestNG 7, Allure, WebDriverManager)
├── testng.xml              3 <test> blocks (react/vue/angular), same test class, parallel
├── src/main/java/com/todomvc/
│   ├── config/TestConfig.java       resolves framework/seed/baseUrl/headless from -D flags
│   ├── enums/Framework.java         react/vue/angular ids + default URLs
│   ├── enums/FilterType.java        ALL/ACTIVE/COMPLETED
│   ├── models/Todo.java             single todo value object
│   ├── models/TodoModel.java        independent expected-state oracle
│   ├── generators/ActionGenerator.java   seeded, deterministic action sequence
│   ├── pages/TodoPage.java          Page Object — shared TodoMVC DOM contract
│   └── utils/Action.java            action type + payload (extensible)
│   └── utils/TestReporter.java      failure artifact capture
└── src/test/java/com/todomvc/tests/
    ├── BaseTodoTest.java            driver lifecycle + action dispatch + assertions
    └── CrossFrameworkTest.java      the actual test (framework-agnostic)
```

## Running

```bash
# All three frameworks, in parallel, same fixed default seed
mvn test

# Single framework, explicit seed
mvn test -Dframework=vue -Dseed=42

# Different action count (default 30)
mvn test -Dframework=react -Dseed=42 -DactionCount=50

# Point at a locally-hosted build instead of todomvc.com
mvn test -Dframework=angular -DbaseUrl=http://localhost:8080/

# Non-headless (debugging)
mvn test -Dframework=react -Dheadless=false
```

Allure report:
```bash
mvn allure:serve
```

## Determinism guarantee

`ActionGenerator(seed).generate(n)` uses a single `java.util.Random(seed)`
and no other entropy source (no wall clock, no DOM reads, no thread
ordering dependence). The same seed always produces the same ordered list
of `Action` objects, including their text payloads and target indices —
independently of which framework the suite is later pointed at. This is
what makes a failure reproducible: re-run with the same `-Dseed`.

## No anti-patterns

- **No duplicated tests** — one `@Test` method, one dispatch switch; three
  frameworks are three `testng.xml` `<test>` blocks pointing the same
  class at different URLs, not three copies of test logic.
- **No fixed sleeps** — every `TodoPage` interaction waits on an explicit
  `WebDriverWait` condition (element count change, class attribute change,
  editing-mode exit), never `Thread.sleep`.
- **Expected state is never derived from the app** — `TodoModel` is
  mutated only by replaying the same `Action` fed to `TodoPage`; the two
  are computed independently and then diffed.

## On failure

`TestReporter` writes to `target/failure-reports/<framework>/<seed>-<timestamp>/`:
- `report.txt` — seed, full action history with the failing step marked,
  expected state, actual state, exception (if any)
- `screenshot.png`
- `trace.zip` (if trace capture is wired up — see the hook comment in
  `BaseTodoTest.failWithReport`)

All of the above are also attached to the Allure report.

## Extending the suite: TOGGLE_ALL (added during review)

Requirement: add a new `TOGGLE_ALL` action **without rewriting the
framework**. Here is the complete diff surface — three small, additive
changes, nothing structural:

1. **`Action.java`** — add `TOGGLE_ALL` to the `ActionType` enum and a
   `Action.toggleAll()` factory method.
2. **`TodoModel.java`** — add a `toggleAll()` mutator implementing TodoMVC
   semantics (if not all completed → complete all; if all completed →
   activate all).
3. **`TodoPage.java`** — add a `toggleAll()` method that clicks
   `.toggle-all` and waits for all visible items to reach a consistent
   completed state.
4. **`BaseTodoTest.executeAction()`** — add one `case TOGGLE_ALL:` calling
   both of the above.
5. **`ActionGenerator.java`** *(optional)* — add `TOGGLE_ALL` to the
   weighted action mix if you want it seeded automatically; otherwise it's
   usable directly via `Action.toggleAll()` in any hand-written test.

No changes were needed to: `CrossFrameworkTest`, `testng.xml`,
`TestConfig`, `TestReporter`, `Todo`, `FilterType`, `Framework`, or the
generator's core algorithm/determinism contract. This is the extension
point the whole design (single dispatch switch keyed off an open enum,
model/page kept in lockstep per action) was built around.
