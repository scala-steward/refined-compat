package hearth
package refined

import eu.timepit.refined.api.{RefType, Refined, Validate}

import scala.language.implicitConversions
import scala.quoted.*

private class RefinedMacros(q: Quotes) extends MacroCommonsScala3(using q), RefinedMacro

object auto {

  implicit inline def autoRefineV[T, P](inline t: T)(implicit
      inline v: Validate[T, P]
  ): Refined[T, P] = ${ autoRefineVImpl[T, P]('t, 'v) }

  private def autoRefineVImpl[T: Type, P: Type](
      t: Expr[T],
      v: Expr[Validate[T, P]]
  )(using q: Quotes): Expr[Refined[T, P]] =
    new RefinedMacros(q).autoRefineImpl[T, P](t, v)

  implicit def autoUnwrap[F[_, _], T, P](tp: F[T, P])(implicit rt: RefType[F]): T =
    rt.unwrap(tp)
}
