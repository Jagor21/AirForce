package com.flavo_oculus.air_force

import android.content.Intent
import android.net.Uri
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView

class AirForceWebViewClient(private val activity: MainActivity) : WebChromeClient() {

    override fun onShowFileChooser(
        webView: WebView?,
        filePathCallback: ValueCallback<Array<Uri>>?,
        fileChooserParams: FileChooserParams?
    ): Boolean {
        activity.filePath = filePathCallback
        val contentIntent = Intent(Intent.ACTION_GET_CONTENT)
        contentIntent.apply {
            type = "*/*"
            addCategory(Intent.CATEGORY_OPENABLE)
            activity.getFile.launch(contentIntent)
        }
        return true
    }
}