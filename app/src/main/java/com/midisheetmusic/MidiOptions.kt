/*
- * Copyright (c) 2007-2011 Madhav Vaidyanathan
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

import android.graphics.Color
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.Serializable

/**
 * Contains the available options for
 * modifying the sheet music and sound.  These options are collected
 * from the SettingsActivity, and are passed to the SheetMusic and
 * MidiPlayer classes.
 */
class MidiOptions : Serializable {

    var showPiano = false

    /** Display the piano  */
    lateinit var tracks: BooleanArray

    /** Which tracks to display (true = display)  */
    lateinit var instruments: IntArray

    /** Which instruments to use per track  */
    @kotlin.jvm.JvmField
    var useDefaultInstruments = false

    /** If true, don't change instruments  */
    @kotlin.jvm.JvmField
    var scrollVert = false

    /** Whether to scroll vertically or horizontally  */
    var largeNoteSize = false

    /** Display large or small note sizes  */
    @kotlin.jvm.JvmField
    var twoStaffs = false

    /** Combine tracks into two staffs ?  */
    @kotlin.jvm.JvmField
    var showNoteLetters = 0

    /** Show the letters (A, A#, etc) next to the notes  */
    @kotlin.jvm.JvmField
    var showLyrics = false

    /** Show the lyrics under each note  */
    @kotlin.jvm.JvmField
    var showMeasures = false

    /** Show the measure numbers for each staff  */
    @kotlin.jvm.JvmField
    var shifttime = 0

    /** Shift note starttimes by the given amount  */
    @kotlin.jvm.JvmField
    var transpose = 0

    /** Shift note key up/down by given amount  */
    @kotlin.jvm.JvmField
    var key = 0

    /** Use the given KeySignature (NoteScale)  */
    @kotlin.jvm.JvmField
    var time: TimeSignature? = null

    /** Use the given time signature (null for default)  */
    @kotlin.jvm.JvmField
    var defaultTime: TimeSignature? = null

    /** The default time signature  */
    @kotlin.jvm.JvmField
    var combineInterval = 0

    /** Combine notes within given time interval (msec)  */
    @kotlin.jvm.JvmField
    var shade1Color = 0

    /** The color to use for shading  */
    @kotlin.jvm.JvmField
    var shade2Color = 0

    /** The color to use for shading the left hand piano  */
    lateinit var mute: BooleanArray

    /** Which tracks to mute (true = mute)  */
    @kotlin.jvm.JvmField
    var tempo = 0

    /** The tempo, in microseconds per quarter note  */
    @kotlin.jvm.JvmField
    var pauseTime = 0

    /** Start the midi music at the given pause time  */
    var playMeasuresInLoop = false

    /** Play the selected measures in a loop  */
    var playMeasuresInLoopStart = 0

    /** Start measure to play in loop  */
    var playMeasuresInLoopEnd = 0

    /** End measure to play in loop  */
    var lastMeasure = 0

    /** The last measure in the song  */
    @kotlin.jvm.JvmField
    var useColors = false
    @kotlin.jvm.JvmField
    var noteColors: IntArray? = null
    var midiShift = 0

    constructor() {}

    /* Initialize the default settings/options for the given MidiFile */
    constructor(midifile: MidiFile) {
        showPiano = true
        val num_tracks = midifile.tracks!!.size
        tracks = BooleanArray(num_tracks)
        mute = BooleanArray(num_tracks)
        for (i in tracks.indices) {
            tracks[i] = true
            mute[i] = false
            if (midifile.tracks!![i].instrumentName == "Percussion") {
                tracks[i] = false
                mute[i] = true
            }
        }
        useDefaultInstruments = true
        instruments = IntArray(num_tracks)
        for (i in instruments.indices) {
            instruments[i] = midifile.tracks!![i].instrument
        }
        scrollVert = false
        largeNoteSize = true
        twoStaffs = tracks.size != 2
        showNoteLetters = NoteNameNone
        showMeasures = false
        showLyrics = true
        shifttime = 0
        transpose = 0
        midiShift = 0
        time = null
        defaultTime = midifile.time
        key = -1
        combineInterval = 40
        shade1Color = Color.rgb(210, 205, 220)
        shade2Color = Color.rgb(150, 200, 220)
        useColors = false
        noteColors = IntArray(12)
        noteColors!![0] = Color.rgb(180, 0, 0)
        noteColors!![1] = Color.rgb(230, 0, 0)
        noteColors!![2] = Color.rgb(220, 128, 0)
        noteColors!![3] = Color.rgb(130, 130, 0)
        noteColors!![4] = Color.rgb(187, 187, 0)
        noteColors!![5] = Color.rgb(0, 100, 0)
        noteColors!![6] = Color.rgb(0, 140, 0)
        noteColors!![7] = Color.rgb(0, 180, 180)
        noteColors!![8] = Color.rgb(0, 0, 120)
        noteColors!![9] = Color.rgb(0, 0, 180)
        noteColors!![10] = Color.rgb(88, 0, 147)
        noteColors!![11] = Color.rgb(129, 0, 215)
        tempo = midifile.time!!.tempo
        pauseTime = 0
        lastMeasure = midifile.EndTime() / midifile.time!!.measure
        playMeasuresInLoop = false
        playMeasuresInLoopStart = 0
        playMeasuresInLoopEnd = lastMeasure
    }

