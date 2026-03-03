package com.dodam.analogueapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class ShareBottomSheetFragment : BottomSheetDialogFragment() {

    // 클릭 이벤트를 처리할 인터페이스 정의
    interface ShareActionListener {
        fun onShareToInstagramStory()
        fun onShareWithOtherApps()
        fun onCopyLink()
    }

    var listener: ShareActionListener? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // 배경을 투명하게 하여 둥근 모서리가 보이게 함
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        return inflater.inflate(R.layout.bottom_sheet_share, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 각 버튼 클릭 리스너 연결
        view.findViewById<LinearLayout>(R.id.btnShareInstagram).setOnClickListener {
            listener?.onShareToInstagramStory()
            dismiss()
        }

        // 페이스북 등 다른 앱 버튼도 동일하게 처리...
        view.findViewById<LinearLayout>(R.id.btnShareFacebook).setOnClickListener {
            Toast.makeText(context, "페이스북 공유 준비 중", Toast.LENGTH_SHORT).show()
            dismiss()
        }

        view.findViewById<LinearLayout>(R.id.btnCopyLink).setOnClickListener {
            listener?.onCopyLink()
            dismiss()
        }

        view.findViewById<LinearLayout>(R.id.btnShareOther).setOnClickListener {
            listener?.onShareWithOtherApps()
            dismiss()
        }
    }
}