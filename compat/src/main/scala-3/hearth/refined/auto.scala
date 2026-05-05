package hearth
package refined

import eu.timepit.refined.api.{Inference, RefType, Refined, Validate}

import scala.language.implicitConversions
import scala.quoted.*

private class RefinedMacros(q: Quotes) extends MacroCommonsScala3(using q), RefinedMacro

object auto {

  implicit inline def autoRefineV[T, P](inline t: T)(implicit
      inline v: Validate[T, P]
  ): Refined[T, P] = ${ Macros.autoRefineV[T, P]('t, 'v) }

  implicit inline def autoInfer[T, A, B](inline ta: Refined[T, A])(implicit
      inline ir: Inference[A, B]
  ): Refined[T, B] = ${ Macros.autoInfer[T, A, B]('ta, 'ir) }

  implicit def autoUnwrap[F[_, _], T, P](tp: F[T, P])(implicit rt: RefType[F]): T =
    rt.unwrap(tp)
}

private object Macros {

  def autoRefineV[T: Type, P: Type](
      t: Expr[T],
      v: Expr[Validate[T, P]]
  )(using q: Quotes): Expr[Refined[T, P]] =
    new RefinedMacros(q).autoRefineImpl[T, P](t, v)

  def autoInfer[T: Type, A: Type, B: Type](
      ta: Expr[Refined[T, A]],
      ir: Expr[Inference[A, B]]
  )(using q: Quotes): Expr[Refined[T, B]] =
    new RefinedMacros(q).autoInferImpl[T, A, B](ta, ir)

  def refineMV[T: Type, P: Type](
      t: Expr[T],
      v: Expr[Validate[T, P]]
  )(using q: Quotes): Expr[Refined[T, P]] =
    new RefinedMacros(q).refineMVImpl[T, P](t, v)

  def applyRef[FTP: Type, T: Type, P: Type](
      t: Expr[T],
      v: Expr[Validate[T, P]]
  )(using q: Quotes): Expr[FTP] =
    new RefinedMacros(q).applyRefImpl[FTP, T, P](t, v)
}
