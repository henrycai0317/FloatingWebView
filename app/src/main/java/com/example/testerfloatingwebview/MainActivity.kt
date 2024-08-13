package com.example.testerfloatingwebview

import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.example.testerfloatingwebview.databinding.ActivityMainBinding
import kotlin.math.absoluteValue

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var webViewPopup: TesterWebViewSaveState? = null
    private var isPopupMinimized = false

    private var dX: Float = 0f
    private var dY: Float = 0f
    private var initialX: Float = 0f
    private var initialY: Float = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 初始化ViewBinding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupFloatingActionButton()
    }




    override fun onBackPressed() {
        if (webViewPopup != null && webViewPopup?.isShowing == true) {
            webViewPopup?.handleBackPressed()
        } else {
            super.onBackPressed()
        }
    }

    private fun setupFloatingActionButton() {
        binding.fab.apply {
            isFocusable = true
            isClickable = true
            setOnTouchListener { view, motionEvent ->
                when (motionEvent.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        dX = view.x - motionEvent.rawX
                        dY = view.y - motionEvent.rawY
                        initialX = motionEvent.rawX
                        initialY = motionEvent.rawY
                        return@setOnTouchListener true
                    }

                    MotionEvent.ACTION_MOVE -> {
                        view.animate()
                            .x(motionEvent.rawX + dX)
                            .y(motionEvent.rawY + dY)
                            .setDuration(0)
                            .start()
                        return@setOnTouchListener true
                    }

                    MotionEvent.ACTION_UP -> {
                        val deltaX = motionEvent.rawX - initialX
                        val deltaY = motionEvent.rawY - initialY

                        // 如果移动距离很小，触发点击事件
                        if (deltaX.absoluteValue < 10 && deltaY.absoluteValue < 10) {
                            toggleWebViewPopup()
                        }
                        return@setOnTouchListener true
                    }

                    else -> return@setOnTouchListener false
                }
            }
        }

    }

    private fun toggleWebViewPopup() {
        if (webViewPopup == null) {
            // 检查 Activity 是否仍然有效
            if (!isFinishing && !isDestroyed) {
                // 如果 PopupWindow 不存在，创建并显示
                webViewPopup = TesterWebViewSaveState(this, this::minimizePopup)
                webViewPopup?.showAtLocation(binding.root, android.view.Gravity.CENTER, 0, 0)

                // 添加拖动和缩小逻辑
                setupPopupDragAndDrop(webViewPopup!!)
            }
        } else if (isPopupMinimized) {
            // 如果 PopupWindow 已经最小化，则恢复它
            restorePopup()
        }
    }

    private fun minimizePopup() {
        // 通过缩放动画缩小 PopupWindow 并将其标记为缩小状态
        webViewPopup?.contentView?.animate()?.scaleX(0.3f)?.scaleY(0.3f)?.setDuration(300)
            ?.withEndAction {
                webViewPopup?.dismiss() // 隐藏 PopupWindow
                isPopupMinimized = true
                binding.fab.isVisible = true // 显示 FloatingActionButton
            }?.start()
    }

    private fun restorePopup() {
        // 恢复缩小的 PopupWindow 并将其标记为非缩小状态
        webViewPopup?.let {
            it.showAtLocation(binding.root, android.view.Gravity.CENTER, 0, 0)
            it.contentView?.apply {
                scaleX = 0.3f
                scaleY = 0.3f
                translationY = 0f // 确保位置复位
                animate()?.scaleX(1f)?.scaleY(1f)?.translationY(0f)?.setDuration(300)?.start()
            }
            isPopupMinimized = false
        }
    }

    private fun setupPopupDragAndDrop(popup: TesterWebViewSaveState) {
        popup.contentView.setOnTouchListener(object : View.OnTouchListener {
            private var initialY: Float = 0f
            private var downY: Float = 0f
            private var isLongPressing = false

            override fun onTouch(view: View, motionEvent: MotionEvent): Boolean {
                when (motionEvent.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        initialY = motionEvent.rawY
                        downY = motionEvent.rawY
                        isLongPressing = true

                        // 检查长按
                        view.postDelayed({
                            if (isLongPressing) {
                                // 长按开始，用户可以拖动缩小
                            }
                        }, 500)

                        return true
                    }

                    MotionEvent.ACTION_MOVE -> {
                        val deltaY = motionEvent.rawY - initialY

                        if (deltaY > 0 && isLongPressing) {
                            popup.contentView.translationY = deltaY
                        }
                        return true
                    }

                    MotionEvent.ACTION_UP -> {
                        isLongPressing = false
                        val deltaY = motionEvent.rawY - downY

                        // 如果长按后下滑距离足够大，则缩小 PopupWindow
                        if (deltaY > 300) {
                            minimizePopup()
                        } else {
                            // 否则回弹回初始位置
                            popup.contentView.animate().translationY(0f).setDuration(200).start()
                        }
                        return true
                    }

                    else -> return false
                }
            }
        })
    }
}








