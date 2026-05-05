# refined-compat

Compile-time refinement validation for [refined](https://github.com/fthomas/refined) types on Scala 3, powered by [Hearth](https://github.com/kubuszok/hearth)'s `semiEval`.

## Problem

Refined's `import eu.timepit.refined.auto._` provides implicit macros that validate literal assignments at compile time on Scala 2. These macros rely on `c.eval` which was never ported to Scala 3 ([refined#932](https://github.com/fthomas/refined/issues/932)), so code like this fails on Scala 3:

```scala
import eu.timepit.refined.auto._
val x: Int Refined Positive = 5 // works on Scala 2, fails on Scala 3
```

## Solution

This library provides `import hearth.refined.auto._` as a drop-in replacement that works on both Scala 2.13 and Scala 3:

```scala
import hearth.refined.auto._
val x: Int Refined Positive = 5    // compiles on both Scala 2 and 3
val y: String Refined NonEmpty = "" // fails at compile time on both
```

## Setup

```scala
// build.sbt
libraryDependencies += "com.kubuszok" %% "refined-compat" % "<version>"
```

Replace `import eu.timepit.refined.auto._` with `import hearth.refined.auto._`.

## Supported predicates

The following predicates are validated at compile time:

- **Numeric**: `Positive`, `NonNegative`, `Negative`, `NonPositive` (and underlying `Greater[N]`, `Less[N]`, `GreaterEqual[N]`, `LessEqual[N]`)
- **Collection**: `NonEmpty`, `Empty`
- **Boolean combinators**: `Not[P]`, `And[A, B]`, `Or[A, B]`

For predicates not yet supported at compile time, use `refineV` for runtime validation.

## How it works

1. Uses Hearth's `semiEval` to extract the literal value from the expression AST at compile time
2. Tries to evaluate the `Validate[T, P]` instance via `semiEval` (works for simple instances)
3. Falls back to matching the predicate type `P` for common refined predicates
4. If validation passes, emits `Refined.unsafeApply(value)`; if it fails, aborts compilation with an error message

## License

Apache 2.0
