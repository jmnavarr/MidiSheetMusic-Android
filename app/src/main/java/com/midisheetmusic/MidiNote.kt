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

/** @class MidiNote
 * A MidiNote contains
 *
 * starttime - The time (measured in pulses) when the note is pressed.
 * channel   - The channel the note is from.  This is used when matching
 * NoteOff events with the corresponding NoteOn event.
 * The channels for the NoteOn and NoteOff events must be
 * the same.
 * notenumber - The note number, from 0 to 127.  Middle C is 60.
 * duration  - The time duration (measured in pulses) after which the
 * note is released.
 *
 * A MidiNote is created when we encounter a NoteOff event.  The duration
 * is initially unknown (set to 0).  When the corresponding NoteOff event
 * is found, the duration is set by the method NoteOff().
 */
class MidiNote(var startTime: Int,
               /** The start time, in pulses  */
               var channel: Int, notenumber: Int, duration: Int) : Comparator<MidiNote?> {

    /** The channel  */
    var number: Int

    /** The note, from 0 to 127. Middle C is 60  */
    var duration: Int
    val endTime: Int
        get() = startTime + duration

    /* A NoteOff event occurs for this note at the given time.
   * Calculate the note duration based on the noteoff event.
   */
    fun NoteOff(endtime: Int) {
        duration = endtime - startTime
    }

    /** Compare two MidiNotes based on their start times.
     * If the start times are equal, compare by their numbers.
     */
    override fun compare(x: MidiNote?, y: MidiNote?): Int {
        return if (x!!.startTime == y!!.startTime) x.number - y.number else x.startTime - y.startTime
    }

    fun Clone(): MidiNote {
        return MidiNote(startTime, channel, number, duration)
    }

    override fun toString(): String {
        val scale = arrayOf("A", "A#", "B", "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#")
        return String.format("MidiNote channel=%1\$s number=%2\$s %3\$s start=%4\$s duration=%5\$s",
                channel, number, scale[(number + 3) % 12], startTime, duration)
    }

    /** The duration, in pulses  */ /* Create a new MidiNote.  This is called when a NoteOn event is
     * encountered in the MidiFile.
     */
    init {
        channel = channel
        number = notenumber
        this.duration = duration
    }

}