package com.dmitrybrant.activities


import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Rect
import android.hardware.*
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import com.dmitrybrant.modelviewer.R
import dmax.dialog.SpotsDialog
import kotlinx.android.synthetic.main.activity_camera.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Created by dharamveer on 20/3/18.
 */

class CameraActivity : Activity(), Camera.PictureCallback, SurfaceHolder.Callback {

    private var mCamera: Camera? = null
    private var mCameraImage: ImageView? = null
    private var mCameraPreview: SurfaceView? = null
    private var mCaptureImageButton: Button? = null
    private var mCameraData: ByteArray? = null
    private var mIsCapturing: Boolean = false
    //TextView textLIGHT_available, textLIGHT_reading;
    private var btnTop: Button? = null
    private var btnBottom: Button? = null
    private var globalLight: Double = 0.toDouble()

    private val mCaptureImageButtonClickListener = View.OnClickListener {

        captureImage()

    }

    private val mRecaptureImageButtonClickListener = View.OnClickListener { setupImageCapture() }

    private val mDoneButtonClickListener = View.OnClickListener {



        if (mCameraData != null) {
            val intent = Intent()
            intent.putExtra(EXTRA_CAMERA_DATA, mCameraData)
            setResult(Activity.RESULT_OK, intent)


        } else {
            setResult(Activity.RESULT_CANCELED)
        }
        finish()

    }


