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

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView

/**
 * The ListAdapter for displaying the list of songs,
 * and for displaying the list of files in a directory.
 *
 *
 * Similar to the array adapter, but adds an icon
 * to the left side of each item displayed.
 * Midi files show a NotePair icon.
 */
class IconArrayAdapter<T>(context: Context, resourceId: Int, objects: List<T>?) : ArrayAdapter<T>(context, resourceId, objects) {
    private val inflater: LayoutInflater

    /** Load the NotePair image into memory.  */
    fun LoadImages(context: Context) {
        if (midiIcon == null) {
            val res = context.resources
            midiIcon = BitmapFactory.decodeResource(res, R.drawable.notepair)
            directoryIcon = BitmapFactory.decodeResource(res, R.drawable.directoryicon)
        }
    }

    /** Create a view for displaying a song in the ListView.
     * The view consists of a Note Pair icon on the left-side,
     * and the name of the song.
     */
    override fun getView(position: Int, convertView: View, parent: ViewGroup): View {
        var convertView = convertView
        if (convertView == null) {
            // TODO: Make linter happy (Avoid passing null as the view root)
            convertView = inflater.inflate(R.layout.choose_song_item, null)
        }
        val text = convertView.findViewById<TextView>(R.id.choose_song_name)
        val image = convertView.findViewById<ImageView>(R.id.choose_song_icon)
        text.highlightColor = Color.WHITE
        val file = getItem(position) as FileUri
        if (file != null) {
            if (file.isDirectory) {
                image.setImageBitmap(directoryIcon)
                text.text = file.toString()
            } else {
                image.setImageBitmap(midiIcon)
                text.text = file.toString()
            }
        } else {
            text.setText(R.string.err_file_not_found)
        }
        return convertView
    }

    companion object {
        private var midiIcon /* The midi icon */: Bitmap? = null
        private var directoryIcon /* The directory icon */: Bitmap? = null
    }

    /** Create a new IconArrayAdapter. Load the NotePair image  */
    init {
        LoadImages(context)
        inflater = LayoutInflater.from(context)
    }
}