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

import android.app.Activity
import android.app.AlertDialog
import android.app.TabActivity
import android.content.DialogInterface
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.util.Log
import com.midisheetmusic.FileUri.Companion.equalJson
import org.json.JSONArray

/**
 * ChooseSongActivity is a tabbed view for choosing a song to play.
 * There are 3 tabs:
 *
 *  *  All    (AllSongsActivity)    : Display a list of all songs
 *  *  Recent (RecentSongsActivity) : Display of list of recently opened songs
 *  *  Browse (FileBrowserActivity) : Let the user browse the filesystem for songs
 */
class ChooseSongActivity : TabActivity() {
    private val LOG_TAG = ChooseSongActivity::class.java.simpleName
    public override fun onCreate(state: Bundle) {
        globalActivity = this
        super.onCreate(state)
        val allFilesIcon = BitmapFactory.decodeResource(this.resources, R.drawable.allfilesicon)
        val recentFilesIcon = BitmapFactory.decodeResource(this.resources, R.drawable.recentfilesicon)
        val browseFilesIcon = BitmapFactory.decodeResource(this.resources, R.drawable.browsefilesicon)
        val tabHost = tabHost
        tabHost.addTab(tabHost.newTabSpec("All")
                .setIndicator("All", BitmapDrawable(this.resources, allFilesIcon))
                .setContent(Intent(this, AllSongsActivity::class.java)))
        tabHost.addTab(tabHost.newTabSpec("Recent")
                .setIndicator("Recent", BitmapDrawable(this.resources, recentFilesIcon))
                .setContent(Intent(this, RecentSongsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)))
        tabHost.addTab(tabHost.newTabSpec("Browse")
                .setIndicator("Browse", BitmapDrawable(this.resources, browseFilesIcon))
                .setContent(Intent(this, FileBrowserActivity::class.java)))
    }

    fun doOpenFile(file: FileUri) {
        val data = file.getData(this)
        if (data == null || data.size <= 6 || !MidiFile.hasMidiHeader(data)) {
            showErrorDialog("Error: Unable to open song: $file", this)
            return
        }
        updateRecentFile(file)
        val intent = Intent(Intent.ACTION_VIEW, file.uri, this, SheetMusicActivity::class.java)
        intent.putExtra(SheetMusicActivity.MidiTitleID, file.toString())
        startActivity(intent)
    }

    /** Save the given FileUri into the "recentFiles" preferences.
     * Save a maximum of 10 recent files.
     */
    fun updateRecentFile(recentfile: FileUri) {
        try {
            val settings = getSharedPreferences("midisheetmusic.recentFiles", 0)
            val editor = settings.edit()
            val prevRecentFiles: JSONArray
            val recentFilesString = settings.getString("recentFiles", null)
            prevRecentFiles = recentFilesString?.let { JSONArray(it) } ?: JSONArray()
            val recentFiles = JSONArray()
            val recentFileJson = recentfile.toJson()
            recentFiles.put(recentFileJson)
            for (i in 0 until prevRecentFiles.length()) {
                if (i >= 10) {
                    break // only store 10 most recent files
                }
                val file = prevRecentFiles.getJSONObject(i)
                if (!equalJson(recentFileJson!!, file)) {
                    recentFiles.put(file)
                }
            }
            editor.putString("recentFiles", recentFiles.toString())
            editor.apply()
        } catch (e: Exception) {
            Log.e(LOG_TAG, Thread.currentThread().stackTrace[2].methodName, e)
        }
    }

    companion object {
        var globalActivity: ChooseSongActivity? = null
        fun openFile(file: FileUri) {
            globalActivity!!.doOpenFile(file)
        }

        /** Show an error dialog with the given message  */
        fun showErrorDialog(message: String?, activity: Activity?) {
            val builder = AlertDialog.Builder(activity)
            builder.setMessage(message)
            builder.setCancelable(false)
            builder.setPositiveButton("OK") { dialog: DialogInterface?, id: Int -> }
            val alert = builder.create()
            alert.show()
        }
    }
}