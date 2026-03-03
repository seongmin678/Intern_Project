package com.dodam.analogueapp

import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
// ⬇️ 아래 3줄이 반드시 있어야 빨간 에러가 안 납니다.
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File

class GalleryActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gallery)

        // 1. 상단바 뷰 찾기
        val topBar = findViewById<View>(R.id.topBar)

        // 2. 상태바 높이만큼 상단 패딩 추가 (여기가 핵심!)
        // 이렇게 하면 카메라 구멍만큼만 딱 내려옵니다.
        ViewCompat.setOnApplyWindowInsetsListener(topBar) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            // 기존 패딩은 유지하면서 Top에만 상태바 높이를 더해줍니다.
            v.updatePadding(top = systemBars.top)

            insets
        }

        // 3. 애니메이션 설정
        overrideActivityTransition(
            AppCompatActivity.OVERRIDE_TRANSITION_OPEN,
            R.anim.slide_in_right,
            R.anim.slide_out_left
        )

        // 4. 뒤로가기 버튼
        findViewById<View>(R.id.btnGalleryBack).setOnClickListener { finish() }

        // 5. 리스트 설정
        val files = loadSavedFiles()
        val recyclerView = findViewById<RecyclerView>(R.id.galleryRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        recyclerView.adapter = TapeAdapter(files) { file ->
            val intent = Intent(this, DetailActivity::class.java)
            intent.putExtra("photoPath", file.absolutePath)
            startActivity(intent)
        }
    }

    private fun loadSavedFiles(): List<File> {
        val dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return dir?.listFiles { file -> file.extension == "jpg" }?.toList()?.reversed() ?: emptyList()
    }

    override fun finish() {
        super.finish()
        overrideActivityTransition(
            AppCompatActivity.OVERRIDE_TRANSITION_CLOSE,
            R.anim.slide_in_left,
            R.anim.slide_out_right
        )
    }
}

// TapeAdapter 부분은 기존 코드 그대로 두시면 됩니다.
class TapeAdapter(private val files: List<File>, private val onClick: (File) -> Unit) :
    RecyclerView.Adapter<TapeAdapter.TapeViewHolder>() {

    class TapeViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.txtTapeTitle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TapeViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_video_tape, parent, false)
        return TapeViewHolder(view)
    }

    override fun onBindViewHolder(holder: TapeViewHolder, position: Int) {
        val file = files[position]
        holder.title.text = file.nameWithoutExtension
        holder.itemView.setOnClickListener { onClick(file) }
    }

    override fun getItemCount() = files.size
}