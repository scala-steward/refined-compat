package hearth
package refined

import eu.timepit.refined.api.{Refined, Validate}

import scala.language.experimental.macros

trait RefinedTypeOpsM[FTP, T] {

  def apply[P](t: T)(implicit ev: Refined[T, P] =:= FTP, v: Validate[T, P]): FTP =
    macro RefinedMacros.applyRefImpl_[FTP, T, P]
}
