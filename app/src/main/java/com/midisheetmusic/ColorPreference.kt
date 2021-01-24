/*
 * Copyright (c) 2012 Madhav Vaidyanathan
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

import android.content.Context
import android.view.View
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder

/**
 * Used in a PreferenceScreen to let
 * the user choose a color for an option.
 *
 *
 * This Preference displays text, plus an additional color box
 */
class ColorPreference(private val ctx: Context) : Preference(ctx), ColorChangedListener {
    private var colorview /* The view displaying the selected color */: View? = null
    private var color /* The selected color */ = 0

    
    fun setColor(value: Int) {
        color = value
        if (colorview != null) {
            colorview!!.setBackgroundColor(color)
        }
    }

    fun getColor(): Int {
        return color
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        colorview = holder.findViewById(R.id.color_preference_widget)
        if (color != 0) {
            (colorview as View).setBackgroundColor(color)
        }
    }

    /* When clicked, display the color picker dialog */
    override fun onClick() {
        val dialog = ColorDialog(context, this, color)
        dialog.show()
    }

    /* When the color picker dialog returns, update the color */
    override fun colorChanged(value: Int) {
        color = value
        colorview!!.setBackgroundColor(color)
    }

    init {
        widgetLayoutResource = R.layout.color_preference
    }
}