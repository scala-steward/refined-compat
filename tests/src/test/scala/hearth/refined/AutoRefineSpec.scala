package hearth
package refined

import eu.timepit.refined.api.Refined
import eu.timepit.refined.collection.NonEmpty
import eu.timepit.refined.numeric.{NonNegative, Positive}
import hearth.refined.auto._

final class AutoRefineSpec extends munit.FunSuite {

  // autoRefineV

  test("autoRefineV should accept positive Int literal") {
    val x: Int Refined Positive = 5
    assertEquals(x.value, 5)
  }

  test("autoRefineV should accept non-negative Int literal 0") {
    val x: Int Refined NonNegative = 0
    assertEquals(x.value, 0)
  }

  test("autoRefineV should accept non-empty String literal") {
    val x: String Refined NonEmpty = "hello"
    assertEquals(x.value, "hello")
  }

  test("autoRefineV should reject negative Int at compile time") {
    val errors = compileErrors("""
      import eu.timepit.refined.api.Refined
      import eu.timepit.refined.numeric.Positive
      import hearth.refined.auto._
      val x: Int Refined Positive = -1
    """)
    assert(errors.nonEmpty, "Expected compilation error for negative value with Positive predicate")
  }

  test("autoRefineV should reject empty String at compile time") {
    val errors = compileErrors("""
      import eu.timepit.refined.api.Refined
      import eu.timepit.refined.collection.NonEmpty
      import hearth.refined.auto._
      val x: String Refined NonEmpty = ""
    """)
    assert(errors.nonEmpty, "Expected compilation error for empty string with NonEmpty predicate")
  }

  // autoUnwrap

  test("autoUnwrap should unwrap refined value to base type") {
    val x: Int Refined Positive = 42
    val y: Int = x
    assertEquals(y, 42)
  }

  // autoInfer

  test("autoInfer should infer Positive => NonNegative") {
    val pos: Int Refined Positive = 5
    val nonNeg: Int Refined NonNegative = pos
    assertEquals(nonNeg.value, 5)
  }

  // refineMV

  test("refineMV should validate at compile time") {
    val x = hearth.refined.refineMV[Positive](42)
    assertEquals(x.value, 42)
  }

  test("refineMV should reject invalid values at compile time") {
    val errors = compileErrors("""
      import eu.timepit.refined.numeric.Positive
      hearth.refined.refineMV[Positive](-1)
    """)
    assert(errors.nonEmpty, "Expected compilation error for negative value")
  }

  // RefinedTypeOpsM

  test("RefinedTypeOpsM should validate via apply") {
    type PosInt = Int Refined Positive
    object PosInt extends RefinedTypeOpsM[PosInt, Int]
    val x = PosInt(10)
    assertEquals(x.value, 10)
  }

  test("RefinedTypeOpsM should reject invalid values") {
    val errors = compileErrors("""
      import eu.timepit.refined.api.Refined
      import eu.timepit.refined.numeric.Positive
      import hearth.refined.RefinedTypeOpsM
      type PosInt = Int Refined Positive
      object PosInt extends RefinedTypeOpsM[PosInt, Int]
      PosInt(-5)
    """)
    assert(errors.nonEmpty, "Expected compilation error for negative value")
  }
}
