package com.dodam.analogueapp

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat             // API 35 대응용
import androidx.core.view.WindowInsetsCompat     // API 35 대응용
import java.io.File
import java.io.FileOutputStream

class DetailActivity : AppCompatActivity(), ShareBottomSheetFragment.ShareActionListener {

    // 🎵 미디어 재생 관련 변수
    private var mediaPlayer: MediaPlayer? = null
    private var isPlaying = false
    private lateinit var seekBar: SeekBar
    private lateinit var btnPlayPause: ImageView
    private val handler = Handler(Looper.getMainLooper())
    private var currentAudioPath: String? = null

    // 🖼️ UI 관련 변수
    private lateinit var rootView: ConstraintLayout
    private lateinit var btnBack: ImageView
    private lateinit var btnShare: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)

        // 1. UI 초기화
        rootView = findViewById(R.id.rootLayout)
        btnBack = findViewById(R.id.btnDetailBack)
        btnShare = findViewById(R.id.btnShare)

        // ✅ [상태바 겹침 해결] 시스템 바(상태바, 네비게이션바)만큼 패딩 적용
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val imgDetail = findViewById<ImageView>(R.id.detailImage)
        val txtTitle = findViewById<TextView>(R.id.txtDetailTitle)
        val txtLocation = findViewById<TextView>(R.id.txtLocation)
        val txtDescription = findViewById<TextView>(R.id.txtDescription)
        val layoutEmotions = findViewById<LinearLayout>(R.id.layoutEmotions)

        btnPlayPause = findViewById(R.id.btnPlayPause)
        seekBar = findViewById(R.id.audioSeekBar)

        // 2. 데이터 가져오기
        val photoPath = intent.getStringExtra("photoPath")

        if (photoPath != null && File(photoPath).exists()) {
            val photoFile = File(photoPath)

            // 📸 사진 표시
            val bitmap = BitmapFactory.decodeFile(photoPath)
            imgDetail.setImageBitmap(bitmap)

            // 🏷️ 제목 설정
            val title = photoFile.nameWithoutExtension
            txtTitle.text = title

            // 📂 텍스트 파일(.txt) 읽기
            val textFile = File(photoFile.parent, "$title.txt")
            if (textFile.exists()) {
                val lines = textFile.readLines()
                if (lines.isNotEmpty()) {
                    txtLocation.text = lines.getOrElse(0) { "위치 정보 없음" }
                    val emotion = lines.getOrElse(1) { "" }
                    if (emotion.isNotBlank()) addEmotionTag(layoutEmotions, emotion)
                    val desc = if (lines.size > 2) lines.subList(2, lines.size).joinToString("\n") else ""
                    txtDescription.text = desc
                }
            }

            // 🎵 오디오 파일(.m4a) 찾기
            val audioFile = File(photoFile.parent, "$title.m4a")
            if (audioFile.exists() && audioFile.length() > 0) {
                currentAudioPath = audioFile.absolutePath
            }
        }

        // 3. 버튼 리스너
        btnPlayPause.setOnClickListener {
            if (currentAudioPath != null) {
                if (isPlaying) pauseAudio() else playAudio(currentAudioPath!!)
            } else {
                Toast.makeText(this, "재생할 녹음 파일이 없습니다.", Toast.LENGTH_SHORT).show()
            }
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) mediaPlayer?.seekTo(progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        btnShare.setOnClickListener {
            val bottomSheet = ShareBottomSheetFragment()
            bottomSheet.listener = this
            bottomSheet.show(supportFragmentManager, "ShareBottomSheet")
        }

        btnBack.setOnClickListener { finish() }
    }

    // --- 오디오 재생 기능 ---
    private fun playAudio(path: String) {
        try {
            if (mediaPlayer == null) {
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(path)
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
                    )
                    prepare()
                    setOnCompletionListener { stopAudio() }
                }
                seekBar.max = mediaPlayer!!.duration
            }
            mediaPlayer?.start()
            isPlaying = true
            btnPlayPause.setImageResource(android.R.drawable.ic_media_pause)
            updateSeekBar()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun pauseAudio() {
        mediaPlayer?.pause()
        isPlaying = false
        btnPlayPause.setImageResource(android.R.drawable.ic_media_play)
    }

    private fun stopAudio() {
        mediaPlayer?.let {
            if (it.isPlaying) it.stop()
            it.release()
        }
        mediaPlayer = null
        isPlaying = false
        btnPlayPause.setImageResource(android.R.drawable.ic_media_play)
        seekBar.progress = 0
    }

    private fun updateSeekBar() {
        if (mediaPlayer != null && isPlaying) {
            seekBar.progress = mediaPlayer!!.currentPosition
            handler.postDelayed({ updateSeekBar() }, 100)
        }
    }

    // --- 공유 및 기타 기능 ---
    private fun addEmotionTag(layout: LinearLayout, text: String) {
        layout.removeAllViews()
        val textView = TextView(this)
        textView.text = text
        textView.setTextColor(Color.WHITE)
        textView.setPadding(40, 16, 40, 16)
        val bg = android.graphics.drawable.GradientDrawable()
        bg.cornerRadius = 50f
        bg.setColor(when (text) {
            "사랑해" -> Color.parseColor("#D7A2A2")
            "고마워" -> Color.parseColor("#9FA88F")
            "미안해" -> Color.parseColor("#90A4AE")
            else -> Color.parseColor("#A1887F")
        })
        textView.background = bg
        layout.addView(textView)
    }

    override fun onShareToInstagramStory() {
        val imageUri = captureScreenWithoutButtons()
        if (imageUri != null) {
            val intent = Intent("com.instagram.share.ADD_TO_STORY").apply {
                setDataAndType(imageUri, "image/*")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                putExtra("interactive_asset_uri", imageUri)
            }
            startActivity(intent)
        }
    }

    override fun onShareWithOtherApps() {
        captureScreenWithoutButtons()?.let { uri ->
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "공유하기"))
        }
    }

    override fun onCopyLink() { Toast.makeText(this, "링크 복사됨", Toast.LENGTH_SHORT).show() }

    private fun captureScreenWithoutButtons(): Uri? {
        btnBack.visibility = View.INVISIBLE
        btnShare.visibility = View.INVISIBLE
        val bitmap = Bitmap.createBitmap(rootView.width, rootView.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.parseColor("#EFEBE9"))
        rootView.draw(canvas)
        btnBack.visibility = View.VISIBLE
        btnShare.visibility = View.VISIBLE
        return try {
            val file = File(cacheDir, "shared_memory.png")
            FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
        } catch (e: Exception) { null }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAudio()
        handler.removeCallbacksAndMessages(null)
    }
}