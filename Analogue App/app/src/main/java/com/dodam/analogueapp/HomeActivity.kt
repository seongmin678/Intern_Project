package com.dodam.analogueapp

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity

class HomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // 1. 🗄️ 서랍(갤러리) 버튼 연결
        // activity_home.xml에 android:id="@+id/btnDrawer"가 있어야 작동합니다.
        val btnDrawer = findViewById<View>(R.id.btnDrawer)
        btnDrawer?.setOnClickListener {
            val intent = Intent(this, GalleryActivity::class.java)
            startActivity(intent)
            // 부드러운 화면 전환 효과
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        // (로그아웃 버튼 코드는 삭제했습니다. 이제 에러가 나지 않습니다.)

        // 2. 📸 카메라 버튼 연결
        // activity_home.xml에 android:id="@+id/btnCamera"가 있어야 작동합니다.
        val btnCamera = findViewById<View>(R.id.btnCamera)
        btnCamera?.setOnClickListener {
            showCameraGuide()
        }
    }

    // 📷 카메라 가이드 팝업 띄우기
    private fun showCameraGuide() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.view_camera_guide)

        // 팝업창 배경을 투명하게 설정 (모서리 둥글게 보이기 위해 필수)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        // [확인] 버튼 누르면 카메라 실행
        val confirmButton = dialog.findViewById<View>(R.id.btnGuideConfirm)
        confirmButton?.setOnClickListener {
            dialog.dismiss() // 팝업 닫기
            val intent = Intent(this, CameraActivity::class.java)
            startActivity(intent)
        }

        // [닫기] 버튼 누르면 팝업만 닫기
        val closeButton = dialog.findViewById<View>(R.id.btnGuideClose)
        closeButton?.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
}