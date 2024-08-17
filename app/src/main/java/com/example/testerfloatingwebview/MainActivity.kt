package com.example.testerfloatingwebview

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.RectF
import android.os.Bundle
import android.transition.TransitionManager
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.testerfloatingwebview.databinding.ActivityMainBinding
import com.google.android.material.transition.platform.MaterialContainerTransform
import kotlin.math.absoluteValue

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var webViewCardView: TesterWebViewCardView? = null
    private var isCardMinimized = false
    private var dX: Float = 0f
    private var dY: Float = 0f
    private var initialX: Float = 0f
    private var initialY: Float = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initCardWebView()
        initView()
    }

    private fun initView() {
        binding.apply {
            setupFloatingActionButton()
            btLaunchWebView.setOnClickListener {
                toggleWebViewCardView()
            }
        }
    }

    private fun initCardWebView() {
        webViewCardView = TesterWebViewCardView(this@MainActivity) {
            minimizeCardView()
        }.apply {
            visibility = View.GONE
        }
        binding.root.addView(webViewCardView)
        setupCardDragAndDrop(webViewCardView)
    }

    override fun onBackPressed() {
        if (webViewCardView?.visibility == View.VISIBLE) {
            webViewCardView?.handleBackPressed()
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
                                webViewCardView?.visibility = View.GONE
                                fab.visibility = View.GONE
                                webViewCardView = null
                            }

                            fabView.animate().scaleX(1f).scaleY(1f).setDuration(150).start()
                            fabView.animate().z(0f).setDuration(0).start()

                            if (deltaX.absoluteValue < 10 && deltaY.absoluteValue < 10) {
                                toggleWebViewCardView()
                            }
                        }
                        return@setOnTouchListener true
                    }

                    else -> return@setOnTouchListener false
                }
            }
        }
    }

    private fun toggleWebViewCardView() {
        if (webViewCardView == null) {
            initCardWebView()
        }

        if (webViewCardView?.visibility == View.GONE) {
            expandCardView()
        } else if (isCardMinimized) {
            restoreCardView()
        }
    }

    private fun expandCardView() {
        val transform = MaterialContainerTransform().apply {
            startView = binding.fab
            endView = webViewCardView
            addTarget(webViewCardView)
            duration = 500L
            scrimColor = Color.TRANSPARENT
        }

        TransitionManager.beginDelayedTransition(binding.root, transform)
        webViewCardView?.visibility = View.VISIBLE
        webViewCardView?.translationY = 0f //確保CardWebView 回原位
        binding.dimBackground.visibility = View.VISIBLE  // 顯示背景遮罩
        binding.fab.visibility = View.GONE
        isCardMinimized = false
    }

    private fun minimizeCardView() {
        val transform = MaterialContainerTransform().apply {
            startView = webViewCardView
            endView = binding.fab
            addTarget(binding.fab)
            duration = 500L
            scrimColor = Color.TRANSPARENT
        }

        TransitionManager.beginDelayedTransition(binding.root, transform)
        webViewCardView?.visibility = View.GONE
        binding.dimBackground.visibility = View.GONE  // 隱藏背景遮罩
        binding.fab.visibility = View.VISIBLE
        isCardMinimized = true
    }

    private fun restoreCardView() {
        expandCardView()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupCardDragAndDrop(cardView: TesterWebViewCardView?) {
        cardView?.setOnTouchListener(object : View.OnTouchListener {
            private var initialY: Float = 0f
            private var downY: Float = 0f

            override fun onTouch(view: View, motionEvent: MotionEvent): Boolean {
                when (motionEvent.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        initialY = motionEvent.rawY
                        downY = motionEvent.rawY
                        view.animate().scaleX(1.05f).scaleY(1.05f).setDuration(150).start()
                        return true
                    }

                    MotionEvent.ACTION_MOVE -> {
                        val deltaY = motionEvent.rawY - initialY
                        if (deltaY > 0) {
                            cardView.apply {
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
                        view.animate().scaleX(1f).scaleY(1f).setDuration(150).start()

                        if (deltaY > 300) {
                            minimizeCardView()
                        } else {
                            cardView.animate().translationY(0f).setDuration(200).start()
                        }
                        return true
                    }

                    else -> return false
                }
            }
        })
    }

    private fun isViewIntersecting(
        fabIconView: View,
        closeIconView: View,
        fabIconOffsetX: Float,
        fabIconOffsetY: Float
    ): Boolean {
        val rect1 = RectF(
            fabIconOffsetX, fabIconOffsetY,
            fabIconOffsetX + fabIconView.width, fabIconOffsetY + fabIconView.height
        )

        val rect2 = RectF(
            closeIconView.x, closeIconView.y,
            closeIconView.x + closeIconView.width, closeIconView.y + closeIconView.height
        )

        return RectF.intersects(rect1, rect2)
    }
}










