package io.github.catlandor.imagepicker

import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObjectNotFoundException
import androidx.test.uiautomator.UiSelector
import androidx.test.uiautomator.Until

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
     * This method is used to click the 'Allow' button in the permission dialog.
     * It is called when the camera location permission dialog appears.
     * ('Allow camera to access this device's location?')
     * The method checks for the presence of the dialog and clicks the 'Allow' button if found.
     * If the dialog is not found, it does nothing.
     *
     * @param device The UiDevice instance used to interact with the UI.
     * @param threadTimeout The timeout duration for waiting for the dialog to appear.
     * @param openCameraFunc A lambda function to open the camera.
     */
    fun tryClickCameraLocationAllowButton(
        device: UiDevice,
        threadTimeout: Long,
        openCameraFunc: () -> Unit
    ) {
        val resourceNameAllowButton =
            "com.android.packageinstaller:id/permission_allow_button"
        val objectExists =
            device.wait(Until.hasObject(By.res(resourceNameAllowButton)), threadTimeout)
        if (!objectExists) {
            return
        }

        device
            // for denying: permission_deny_button
            .findObject(By.res(resourceNameAllowButton))
            ?.click()

        device.wait(
            Until.hasObject(By.textContains("Next")),
            threadTimeout
        )

        // Since a really annoying additional question appears ('Remember photo location?') and
        // afterwards the camera forgets the context it was opened from, we instead just return
        // to the app and click the 'add image for tea' button again.
        // This time, the permission is already granted and the camera opens directly.
        // Alternatively, it could be possible to find the 'Next' button like this
        // device.findObject(UiSelector().textContains("Next"))
        // (yet the problem still persists that the camera forgets the context)
        device.pressBack()

        openCameraFunc()
    }
}
