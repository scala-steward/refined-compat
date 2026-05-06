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

## Provided API

### `auto._` — implicit conversions

| Method | What it does |
|--------|-------------|
| `autoRefineV` | Implicit `T` → `Refined[T, P]` with compile-time validation |
| `autoInfer` | Implicit `Refined[T, A]` → `Refined[T, B]` when `Inference[A, B]` holds |
| `autoUnwrap` | Implicit `F[T, P]` → `T` (unwrapping) |

### `refineMV[P](value)` — explicit compile-time validation

```scala
import hearth.refined.refineMV
import eu.timepit.refined.numeric.Positive

val x = refineMV[Positive](42)   // compiles
val y = refineMV[Positive](-1)   // compilation error
```

### `RefinedTypeOpsM[FTP, T]` — custom refined type companions

```scala
import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.Positive
import hearth.refined.RefinedTypeOpsM

type PosInt = Int Refined Positive
object PosInt extends RefinedTypeOpsM[PosInt, Int]

val x = PosInt(42)   // compiles, validated at compile time
val y = PosInt(-1)   // compilation error
```

## How it works

1. Uses Hearth's `semiEval` to extract the value from the expression AST at compile time
2. Uses `semiEval` to evaluate the `Validate[T, P]` (or `Inference[A, B]`) instance — this works for any predicate whose `Validate` instance can be reconstructed from the classpath, including predicates that use lambdas, blocks, and inherited methods
3. Calls `validate.validate(value)` on the reconstructed instance at macro expansion time
4. If validation passes, emits `Refined.unsafeApply(value)`; if it fails, aborts compilation with an error message

## License

Apache 2.0
