package hearth

import eu.timepit.refined.api.{Refined, Validate}

import scala.language.experimental.macros

package object refined {

  def refineMV[P]: RefineMVBuilder[P] = new RefineMVBuilder[P]

  final class RefineMVBuilder[P] {

    def apply[T](t: T)(implicit v: Validate[T, P]): Refined[T, P] =
      macro RefinedMacros.refineMVImpl_[T, P]
  }
}
