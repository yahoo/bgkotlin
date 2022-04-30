//
// Copyright Yahoo 2021
//
package com.yahoo.behaviorgraph

import com.yahoo.behaviorgraph.platform.PlatformSupport
import org.junit.Assert
import org.junit.Test

class PlatformTests : AbstractBehaviorGraphTest() {
    @org.junit.Before
    override fun setUp() {
        super.setUp()
    }

    override fun setupPlatformSupport() {
        PlatformSupport.platformSupport = object : PlatformSupport {
            override fun getCurrentTimeMillis(): Long {
                return System.currentTimeMillis()
            }

            var firstTime = true
            override fun isMainThread(): Boolean {
                if (firstTime) {
                    firstTime = false
                    return false
                }
                return true
            }
        }
    }
}
