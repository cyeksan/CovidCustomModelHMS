package com.example.covid_19custommodel

import android.Manifest
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.text.TextUtils
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.huawei.hms.api.ConnectionResult
import com.huawei.hms.api.HuaweiApiAvailability
import java.util.*

class MainActivity : AppCompatActivity() {
    private var detector: ModelDetector? = null
    private var bitmap: Bitmap? = null
    private var runButton: Button? = null
    private var choose: Button? = null
    private var capturedImage: ImageView? = null
    private var resultText: TextView? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        runButton = findViewById(R.id.Run)
        choose = findViewById(R.id.choosePicture)
        capturedImage = findViewById(R.id.capturedImageView)
        resultText = findViewById(R.id.resultText)

        runButton?.setOnClickListener {
            detector = ModelDetector(this)
            detector!!.loadModelFromAssets()
            checkHmsAndRun()
        }

        choose?.setOnClickListener {
            choosePhoto()
        }
    }

    override fun onStop() {
        super.onStop()
        if (detector != null) {
            detector!!.close()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        bitmap = processIntent(requestCode, resultCode, data)
        capturedImage!!.setImageBitmap(bitmap)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSION_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                choosePhoto()
            } else {
                Toast.makeText(this, "no permission", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * HMS Core is a mobile service framework based on Huawei terminal devices and the Android platform,
    providing basic service capabilities. Huawei phones are installed with HMS Core SDK by default.
    In order to use Huawei Mobile Services such as ML Kit from a non-Huawei phone, HMS Core should be
    installed. This function checks if a mobile phone has Huawei Mobile Services SDK. If not, redirects
    the user to download HMS. After HMS Core SDK is installed, Huawei Mobile Services can be accessed
    from non-Huawei phones.
     */
    private fun checkHmsAndRun() {
        val hmsAvailability =
            HuaweiApiAvailability.getInstance().isHuaweiMobileServicesAvailable(baseContext)
        val isHmsAvailable = (hmsAvailability == ConnectionResult.SUCCESS)

        if (!isHmsAvailable) {
            AlertDialog.Builder(this)
                .setTitle("Huawei Mobile Services")
                .setMessage(
                    "In order to access Huawei mobile services, HMS Core SDK should be installed. Please install HMS Core to use Huawei services."

                )
                .setPositiveButton("GO TO GOOGLE PLAY") { _, _ ->
                    try {
                        startActivity(
                            Intent(
                                Intent.ACTION_VIEW, Uri.parse(
                                    ("https://play.google.com/store/apps/details?id=com.huawei.hwid&gl=US")
                                )
                            )
                        )
                    } catch (ex: ActivityNotFoundException) {
                        Log.e("MainActivity", ex.localizedMessage!!)
                    }
                }
                .setNegativeButton("CLOSE", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show()
        } else {
            runOnClick()
        }
    }

    private fun processIntent(requestCode: Int, resultCode: Int, data: Intent?): Bitmap? {
        if (requestCode == RC_CHOOSE_PHOTO) {
            if (data == null) {
                return null
            }
            val uri: Uri? = data.data
            val filePath = FileUtil.getFilePathByUri(this, uri!!)
            if (!TextUtils.isEmpty(filePath)) {
                Log.e(TAG, "file is $filePath")
                return BitmapFactory.decodeFile(filePath)
            }
        }
        return null
    }

    private fun checkPermissionIfNecessary() {
        if (ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_DENIED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ),
                REQUEST_PERMISSION_CODE
            )
        } else {
            val intentToPickPic = Intent(Intent.ACTION_PICK, null)
            intentToPickPic.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*")
            startActivityForResult(intentToPickPic, RC_CHOOSE_PHOTO)
        }
    }

    private fun choosePhoto() {
        checkPermissionIfNecessary()
    }

    private fun runOnClick() {
        detector!!.predict(bitmap,
            { mlModelOutputs ->
                Log.i(TAG, "interpret get result")
                val result = detector!!.resultPostProcess(mlModelOutputs!!)
                showResult(result)
                Log.i(TAG, "result: $result")
            }) { e ->
            e.printStackTrace()
            Log.e(TAG, "interpret failed, because " + e.message)
            Toast.makeText(
                this@MainActivity,
                "interpret failed, because" + e.message,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun showResult(result: String?) {
        Toast.makeText(this, result, Toast.LENGTH_LONG).show()
    }

    companion object {
        private const val TAG = "HMS_HIAI_MINDSPORE"
        private const val REQUEST_PERMISSION_CODE = 10
        private const val RC_CHOOSE_PHOTO = 2
    }
}