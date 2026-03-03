package com.dodam.analogueapp

import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.view.ViewCompat             // ✅ 추가
import androidx.core.view.WindowInsetsCompat     // ✅ 추가
import java.io.File
import java.io.FileOutputStream

class SaveActivity : AppCompatActivity() {

    private var selectedEmotion: String = ""
    private var currentPhotoPath: String? = null
    private var currentAudioPath: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_save)

        // 1. 뷰 연결
        val rootLayout = findViewById<View>(R.id.rootLayout) // ✅ XML 최상위 레이아웃 ID
        val imgPreview = findViewById<ImageView>(R.id.imgSavePreview)
        val etTitle = findViewById<EditText>(R.id.etTitle)
        val etLocation = findViewById<EditText>(R.id.etLocation)
        val etDescription = findViewById<EditText>(R.id.etDescription)
        val btnSave = findViewById<AppCompatButton>(R.id.btnSave)
        val btnBack = findViewById<ImageView>(R.id.btnBack)

        val btnLove = findViewById<AppCompatButton>(R.id.btnLove)
        val btnThanks = findViewById<AppCompatButton>(R.id.btnThanks)
        val btnSorry = findViewById<AppCompatButton>(R.id.btnSorry)

        // ✅ [추가] API 35 상태바 겹침 방지 설정
        if (rootLayout != null) {
            ViewCompat.setOnApplyWindowInsetsListener(rootLayout) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            }
        }

        // 2. 전달받은 경로 확인
        currentPhotoPath = intent.getStringExtra("photoPath")
        currentAudioPath = intent.getStringExtra("audioPath")

        // 3. 사진 띄우기
        if (currentPhotoPath != null) {
            val file = File(currentPhotoPath!!)
            if (file.exists()) {
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                val rotatedBitmap = rotateBitmapIfNeeded(bitmap, file.absolutePath)
                imgPreview.setImageBitmap(rotatedBitmap)
            }
        }

        // 4. 감정 선택 로직
        fun updateEmotionUI(selectedBtn: AppCompatButton, emotion: String) {
            selectedEmotion = emotion
            btnLove.alpha = 0.5f; btnThanks.alpha = 0.5f; btnSorry.alpha = 0.5f
            selectedBtn.alpha = 1.0f
        }
        btnLove.setOnClickListener { updateEmotionUI(btnLove, "사랑해") }
        btnThanks.setOnClickListener { updateEmotionUI(btnThanks, "고마워") }
        btnSorry.setOnClickListener { updateEmotionUI(btnSorry, "미안해") }

        btnBack.setOnClickListener { finish() }

        // 5. 저장 버튼 클릭
        btnSave.setOnClickListener {
            val title = etTitle.text.toString().trim()
            if (title.isEmpty()) {
                Toast.makeText(this, "제목을 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 파일 영구 저장 실행
            val savedFile = saveFilesToPermanentStorage(title)

            if (savedFile != null) {
                // 텍스트 저장
                saveTextData(savedFile, title, etLocation.text.toString(), etDescription.text.toString())

                // 완료 화면으로 이동
                val intent = Intent(this, CompletionActivity::class.java)
                intent.putExtra("VIDEO_TITLE", title)
                startActivity(intent)

                // ✅ 최신 애니메이션 방식 적용
                overrideActivityTransition(
                    AppCompatActivity.OVERRIDE_TRANSITION_OPEN,
                    R.anim.slide_in_right,
                    R.anim.slide_out_left
                )
                finish()
            } else {
                Toast.makeText(this, "저장에 실패했습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 📂 파일 저장 로직 (사진 + 오디오)
    private fun saveFilesToPermanentStorage(title: String): File? {
        if (currentPhotoPath == null) return null

        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        if (storageDir != null && !storageDir.exists()) storageDir.mkdirs()

        val sourcePhoto = File(currentPhotoPath!!)
        val destPhoto = File(storageDir, "${title}.jpg")

        return try {
            sourcePhoto.copyTo(destPhoto, overwrite = true)

            if (currentAudioPath != null) {
                val sourceAudio = File(currentAudioPath!!)
                if (sourceAudio.exists()) {
                    val destAudio = File(storageDir, "${title}.m4a")
                    sourceAudio.copyTo(destAudio, overwrite = true)
                    Log.d("SaveActivity", "오디오 저장 성공: ${destAudio.absolutePath}")
                }
            }

            destPhoto
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun saveTextData(photoFile: File, title: String, location: String, description: String) {
        val folder = photoFile.parentFile
        val textFile = File(folder, "${title}.txt")
        try {
            val content = "$location\n$selectedEmotion\n$description"
            FileOutputStream(textFile).use { it.write(content.toByteArray()) }
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun rotateBitmapIfNeeded(bitmap: android.graphics.Bitmap, path: String): android.graphics.Bitmap {
        try {
            val ei = ExifInterface(path)
            return when (ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> rotateImage(bitmap, 90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> rotateImage(bitmap, 180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> rotateImage(bitmap, 270f)
                else -> bitmap
            }
        } catch (e: Exception) { return bitmap }
    }
    private fun rotateImage(source: android.graphics.Bitmap, angle: Float): android.graphics.Bitmap {
        val matrix = Matrix().apply { postRotate(angle) }
        return android.graphics.Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }
}