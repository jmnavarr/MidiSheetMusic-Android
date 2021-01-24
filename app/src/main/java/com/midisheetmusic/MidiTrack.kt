/*
 * Copyright (c) 2007-2011 Madhav Vaidyanathan
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

import java.util.*

/** @class MidiTrack
 * The MidiTrack takes as input the raw MidiEvents for the track, and gets:
 * - The list of midi notes in the track.
 * - The first instrument used in the track.
 *
 * For each NoteOn event in the midi file, a new MidiNote is created
 * and added to the track, using the AddNote() method.
 *
 * The NoteOff() method is called when a NoteOff event is encountered,
 * in order to update the duration of the MidiNote.
 */
class MidiTrack {
    private var tracknum: Int

    /** The track number  */
    var notes: ArrayList<MidiNote>
        private set

    /** List of Midi notes  */
    var instrument: Int

    /** Instrument for this track  */
    private var lyrics: ArrayList<MidiEvent>? = null
    /** The lyrics in this track  */
    /** Create an empty MidiTrack.  Used by the Clone method  */
    constructor(tracknum: Int) {
        this.tracknum = tracknum
        notes = ArrayList(20)
        instrument = 0
    }

    /** Create a MidiTrack based on the Midi events.  Extract the NoteOn/NoteOff
     * events to gather the list of MidiNotes.
     */
    constructor(events: ArrayList<MidiEvent>, tracknum: Int) {
        this.tracknum = tracknum
        notes = ArrayList(events.size)
        instrument = 0
        for (mevent in events) {
            if (mevent.EventFlag == MidiFile.EventNoteOn && mevent.Velocity > 0) {
                val note = MidiNote(mevent.StartTime, mevent.Channel.toInt(), mevent.Notenumber.toInt(), 0)
                AddNote(note)
            } else if (mevent.EventFlag == MidiFile.EventNoteOn && mevent.Velocity.toInt() == 0) {
                NoteOff(mevent.Channel.toInt(), mevent.Notenumber.toInt(), mevent.StartTime)
            } else if (mevent.EventFlag == MidiFile.EventNoteOff) {
                NoteOff(mevent.Channel.toInt(), mevent.Notenumber.toInt(), mevent.StartTime)
            } else if (mevent.EventFlag == MidiFile.EventProgramChange) {
                instrument = mevent.Instrument.toInt()
            } else if (mevent.Metaevent == MidiFile.MetaEventLyric) {
                AddLyric(mevent)
                if (lyrics == null) {
                    lyrics = ArrayList()
                }
                lyrics!!.add(mevent)
            }
        }
        if (notes.size > 0 && notes[0].channel == 9) {
            instrument = 128 /* Percussion */
        }
    }

    fun trackNumber(): Int {
        return tracknum
    }

    fun getLyrics(): ArrayList<MidiEvent>? {
        return lyrics
    }

    fun setLyrics(value: ArrayList<MidiEvent>?) {
        lyrics = value
    }

    val instrumentName: String
        get() = if (instrument >= 0 && instrument <= 128) MidiFile.Instruments[instrument] else ""

    /** Add a MidiNote to this track.  This is called for each NoteOn event  */
    fun AddNote(m: MidiNote) {
        notes.add(m)
    }

    /** A NoteOff event occured.  Find the MidiNote of the corresponding
     * NoteOn event, and update the duration of the MidiNote.
     */
    fun NoteOff(channel: Int, notenumber: Int, endtime: Int) {
        for (i in notes.indices.reversed()) {
            val note = notes[i]
            if (note.channel == channel && note.number == notenumber && note.duration == 0) {
                note.NoteOff(endtime)
                return
            }
        }
    }

    /** Add a lyric event to this track  */
    fun AddLyric(mevent: MidiEvent) {
        if (lyrics == null) {
            lyrics = ArrayList()
        }
        lyrics!!.add(mevent)
    }

    /** Return a deep copy clone of this MidiTrack.  */
    fun Clone(): MidiTrack {
        val track = MidiTrack(trackNumber())
        track.instrument = instrument
        for (note in notes) {
            track.notes.add(note.Clone())
        }
        if (lyrics != null) {
            track.lyrics = ArrayList()
            track.lyrics!!.addAll(lyrics!!)
        }
        return track
    }

    override fun toString(): String {
        val result = StringBuilder(
                "Track number=$tracknum instrument=$instrument\n")
        for (n in notes) {
            result.append(n).append("\n")
        }
        result.append("End Track\n")
        return result.toString()
    }
}