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
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.preference.*

/**
 * This activity is created by the "Settings" menu option.
 * The user can change settings such as:
 *  *  Which tracks to display
 *  *  Which tracks to mute
 *  *  Which instruments to use during playback
 *  *  Whether to scroll horizontally or vertically
 *  *  Whether to display the piano or not
 *  *  Whether to display note letters or not
 *  *  Transpose the notes to another key
 *  *  Change the key signature or time signature displayed
 *  *  Change how notes are combined into chords (the time interval)
 *  *  Change the colors for shading the left/right hands.
 *  *  Whether to display measure numbers
 *  *  Play selected measures in a loop
 *
 *
 * When created, pass an Intent parameter containing MidiOptions.
 * When destroyed, this activity passes the result MidiOptions to the Intent.
 */
class SettingsActivity : AppCompatActivity() {
    private var defaultOptions: MidiOptions? = null

    /** The initial option values  */
    private var options: MidiOptions? = null
    /** The option values  */
    /** Create the Settings activity. Retrieve the initial option values
     * (MidiOptions) from the Intent.
     */
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        options = intent.getSerializableExtra(settingsID) as MidiOptions
        defaultOptions = intent.getSerializableExtra(defaultSettingsID) as MidiOptions

        // Pass options to the fragment
        val settingsFragment: Fragment = SettingsFragment()
        val bundle = Bundle()
        bundle.putSerializable(settingsID, options)
        bundle.putSerializable(defaultSettingsID, defaultOptions)
        settingsFragment.arguments = bundle
        supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, settingsFragment)
                .commit()
        val actionBar = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)
    }

    /** Handle 'Up' button press  */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
        }
        return super.onOptionsItemSelected(item)
    }

    /** When the back button is pressed, update the MidiOptions.
     * Return the updated options as the 'result' of this Activity.
     */
    override fun onBackPressed() {
        // Make sure `options` is updated with the changes
        val settingsFragment = supportFragmentManager
                .findFragmentById(R.id.settings) as SettingsFragment?
        settingsFragment?.updateOptions()
        val intent = Intent()
        intent.putExtra(settingsID, options)
        setResult(RESULT_OK, intent)
        super.onBackPressed()
    }

    class SettingsFragment : PreferenceFragmentCompat(), Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener {
        private var defaultOptions: MidiOptions? = null

        /** The initial option values  */
        private var options: MidiOptions? = null

        /** The option values  */
        private var restoreDefaults: Preference? = null

        /** Restore default settings  */
        private lateinit var selectTracks: Array<SwitchPreferenceCompat?>

        /** Which tracks to display  */
        private lateinit var playTracks: Array<SwitchPreferenceCompat?>

        /** Which tracks to play  */
        private lateinit var selectInstruments: Array<ListPreference?>

        /** Instruments to use per track  */
        private var setAllToPiano: Preference? = null

        /** Set all instruments to piano  */
        private var showLyrics: SwitchPreferenceCompat? = null

        /** Show the lyrics  */
        private var twoStaffs: SwitchPreferenceCompat? = null

        /** Combine tracks into two staffs  */
        private var showNoteLetters: ListPreference? = null

        /** Show the note letters  */
        private var transpose: ListPreference? = null

        /** Transpose notes  */
        private var midiShift: ListPreference? = null

        /** Control MIDI shift  */
        private var key: ListPreference? = null

        /** Key Signature to use  */
        private var time: ListPreference? = null

        /** Time Signature to use  */
        private var combineInterval: ListPreference? = null

        /** Interval (msec) to combine notes  */
        private lateinit var noteColors: Array<ColorPreference?>
        private var useColors: SwitchPreferenceCompat? = null
        private var shade1Color: ColorPreference? = null

        /** Right-hand color  */
        private var shade2Color: ColorPreference? = null

        /** Left-hand color  */
        private var ctx: Context? = null
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String) {
            if (arguments != null) {
                options = arguments!!.getSerializable(settingsID) as MidiOptions
                defaultOptions = arguments!!.getSerializable(defaultSettingsID) as MidiOptions
            }
            ctx = preferenceManager.context
            createView()
        }

        /** Create all the preference widgets in the view  */
        private fun createView() {
            val root = preferenceManager.createPreferenceScreen(context)
            createRestoreDefaultPrefs(root)
            createDisplayTrackPrefs(root)
            createPlayTrackPrefs(root)
            createInstrumentPrefs(root)
            val sheetTitle = PreferenceCategory(context)
            sheetTitle.setTitle(R.string.sheet_prefs_title)
            root.addPreference(sheetTitle)
            createShowLyricsPrefs(root)
            if (options!!.tracks.size != 2) {
                createTwoStaffsPrefs(root)
            }
            createShowLetterPrefs(root)
            createTransposePrefs(root)
            createMidiShiftPrefs(root)
            createKeySignaturePrefs(root)
            createTimeSignaturePrefs(root)
            createCombineIntervalPrefs(root)
            createColorPrefs(root)
            preferenceScreen = root
        }

        /** For each list dialog, we display the value selected in the "summary" text.
         * When a new value is selected from the list dialog, update the summary
         * to the selected entry.
         */
        override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
            val list = preference as ListPreference
            val index = list.findIndexOfValue(newValue as String)
            val entry = list.entries[index]
            preference.setSummary(entry)
            return true
        }

        /** When the 'restore defaults' preference is clicked, restore the default settings  */
        override fun onPreferenceClick(preference: Preference): Boolean {
            if (preference === restoreDefaults) {
                options = defaultOptions!!.copy()
                createView()
            } else if (preference === setAllToPiano) {
                for (i in options!!.instruments.indices) {
                    options!!.instruments[i] = 0
                }
                createView()
            }
            return true
        }

        /** Create the "Select Tracks to Display" checkboxes.  */
        private fun createDisplayTrackPrefs(root: PreferenceScreen) {
            val displayTracksTitle = PreferenceCategory(context)
            displayTracksTitle.setTitle(R.string.select_tracks_to_display)
            root.addPreference(displayTracksTitle)
            selectTracks = arrayOfNulls(options!!.tracks.size)
            for (i in options!!.tracks.indices) {
                selectTracks[i] = SwitchPreferenceCompat(context)
                selectTracks[i]!!.title = "Track $i"
                selectTracks[i]!!.isChecked = options!!.tracks[i]
                root.addPreference(selectTracks[i])
            }
        }

        /** Create the "Select Tracks to Play" checkboxes.  */
        private fun createPlayTrackPrefs(root: PreferenceScreen) {
            val playTracksTitle = PreferenceCategory(context)
            playTracksTitle.setTitle(R.string.select_tracks_to_play)
            root.addPreference(playTracksTitle)
            playTracks = arrayOfNulls(options!!.mute.size)
            for (i in options!!.mute.indices) {
                playTracks[i] = SwitchPreferenceCompat(context)
                playTracks[i]!!.title = "Track $i"
                playTracks[i]!!.isChecked = !options!!.mute[i]
                root.addPreference(playTracks[i])
            }
        }

        /** Create the "Select Instruments For Each Track " lists.
         * The list of possible instruments is in MidiFile.java.
         */
        private fun createInstrumentPrefs(root: PreferenceScreen) {
            val selectInstrTitle = PreferenceCategory(context)
            selectInstrTitle.setTitle(R.string.select_instruments_per_track)
            root.addPreference(selectInstrTitle)
            selectInstruments = arrayOfNulls(options!!.tracks.size)
            for (i in options!!.instruments.indices) {
                selectInstruments[i] = ListPreference(context)
                selectInstruments[i]!!.onPreferenceChangeListener = this
                selectInstruments[i]!!.entries = MidiFile.Instruments
                selectInstruments[i]!!.key = "select_instruments_$i"
                selectInstruments[i]!!.entryValues = MidiFile.Instruments
                selectInstruments[i]!!.title = "Track $i"
                selectInstruments[i]!!.setValueIndex(options!!.instruments[i])
                selectInstruments[i]!!.summary = selectInstruments[i]!!.entry
                root.addPreference(selectInstruments[i])
            }
            setAllToPiano = Preference(context)
            setAllToPiano!!.setTitle(R.string.set_all_to_piano)
            setAllToPiano!!.onPreferenceClickListener = this
            root.addPreference(setAllToPiano)
        }

        /** Create the "Show Lyrics" preference  */
        private fun createShowLyricsPrefs(root: PreferenceScreen) {
            showLyrics = SwitchPreferenceCompat(context)
            showLyrics!!.setTitle(R.string.show_lyrics)
            showLyrics!!.isChecked = options!!.showLyrics
            root.addPreference(showLyrics)
        }

        /** Create the "Show Note Letters" preference  */
        private fun createShowLetterPrefs(root: PreferenceScreen) {
            showNoteLetters = ListPreference(context)
            showNoteLetters!!.onPreferenceChangeListener = this
            showNoteLetters!!.key = "show_note_letters"
            showNoteLetters!!.setTitle(R.string.show_note_letters)
            showNoteLetters!!.setEntries(R.array.show_note_letter_entries)
            showNoteLetters!!.setEntryValues(R.array.show_note_letter_values)
            showNoteLetters!!.setValueIndex(options!!.showNoteLetters)
            showNoteLetters!!.summary = showNoteLetters!!.entry
            //            DialogPreference x = new DialogPreference(context);
            root.addPreference(showNoteLetters)
        }

        /** Create the "Combine to Two Staffs" preference.  */
        private fun createTwoStaffsPrefs(root: PreferenceScreen) {
            twoStaffs = SwitchPreferenceCompat(context)
            if (options!!.tracks.size == 1) {
                twoStaffs!!.setTitle(R.string.split_to_two_staffs)
                twoStaffs!!.setSummary(R.string.split_to_two_staffs_summary)
            } else {
                twoStaffs!!.setTitle(R.string.combine_to_two_staffs)
                twoStaffs!!.setSummary(R.string.combine_to_two_staffs_summary)
            }
            twoStaffs!!.isChecked = options!!.twoStaffs
            root.addPreference(twoStaffs)
        }

        /** Create the "Transpose Notes" preference.
         * The values range from 12, 11, 10, .. -10, -11, -12
         */
        private fun createTransposePrefs(root: PreferenceScreen) {
            val transposeIndex = 12 - options!!.transpose
            transpose = ListPreference(context)
            transpose!!.key = "transpose"
            transpose!!.onPreferenceChangeListener = this
            transpose!!.setTitle(R.string.transpose)
            transpose!!.setEntries(R.array.transpose_entries)
            transpose!!.setEntryValues(R.array.transpose_values)
            transpose!!.setValueIndex(transposeIndex)
            transpose!!.summary = transpose!!.entry
            root.addPreference(transpose)
        }

        /** Create the "Shift MIDI Input" preference.
         * It shifts the input received via MIDI interface with
         * a value in the range 12, 11, 10, .. -10, -11, -12
         */
        private fun createMidiShiftPrefs(root: PreferenceScreen) {
            val midiShiftIndex = 12 - options!!.midiShift
            midiShift = ListPreference(context)
            midiShift!!.key = "midi_shift"
            midiShift!!.onPreferenceChangeListener = this
            midiShift!!.setTitle(R.string.midiShift)
            midiShift!!.setEntries(R.array.transpose_entries)
            midiShift!!.setEntryValues(R.array.transpose_values)
            midiShift!!.setValueIndex(midiShiftIndex)
            midiShift!!.summary = midiShift!!.entry
            root.addPreference(midiShift)
        }

        /** Create the "Key Signature" preference  */
        private fun createKeySignaturePrefs(root: PreferenceScreen) {
            key = ListPreference(context)
            key!!.onPreferenceChangeListener = this
            key!!.key = "key_signature"
            key!!.setTitle(R.string.key_signature)
            key!!.setEntries(R.array.key_signature_entries)
            key!!.setEntryValues(R.array.key_signature_values)
            key!!.setValueIndex(options!!.key + 1)
            key!!.summary = key!!.entry
            root.addPreference(key)
        }

        /** Create the "Time Signature" preference  */
        private fun createTimeSignaturePrefs(root: PreferenceScreen) {
            val values = arrayOf("Default", "3/4", "4/4")
            var selected = 0
            if (options!!.time != null && options!!.time!!.numerator == 3) selected = 1 else if (options!!.time != null && options!!.time!!.numerator == 4) selected = 2
            time = ListPreference(context)
            time!!.key = "time_signature"
            time!!.onPreferenceChangeListener = this
            time!!.setTitle(R.string.time_signature)
            time!!.entries = values
            time!!.entryValues = values
            time!!.setValueIndex(selected)
            time!!.summary = time!!.entry
            root.addPreference(time)
        }

        /** Create the "Combine Notes Within Interval"  preference.
         * Notes within N milliseconds are combined into a single chord,
         * even though their start times may be slightly different.
         */
        private fun createCombineIntervalPrefs(root: PreferenceScreen) {
            val selected = options!!.combineInterval / 20 - 1
            combineInterval = ListPreference(context)
            combineInterval!!.key = "combine_interval"
            combineInterval!!.onPreferenceChangeListener = this
            combineInterval!!.setTitle(R.string.combine_interval)
            combineInterval!!.setEntries(R.array.combine_interval_entries)
            combineInterval!!.setEntryValues(R.array.combine_interval_values)
            combineInterval!!.setValueIndex(selected)
            combineInterval!!.summary = combineInterval!!.entry
            root.addPreference(combineInterval)
        }

        /* Create the "Left-hand color" and "Right-hand color" preferences */
        private fun createColorPrefs(root: PreferenceScreen) {
            val localPreferenceCategory = PreferenceCategory(context)
            localPreferenceCategory.title = "Select Colors"
            root.addPreference(localPreferenceCategory)
            shade1Color = ColorPreference(context!!)
            shade1Color!!.setColor(options!!.shade1Color)
            shade1Color!!.setTitle(R.string.right_hand_color)
            root.addPreference(shade1Color)
            shade2Color = ColorPreference(context!!)
            shade2Color!!.setColor(options!!.shade2Color)
            shade2Color!!.setTitle(R.string.left_hand_color)
            root.addPreference(shade2Color)
            useColors = SwitchPreferenceCompat(context)
            useColors!!.setTitle(R.string.use_note_colors)
            useColors!!.isChecked = options!!.useColors
            useColors!!.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { preference: Preference?, isChecked: Any ->
                for (noteColorPref in noteColors) {
                    noteColorPref!!.isVisible = isChecked as Boolean
                }
                true
            }
            root.addPreference(useColors)
            noteColors = arrayOfNulls(options!!.noteColors!!.size)
            for (i in 0..11) {
                noteColors[i] = ColorPreference(context!!)
                noteColors[i]!!.setColor(options!!.noteColors!![i])
                noteColors[i]!!.title = arrayOf("A", "A#", "B", "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#")[i]
                noteColors[i]!!.isVisible = options!!.useColors
                root.addPreference(noteColors[i])
            }
        }

        /** Create the "Restore Default Settings" preference  */
        private fun createRestoreDefaultPrefs(root: PreferenceScreen) {
            restoreDefaults = Preference(context)
            restoreDefaults!!.setTitle(R.string.restore_defaults)
            restoreDefaults!!.onPreferenceClickListener = this
            root.addPreference(restoreDefaults)
        }

        /** Update the MidiOptions based on the preferences selected.  */
        fun updateOptions() {
            for (i in options!!.tracks.indices) {
                options!!.tracks[i] = selectTracks[i]!!.isChecked
            }
            for (i in options!!.mute.indices) {
                options!!.mute[i] = !playTracks[i]!!.isChecked
            }
            for (i in options!!.noteColors!!.indices) {
                options!!.noteColors!![i] = noteColors[i]!!.getColor()
            }
            for (i in options!!.tracks.indices) {
                val entry = selectInstruments[i]
                options!!.instruments[i] = entry!!.findIndexOfValue(entry.value)
            }
            options!!.showLyrics = showLyrics!!.isChecked
            if (twoStaffs != null) options!!.twoStaffs = twoStaffs!!.isChecked else options!!.twoStaffs = false
            options!!.showNoteLetters = showNoteLetters!!.value.toInt()
            options!!.transpose = transpose!!.value.toInt()
            options!!.midiShift = midiShift!!.value.toInt()
            options!!.key = key!!.value.toInt()
            when (time!!.value) {
                "Default" -> options!!.time = null
                "3/4" -> options!!.time = TimeSignature(3, 4, options!!.defaultTime!!.quarter,
                        options!!.defaultTime!!.tempo)
                "4/4" -> options!!.time = TimeSignature(4, 4, options!!.defaultTime!!.quarter,
                        options!!.defaultTime!!.tempo)
            }
            options!!.combineInterval = combineInterval!!.value.toInt()
            options!!.shade1Color = shade1Color!!.getColor()
            options!!.shade2Color = shade2Color!!.getColor()
            options!!.useColors = useColors!!.isChecked
        }

        override fun onStop() {
            updateOptions()
            super.onStop()
        }

        override fun onPause() {
            updateOptions()
            super.onPause()
        }
    }

    companion object {
        const val settingsID = "settings"
        const val defaultSettingsID = "defaultSettings"
    }
}