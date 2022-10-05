//
// Copyright Yahoo 2021
//
package behaviorgraph

import java.lang.reflect.Field
import kotlin.test.*
import kotlin.reflect.KClass

abstract class AbstractBehaviorGraphTest
{
    open class TestExtent(g: Graph) : Extent<TestExtent>(g)

    lateinit var g: Graph
    protected lateinit var setupExt: TestExtent
    lateinit var ext: TestExtent
    lateinit var r_a: State<Long>
    lateinit var r_b: State<Long>
    lateinit var r_c: State<Long>
    protected fun assertBehaviorGraphException(lambda: () -> Unit) {
        try {
            lambda()
        } catch (e: BehaviorGraphException) {
            return
        }
        fail("did not catch expected BehaviorGraphException")
    }

    /**
     * Usage: assertExpectedException(Exception::class) {...lambda to run}
     * expects to match on the exact expectedClass
     */
    protected fun assertExpectedException(expectedClass: KClass<*>, lambda: () -> Unit) {
        try {
            lambda()
        } catch (e: Exception) {
            if (e.javaClass == expectedClass.java) {
                return
            }
            fail("unexpected exception. Expected: $expectedClass but found $e")
        }
        fail("did not catch expected exception: $expectedClass")
    }

    protected  fun assertNoThrow(lambda: () -> Unit) {
        try {
            lambda()
        } catch (e: Exception) {
            fail("Unexpected exception")
        }
        assertTrue(true, "Did not throw")
    }

    protected fun reflectionGetField(obj: Any, name: String): Any? {
        val field: Field = obj.javaClass.getDeclaredField(name)
        field.trySetAccessible()
        return field.get(obj)
    }

    @org.junit.Before
    open fun setUp() {
        g = Graph()
        setupExt = TestExtent(g)
        ext = TestExtent(g)
        r_a = setupExt.state(0, "r_a")
        r_b = setupExt.state( 0, "r_b")
        r_c = setupExt.state(0, "r_c")
        setupExt.addToGraphWithAction()
        setupExt.addChildLifetime(ext)
    }
}
