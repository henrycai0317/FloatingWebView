package com.example.testerfloatingwebview

import android.annotation.SuppressLint
import android.graphics.RectF
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.example.testerfloatingwebview.databinding.ActivityMainBinding
import kotlin.math.absoluteValue

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var webViewPopup: TesterWebViewSaveState? = null
    private var isPopupMinimized = false
    private var initFabIconOffsetX: Float = 0f //原始浮動按鈕一開始顯示X軸位置
    private var initFabIconOffsetY: Float = 0f //原始浮動按鈕一開始顯示Y軸位置
    private var isFirstOpenWebView = true

    private var dX: Float = 0f
    private var dY: Float = 0f
    private var initialX: Float = 0f
    private var initialY: Float = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 初始化ViewBinding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initView()

    }

    private fun initView() {
        binding.apply {
            setupFloatingActionButton()
            btLaunchWebView.setOnClickListener {
                toggleWebViewPopup()
            }
        }
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
            setOnTouchListener { fabView, motionEvent ->
                when (motionEvent.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        dX = fabView.x - motionEvent.rawX
                        dY = fabView.y - motionEvent.rawY
                        initialX = motionEvent.rawX
                        initialY = motionEvent.rawY

                        // Start stretching the FAB
                        fabView.animate().scaleX(1.2f).scaleY(1.2f).setDuration(150).start()

                        return@setOnTouchListener true
                    }

                    MotionEvent.ACTION_MOVE -> {
                        binding.apply {
                            closeIconView.visibility = View.VISIBLE

                            val newX = (motionEvent.rawX + dX).coerceIn(
                                0f, (root.width - fabView.width).toFloat()
                            )
                            val newY = (motionEvent.rawY + dY).coerceIn(
                                0f, (root.height - fabView.height).toFloat()
                            )

                            if (isViewIntersecting(fabView, closeIconView, newX, newY)) {
                                closeIconView.setImageDrawable(
                                    ContextCompat.getDrawable(
                                        this@MainActivity,
                                        R.drawable.year_review_cancel_red
                                    )
                                )
                            } else {
                                closeIconView.setImageDrawable(
                                    ContextCompat.getDrawable(
                                        this@MainActivity,
                                        R.drawable.year_review_cancel_black
                                    )
                                )
                            }
                            fabView.animate().x(newX).y(newY).setDuration(0).start()
                        }
                        return@setOnTouchListener true
                    }

                    MotionEvent.ACTION_UP -> {
                        binding.apply {
                            closeIconView.visibility = View.GONE
                            val deltaX = motionEvent.rawX - initialX
                            val deltaY = motionEvent.rawY - initialY

                            val newX = (motionEvent.rawX + dX).coerceIn(
                                0f, (root.width - fabView.width).toFloat()
                            )
                            val newY = (motionEvent.rawY + dY).coerceIn(
                                0f, (root.height - fabView.height).toFloat()
                            )

                            if (isViewIntersecting(fabView, closeIconView, newX, newY)) {
                                webViewPopup = null
                                fab.visibility = View.GONE
                            }

                            // Shrink back to original size
                            fabView.animate().scaleX(1f).scaleY(1f).setDuration(150).start()

                            fabView.animate().z(0f).setDuration(0).start()

                            if (deltaX.absoluteValue < 10 && deltaY.absoluteValue < 10) {
                                toggleWebViewPopup()
                            }
                        }
                        return@setOnTouchListener true
                    }

                    else -> return@setOnTouchListener false
                }
            }
        }
    }


    /** 判斷兩個圖片是否相交*/
    private fun isViewIntersecting(
        fabIconView: View,
        closeIconView: View,
        fabIconOffsetX: Float,
        fabIconOffsetY: Float
    ): Boolean {
        // 計算view1的矩形範圍
        val rect1 = RectF(
            fabIconOffsetX, fabIconOffsetY,
            fabIconOffsetX + fabIconView.width, fabIconOffsetY + fabIconView.height
        )

        // 計算view2的矩形範圍
        val rect2 = RectF(
            closeIconView.x, closeIconView.y,
            closeIconView.x + closeIconView.width, closeIconView.y + closeIconView.height
        )

        // 判斷兩個矩形是否相交
        return RectF.intersects(rect1, rect2)
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

            animate().scaleX(0f).scaleY(0f).setDuration(300).withEndAction {
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

                animate().scaleX(1f).scaleY(1f).setDuration(300).start()
            }
            isPopupMinimized = false
        }
    }

    private fun setupPopupDragAndDrop(popup: TesterWebViewSaveState) {
        popup.contentView.setOnTouchListener(object : View.OnTouchListener {
            private var initialY: Float = 0f
            private var downY: Float = 0f

            @SuppressLint("ClickableViewAccessibility")
            override fun onTouch(view: View, motionEvent: MotionEvent): Boolean {
                when (motionEvent.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        initialY = motionEvent.rawY
                        downY = motionEvent.rawY

                        // Start stretching the popup
                        view.animate().scaleX(1.05f).scaleY(1.05f).setDuration(150).start()

                        return true
                    }

                    MotionEvent.ACTION_MOVE -> {
                        val deltaY = motionEvent.rawY - initialY
                        if (deltaY > 0) {
                            popup.contentView.apply {
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

                        // Shrink back when released
                        view.animate().scaleX(1f).scaleY(1f).setDuration(150).start()

                        if (deltaY > 300) {
                            minimizePopup()
                        } else {
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








