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

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.media.MediaScannerConnection
import android.os.Bundle
import android.os.Environment
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.drawerlayout.widget.DrawerLayout
import com.midisheetmusic.drawerItems.ExpandableSwitchDrawerItem
import com.midisheetmusic.sheets.ClefSymbol
import com.mikepenz.materialdrawer.Drawer
import com.mikepenz.materialdrawer.Drawer.OnDrawerItemClickListener
import com.mikepenz.materialdrawer.DrawerBuilder
import com.mikepenz.materialdrawer.holder.StringHolder
import com.mikepenz.materialdrawer.interfaces.OnCheckedChangeListener
import com.mikepenz.materialdrawer.model.DividerDrawerItem
import com.mikepenz.materialdrawer.model.SecondaryDrawerItem
import com.mikepenz.materialdrawer.model.SecondarySwitchDrawerItem
import com.mikepenz.materialdrawer.model.SwitchDrawerItem
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem
import java.io.*
import java.net.URLEncoder
import java.util.zip.CRC32

/**
 * SheetMusicActivity is the main activity. The main components are:
 *
 *  *  MidiPlayer : The buttons and speed bar at the top.
 *  *  Piano : For highlighting the piano notes during playback.
 *  *  SheetMusic : For highlighting the sheet music notes during playback.
 */
class SheetMusicActivity : MidiHandlingActivity() {
    private var player /* The play/stop/rewind toolbar */: MidiPlayer? = null
    private var piano /* The piano at the top */: Piano? = null
    private var sheet /* The sheet music */: SheetMusic? = null
    private var layout /* The layout */: LinearLayout? = null
    private var midifile /* The midi file to play */: MidiFile? = null
    private var options /* The options for sheet music and sound */: MidiOptions? = null
    private var midiCRC /* CRC of the midi bytes */: Long = 0
    private var drawer: Drawer? = null

    /** Create this SheetMusicActivity.
     * The Intent should have two parameters:
     * - data: The uri of the midi file to open.
     * - MidiTitleID: The title of the song (String)
     */
    public override fun onCreate(state: Bundle) {
        super.onCreate(state)

        // Hide the navigation bar before the views are laid out
        hideSystemUI()
        setContentView(R.layout.sheet_music_layout)

        // Keep screen on
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        ClefSymbol.LoadImages(this)
        TimeSigSymbol.LoadImages(this)

        // Parse the MidiFile from the raw bytes
        val uri = this.intent.data
        if (uri == null) {
            finish()
            return
        }
        var title = this.intent.getStringExtra(MidiTitleID)
        if (title == null) {
            title = uri.lastPathSegment
        }
        val file = FileUri(uri, title)
        title = "MidiSheetMusic: $title"
        val data: ByteArray
        try {
            data = file.getData(this)!!
            midifile = MidiFile(data, title)
        } catch (e: MidiFileException) {
            finish()
            return
        }

        // Initialize the settings (MidiOptions).
        // If previous settings have been saved, use those
        options = MidiOptions(midifile as MidiFile)
        val crc = CRC32()
        crc.update(data)
        midiCRC = crc.value
        val settings = getPreferences(0)
        options!!.scrollVert = settings.getBoolean("scrollVert", false)
        options!!.shade1Color = settings.getInt("shade1Color", options!!.shade1Color)
        options!!.shade2Color = settings.getInt("shade2Color", options!!.shade2Color)
        options!!.showPiano = settings.getBoolean("showPiano", true)
        val json = settings.getString("" + midiCRC, null)
        val savedOptions = MidiOptions.fromJson(json)
        if (savedOptions != null) {
            options!!.merge(savedOptions)
        }
        createViews()
    }

