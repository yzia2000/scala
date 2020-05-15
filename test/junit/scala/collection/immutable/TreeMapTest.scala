package scala.collection.immutable

import org.junit.Assert._
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

import scala.tools.testkit.AllocationTest

@RunWith(classOf[JUnit4])
class TreeMapTest extends AllocationTest {

  @Test
  def hasCorrectDropAndTakeMethods(): Unit = {
    val tree = TreeMap(1 -> "a", 2 -> "b", 3 -> "c")

    assertEquals(TreeMap.empty[Int, String], tree take Int.MinValue)
    assertEquals(TreeMap.empty[Int, String], tree takeRight Int.MinValue)
    assertEquals(tree, tree drop Int.MinValue)
    assertEquals(tree, tree dropRight Int.MinValue)
  }

  @Test
  def factoryReuse(): Unit = {
    val m = TreeMap("a" -> "a")
    assertSame(m, TreeMap.from(m))
  }
  @Test
  def testWithDefaultValue: Unit = {
    val m1 = TreeMap(1 -> "a", 2 -> "b")
    val m2 = m1.withDefaultValue("missing")
    assertEquals("a", m2(1))
    assertEquals("missing", m2(3))
  }
  @Test
  def testWithDefault: Unit = {
    val m1 = TreeMap(1 -> "a", 2 -> "b")

    val m2: Map[Int, String] =
      m1.withDefault(i => (i + 1).toString)
        .updated(1, "aa")
        .updated(100, "bb")
        .concat(List(500 -> "c", 501 -> "c"))

    assertEquals(m2(1), "aa")
    assertEquals(m2(2), "b")
    assertEquals(m2(3), "4")
    assertEquals(m2(4), "5")
    assertEquals(m2(500), "c")
    assertEquals(m2(501), "c")
    assertEquals(m2(502), "503")

    val m3: Map[Int, String] = m2 - 1
    assertEquals(m3(1), "2")

    val m4: Map[Int, String] = m3 -- List(2, 100)
    assertEquals(m4(2), "3")
    assertEquals(m4(100), "101")
  }

  @Test def entriesEqualSimple: Unit = {
    val tree1 = TreeMap(1 -> "a", 2 -> "b", 3 -> "c")
    val tree2 = TreeMap(1 -> "a", 2 -> "b", 3 -> "c")
    assertEquals(tree1, tree2)
  }
  @Test def entriesEqual: Unit = {
    val b1 = TreeMap.newBuilder[Int, String]
    for ( i <- 10 to 1000) {
      b1 += i -> s"$i value"
    }
    val tree1 = b1.result()
    val b2 = TreeMap.newBuilder[Int, String]
    for ( i <- 1 to 1000) {
      b2 += i -> s"$i value"
    }
    val tree2 = b2.result().drop(9)

    assertEquals(tree1, tree2)
    assertNotEquals(tree1, (tree2+ (9999 -> "zzz")))
    assertNotEquals((tree1+ (9999 -> "zzz")), (tree2))
    assertEquals((tree1+ (9999 -> "zzz")), (tree2+ (9999 -> "zzz")))
    assertNotEquals((tree1+ (9999 -> "zzz")), (tree2+ (9999999 -> "zzz")))
  }
  @Test def equalFastPath: Unit = {
    class V(val s: String) {
      override def equals(obj: Any): Boolean = obj match {
        case v:V => v.s == s
      }
    }
    var compareCount = 0
    class K(val s: String) extends Ordered[K] {
      override def toString: String = s"K-$s"

      override def compare(that: K): Int = {
        val res = s.compareTo(that.s)
        compareCount += 1
        res
      }

      override def equals(obj: Any): Boolean = {
        fail("equals should not be called = the trees should be ordered and compared via the sort order")
        false
      }
    }
    val b1 = TreeMap.newBuilder[K, V]
    for ( i <- 10 to 1000) {
      b1 += new K(i.toString) -> new V(s"$i value")
    }
    val tree1 = b1.result()
    compareCount = 0
    nonAllocating(assertEquals(tree1, tree1))
    assertEquals(0, compareCount)
    var exp = tree1.drop(5)
    var act = tree1.drop(5)

    compareCount = 0
    onlyAllocates(240)(assertEquals(exp, act))
    assertEquals(0, compareCount)

    exp += new K("XXX") -> new V("YYY")
    act += new K("XXX") -> new V("YYY")

    compareCount = 0
    assertEquals(exp, act)
    assertTrue(compareCount.toString, compareCount < 30)

    onlyAllocates(408)(assertEquals(exp, act))
  }
  @Test
  def plusWithContains() {
    val data = Array.tabulate(1000)(i => s"${i}Key" -> s"${i}Value")
    val tree = (TreeMap.newBuilder[String, String] ++= data).result

    data foreach {
      case (k, v) =>
        assertSame(tree, nonAllocating(tree.updated(k, v)))
    }
  }
}
