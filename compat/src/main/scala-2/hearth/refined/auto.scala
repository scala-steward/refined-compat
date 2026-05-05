package hearth
package refined

import eu.timepit.refined.api.{Inference, RefType, Refined, Validate}

import scala.language.experimental.macros
import scala.language.implicitConversions
import scala.reflect.macros.blackbox

final class RefinedMacros(val c: blackbox.Context) extends MacroCommonsScala2 with RefinedMacro {

  def autoRefineVImpl[T: c.WeakTypeTag, P: c.WeakTypeTag](
      t: c.Expr[T]
  )(
      v: c.Expr[Validate[T, P]]
  ): c.Expr[Refined[T, P]] = autoRefineImpl[T, P](t, v)

  def autoInferImpl_[T: c.WeakTypeTag, A: c.WeakTypeTag, B: c.WeakTypeTag](
      ta: c.Expr[Refined[T, A]]
  )(
      ir: c.Expr[Inference[A, B]]
  ): c.Expr[Refined[T, B]] = autoInferImpl[T, A, B](ta, ir)

  def refineMVImpl_[T: c.WeakTypeTag, P: c.WeakTypeTag](
      t: c.Expr[T]
  )(
      v: c.Expr[Validate[T, P]]
  ): c.Expr[Refined[T, P]] = refineMVImpl[T, P](t, v)

  def applyRefImpl_[FTP: c.WeakTypeTag, T: c.WeakTypeTag, P: c.WeakTypeTag](
      t: c.Expr[T]
  )(
      ev: c.Expr[Refined[T, P] =:= FTP],
      v: c.Expr[Validate[T, P]]
  ): c.Expr[FTP] = applyRefImpl[FTP, T, P](t, v)
}

object auto {

  implicit def autoRefineV[T, P](t: T)(implicit
      v: Validate[T, P]
  ): Refined[T, P] = macro RefinedMacros.autoRefineVImpl[T, P]

  implicit def autoInfer[T, A, B](ta: Refined[T, A])(implicit
      ir: Inference[A, B]
  ): Refined[T, B] = macro RefinedMacros.autoInferImpl_[T, A, B]

  implicit def autoUnwrap[F[_, _], T, P](tp: F[T, P])(implicit rt: RefType[F]): T =
    rt.unwrap(tp)
}
