package com.example.testerfloatingwebview

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.PopupWindow
import com.example.testerfloatingwebview.databinding.InAppBrowserTesterActivityBinding

class TesterWebViewSaveState(val context: Context, val onClose:() -> Unit) : PopupWindow(context) {

    private val activityBinding: InAppBrowserTesterActivityBinding =
        InAppBrowserTesterActivityBinding.inflate(LayoutInflater.from(context))


    init {
        setBackgroundDrawable(null) // 移除默认背景
        contentView = activityBinding.root
        width = ViewGroup.LayoutParams.MATCH_PARENT
        height = ViewGroup.LayoutParams.MATCH_PARENT
        isFocusable = true // 允许获取焦点
        initWebView()
        initListener()
    }

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
                webView.settings.builtInZoomControls = true
                webView.webChromeClient = WebChromeClient()
                webView.loadUrl(iLastUrl)
                webView.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, url: String) {
                        super.onPageFinished(view, url)
                    }
                }
            }
        }
    }

    private fun initListener() {
        activityBinding.apply {
            ivClose.setOnClickListener {
                this@TesterWebViewSaveState.onClose?.let { it1 -> it1() }
            }
            ivBack.setOnClickListener {
                if (webView.canGoBack()) {
                    webView.goBack()
                } else {
                    this@TesterWebViewSaveState.dismiss()
                }
            }

        }
    }
}