    /* Convert this MidiOptions object into a JSON string. */
    fun toJson(): String? {
        return try {
            val json = JSONObject()
            val jsonTracks = JSONArray()
            for (value in tracks) {
                jsonTracks.put(value)
            }
            val jsonMute = JSONArray()
            for (value in mute) {
                jsonMute.put(value)
            }
            val jsonInstruments = JSONArray()
            for (value in instruments) {
                jsonInstruments.put(value)
            }
            val jsonColors = JSONArray()
            for (value in noteColors!!) {
                jsonColors.put(value)
            }
            if (time != null) {
                val jsonTime = JSONObject()
                jsonTime.put("numerator", time!!.numerator)
                jsonTime.put("denominator", time!!.denominator)
                jsonTime.put("quarter", time!!.quarter)
                jsonTime.put("tempo", time!!.tempo)
                json.put("time", jsonTime)
            }
            json.put("versionCode", 7)
            json.put("tracks", jsonTracks)
            json.put("mute", jsonMute)
            json.put("instruments", jsonInstruments)
            json.put("useDefaultInstruments", useDefaultInstruments)
            json.put("scrollVert", scrollVert)
            json.put("showPiano", showPiano)
            json.put("showLyrics", showLyrics)
            json.put("twoStaffs", twoStaffs)
            json.put("showNoteLetters", showNoteLetters)
            json.put("transpose", transpose)
            json.put("midiShift", midiShift)
            json.put("key", key)
            json.put("combineInterval", combineInterval)
            json.put("shade1Color", shade1Color)
            json.put("shade2Color", shade2Color)
            json.put("useColors", useColors)
            json.put("noteColors", jsonColors)
            json.put("showMeasures", showMeasures)
            json.put("playMeasuresInLoop", playMeasuresInLoop)
            json.put("playMeasuresInLoopStart", playMeasuresInLoopStart)
            json.put("playMeasuresInLoopEnd", playMeasuresInLoopEnd)
            json.toString()
        } catch (e: JSONException) {
            null
        } catch (e: NullPointerException) {
            null
        }
    }

    /* Merge in the saved options to this MidiOptions.*/
    fun merge(saved: MidiOptions) {
        if (saved.tracks.size == tracks.size) {
            System.arraycopy(saved.tracks, 0, tracks, 0, tracks.size)
        }
        if (saved.mute.size == mute.size) {
            System.arraycopy(saved.mute, 0, mute, 0, mute.size)
        }
        if (saved.instruments.size == instruments.size) {
            System.arraycopy(saved.instruments, 0, instruments, 0, instruments.size)
        }
        if (saved.mute.size == mute.size) {
            System.arraycopy(saved.mute, 0, mute, 0, mute.size)
        }
        if (saved.useColors && saved.noteColors != null) {
            noteColors = saved.noteColors
        }
        if (saved.time != null) {
            time = TimeSignature(saved.time!!.numerator, saved.time!!.denominator,
                    saved.time!!.quarter, saved.time!!.tempo)
        }
        useDefaultInstruments = saved.useDefaultInstruments
        scrollVert = saved.scrollVert
        showPiano = saved.showPiano
        showLyrics = saved.showLyrics
        twoStaffs = saved.twoStaffs
        showNoteLetters = saved.showNoteLetters
        transpose = saved.transpose
        midiShift = saved.midiShift
        key = saved.key
        combineInterval = saved.combineInterval
        shade1Color = saved.shade1Color
        shade2Color = saved.shade2Color
        useColors = saved.useColors
        showMeasures = saved.showMeasures
        playMeasuresInLoop = saved.playMeasuresInLoop
        playMeasuresInLoopStart = saved.playMeasuresInLoopStart
        playMeasuresInLoopEnd = saved.playMeasuresInLoopEnd
    }

    override fun toString(): String {
        val result = StringBuilder("MidiOptions: tracks: ")
        for (track in tracks) {
            result.append(track).append(", ")
        }
        result.append(" Instruments: ")
        for (instrument in instruments) {
            result.append(instrument).append(", ")
        }
        result.append(" scrollVert ").append(scrollVert)
        result.append(" twoStaffs ").append(twoStaffs)
        result.append(" transpose").append(transpose)
        result.append(" midiShift").append(midiShift)
        result.append(" key ").append(key)
        result.append(" combine ").append(combineInterval)
        result.append(" tempo ").append(tempo)
        result.append(" pauseTime ").append(pauseTime)
        if (time != null) {
            result.append(" time ").append(time.toString())
        }
        return result.toString()
    }

