package hearth
package refined

import eu.timepit.refined.api.{RefType, Refined, Validate}

import scala.language.experimental.macros
import scala.language.implicitConversions
import scala.reflect.macros.blackbox

final class RefinedMacros(val c: blackbox.Context) extends MacroCommonsScala2 with RefinedMacro {

  def autoRefineVImpl[T: c.WeakTypeTag, P: c.WeakTypeTag](
      t: c.Expr[T]
  )(
      v: c.Expr[Validate[T, P]]
  ): c.Expr[Refined[T, P]] = autoRefineImpl[T, P](t, v)
}

object auto {

  implicit def autoRefineV[T, P](t: T)(implicit
      v: Validate[T, P]
  ): Refined[T, P] = macro RefinedMacros.autoRefineVImpl[T, P]

  implicit def autoUnwrap[F[_, _], T, P](tp: F[T, P])(implicit rt: RefType[F]): T =
    rt.unwrap(tp)
}
