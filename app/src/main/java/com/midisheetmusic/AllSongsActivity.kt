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

import android.app.ListActivity
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ListView
import android.widget.Toast
import com.midisheetmusic.ChooseSongActivity.Companion.openFile
import java.io.File
import java.io.IOException
import java.util.*

/**
 * ScanMidiFiles class is used to scan for midi files
 * on a background thread.
 */
class ScanMidiFiles : AsyncTask<Int?, Int?, ArrayList<FileUri>?>() {
    private var songlist: ArrayList<FileUri>? = null
    private var rootdir: File? = null
    private var activity: AllSongsActivity? = null
    fun setActivity(activity: AllSongsActivity?) {
        this.activity = activity
    }

    override fun onPreExecute() {
        songlist = ArrayList()
        try {
            rootdir = Environment.getExternalStorageDirectory()
            val message = Toast.makeText(activity, "Scanning " + (rootdir as File).getAbsolutePath() + " for MIDI files", Toast.LENGTH_SHORT)
            message.show()
        } catch (e: Exception) {
        }
    }

    override fun doInBackground(vararg params: Int?): ArrayList<FileUri>? {
        if (rootdir == null) {
            return songlist
        }
        try {
            loadMidiFilesFromDirectory(rootdir!!, 1)
        } catch (e: Exception) {
        }
        return songlist
    }

    override fun onProgressUpdate(vararg progress: Int?) {}
    override fun onPostExecute(result: ArrayList<FileUri>?) {
        val act = activity
        activity = null
        act!!.scanDone(songlist)
        val message = Toast.makeText(act, "Found " + songlist!!.size + " MIDI files", Toast.LENGTH_SHORT)
        message.show()
    }

    override fun onCancelled() {
        activity = null
    }

    /* Given a directory, add MIDI files (ending in .mid) to the songlist.
     * If the directory contains subdirectories, call this method recursively.
     */
    @Throws(IOException::class)
    private fun loadMidiFilesFromDirectory(dir: File, depth: Int) {
        if (isCancelled) {
            return
        }
        if (depth > 10) {
            return
        }
        val files = dir.listFiles() ?: return
        for (file in files) {
            if (file == null) {
                continue
            }
            if (isCancelled) {
                return
            }
            if (file.name.endsWith(".mid") || file.name.endsWith(".MID") ||
                    file.name.endsWith(".midi")) {
                val uri = Uri.parse("file://" + file.absolutePath)
                val displayName = uri.lastPathSegment
                val song = FileUri(uri, displayName)
                songlist!!.add(song)
            }
        }
        for (file in files) {
            if (isCancelled) {
                return
            }
            try {
                if (file.isDirectory) {
                    loadMidiFilesFromDirectory(file, depth + 1)
                }
            } catch (e: Exception) {
            }
        }
    }
}

/**
 * AllSongsActivity is used to display a list of
 * songs to choose from.  The list is created from the songs
 * shipped with MidiSheetMusic (in the assets directory), and
 * also by searching for midi files in the internal/external
 * device storage.
 * <br></br><br></br>
 * When a song is chosen, this calls the SheetMusicAcitivty, passing
 * the raw midi byte[] data as a parameter in the Intent.
 */
class AllSongsActivity : ListActivity(), TextWatcher {
    /** The complete list of midi files  */
    var songlist: ArrayList<FileUri>? = null

    /** Textbox to filter the songs by name  */
    var filterText: EditText? = null

    /** Task to scan for midi files  */
    var scanner: ScanMidiFiles? = null
    var adapter: IconArrayAdapter<FileUri>? = null

    /* When this activity changes orientation, save the songlist,
     * so we don't have to re-scan for midi songs.
     */
    override fun onRetainNonConfigurationInstance(): Any {
        return songlist!!
    }

    public override fun onCreate(state: Bundle) {
        super.onCreate(state)
        setContentView(R.layout.choose_song)
        title = "MidiSheetMusic: Choose Song"

        /* If we're restarting from an orientation change,
         * load the saved song list.
         */songlist = lastNonConfigurationInstance as ArrayList<FileUri>
        if (songlist != null) {
            adapter = IconArrayAdapter(this, android.R.layout.simple_list_item_1, songlist)
            this.listAdapter = adapter
        }
    }

