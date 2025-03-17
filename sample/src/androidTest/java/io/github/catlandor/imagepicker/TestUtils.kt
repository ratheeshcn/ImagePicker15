package io.github.catlandor.imagepicker

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import androidx.test.uiautomator.UiDevice
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

object TestUtils {
    /**
     * This rule will allow the tests to run even if the device's screen is off or locked.
     * Allows a developer to fire and forget running the UI Test across different devices or on the CI
     * emulator.
     */
    class RunWhenScreenOffOrLockedRule : TestRule {
        override fun apply(base: Statement, description: Description): Statement =
            object : Statement() {
                override fun evaluate() {
                    // Turn screen on
                    UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).wakeUp()

                    // Allow any activity to run when locked
                    ActivityLifecycleMonitorRegistry
                        .getInstance()
                        .addLifecycleCallback { activity, stage ->
                            if (stage === Stage.PRE_ON_CREATE) {
                                activity.setShowWhenLocked(true)
                            }
                        }

                    // Continue with other statements
                    base.evaluate()
                }
            }
    }
}
