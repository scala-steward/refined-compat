package hearth
package refined

import eu.timepit.refined.api.Refined
import eu.timepit.refined.collection.NonEmpty
import eu.timepit.refined.numeric.{NonNegative, Positive}
import hearth.refined.auto._

final class AutoRefineSpec extends munit.FunSuite {

  test("autoRefineV should accept positive Int literal") {
    val x: Int Refined Positive = 5
    assertEquals(x.value, 5)
  }

  test("autoRefineV should accept non-negative Int literal 0") {
    val x: Int Refined NonNegative = 0
    assertEquals(x.value, 0)
  }

  // TODO: requires lambda support in semiEval
  // test("autoRefineV should accept non-empty String literal") {
  //   val x: String Refined NonEmpty = "hello"
  //   assertEquals(x.value, "hello")
  // }

  test("autoRefineV should reject negative Int at compile time") {
    val errors = compileErrors("""
      import eu.timepit.refined.api.Refined
      import eu.timepit.refined.numeric.Positive
      import hearth.refined.auto._
      val x: Int Refined Positive = -1
    """)
    assert(errors.nonEmpty, "Expected compilation error for negative value with Positive predicate")
  }

  // TODO: requires lambda support in semiEval
  // test("autoRefineV should reject empty String at compile time") {
  //   val errors = compileErrors("""
  //     import eu.timepit.refined.api.Refined
  //     import eu.timepit.refined.collection.NonEmpty
  //     import hearth.refined.auto._
  //     val x: String Refined NonEmpty = ""
  //   """)
  //   assert(errors.nonEmpty, "Expected compilation error for empty string with NonEmpty predicate")
  // }

  test("autoUnwrap should unwrap refined value to base type") {
    val x: Int Refined Positive = 42
    val y: Int = x
    assertEquals(y, 42)
  }
}
