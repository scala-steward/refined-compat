package hearth
package refined

import eu.timepit.refined.api.{Refined, Validate}

import scala.quoted.*

inline def refineMV[P]: RefineMVBuilder[P] = new RefineMVBuilder[P]

final class RefineMVBuilder[P] {

  inline def apply[T](inline t: T)(implicit inline v: Validate[T, P]): Refined[T, P] =
    ${ Macros.refineMV[T, P]('t, 'v) }
}
