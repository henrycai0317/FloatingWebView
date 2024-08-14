package com.example.testerfloatingwebview

import android.annotation.SuppressLint
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

    @SuppressLint("ClickableViewAccessibility")
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
                        // 計算可移動區域的邊界
                        val newX = (motionEvent.rawX + dX).coerceIn(
                            0f,
                            (binding.root.width - view.width).toFloat()
                        )
                        val newY = (motionEvent.rawY + dY).coerceIn(
                            0f,
                            (binding.root.height - view.height).toFloat()
                        )

                        view.animate()
                            .x(newX)
                            .y(newY)
                            .setDuration(0)
                            .start()
                        return@setOnTouchListener true
                    }

                    MotionEvent.ACTION_UP -> {
                        val deltaX = motionEvent.rawX - initialX
                        val deltaY = motionEvent.rawY - initialY

                        // 恢復Z軸位置
                        view.animate().z(0f).setDuration(0).start()

                        // 如果移動距離很小，觸發點擊事件
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
        binding.fab.visibility = View.GONE
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
        webViewPopup?.contentView?.apply {
            // 設定縮放中心為FloatingActionButton的當前位置
            pivotX = binding.fab.x + binding.fab.width / 2
            pivotY = binding.fab.y + binding.fab.height / 2
            translationY = 0f // 确保位置复位

            animate()
                .scaleX(0f)
                .scaleY(0f)
                .setDuration(300)
                .withEndAction {
                    webViewPopup?.dismiss() // 隱藏 PopupWindow
                    isPopupMinimized = true
                    binding.fab.isVisible = true // 顯示 FloatingActionButton
                }.start()
        }
    }

    private fun restorePopup() {
        webViewPopup?.let { popup ->
            // 顯示PopupWindow並設置初始縮放和位置
            popup.showAtLocation(
                binding.root,
                android.view.Gravity.NO_GRAVITY,
                binding.fab.x.toInt(),
                binding.fab.y.toInt()
            )

            popup.contentView?.apply {
                scaleX = 0f
                scaleY = 0f
                // 設定縮放中心為FloatingActionButton的當前位置
                pivotX = binding.fab.x + binding.fab.width / 2
                pivotY = binding.fab.y + binding.fab.height / 2

                animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(300)
                    .start()
            }
            isPopupMinimized = false
        }
    }

    private fun setupPopupDragAndDrop(popup: TesterWebViewSaveState) {
        popup.contentView.setOnTouchListener(object : View.OnTouchListener {
            private var initialY: Float = 0f
            private var downY: Float = 0f

            override fun onTouch(view: View, motionEvent: MotionEvent): Boolean {
                when (motionEvent.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        initialY = motionEvent.rawY
                        downY = motionEvent.rawY
                        return true
                    }

                    MotionEvent.ACTION_MOVE -> {
                        val deltaY = motionEvent.rawY - initialY
                        popup.contentView.apply {
                            if (deltaY > 0) {
                                if (deltaY > 300) {
                                    scaleX = 0.8f
                                    scaleY = 0.8f
                                    pivotY = motionEvent.rawY
                                    pivotX = motionEvent.rawX
                                }
                                translationY = deltaY
                            }
                        }
                        return true
                    }

                    MotionEvent.ACTION_UP -> {
                        val deltaY = motionEvent.rawY - downY

                        // 如果长按后下滑距离足够大，则缩小 PopupWindow
                        if (deltaY > 300) {
                            minimizePopup()
                        } else {
                            // 否则回弹回初始位置
                            popup.contentView.apply {
                                scaleX = 1f
                                scaleY = 1f
                                pivotY = 0f
                                pivotX = 0f
                                animate().translationY(0f).setDuration(200).start()
                            }
                        }
                        return true
                    }

                    else -> return false
                }
            }
        })
    }
}








