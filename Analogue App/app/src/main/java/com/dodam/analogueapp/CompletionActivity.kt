package com.dodam.analogueapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat             // ✅ API 35 대응 추가
import androidx.core.view.WindowInsetsCompat     // ✅ API 35 대응 추가

class CompletionActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_completion)

        // ✅ [상태바 겹침 해결] 최상위 레이아웃에 시스템 바만큼 패딩 적용
        // activity_completion.xml의 가장 바깥 태그에 android:id="@+id/rootLayout"이 있어야 합니다.
        val root = findViewById<View>(R.id.rootLayout)
        if (root != null) {
            ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            }
        }

        // 1. 이전 화면에서 전달받은 제목 설정
        val videoTitle = intent.getStringExtra("VIDEO_TITLE") ?: "제목 없음"
        findViewById<TextView>(R.id.txtVideoTitle).text = videoTitle

        // 2. 뷰 참조
        val btnGoHome = findViewById<View>(R.id.btnGoHome) // 🔴 팝업 내 빨간 버튼
        val btnBackArrow = findViewById<ImageView>(R.id.btnBackArrow) // ⏪ 상단 되감기 버튼

        // 3. 🔴 빨간 버튼 클릭 시: HomeActivity로 이동
        btnGoHome.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            // 기존 기록을 지우고 홈을 새로 시작
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)

            // 최신 API 대응 애니메이션 적용
            overrideActivityTransition(
                AppCompatActivity.OVERRIDE_TRANSITION_OPEN,
                R.anim.slide_in_left,
                R.anim.slide_out_right
            )
        }

        // 4. ⏪ 되감기 버튼 클릭 시: 이전 화면으로 복귀
        btnBackArrow.setOnClickListener {
            finish()
            overrideActivityTransition(
                AppCompatActivity.OVERRIDE_TRANSITION_CLOSE,
                R.anim.slide_in_left,
                R.anim.slide_out_right
            )
        }
    }

    // 뒤로가기 버튼(시스템) 대응
    override fun onBackPressed() {
        super.onBackPressed()
        overrideActivityTransition(
            AppCompatActivity.OVERRIDE_TRANSITION_CLOSE,
            R.anim.slide_in_left,
            R.anim.slide_out_right
        )
    }
}