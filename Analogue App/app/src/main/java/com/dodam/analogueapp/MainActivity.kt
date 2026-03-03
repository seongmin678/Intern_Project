package com.dodam.analogueapp

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ✅ 로고가 그려진 xml 파일명을 여기에 넣으세요!
        // 아까 만든 디자인 파일이 activity_login이라면 그걸 쓰면 됩니다.
        setContentView(R.layout.activity_main)

        // --- 애니메이션 효과 ---
        val logo = findViewById<View>(R.id.imgLogo)
        val text = findViewById<View>(R.id.txtAppName)

        if (logo != null && text != null) {
            logo.alpha = 0f
            text.alpha = 0f
            logo.animate().alpha(1f).setDuration(1500).start()
            text.animate().alpha(1f).setDuration(1500).start()
        }

        // ✅ 2초 뒤에 HomeActivity로 이동
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
            finish() // 뒤로가기 방지
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }, 2000)
    }
}