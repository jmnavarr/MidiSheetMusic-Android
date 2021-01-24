/*
 * Copyright (c) 2011-2013 Madhav Vaidyanathan
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

import android.R
import android.app.ListActivity
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ListView
import org.json.JSONArray
import java.util.*

/** @class RecentSongsActivity
 * The RecentSongsActivity class displays a list of songs
 * that were recently accessed.  The list comes from the
 * SharedPreferences ????
 */
class RecentSongsActivity : ListActivity() {
    private var filelist /* List of recent files opened */: ArrayList<FileUri>? = null
    public override fun onCreate(state: Bundle) {
        super.onCreate(state)
        title = "MidiSheetMusic: Recent Songs"
        listView.setBackgroundColor(Color.rgb(0, 0, 0))
        // Load the list of songs
        loadFileList()
        val adapter = IconArrayAdapter(this, R.layout.simple_list_item_1, filelist)
        this.listAdapter = adapter
    }

    private fun loadFileList() {
        filelist = ArrayList()
        val settings = getSharedPreferences("midisheetmusic.recentFiles", 0)
        val recentFilesString = settings.getString("recentFiles", null) ?: return
        try {
            val jsonArray = JSONArray(recentFilesString)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val file = FileUri.fromJson(obj, this)
                if (file != null) {
                    filelist!!.add(file)
                }
            }
        } catch (e: Exception) {
        }
    }

    public override fun onResume() {
        super.onResume()
        loadFileList()
    }

    /** When a user selects a song, open the SheetMusicActivity.  */
    override fun onListItemClick(parent: ListView, view: View, position: Int, id: Long) {
        super.onListItemClick(parent, view, position, id)
        val file = this.listAdapter.getItem(position) as FileUri
        ChooseSongActivity.openFile(file)
    }
}