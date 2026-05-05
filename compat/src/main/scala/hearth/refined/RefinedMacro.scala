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
    v.semiEval match {
      case Right(validate) =>
        val result = validate.validate(tValue)
        if (!result.isPassed)
          Environment.reportErrorAndAbort(s"Predicate failed: ${validate.showResult(tValue, result)}")
      case Left(_) =>
        validateByPredicateType(tValue, Type[P].plainPrint)
    }
    Expr.quote { Refined.unsafeApply[T, P](Expr.splice(t)) }
  }

  private def validateByPredicateType[T](value: T, predicateType: String): Unit = {
    val valid = matchPredicate(value, predicateType)
    valid match {
      case Some(true) => ()
      case Some(false) =>
        Environment.reportErrorAndAbort(s"Predicate $predicateType failed for value: $value")
      case None =>
        Environment.reportErrorAndAbort(
          s"Cannot validate predicate $predicateType at compile time for value: $value. " +
            s"Use refineV for runtime validation instead."
        )
    }
  }

  private def matchPredicate[T](value: T, predicateType: String): Option[Boolean] = {
    val p = predicateType.trim
    if (p.contains("Not[") && p.endsWith("]")) {
      val inner = p.substring(p.indexOf("Not[") + 4, p.length - 1)
      matchPredicate(value, inner).map(!_)
    } else if (p.contains("And[") && p.endsWith("]")) {
      splitBinaryPredicate(p, "And[").flatMap { case (a, b) =>
        for { ra <- matchPredicate(value, a); rb <- matchPredicate(value, b) } yield ra && rb
      }
    } else if (p.contains("Or[") && p.endsWith("]")) {
      splitBinaryPredicate(p, "Or[").flatMap { case (a, b) =>
        for { ra <- matchPredicate(value, a); rb <- matchPredicate(value, b) } yield ra || rb
      }
    } else if (p.endsWith("Empty")) {
      matchEmpty(value)
    } else if (p.contains("Greater[")) {
      extractBound(p, "Greater[").flatMap(bound => asDouble(value).map(_ > bound))
    } else if (p.contains("Less[")) {
      extractBound(p, "Less[").flatMap(bound => asDouble(value).map(_ < bound))
    } else if (p.contains("GreaterEqual[")) {
      extractBound(p, "GreaterEqual[").flatMap(bound => asDouble(value).map(_ >= bound))
    } else if (p.contains("LessEqual[")) {
      extractBound(p, "LessEqual[").flatMap(bound => asDouble(value).map(_ <= bound))
    } else {
      None
    }
  }

  private def matchEmpty[T](value: T): Option[Boolean] = value match {
    case s: String      => Some(s.isEmpty)
    case i: Iterable[_] => Some(i.isEmpty)
    case _              => None
  }

  private def asDouble[T](value: T): Option[Double] = value match {
    case n: Number => Some(n.doubleValue)
    case _         => None
  }

  private def extractBound(predicateType: String, prefix: String): Option[Double] = {
    val start = predicateType.indexOf(prefix) + prefix.length
    val inner = predicateType.substring(start, predicateType.indexOf(']', start))
    val cleaned = inner.replaceAll("[^0-9.\\-]", "")
    if (cleaned.isEmpty || inner.contains("_0") || inner.endsWith(".0")) {
      if (inner.contains("_0") || inner.contains("_0]")) Some(0.0)
      else scala.util.Try(cleaned.toDouble).toOption
    } else {
      scala.util.Try(cleaned.toDouble).toOption
    }
  }

  private def splitBinaryPredicate(p: String, prefix: String): Option[(String, String)] = {
    val start = p.indexOf(prefix) + prefix.length
    val content = p.substring(start, p.length - 1)
    var depth = 0
    var commaPos = -1
    var i = 0
    while (i < content.length && commaPos < 0) {
      content.charAt(i) match {
        case '[' => depth += 1
        case ']' => depth -= 1
        case ',' if depth == 0 => commaPos = i
        case _                 => ()
      }
      i += 1
    }
    if (commaPos >= 0) Some((content.substring(0, commaPos).trim, content.substring(commaPos + 1).trim))
    else None
  }
}