    /* Create the MidiPlayer and Piano views */
    fun createViews() {
        layout = findViewById(R.id.sheet_content)
        val scrollVertically = SwitchDrawerItem()
                .withName(R.string.scroll_vertically)
                .withChecked(options!!.scrollVert)
                .withOnCheckedChangeListener(object : OnCheckedChangeListener {
                    override fun onCheckedChanged(drawerItem: IDrawerItem<*>, buttonView: CompoundButton, isChecked: Boolean) {
                        options!!.scrollVert = isChecked
                        createSheetMusic(options)
                    }
                })
        val useColors = SwitchDrawerItem()
                .withName(R.string.use_note_colors)
                .withChecked(options!!.useColors)
                .withOnCheckedChangeListener(object : OnCheckedChangeListener {
                    override fun onCheckedChanged(drawerItem: IDrawerItem<*>, buttonView: CompoundButton, isChecked: Boolean) {
                        options!!.useColors = isChecked
                        createSheetMusic(options)
                    }
                })
        val showMeasures = SecondarySwitchDrawerItem()
                .withName(R.string.show_measures)
                .withLevel(2)
                .withChecked(options!!.showMeasures)
                .withOnCheckedChangeListener(object : OnCheckedChangeListener {
                    override fun onCheckedChanged(drawerItem: IDrawerItem<*>, buttonView: CompoundButton, isChecked: Boolean) {
                        options!!.showMeasures = isChecked
                        createSheetMusic(options)
                    }
                })
        val loopStart = SecondaryDrawerItem()
                .withIdentifier(ID_LOOP_START.toLong())
                .withBadge(Integer.toString(options!!.playMeasuresInLoopStart + 1))
                .withName(R.string.play_measures_in_loop_start)
                .withLevel(2)
        val loopEnd = SecondaryDrawerItem()
                .withIdentifier(ID_LOOP_END.toLong())
                .withBadge(Integer.toString(options!!.playMeasuresInLoopEnd + 1))
                .withName(R.string.play_measures_in_loop_end)
                .withLevel(2)
        val loopSettings = ExpandableSwitchDrawerItem()
                .withIdentifier(ID_LOOP_ENABLE.toLong())
                .withName(R.string.loop_on_measures)
                .withChecked(options!!.playMeasuresInLoop)
                .withOnCheckedChangeListener(object : OnCheckedChangeListener {
                    override fun onCheckedChanged(drawerItem: IDrawerItem<*>, buttonView: CompoundButton, isChecked: Boolean) {
                        options!!.playMeasuresInLoop = isChecked
                    }
                })
                .withSubItems(showMeasures, loopStart, loopEnd)

        // Drawer
        drawer = DrawerBuilder()
                .withActivity(this)
                .withInnerShadow(true)
                .addDrawerItems(
                        scrollVertically,
                        useColors,
                        loopSettings,
                        DividerDrawerItem()
                )
                .inflateMenu(R.menu.sheet_menu)
                .withOnDrawerItemClickListener(object : OnDrawerItemClickListener {
                    override fun onItemClick(view: View?, position: Int, drawerItem: IDrawerItem<*>): Boolean {
                        return drawerItemClickListener(drawerItem)
                    }
                })
                .withDrawerGravity(Gravity.RIGHT)
                .build()

        // Make sure that the view extends over the navigation buttons area
        drawer!!.drawerLayout.fitsSystemWindows = false
        // Lock the drawer so swiping doesn't open it
        drawer!!.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        player = MidiPlayer(this)
        player!!.setDrawer(drawer)
        (layout as LinearLayout).addView(player)
        piano = Piano(this)
        (layout as LinearLayout).addView(piano)
        player!!.SetPiano(piano)
        (layout as LinearLayout).requestLayout()
        player!!.setSheetUpdateRequestListener(object : SheetUpdateRequestListener {
            override fun onSheetUpdateRequest() {
                createSheetMusic(options)
            }
        })
        createSheetMusic(options)
    }

    /** Create the SheetMusic view with the given options  */
    private fun createSheetMusic(options: MidiOptions?) {
        if (sheet != null) {
            layout!!.removeView(sheet)
        }
        piano!!.visibility = if (options!!.showPiano) View.VISIBLE else View.GONE
        sheet = SheetMusic(this)
        sheet!!.init(midifile!!, options)
        sheet!!.setPlayer(player)
        layout!!.addView(sheet)
        piano!!.SetMidiFile(midifile, options, player)
        piano!!.SetShadeColors(options.shade1Color, options.shade2Color)
        player!!.SetMidiFile(midifile!!, options, sheet)
        player!!.updateToolbarButtons()
        layout!!.requestLayout()
        sheet!!.draw()
    }

