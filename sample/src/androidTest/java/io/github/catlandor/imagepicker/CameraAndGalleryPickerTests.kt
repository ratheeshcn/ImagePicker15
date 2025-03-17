package io.github.catlandor.imagepicker

import android.app.Activity
import android.app.Instrumentation
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.net.Uri
import android.os.Build
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObjectNotFoundException
import androidx.test.uiautomator.UiSelector
import androidx.test.uiautomator.Until
import io.github.catlandor.imagepicker.sample.MainActivity
import io.github.catlandor.imagepicker.sample.R
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

@RunWith(AndroidJUnit4::class)
class CameraAndGalleryPickerTests {
    private val threadTimeout = 5000L
    private val cameraButtonShutter =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            "com.android.camera2:id/shutter_button"
        } else {
            "com.android.camera:id/shutter_button"
        }
    private val cameraButtonDone =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            "com.android.camera2:id/done_button"
        } else {
            "com.android.camera:id/btn_done"
        }

    private lateinit var instrumentation: Instrumentation
    private lateinit var device: UiDevice
    private lateinit var resources: Resources

    @Before
    fun startMainActivityFromHomeScreen() {
        instrumentation = InstrumentationRegistry.getInstrumentation()
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        resources = InstrumentationRegistry.getInstrumentation().targetContext.resources

        Intents.init()
        mockMediaSelection(InstrumentationRegistry.getInstrumentation().targetContext)
    }

    @After
    fun tearDown() {
        Intents.release()
    }

    @get:Rule
    var activityScenarioRule = activityScenarioRule<MainActivity>()

    @get:Rule
    val screenLockRule = TestUtils.RunWhenScreenOffOrLockedRule()

    @Test
    fun addProfileImage_PhotoOption_PhotoAdded() {
        onView(withId(R.id.fab_add_photo)).perform(click())

        // Now click on the button in your app that launches the camera.
        onView(withId(R.id.lytCameraPick)).perform(click())

        val cameraButtons = arrayOf<String?>(cameraButtonShutter, cameraButtonDone)

        executeUiAutomatorActions(device, cameraButtons, threadTimeout)

        device.wait(
            Until.hasObject(By.displayId(R.id.state_rotate).clickable(true)),
            threadTimeout
        )

        onView(withId(R.id.state_rotate)).perform(click())
        onView(withId(R.id.wrapper_rotate_by_angle)).perform(click())
        onView(withId(R.id.menu_crop)).perform(click())

        onView(withId(R.id.fab_add_photo)).check(matches(isDisplayed()))
    }

    @Test
    fun addProfileImage_GalleryOption_ImageAdded() {
        onView(withId(R.id.fab_add_photo)).perform(click())

        // Following call opens the gallery, which is mocked (see 'mockMediaSelection')
        onView(withId(R.id.lytGalleryPick)).perform(click())

        device.wait(
            Until.hasObject(By.displayId(R.id.menu_crop).clickable(true)),
            threadTimeout
        )

        onView(withId(R.id.menu_crop)).perform(click())

        onView(withId(R.id.fab_add_photo)).check(matches(isDisplayed()))
    }

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

    private fun mockMediaSelection(context: Context) {
        val assetManager = InstrumentationRegistry.getInstrumentation().context
        val inputStream = assetManager.assets.open("TestImage.png")
        val tempFile = File(context.getExternalFilesDir(null), "temp_image.png")
        val outputStream = FileOutputStream(tempFile)

        inputStream.use { input ->
            outputStream.use { output ->
                input.copyTo(output)
            }
        }

        val uri = Uri.fromFile(tempFile)

        val resultData = Intent()
        resultData.data = uri

        intending(hasAction(Intent.ACTION_GET_CONTENT))
            .respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, resultData))
    }
}
