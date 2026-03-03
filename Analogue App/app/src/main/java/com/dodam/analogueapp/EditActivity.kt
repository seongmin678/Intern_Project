package com.dodam.analogueapp

import android.content.ContentValues
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class EditActivity : AppCompatActivity() {

    private var currentPhotoPath: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit) // activity_edit.xml과 연결

        val imgPreview = findViewById<ImageView>(R.id.imgPreview)
        val btnRetake = findViewById<TextView>(R.id.btnRetake)
        val btnOk = findViewById<TextView>(R.id.btnOk)

        // 카메라에서 넘겨준 사진 경로 받기
        currentPhotoPath = intent.getStringExtra("photoPath")

        // 사진 화면에 띄우기
        if (currentPhotoPath != null) {
            val file = File(currentPhotoPath!!)
            if (file.exists()) {
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                imgPreview.setImageBitmap(bitmap)
            }
        }

        // 🔙 다시 찍기 (뒤로가기)
        btnRetake.setOnClickListener {
            // 현재 임시 파일 삭제 (선택사항)
            currentPhotoPath?.let { path -> File(path).delete() }
            finish() // 카메라 화면으로 복귀
        }

        // ✅ 저장하기 (갤러리에 저장 후 홈으로)
        btnOk.setOnClickListener {
            if (currentPhotoPath != null) {
                saveImageToGallery(currentPhotoPath!!)
                Toast.makeText(this, "사진이 저장되었습니다!", Toast.LENGTH_SHORT).show()

                // 저장 후 홈으로 이동
                val intent = Intent(this, HomeActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                startActivity(intent)
                finish()
            }
        }
    }

    // 갤러리에 사진 저장하는 함수
    private fun saveImageToGallery(path: String) {
        val file = File(path)
        val fileName = file.name

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10 이상
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/AnalogueApp")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }

            val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val itemUri = contentResolver.insert(collection, values)

            itemUri?.let { uri ->
                contentResolver.openOutputStream(uri)?.use { out ->
                    FileInputStream(file).use { input -> input.copyTo(out) }
                }
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                contentResolver.update(uri, values, null, null)
            }
        } else {
            // Android 9 이하
            val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val destFolder = File(picturesDir, "AnalogueApp")
            if (!destFolder.exists()) destFolder.mkdirs()

            val destFile = File(destFolder, fileName)
            FileInputStream(file).use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
        }
    }
}