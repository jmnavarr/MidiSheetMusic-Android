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
package com.midisheetmusic.sheets

import com.midisheetmusic.MidiNote
import java.util.*

/** @class ClefMeasures
 * The ClefMeasures class is used to report what Clef (Treble or Bass) a
 * given measure uses.
 */
class ClefMeasures(notes: ArrayList<MidiNote>,
                   /** The clefs used for each measure (for a single track)  */
                   private val measure: Int) {
    private val clefs: ArrayList<Clef>

    /** Given a time (in pulses), return the clef used for that measure.  */
    fun GetClef(starttime: Int): Clef {

        /* If the time exceeds the last measure, return the last measure */
        return if (starttime / measure >= clefs.size) {
            clefs[clefs.size - 1]
        } else {
            clefs[starttime / measure]
        }
    }

    companion object {
        /** Calculate the best clef to use for the given notes.  If the
         * average note is below Middle C, use a bass clef.  Else, use a treble
         * clef.
         */
        private fun MainClef(notes: ArrayList<MidiNote>): Clef {
            val middleC = WhiteNote.MiddleC.number
            var total = 0
            for (m in notes) {
                total += m.number
            }
            return if (notes.size == 0) {
                Clef.Treble
            } else if (total / notes.size >= middleC) {
                Clef.Treble
            } else {
                Clef.Bass
            }
        }
    }
    /** The length of a measure, in pulses  */
    /** Given the notes in a track, calculate the appropriate Clef to use
     * for each measure.  Store the result in the clefs list.
     * @param notes  The midi notes
     * @param measurelen The length of a measure, in pulses
     */
    init {
        val mainclef = MainClef(notes)
        var nextmeasure = measure
        var pos = 0
        var clef = mainclef
        clefs = ArrayList()
        while (pos < notes.size) {
            /* Sum all the notes in the current measure */
            var sumnotes = 0
            var notecount = 0
            while (pos < notes.size && notes[pos].startTime < nextmeasure) {
                sumnotes += notes[pos].number
                notecount++
                pos++
            }
            if (notecount == 0) notecount = 1

            /* Calculate the "average" note in the measure */
            val avgnote = sumnotes / notecount
            if (avgnote == 0) {
                /* This measure doesn't contain any notes.
                 * Keep the previous clef.
                 */
            } else if (avgnote >= WhiteNote.BottomTreble.number) {
                clef = Clef.Treble
            } else if (avgnote <= WhiteNote.TopBass.number) {
                clef = Clef.Bass
            } else {
                /* The average note is between G3 and F4. We can use either
                 * the treble or bass clef.  Use the "main" clef, the clef
                 * that appears most for this track.
                 */
                clef = mainclef
            }
            clefs.add(clef)
            nextmeasure += measure
        }
        clefs.add(clef)
    }
}