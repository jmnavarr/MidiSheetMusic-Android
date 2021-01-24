/*
 * Copyright (c) 2011-2012 Madhav Vaidyanathan
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License version 2.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 */
package com.midisheetmusic

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.webkit.WebView

/** @class HelpActivity
 * The HelpActivity displays the help.html file in the assets directory.
 */
class HelpActivity : Activity() {
    public override fun onCreate(savedInstanceState: Bundle) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.help)
        val view = findViewById<View>(R.id.help_webview) as WebView
        view.settings.javaScriptEnabled = false
        view.loadUrl("file:///android_asset/help.html")
    }
}