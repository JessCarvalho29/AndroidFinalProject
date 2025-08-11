package com.example.week10

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import android.widget.VideoView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class Camera : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private var userId: String? = null
    private lateinit var previewView: PreviewView
    private lateinit var imageView: ImageView
    private lateinit var videoView: VideoView
    private lateinit var imageCapture: ImageCapture
    private lateinit var cameraExecutor: ExecutorService
    private var photoUri: Uri ? = null
    private val storage = FirebaseStorage.getInstance()
    private lateinit var videoCapture: VideoCapture<Recorder>
    private var activeRecording: Recording? = null
    private var videoUri: Uri? = null
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions ())
    {
        permissions ->
        val cameraGranted = permissions[Manifest.permission.CAMERA]
        val audioGranted = permissions[Manifest.permission.RECORD_AUDIO]
        if (cameraGranted == true && audioGranted == true) { startCamera() }
    else { showToast("Camera & Audio permissions are required") }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_camera)

        db = FirebaseFirestore.getInstance()
        userId = FirebaseAuth.getInstance().currentUser?.uid

        previewView = findViewById(R.id.previewView)
        imageView = findViewById(R.id.imageView)
        videoView = findViewById(R.id.videoView)
        cameraExecutor = Executors.newSingleThreadExecutor()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED){
            startCamera()
        } else {
            requestPermissionLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
        }

        findViewById<Button>(R.id.btnTakePhoto).setOnClickListener {
            takePhoto()
        }

        findViewById<Button>(R.id.btnExtractPhoto).setOnClickListener {
            if (photoUri != null) {
                imageView.visibility = View.VISIBLE
                videoView.visibility = View.GONE
                imageView.setImageURI(photoUri)
            } else {
                showToast("No photo available")
            }
        }

        var isRecording = false
        val recordVideo = findViewById<Button>(R.id.btnRecordVideo)
        recordVideo.setOnClickListener {
            if (isRecording) {
                stopRecording()
                recordVideo.text = "Start Video"
            } else {
                startRecording()
                recordVideo.text = "Stop Video"
            }
            isRecording = !isRecording
        }

        // EXTRA FEATURE
        findViewById<Button>(R.id.btnExtractVideo).setOnClickListener {
            if (videoUri != null) {
                imageView.visibility = View.GONE
                videoView.visibility = View.VISIBLE
                videoView.setVideoURI(videoUri)
                videoView.start()
            } else {
                showToast("No video available")
            }
        }

        findViewById<ImageButton>(R.id.imageBtnPreviousPage).setOnClickListener {
            finish()
        }

        findViewById<Button>(R.id.buttonExit).setOnClickListener {
            val intent = Intent(this, Feedback::class.java)
            startActivity(intent)
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener( {
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.surfaceProvider = previewView.surfaceProvider
            }
            imageCapture = ImageCapture.Builder().build()

            val recorder = Recorder.Builder().setQualitySelector(QualitySelector.from(Quality.HD)).build()
            videoCapture = VideoCapture.withOutput(recorder)

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture, videoCapture)
        },
            ContextCompat.getMainExecutor(this))
    }

    private fun showToast (message: String){
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun takePhoto() {
        val photoFile = File(getOutputDirectory(), "${System.currentTimeMillis()}.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
        imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(this), object: ImageCapture.OnImageSavedCallback {

            override fun onImageSaved(result: ImageCapture.OutputFileResults) {
                photoUri = Uri.fromFile(photoFile)
                showToast("Photo saved")
                uploadToFirebase(photoUri!!)
            }

            override fun onError(e: ImageCaptureException){
                showToast("Error: ${e.message}")
            }

        })
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private fun startRecording() {
        val videoFile = File(getOutputDirectory(), "${System.currentTimeMillis()}.mp4")
        val outputOptions = FileOutputOptions.Builder(videoFile).build()
        activeRecording = videoCapture.output.prepareRecording(this, outputOptions).withAudioEnabled().start(
            ContextCompat.getMainExecutor(this)) { event ->
            if (event is VideoRecordEvent.Finalize) {
                if (event.hasError()){
                    showToast("Error recording video: ${event.error}")
                } else {
                    videoUri = Uri.fromFile(videoFile)
                    showToast("Video saved")
                    uploadToFirebase(videoUri!!)
                }
            }
        }
    }

    private fun stopRecording() {
        activeRecording?.stop()
        activeRecording = null
    }

    private fun uploadToFirebase(uri: Uri) {
        lateinit var ref : StorageReference
        uri.lastPathSegment?.let {
            ref = if (it.contains(".jpg")) {
                storage.reference.child("${userId}/Images/${uri.lastPathSegment}")
            } else {
                storage.reference.child("${userId}/Videos/${uri.lastPathSegment}")
            }
        }

        ref.putFile(uri)
            .addOnSuccessListener { showToast("Upload successful") }
            .addOnFailureListener { showToast("Upload failed") }
    }

    private fun getOutputDirectory() : File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, "CameraxFirebase").apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists()) mediaDir else filesDir
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}