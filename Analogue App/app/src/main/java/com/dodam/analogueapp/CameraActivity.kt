package com.dodam.analogueapp

import android.Manifest
import android.animation.ObjectAnimator
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.media.MediaRecorder
import android.os.*
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors
import kotlin.concurrent.timer

class CameraActivity : AppCompatActivity() {

    private var imageCapture: ImageCapture? = null
    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private val handler = Handler(Looper.getMainLooper())

    private lateinit var viewFinder: PreviewView
    private lateinit var flashView: View
    private lateinit var txtTimeStamp: TextView

    // UI 요소
    private lateinit var imgPhotoReview: ImageView
    private lateinit var btnRetry: View  // 휴지통
    private lateinit var btnCapture: View
    private lateinit var btnRecord: View

    // 녹화 관련 UI
    private lateinit var recordingOverlay: View
    private lateinit var overlayRecorderView: HorizontalRecorderView
    private lateinit var txtOverlayTimer: TextView

    // 녹화 기능 변수
    private var mediaRecorder: MediaRecorder? = null
    private var audioFile: File? = null
    private var currentPhotoName: String = ""
    private var isRecording = false
    private var recordTimer: Timer? = null
    private var startTime = 0L

    // 임시 사진 파일
    private var tempPhotoFile: File? = null

