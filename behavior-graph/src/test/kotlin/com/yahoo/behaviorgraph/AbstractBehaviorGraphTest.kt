//
// Copyright Yahoo 2021
//
package com.yahoo.behaviorgraph

import com.yahoo.behaviorgraph.exception.BehaviorGraphException
import com.yahoo.behaviorgraph.platform.PlatformSupport
import org.junit.Assert.fail
import kotlin.reflect.KClass

abstract class AbstractBehaviorGraphTest
{
    class TestExtent(g: Graph) : Extent<TestExtent>(g)

    lateinit var g: Graph
    protected lateinit var setupExt: TestExtent
    lateinit var ext: Extent<Extent>
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

    @org.junit.Before
    open fun setUp() {
        setupPlatformSupport()

        g = Graph()
        setupExt = TestExtent(g)
        ext = Extent(g)
        r_a = setupExt.state(0, "r_a")
        r_b = setupExt.state( 0, "r_b")
        r_c = setupExt.state(0, "r_c")
        setupExt.addToGraphWithAction()
    }

    open fun setupPlatformSupport() {
        PlatformSupport.platformSupport = object: PlatformSupport {
            override fun getCurrentTimeMillis(): Long {
                return System.currentTimeMillis()
            }

            override fun isMainThread(): Boolean {
                return true;
            }

        }
    }

}