    /** Always display this activity in landscape mode.  */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
    }

    /** Create a string list of the numbers between listStart and listEnd (inclusive)  */
    private fun makeStringList(listStart: Int, listEnd: Int): Array<String?> {
        val list = arrayOfNulls<String>(listEnd)
        for (i in list.indices) {
            list[i] = Integer.toString(i + listStart)
        }
        return list
    }

    /** Handle clicks on the drawer menu  */
    fun drawerItemClickListener(item: IDrawerItem<*>): Boolean {
        when (item.identifier.toInt()) {
            R.id.song_settings -> {
                changeSettings()
                drawer!!.closeDrawer()
            }
            R.id.save_images -> {
                showSaveImagesDialog()
                drawer!!.closeDrawer()
            }
            ID_LOOP_START -> {
                // Note that we display the measure numbers starting at 1,
                // but the actual playMeasuresInLoopStart field starts at 0.
                val builder = AlertDialog.Builder(this)
                builder.setTitle(R.string.play_measures_in_loop_start)
                val items = makeStringList(1, options!!.lastMeasure + 1)
                builder.setItems(items) { dialog: DialogInterface?, i: Int ->
                    options!!.playMeasuresInLoopStart = items[i]!!.toInt() - 1
                    // Make sure End is not smaller than Start
                    if (options!!.playMeasuresInLoopStart > options!!.playMeasuresInLoopEnd) {
                        options!!.playMeasuresInLoopEnd = options!!.playMeasuresInLoopStart
                        drawer!!.updateBadge(ID_LOOP_END.toLong(), StringHolder(items[i]))
                    }
                    (item as SecondaryDrawerItem).withBadge(items[i]!!)
                    drawer!!.updateItem(item)
                }
                val alertDialog = builder.create()
                alertDialog.show()
                alertDialog.listView.setSelection(options!!.playMeasuresInLoopStart)
            }
            ID_LOOP_END -> {
                // Note that we display the measure numbers starting at 1,
                // but the actual playMeasuresInLoopEnd field starts at 0.
                val builder = AlertDialog.Builder(this)
                builder.setTitle(R.string.play_measures_in_loop_end)
                val items = makeStringList(1, options!!.lastMeasure + 1)
                builder.setItems(items, DialogInterface.OnClickListener { dialog: DialogInterface?, i: Int ->
                    options!!.playMeasuresInLoopEnd = items.get(i)!!.toInt() - 1
                    // Make sure End is not smaller than Start
                    if (options!!.playMeasuresInLoopStart > options!!.playMeasuresInLoopEnd) {
                        options!!.playMeasuresInLoopStart = options!!.playMeasuresInLoopEnd
                        drawer!!.updateBadge(ID_LOOP_START.toLong(), StringHolder(items.get(i)))
                    }
                    (item as SecondaryDrawerItem).withBadge(items.get(i)!!)
                    drawer!!.updateItem(item)
                })
                val alertDialog = builder.create()
                alertDialog.show()
                alertDialog.getListView().setSelection(options!!.playMeasuresInLoopEnd)
            }
        }
        return true
    }

    /** To change the sheet music options, start the SettingsActivity.
     * Pass the current MidiOptions as a parameter to the Intent.
     * Also pass the 'default' MidiOptions as a parameter to the Intent.
     * When the SettingsActivity has finished, the onActivityResult()
     * method will be called.
     */
    private fun changeSettings() {
        val defaultOptions = MidiOptions(midifile as MidiFile)
        val intent = Intent(this, SettingsActivity::class.java)
        intent.putExtra(SettingsActivity.settingsID, options)
        intent.putExtra(SettingsActivity.defaultSettingsID, defaultOptions)
        startActivityForResult(intent, settingsRequestCode)
    }

    /* Show the "Save As Images" dialog */
    private fun showSaveImagesDialog() {
        val inflator = LayoutInflater.from(this)
        val dialogView = inflator.inflate(R.layout.save_images_dialog, layout, false)
        val filenameView = dialogView.findViewById<EditText>(R.id.save_images_filename)
        filenameView.setText(midifile!!.fileName.replace("_", " "))
        val builder = AlertDialog.Builder(this)
        builder.setTitle(R.string.save_images_str)
        builder.setView(dialogView)
        builder.setPositiveButton("OK"
        ) { builder1: DialogInterface?, whichButton: Int -> saveAsImages(filenameView.text.toString()) }
        builder.setNegativeButton("Cancel"
        ) { builder12: DialogInterface?, whichButton: Int -> }
        val dialog = builder.create()
        dialog.show()
    }

    /* Save the current sheet music as PNG images. */
    private fun saveAsImages(name: String) {
        var filename = name
        try {
            filename = URLEncoder.encode(name, "utf-8")
        } catch (e: UnsupportedEncodingException) {
            Toast.makeText(this, "Error: unsupported encoding in filename", Toast.LENGTH_SHORT).show()
        }
        if (!options!!.scrollVert) {
            options!!.scrollVert = true
            createSheetMusic(options)
        }
        try {
            val numpages = sheet!!.GetTotalPages()
            for (page in 1..numpages) {
                val image = Bitmap.createBitmap(SheetMusic.PageWidth + 40, SheetMusic.PageHeight + 40, Bitmap.Config.ARGB_8888)
                val imageCanvas = Canvas(image)
                sheet!!.DrawPage(imageCanvas, page)
                val path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES + "/MidiSheetMusic")
                val file = File(path, "$filename$page.png")
                path.mkdirs()
                val stream: OutputStream = FileOutputStream(file)
                image.compress(Bitmap.CompressFormat.PNG, 0, stream)
                stream.close()

                // Inform the media scanner about the file
                MediaScannerConnection.scanFile(this, arrayOf(file.toString()), null, null)
            }
        } catch (e: IOException) {
            val builder = AlertDialog.Builder(this)
            builder.setMessage("Error saving image to file " + Environment.DIRECTORY_PICTURES + "/MidiSheetMusic/" + filename + ".png")
            builder.setCancelable(false)
            builder.setPositiveButton("OK") { dialog: DialogInterface?, id: Int -> }
            val alert = builder.create()
            alert.show()
        } catch (e: NullPointerException) {
            val builder = AlertDialog.Builder(this)
            builder.setMessage("Ran out of memory while saving image to file " + Environment.DIRECTORY_PICTURES + "/MidiSheetMusic/" + filename + ".png")
            builder.setCancelable(false)
            builder.setPositiveButton("OK") { dialog: DialogInterface?, id: Int -> }
            val alert = builder.create()
            alert.show()
        }
    }

    /** Show the HTML help screen.  */
    private fun showHelp() {
        val intent = Intent(this, HelpActivity::class.java)
        startActivity(intent)
    }

    /** Save the options in the SharedPreferences  */
    private fun saveOptions() {
        val editor = getPreferences(0).edit()
        editor.putBoolean("scrollVert", options!!.scrollVert)
        editor.putInt("shade1Color", options!!.shade1Color)
        editor.putInt("shade2Color", options!!.shade2Color)
        editor.putBoolean("showPiano", options!!.showPiano)
        for (i in options!!.noteColors!!.indices) {
            editor.putInt("noteColor$i", options!!.noteColors!![i])
        }
        val json = options!!.toJson()
        if (json != null) {
            editor.putString("" + midiCRC, json)
        }
        editor.apply()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        saveOptions()
    }

    /** This is the callback when the SettingsActivity is finished.
     * Get the modified MidiOptions (passed as a parameter in the Intent).
     * Save the MidiOptions.  The key is the CRC checksum of the midi data,
     * and the value is a JSON dump of the MidiOptions.
     * Finally, re-create the SheetMusic View with the new options.
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent) {
        if (requestCode != settingsRequestCode) {
            return
        }
        options = intent.getSerializableExtra(SettingsActivity.settingsID) as MidiOptions

        // Check whether the default instruments have changed.
        for (i in options!!.instruments.indices) {
            if (options!!.instruments[i] !=
                    midifile!!.tracks!![i].instrument) {
                options!!.useDefaultInstruments = false
            }
        }
        saveOptions()

        // Recreate the sheet music with the new options
        createSheetMusic(options)
    }

    /** When this activity resumes, redraw all the views  */
    override fun onResume() {
        super.onResume()
        layout!!.requestLayout()
        player!!.invalidate()
        piano!!.invalidate()
        if (sheet != null) {
            sheet!!.invalidate()
        }
        layout!!.requestLayout()
    }

    /** When this activity pauses, stop the music  */
    override fun onPause() {
        if (player != null) {
            player!!.Pause()
        }
        super.onPause()
    }

    public override fun OnMidiDeviceStatus(connected: Boolean) {
        player!!.OnMidiDeviceStatus(connected)
    }

    public override fun OnMidiNote(note: Int, pressed: Boolean) {
        player!!.OnMidiNote(note, pressed)
    }

    /************************** Hide navigation buttons  */
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemUI()
        }
    }

    private fun hideSystemUI() {
        // Enables sticky immersive mode.
        val decorView = window.decorView
        decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY // Set the content to appear under the system bars so that the
                // content doesn't resize when the system bars hide and show.
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN // Hide the nav bar and status bar
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN)
    }

    companion object {
        const val MidiTitleID = "MidiTitleID"
        const val settingsRequestCode = 1
        const val ID_LOOP_ENABLE = 10
        const val ID_LOOP_START = 11
        const val ID_LOOP_END = 12
    }
}