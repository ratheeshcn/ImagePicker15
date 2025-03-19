package io.github.catlandor.imagepicker

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObjectNotFoundException
import androidx.test.uiautomator.UiSelector
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

object TestUtils {
    const val CAMERA_BUTTON_SHUTTER = "com.android.camera2:id/shutter_button"
    const val CAMERA_BUTTON_DONE = "com.android.camera2:id/done_button"

    /**
     * Executes given ui actions
     * FROM [...](https://medium.com/@karimelbahi/testing-capture-real-image-using-camera-with-espresso-and-ui-automator-f4420d8da143)
     *
     * @param device uidevice
     * @param ids button ids
     * @param timeout timeout length
     * @throws UiObjectNotFoundException if button not found
     */
    @Throws(UiObjectNotFoundException::class)
    fun executeUiAutomatorActions(device: UiDevice, ids: Array<String?>, timeout: Long?) {
        for (id in ids) {
            val `object` = device.findObject(UiSelector().resourceId(id!!))
            if (`object`.waitForExists(timeout!!)) {
                `object`.click()
            }
        }
    }

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