    private val clockRunnable = object : Runnable {
        override fun run() {
            val currentTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            txtTimeStamp.text = currentTime
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        // 1. 뷰 연결
        viewFinder = findViewById(R.id.viewFinder)
        flashView = findViewById(R.id.flashView)
        txtTimeStamp = findViewById(R.id.txtTimeStamp)

        imgPhotoReview = findViewById(R.id.imgPhotoReview)
        btnRetry = findViewById(R.id.btnRetry)

        btnCapture = findViewById(R.id.btnCapture)
        btnRecord = findViewById(R.id.btnRecord)

        recordingOverlay = findViewById(R.id.recordingOverlay)
        overlayRecorderView = findViewById(R.id.overlayRecorderView)
        txtOverlayTimer = findViewById(R.id.txtOverlayTimer)

        // 2. 권한 체크
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO), 100)
        }

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }

        // 3. 버튼 리스너
        btnCapture.setOnClickListener { takePhoto() }

        btnRecord.setOnClickListener {
            if (currentPhotoName.isEmpty()) {
                currentPhotoName = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())
            }
            startRecordingUI()
        }

        btnRetry.setOnClickListener {
            resetCameraUI()
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().setTargetAspectRatio(AspectRatio.RATIO_4_3).build()
                .also { it.setSurfaceProvider(viewFinder.surfaceProvider) }
            imageCapture = ImageCapture.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture)
            } catch (exc: Exception) { Log.e("Camera", "Binding failed", exc) }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        // 플래시 효과
        flashView.visibility = View.VISIBLE
        flashView.alpha = 1f
        ObjectAnimator.ofFloat(flashView, View.ALPHA, 1f, 0f).setDuration(300).start()

        currentPhotoName = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())

        imageCapture.takePicture(ContextCompat.getMainExecutor(this), object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                // 필터 적용
                val bitmap = imageProxyToBitmap(image)
                val filtered = applyAnalogFilter(bitmap)
                image.close()

                // 임시 파일 저장 (SaveActivity로 넘기기 위함)
                val file = File(externalCacheDir, "$currentPhotoName.jpg")
                file.outputStream().use {
                    filtered.compress(Bitmap.CompressFormat.JPEG, 95, it)
                }
                tempPhotoFile = file

                // 리뷰 화면 전환
                handler.post { showReviewUI(filtered) }
            }
            override fun onError(exc: ImageCaptureException) { Log.e("Camera", "Error", exc) }
        })
    }

    private fun showReviewUI(bitmap: Bitmap) {
        imgPhotoReview.setImageBitmap(bitmap)

        // ✅ [추가] 이 뷰를 맨 앞으로 가져옵니다 (카메라 화면을 덮어씀)
        imgPhotoReview.bringToFront()

        imgPhotoReview.visibility = View.VISIBLE
        txtTimeStamp.visibility = View.GONE

        btnCapture.visibility = View.INVISIBLE
        btnRecord.visibility = View.VISIBLE
        btnRetry.visibility = View.VISIBLE

        // 혹시 레이아웃이 겹쳐서 버튼이 안 눌릴 수 있으니 버튼들도 앞으로
        btnRecord.bringToFront()
        btnRetry.bringToFront()
    }

    private fun resetCameraUI() {
        tempPhotoFile?.delete()
        tempPhotoFile = null

        imgPhotoReview.visibility = View.GONE
        txtTimeStamp.visibility = View.VISIBLE

        btnCapture.visibility = View.VISIBLE
        btnRecord.visibility = View.VISIBLE
        btnRetry.visibility = View.GONE
    }

    // --- 녹화 로직 ---

    private fun startRecordingUI() {
        recordingOverlay.visibility = View.VISIBLE
        startRecording() // 녹음 시작
        startTime = System.currentTimeMillis()

        recordTimer = timer(period = 50) {
            if (isRecording && mediaRecorder != null) {
                val millis = System.currentTimeMillis() - startTime
                handler.post {
                    val second = (millis / 1000) % 60
                    val minute = (millis / (1000 * 60)) % 60
                    txtOverlayTimer.text = String.format("%02d:%02d.%02d", minute, second, (millis % 1000) / 10)
                }
            }
        }

        findViewById<View>(R.id.btnRecordExit).setOnClickListener { stopRecordingUI(save = false) }
        findViewById<View>(R.id.btnStopRecordCircle).setOnClickListener { stopRecordingUI(save = true) }
    }

    private fun stopRecordingUI(save: Boolean) {
        recordTimer?.cancel()
        recordTimer = null
        stopRecording() // 녹음 파일 닫기 (저장은 안함)
        recordingOverlay.visibility = View.GONE

        if (save) {
            // ✅ [핵심] SaveActivity로 이동하면서 오디오 경로 전달
            val intent = Intent(this@CameraActivity, SaveActivity::class.java)

            if (tempPhotoFile != null && tempPhotoFile!!.exists()) {
                intent.putExtra("photoPath", tempPhotoFile!!.absolutePath)
            }

            if (audioFile != null && audioFile!!.exists()) {
                intent.putExtra("audioPath", audioFile!!.absolutePath)
            } else {
                Log.e("CameraActivity", "오디오 파일이 생성되지 않았습니다.")
            }

            startActivity(intent)
        }
    }

    private fun startRecording() {
        // 캐시 폴더에 임시 파일 생성
        audioFile = File(externalCacheDir, "$currentPhotoName.m4a")
        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(audioFile?.absolutePath)
            prepare(); start()
            isRecording = true
        }
    }

    private fun stopRecording() {
        if (isRecording) {
            try {
                mediaRecorder?.stop()
                mediaRecorder?.release()
                mediaRecorder = null
                isRecording = false
                // 🚨 주의: 여기서 saveAudioToGallery() 같은 걸 호출하면 안 됩니다!
            } catch (e: Exception) { Log.e("Audio", "Stop failed", e) }
        }
    }

    private fun applyAnalogFilter(source: Bitmap): Bitmap {
        val width = source.width
        val height = source.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint()

        val colorMatrix = ColorMatrix().apply { setSaturation(0.3f) }
        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(source, 0f, 0f, paint)

        val random = Random()
        paint.colorFilter = null
        for (i in 0 until 30000) {
            val x = random.nextFloat() * width
            val y = random.nextFloat() * height
            val alpha = random.nextInt(50) + 20
            paint.color = if (random.nextBoolean()) Color.argb(alpha, 255, 255, 255) else Color.argb(alpha, 0, 0, 0)
            canvas.drawPoint(x, y, paint)
        }

        val timeStamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val textPaint = Paint().apply {
            color = Color.parseColor("#FF9800")
            textSize = width / 25f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            setShadowLayer(5f, 3f, 3f, Color.BLACK)
        }
        val textBounds = Rect()
        textPaint.getTextBounds(timeStamp, 0, timeStamp.length, textBounds)
        canvas.drawText(timeStamp, width - textBounds.width() - (width / 20f), height - (height / 20f), textPaint)

        return bitmap
    }

    private fun imageProxyToBitmap(image: ImageProxy): Bitmap {
        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        val original = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        val matrix = Matrix().apply { postRotate(image.imageInfo.rotationDegrees.toFloat()) }
        return Bitmap.createBitmap(original, 0, 0, original.width, original.height, matrix, true)
    }

    private fun allPermissionsGranted() = arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO).all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onResume() { super.onResume(); handler.post(clockRunnable) }
    override fun onPause() { super.onPause(); handler.removeCallbacks(clockRunnable) }
    override fun onDestroy() { super.onDestroy(); cameraExecutor.shutdown(); recordTimer?.cancel() }
}