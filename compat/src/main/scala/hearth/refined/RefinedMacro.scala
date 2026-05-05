package hearth
package refined

import eu.timepit.refined.api.{Refined, Validate}

trait RefinedMacro { this: MacroCommons =>

  def autoRefineImpl[T: Type, P: Type](
      t: Expr[T],
      v: Expr[Validate[T, P]]
  ): Expr[Refined[T, P]] = {
    val tValue = t.semiEval match {
      case Right(value) => value
      case Left(errors) =>
        Environment.reportErrorAndAbort(
          s"Cannot evaluate expression at compile time: ${errors.mkString(", ")}"
        )
    }
    val validate = v.semiEval match {
      case Right(value) => value
      case Left(errors) =>
        Environment.reportErrorAndAbort(
          s"Cannot evaluate Validate[${Type[T].plainPrint}, ${Type[P].plainPrint}] at compile time: ${errors.mkString(", ")}. " +
            s"AST: ${v.plainAST}. " +
            s"Use refineV for runtime validation instead."
        )
    }
    val result = validate.validate(tValue)
    if (!result.isPassed)
      Environment.reportErrorAndAbort(s"Predicate failed: ${validate.showResult(tValue, result)}")
    Expr.quote { Refined.unsafeApply[T, P](Expr.splice(t)) }
  }
}