    public override fun onResume() {
        super.onResume()
        if (songlist == null || songlist!!.size == 0) {
            songlist = ArrayList()
            loadAssetMidiFiles()
            loadMidiFilesFromProvider(MediaStore.Audio.Media.INTERNAL_CONTENT_URI)
            loadMidiFilesFromProvider(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI)

            // Sort the songlist by name
            if (songlist!!.size > 0) {
                Collections.sort(songlist, songlist!![0])
            }

            // Remove duplicates
            val origlist: ArrayList<FileUri> = (songlist as ArrayList<FileUri>)
            songlist = ArrayList()
            var prevname = ""
            for (file in origlist) {
                if (file.toString() != prevname) {
                    songlist!!.add(file)
                }
                prevname = file.toString()
            }
            adapter = IconArrayAdapter(this, android.R.layout.simple_list_item_1, songlist)
            this.listAdapter = adapter
        }
        filterText = findViewById<View>(R.id.name_filter) as EditText
        filterText!!.addTextChangedListener(this)
        filterText!!.inputType = InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
    }

    /** Scan the SD card for midi songs.  Since this is a lengthy
     * operation, perform the scan in a background thread.
     */
    fun scanForSongs() {
        if (scanner != null) {
            return
        }
        scanner = ScanMidiFiles()
        scanner!!.setActivity(this)
        scanner!!.execute(0)
    }

    fun scanDone(newfiles: ArrayList<FileUri>?) {
        if (songlist == null || newfiles == null) {
            return
        }
        songlist!!.addAll(newfiles)
        // Sort the songlist by name
        Collections.sort(songlist, songlist!![0])

        // Remove duplicates
        val origlist: ArrayList<FileUri> = songlist as ArrayList<FileUri>
        songlist = ArrayList()
        var prevname = ""
        for (file in origlist) {
            if (file.toString() != prevname) {
                songlist!!.add(file)
            }
            prevname = file.toString()
        }
        adapter = IconArrayAdapter(this, android.R.layout.simple_list_item_1, songlist)
        this.listAdapter = adapter
        scanner = null
    }

    /** Load all the sample midi songs from the assets directory into songlist.
     * Look for files ending with ".mid"
     */
    fun loadAssetMidiFiles() {
        try {
            val assets = this.resources.assets
            val files = assets.list("")
            for (path in files) {
                if (path.endsWith(".mid")) {
                    val uri = Uri.parse("file:///android_asset/$path")
                    val file = FileUri(uri, path)
                    songlist!!.add(file)
                }
            }
        } catch (e: IOException) {
        }
    }

    /** Look for midi files (with mime-type audio/midi) in the
     * internal/external storage. Add them to the songlist.
     */
    private fun loadMidiFilesFromProvider(content_uri: Uri) {
        val resolver = contentResolver
        val columns = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.MIME_TYPE
        )
        val selection = MediaStore.Audio.Media.MIME_TYPE + " LIKE '%mid%'"
        val cursor = resolver.query(content_uri, columns, selection, null, null) ?: return
        if (!cursor.moveToFirst()) {
            cursor.close()
            return
        }
        do {
            val idColumn = cursor.getColumnIndex(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)
            val mimeColumn = cursor.getColumnIndex(MediaStore.Audio.Media.MIME_TYPE)
            val id = cursor.getLong(idColumn)
            val title = cursor.getString(titleColumn)
            val mime = cursor.getString(mimeColumn)
            if (mime.endsWith("/midi") || mime.endsWith("/mid")) {
                val uri = Uri.withAppendedPath(content_uri, "" + id)
                val file = FileUri(uri, title)
                songlist!!.add(file)
            }
        } while (cursor.moveToNext())
        cursor.close()
    }

    /** When a song is clicked on, start a SheetMusicActivity.
     * Read the raw byte[] data of the midi file.
     * Pass the raw byte[] data as a parameter in the Intent.
     * Pass the midi file Title as a parameter in the Intent.
     */
    override fun onListItemClick(parent: ListView, view: View, position: Int, id: Long) {
        super.onListItemClick(parent, view, position, id)
        if (scanner != null) {
            scanner!!.cancel(true)
            scanner = null
        }
        val file = this.listAdapter.getItem(position) as FileUri
        openFile(file)
    }

    /** As text is entered in the filter box, filter the list of
     * midi songs to display.
     */
    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
        adapter!!.filter.filter(s)
    }

    override fun afterTextChanged(s: Editable) {}
    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
}