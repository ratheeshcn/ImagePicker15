package io.github.catlandor.imagepicker.provider

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import androidx.activity.result.ActivityResult
import com.yalantis.ucrop.UCrop
import io.github.catlandor.imagepicker.ImagePicker
import io.github.catlandor.imagepicker.ImagePickerActivity
import io.github.catlandor.imagepicker.R
import io.github.catlandor.imagepicker.util.FileUriUtils
import io.github.catlandor.imagepicker.util.FileUtil.getCompressFormat
import io.github.catlandor.imagepicker.util.ImageUtil
import io.github.catlandor.imagepicker.wrapper.UCropOptionsWrapper
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Crop Selected/Captured Image
 *
 * @author Dhaval Patel
 * @version 1.0
 * @since 04 January 2019
 */
class CropProvider(activity: ImagePickerActivity, private val launcher: (Intent) -> Unit) :
    BaseProvider(activity) {
    companion object {
        private const val STATE_CROP_URI = "state.crop_uri"
    }

    private var isMultipleFiles: Boolean = false
    private val maxWidth: Int
    private val maxHeight: Int

    private val uCropBundle: Bundle?
    private val crop: Boolean
    private val outputFormat: Bitmap.CompressFormat?
    private val uCropOptions: UCropOptionsWrapper?

    private var cropImageUri: Uri? = null

    init {
        with(activity.intent.extras ?: Bundle()) {
            maxWidth = getInt(ImagePicker.EXTRA_MAX_WIDTH, 0)
            maxHeight = getInt(ImagePicker.EXTRA_MAX_HEIGHT, 0)
            crop = getBoolean(ImagePicker.EXTRA_CROP, false)
            @Suppress("DEPRECATION")
            outputFormat = this.get(ImagePicker.EXTRA_OUTPUT_FORMAT) as? Bitmap.CompressFormat
            uCropOptions = getParcelable(ImagePicker.EXTRA_UCROP_OPTIONS)

            uCropBundle = activity.intent.extras
        }
    }

    /**
     * Save CameraProvider state
     *
     * mCropImageFile will lose its state when activity is recreated on
     * Orientation change or for Low memory device.
     *
     * Here, We Will save its state for later use
     *
     * Note: To produce this scenario, enable "Don't keep activities" from developer options
     */
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putParcelable(STATE_CROP_URI, cropImageUri)
    }

    /**
     * Retrieve CropProvider state
     */
    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        @Suppress("DEPRECATION")
        cropImageUri = savedInstanceState?.getParcelable(STATE_CROP_URI) as Uri?
    }

    /**
     * Check if crop should be enabled or not
     *
     * @return Boolean. True if Crop should be enabled else false.
     */
    fun isCropEnabled() = crop

    /**
     * Get the output format if it has been set
     *
     * @return Bitmap.CompressFormat?. In case of Null, it will use the extension from the input file
     */
    fun outputFormat() = outputFormat

    /**
     * Start Crop Activity
     */
    @Throws(IOException::class)
    fun startIntent(
        uri: Uri,
        isCamera: Boolean,
        isMultipleFiles: Boolean,
        outputFormat: Bitmap.CompressFormat?
    ) {
        this.isMultipleFiles = isMultipleFiles
        cropImage(
            uri = uri,
            isCamera = isCamera,
            outputFormat = outputFormat
        )
    }

    /**
     * @param uri Image File to be cropped
     * @throws IOException if failed to crop image
     */
    @Throws(IOException::class)
    private fun cropImage(
        uri: Uri,
        isCamera: Boolean,
        outputFormat: Bitmap.CompressFormat?
    ) {
        val path =
            if (isCamera) {
                Environment.DIRECTORY_DCIM
            } else {
                Environment.DIRECTORY_PICTURES
            }
        val extension =
            outputFormat?.let { ".${it.name}" } ?: FileUriUtils.getImageExtension(baseContext, uri)
        cropImageUri = uri

        // Later we will use this bitmap to create the File.
        val selectedBitmap: Bitmap? = ImageUtil.getBitmap(this, uri)
        selectedBitmap?.let {
            // We can access getExternalFilesDir() without asking any storage permission.
            val selectedImgFile =
                File(
                    getExternalFilesDir(path),
                    System.currentTimeMillis().toString() + "_selectedImg" + extension
                )

            convertBitmapToFile(selectedImgFile, it, extension)

            // We have to again create a new file where we will save the cropped image.
            val croppedImgFile =
                File(
                    getExternalFilesDir(path),
                    System.currentTimeMillis().toString() + "_croppedImg" + extension
                )

            val options = uCropOptions?.options ?: UCrop.Options()
            options.setCompressionFormat(getCompressFormat(extension))
            val uCrop =
                UCrop
                    .of(Uri.fromFile(selectedImgFile), Uri.fromFile(croppedImgFile))
                    .withOptions(options)

            if (maxWidth > 0 && maxHeight > 0) {
                uCrop.withMaxResultSize(maxWidth, maxHeight)
            }

            launcher.invoke(uCrop.getIntent(activity))
        } ?: kotlin.run {
            setError(R.string.error_failed_to_crop_image)
        }
    }

    /**
     * This method will be called when final result fot this provider is enabled.
     */
    fun handleResult(result: ActivityResult) {
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = UCrop.getOutput(result.data!!)
            if (uri != null) {
                if (isMultipleFiles) {
                    activity.setMultipleCropImage(uri)
                } else {
                    activity.setCropImage(uri)
                }
            } else {
                setError(R.string.error_failed_to_crop_image)
            }
        } else {
            setResultCancel()
        }
    }

    @Throws(IOException::class)
    private fun convertBitmapToFile(destinationFile: File, bitmap: Bitmap, extension: String) {
        destinationFile.createNewFile()
        val bos = ByteArrayOutputStream()
        bitmap.compress(getCompressFormat(extension), 50, bos)
        val bitmapData = bos.toByteArray()
        val fos = FileOutputStream(destinationFile)
        fos.write(bitmapData)
        fos.flush()
        fos.close()
    }

    /**
     * Delete Crop file is exists
     */
    override fun onFailure() {
        delete()
    }

    /**
     * Delete Crop File, If not required
     *
     * After Image Compression, Crop File will not required
     */
    private fun delete() {
        cropImageUri?.path?.let {
            File(it).delete()
        }
        cropImageUri = null
    }
}
