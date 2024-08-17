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
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.cardview.widget.CardView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.example.testerfloatingwebview.databinding.InAppBrowserTesterActivityBinding
import java.io.ByteArrayOutputStream


class TesterWebViewCardView(
    context: Context,
    private val onClose: () -> Unit
) : CardView(context) {

    private val binding: InAppBrowserTesterActivityBinding =
        InAppBrowserTesterActivityBinding.inflate(LayoutInflater.from(context), this, true)

    init {
        radius = 16f
        cardElevation = 8f
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)

        initWebView()
        initListener()
    }

    fun handleBackPressed() {
        if (binding.webView.canGoBack()) {
            binding.webView.goBack()
        } else {
            onClose()
        }
    }

    @SuppressLint("JavascriptInterface")
    private fun initWebView() {
        binding.apply {
            val newUrl = "https://pokemongolive.com/?hl=zh_Hant"
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
            webView.loadUrl("file:///android_asset/share_example.html")
        }
    }

    private fun initListener() {
        binding.apply {
            ivClose.setOnClickListener {
                onClose()
            }
            ivBack.setOnClickListener {
                if (binding.webView.canGoBack()) {
                    binding.webView.goBack()
                } else {
                    onClose()
                }
            }
        }
    }

    inner class WebAppInterface(private val context: Context) {

        @JavascriptInterface
        fun shareContent(type: String, data: String) {
            when (type) {
                "image" -> {
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
                .override(800, 600)
                .into(object : CustomTarget<Bitmap>() {
                    override fun onResourceReady(
                        resource: Bitmap,
                        transition: Transition<in Bitmap>?
                    ) {
                        val byteArrayOutputStream = ByteArrayOutputStream()
                        resource.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream)
                        val byteArray: ByteArray = byteArrayOutputStream.toByteArray()
                        val base64String = Base64.encodeToString(byteArray, Base64.NO_WRAP)
                        val base64Uri = Uri.parse("data:image/jpeg;base64,$base64String")
                        shareMedia(base64Uri, "image/*")
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {}
                    override fun onLoadFailed(errorDrawable: Drawable?) {
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