    fun copy(): MidiOptions {
        val options = MidiOptions()
        options.tracks = BooleanArray(tracks.size)
        System.arraycopy(tracks, 0, options.tracks, 0, tracks.size)
        options.mute = BooleanArray(mute.size)
        System.arraycopy(mute, 0, options.mute, 0, mute.size)
        options.instruments = IntArray(instruments.size)
        System.arraycopy(instruments, 0, options.instruments, 0, instruments.size)
        options.noteColors = IntArray(noteColors!!.size)
        System.arraycopy(noteColors, 0, options.noteColors, 0, noteColors!!.size)
        options.defaultTime = defaultTime
        options.time = time
        options.useDefaultInstruments = useDefaultInstruments
        options.scrollVert = scrollVert
        options.showPiano = showPiano
        options.showLyrics = showLyrics
        options.twoStaffs = twoStaffs
        options.showNoteLetters = showNoteLetters
        options.transpose = transpose
        options.midiShift = midiShift
        options.key = key
        options.combineInterval = combineInterval
        options.shade1Color = shade1Color
        options.shade2Color = shade2Color
        options.useColors = useColors
        options.showMeasures = showMeasures
        options.playMeasuresInLoop = playMeasuresInLoop
        options.playMeasuresInLoopStart = playMeasuresInLoopStart
        options.playMeasuresInLoopEnd = playMeasuresInLoopEnd
        options.lastMeasure = lastMeasure
        options.tempo = tempo
        options.pauseTime = pauseTime
        options.shifttime = shifttime
        options.largeNoteSize = largeNoteSize
        return options
    }

    companion object {
        // The possible values for showNoteLetters
        const val NoteNameNone = 0
        const val NoteNameLetter = 1
        const val NoteNameFixedDoReMi = 2
        const val NoteNameMovableDoReMi = 3
        const val NoteNameFixedNumber = 4
        const val NoteNameMovableNumber = 5

        /* Initialize the options from a json string */
        fun fromJson(jsonString: String?): MidiOptions? {
            if (jsonString == null) {
                return null
            }
            val options = MidiOptions()
            try {
                val json = JSONObject(jsonString)
                val jsonTracks = json.getJSONArray("tracks")
                options.tracks = BooleanArray(jsonTracks.length())
                for (i in options.tracks.indices) {
                    options.tracks[i] = jsonTracks.getBoolean(i)
                }
                val jsonMute = json.getJSONArray("mute")
                options.mute = BooleanArray(jsonMute.length())
                for (i in options.mute.indices) {
                    options.mute[i] = jsonMute.getBoolean(i)
                }
                val jsonInstruments = json.getJSONArray("instruments")
                options.instruments = IntArray(jsonInstruments.length())
                for (i in options.instruments.indices) {
                    options.instruments[i] = jsonInstruments.getInt(i)
                }
                if (json.has("noteColors")) {
                    val jsonColors = json.getJSONArray("noteColors")
                    options.noteColors = IntArray(jsonColors.length())
                    for (i in options.noteColors!!.indices) {
                        options.noteColors!![i] = jsonColors.getInt(i)
                    }
                }
                if (json.has("time")) {
                    val jsonTime = json.getJSONObject("time")
                    val numer = jsonTime.getInt("numerator")
                    val denom = jsonTime.getInt("denominator")
                    val quarter = jsonTime.getInt("quarter")
                    val tempo = jsonTime.getInt("tempo")
                    options.time = TimeSignature(numer, denom, quarter, tempo)
                }
                options.useDefaultInstruments = json.getBoolean("useDefaultInstruments")
                options.scrollVert = json.getBoolean("scrollVert")
                options.showPiano = json.getBoolean("showPiano")
                options.showLyrics = json.getBoolean("showLyrics")
                options.twoStaffs = json.getBoolean("twoStaffs")
                options.showNoteLetters = json.getInt("showNoteLetters")
                options.transpose = json.getInt("transpose")
                options.midiShift = json.getInt("midiShift")
                options.key = json.getInt("key")
                options.combineInterval = json.getInt("combineInterval")
                options.shade1Color = json.getInt("shade1Color")
                options.shade2Color = json.getInt("shade2Color")
                if (json.has("useColors")) {
                    options.useColors = json.getBoolean("useColors")
                }
                options.showMeasures = json.getBoolean("showMeasures")
                options.playMeasuresInLoop = json.getBoolean("playMeasuresInLoop")
                options.playMeasuresInLoopStart = json.getInt("playMeasuresInLoopStart")
                options.playMeasuresInLoopEnd = json.getInt("playMeasuresInLoopEnd")
            } catch (e: Exception) {
                return null
            }
            return options
        }
    }
}