    private val LightSensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {

            if (event.sensor.type == Sensor.TYPE_LIGHT) {
                // textLIGHT_reading.setText("LIGHT: " + event.values[0]);

                globalLight = event.values[0].toDouble()

            }

        }

        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {

        }
    }


    private fun createDirectoryAndSaveFile(imageToSave: Bitmap, fileName: String) {

        val direct = File(Environment.getExternalStorageDirectory().toString() + "/DirName")

        if (!direct.exists()) {
            val wallpaperDirectory = File("/sdcard/DirName/")
            wallpaperDirectory.mkdirs()
        }

        val file = File(File("/sdcard/DirName/"), fileName)
        if (file.exists()) {
            file.delete()
        }
        try {
            val out = FileOutputStream(file)
            imageToSave.compress(Bitmap.CompressFormat.PNG, 100, out)
            out.flush()
            out.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //Remove title bar
        this.requestWindowFeature(Window.FEATURE_NO_TITLE)

        //Remove notification bar
        this.window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)

        setContentView(R.layout.activity_camera)

        mCameraImage = findViewById(R.id.camera_image_view)
        mCameraImage!!.visibility = View.INVISIBLE

        //textLIGHT_available = findViewById(R.id.textLIGHT_available);
        //textLIGHT_reading = findViewById(R.id.textLIGHT_reading);

        btnTop = findViewById(R.id.btnTop)
        btnBottom = findViewById(R.id.btnBottom)


        mCameraPreview = findViewById(R.id.preview_view)
        val surfaceHolder = mCameraPreview!!.holder
        surfaceHolder.addCallback(this)
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS)

        mCaptureImageButton = findViewById(R.id.capture_image_button)


        mCaptureImageButton!!.setOnClickListener(mCaptureImageButtonClickListener)

        btnTop?.setOnClickListener(View.OnClickListener {

            val locationTop = IntArray(2)
            btnTop!!.getLocationOnScreen(locationTop)
            val Top_x = locationTop[0]
            val Top_y = locationTop[1]

            Log.d("TopX--------------:", Top_x.toString())
            Log.d("TopY--------------:", Top_y.toString())

            btnTop?.setVisibility(View.GONE)

        })


        btnBottom?.setOnClickListener(View.OnClickListener {

            val locationBottom = IntArray(2)
            btnBottom!!.getLocationOnScreen(locationBottom)
            val Bottom_x = locationBottom[0]
            val Bottom_y = locationBottom[1]

            Log.d("Bottom_x--------------:", Bottom_x.toString())
            Log.d("Bottom_y--------------:", Bottom_y.toString())

            btnBottom?.setVisibility(View.GONE)

        })



        val doneButton = findViewById<Button>(R.id.done_button)
        doneButton.setOnClickListener(mDoneButtonClickListener)

        mIsCapturing = true


        val mySensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val LightSensor = mySensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)


        if (LightSensor != null) {
            // textLIGHT_available.setText("Sensor.TYPE_LIGHT Available");
            mySensorManager.registerListener(
                    LightSensorListener,
                    LightSensor,
                    SensorManager.SENSOR_DELAY_NORMAL)

        } else {
            // textLIGHT_available.setText("Sensor.TYPE_LIGHT NOT Available");
        }


    }


    override fun onSaveInstanceState(savedInstanceState: Bundle) {
        super.onSaveInstanceState(savedInstanceState)

        savedInstanceState.putBoolean(KEY_IS_CAPTURING, mIsCapturing)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)

        mIsCapturing = savedInstanceState.getBoolean(KEY_IS_CAPTURING, mCameraData == null)
        if (mCameraData != null) {
            setupImageDisplay()
        } else {
            setupImageCapture()
        }
    }

    override fun onResume() {
        super.onResume()

        if (mCamera == null) {
            try {
                mCamera = Camera.open()
                mCamera!!.setPreviewDisplay(mCameraPreview!!.holder)
                if (mIsCapturing) {
                    mCamera!!.setDisplayOrientation(90)
                    mCamera!!.startPreview()
                }
            } catch (e: Exception) {
                Toast.makeText(this@CameraActivity, "Unable to open camera.", Toast.LENGTH_LONG)
                        .show()
            }

        }

    }

    override fun onPause() {
        super.onPause()

        if (mCamera != null) {
            mCamera!!.release()
            mCamera = null
        }
    }

    override fun onPictureTaken(data: ByteArray, camera: Camera) {
        mCameraData = data

        setupImageDisplay()

    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        if (mCamera != null) {
            try {
                mCamera!!.setPreviewDisplay(holder)
                if (mIsCapturing) {
                    mCamera!!.startPreview()
                }
            } catch (e: IOException) {
                Toast.makeText(this@CameraActivity, "Unable to start camera preview.", Toast.LENGTH_LONG).show()
            }

        }
    }

    override fun surfaceCreated(holder: SurfaceHolder) {}

    override fun surfaceDestroyed(holder: SurfaceHolder) {}

    private fun captureImage() {

        mCamera!!.takePicture(null, null, this)

    }

    private fun setupImageCapture() {
        mCameraImage!!.visibility = View.INVISIBLE
        mCameraPreview!!.visibility = View.VISIBLE
        mCamera!!.startPreview()
        mCaptureImageButton!!.setText(R.string.capture_image)
        mCaptureImageButton!!.setOnClickListener(mCaptureImageButtonClickListener)
    }

    private fun setupImageDisplay() {

        val bitmap = BitmapFactory.decodeByteArray(mCameraData, 0, mCameraData!!.size)

        val dialog = SpotsDialog(this,R.style.CustomProgressDialog)
        dialog.show()

        createDirectoryAndSaveFile(bitmap,"a3dyou");
        dialog.dismiss()
        done_button.performClick()


        mCameraImage!!.setImageBitmap(RotateBitmap(bitmap, 90f))
        mCamera!!.stopPreview()
        mCameraPreview!!.visibility = View.INVISIBLE
        mCameraImage!!.visibility = View.VISIBLE
        mCaptureImageButton!!.setText(R.string.recapture_image)
        mCaptureImageButton!!.setOnClickListener(mRecaptureImageButtonClickListener)
    }

    companion object {

        val EXTRA_CAMERA_DATA = "camera_data"

        private val KEY_IS_CAPTURING = "is_capturing"


        fun RotateBitmap(source: Bitmap, angle: Float): Bitmap {
            val matrix = Matrix()
            matrix.postRotate(angle)
            return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
        }
    }


}

