package com.example.testerfloatingwebview

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import com.bumptech.glide.request.transition.Transition

import android.net.Uri
import android.util.Base64
import android.view.LayoutInflater
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.PopupWindow
import android.widget.Toast
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.example.testerfloatingwebview.databinding.InAppBrowserTesterActivityBinding
import java.io.ByteArrayOutputStream

class TesterWebViewSaveState(val context: Context, val onClose: () -> Unit) : PopupWindow(context) {

    private val activityBinding: InAppBrowserTesterActivityBinding =
        InAppBrowserTesterActivityBinding.inflate(LayoutInflater.from(context))



    init {
        contentView = activityBinding.root
        width = ViewGroup.LayoutParams.MATCH_PARENT
        height = ViewGroup.LayoutParams.MATCH_PARENT
        isFocusable = false // 允许获取焦点
        initWebView()
        initListener()
    }

    fun handleBackPressed() {
        if (activityBinding.webView.canGoBack()) {
            activityBinding.webView.goBack()
        } else {
            this@TesterWebViewSaveState.onClose()
        }
    }

    @SuppressLint("JavascriptInterface")
    private fun initWebView() {
        activityBinding.apply {
            // 加载 URL
            val newUrl = "https://pokemongolive.com/?hl=zh_Hant"

            newUrl.let { iLastUrl ->
                webView.settings.javaScriptEnabled = true
                webView.settings.cacheMode = WebSettings.LOAD_NO_CACHE
                webView.settings.loadsImagesAutomatically = true
                webView.settings.builtInZoomControls = false
                webView.settings.displayZoomControls = false
                webView.settings.useWideViewPort = true
                webView.settings.loadWithOverviewMode = true
                webView.webChromeClient = WebChromeClient()
                webView.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, url: String) {
                        super.onPageFinished(view, url)
                    }
                }
                webView.addJavascriptInterface(WebAppInterface(context), "AndroidTesterIntent")
                // 加載本地 HTML 文件
                webView.loadUrl("file:///android_asset/share_example.html")
            }
        }
    }

    private fun initListener() {
        activityBinding.apply {
            ivClose.setOnClickListener {
                this@TesterWebViewSaveState.onClose()
            }
            ivBack.setOnClickListener {
                if (webView.canGoBack()) {
                    webView.goBack()
                } else {
                    this@TesterWebViewSaveState.onClose()
                }
            }

        }
    }

    inner class WebAppInterface(private val context: Context) {

        @JavascriptInterface
        fun shareContent(type: String, data: String) {
            when (type) {
                "image" -> {
                    // 使用 Glide 處理單張圖片分享的邏輯，下載並轉換為 Base64
                    convertUrlToBase64AndShare(data)
                }

                "text" -> {
                    shareText(data)
                }

                "multiple" -> {
                    val (uris, text) = parseMultipleData(data)
                    shareMultipleWithText(uris, text)
                }
            }
        }

        private fun convertUrlToBase64AndShare(imageUrl: String) {
            Glide.with(context)
                .asBitmap()
                .load(imageUrl)
                .override(800, 600)  // 限制圖片的最大尺寸，防止 OOM
                .into(object : CustomTarget<Bitmap>() {
                    override fun onResourceReady(
                        resource: Bitmap,
                        transition: Transition<in Bitmap>?
                    ) {
                        // 將 Bitmap 轉換為 Base64 字符串
                        val byteArrayOutputStream = ByteArrayOutputStream()
                        resource.compress(
                            Bitmap.CompressFormat.JPEG,
                            50,
                            byteArrayOutputStream
                        )  // 壓縮圖片質量
                        val byteArray: ByteArray = byteArrayOutputStream.toByteArray()
                        val base64String = Base64.encodeToString(byteArray, Base64.NO_WRAP)

                        // 將 Base64 字符串轉換為 URI
                        val base64Uri = Uri.parse("data:image/jpeg;base64,$base64String")

                        // 分享圖片
                        shareMedia(base64Uri, "image/*")
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {
                        // 處理佔位符清理（如果需要）
                    }

                    override fun onLoadFailed(errorDrawable: Drawable?) {
                        // 處理加載失敗的情況
                        Toast.makeText(context, "圖片加載失敗", Toast.LENGTH_SHORT).show()
                    }
                })
        }

        private fun shareMedia(uri: Uri, mimeType: String) {
            val intent = Intent(Intent.ACTION_SEND).apply {
                putExtra(Intent.EXTRA_STREAM, uri)
                type = mimeType
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share via"))
        }

        private fun shareText(text: String) {
            val intent = Intent(Intent.ACTION_SEND).apply {
                putExtra(Intent.EXTRA_TEXT, text)
                type = "text/plain"
            }
            context.startActivity(Intent.createChooser(intent, "Share via"))
        }

        private fun shareMultipleWithText(uris: List<Uri>, text: String) {
            val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
                putExtra(Intent.EXTRA_TEXT, text)
                type = "image/*"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share via"))
        }

        private fun parseMultipleData(data: String): Pair<List<Uri>, String> {
            val parts = data.split(";")
            val uris = parts[0].split(",").map { Uri.parse(it.trim()) }
            val text = parts.getOrNull(1)?.trim() ?: ""
            return Pair(uris, text)
        }
    }


}

