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

import com.midisheetmusic.NoteScale
import com.midisheetmusic.NoteScale.ToNumber
import java.util.*

/** @class WhiteNote
 * The WhiteNote class represents a white key note, a non-sharp,
 * non-flat note.  To display midi notes as sheet music, the notes
 * must be converted to white notes and accidentals.
 *
 * White notes consist of a letter (A thru G) and an octave (0 thru 10).
 * The octave changes from G to A.  After G2 comes A3.  Middle-C is C4.
 *
 * The main operations are calculating distances between notes, and comparing notes.
 */
class WhiteNote(letter: Int, octave: Int) : Comparator<WhiteNote?> {
    /* Get the letter */
    val letter /* The letter of the note, A thru G */: Int

    /* Get the octave */
    val octave /* The octave, 0 thru 10. */: Int

    /** Return the distance (in white notes) between this note
     * and note w, i.e.  this - w.  For example, C4 - A4 = 2,
     */
    fun Dist(w: WhiteNote): Int {
        return (octave - w.octave) * 7 + (letter - w.letter)
    }

    /** Return this note plus the given amount (in white notes).
     * The amount may be positive or negative.  For example,
     * A4 + 2 = C4, and C4 + (-2) = A4.
     */
    fun Add(amount: Int): WhiteNote {
        var num = octave * 7 + letter
        num += amount
        if (num < 0) {
            num = 0
        }
        return WhiteNote(num % 7, num / 7)
    }

    /** Return the midi note number corresponding to this white note.
     * The midi note numbers cover all keys, including sharps/flats,
     * so each octave is 12 notes.  Middle C (C4) is 60.  Some example
     * numbers for various notes:
     *
     * A 2 = 33
     * A#2 = 34
     * G 2 = 43
     * G#2 = 44
     * A 3 = 45
     * A 4 = 57
     * A#4 = 58
     * B 4 = 59
     * C 4 = 60
     */
    val number: Int
        get() {
            var offset = 0
            offset = when (letter) {
                A -> NoteScale.A
                B -> NoteScale.B
                C -> NoteScale.C
                D -> NoteScale.D
                E -> NoteScale.E
                F -> NoteScale.F
                G -> NoteScale.G
                else -> 0
            }
            return ToNumber(offset, octave)
        }

    /** Compare the two notes.  Return
     * < 0  if x is less (lower) than y
     * 0  if x is equal to y
     * > 0  if x is greater (higher) than y
     */
    override fun compare(x: WhiteNote?, y: WhiteNote?): Int {
        return x!!.Dist(y!!)
    }

    /** Return the String <letter><octave> for this note. </octave></letter> */
    override fun toString(): String {
        val s = arrayOf("A", "B", "C", "D", "E", "F", "G")
        return s[letter] + octave
    }

    companion object {
        /* The possible note letters */
        const val A = 0
        const val B = 1
        const val C = 2
        const val D = 3
        const val E = 4
        const val F = 5
        const val G = 6

        /* Common white notes used in calculations */
        var TopTreble = WhiteNote(E, 5)
        var BottomTreble = WhiteNote(F, 4)
        var TopBass = WhiteNote(G, 3)
        var BottomBass = WhiteNote(A, 3)
        var MiddleC = WhiteNote(C, 4)

        /** Return the higher note, x or y  */
        fun Max(x: WhiteNote, y: WhiteNote): WhiteNote {
            return if (x.Dist(y) > 0) x else y
        }

        /** Return the lower note, x or y  */
        fun Min(x: WhiteNote, y: WhiteNote): WhiteNote {
            return if (x.Dist(y) < 0) x else y
        }

        /** Return the top note in the staff of the given clef  */
        fun Top(clef: Clef): WhiteNote {
            return if (clef === Clef.Treble) TopTreble else TopBass
        }

        /** Return the bottom note in the staff of the given clef  */
        fun Bottom(clef: Clef): WhiteNote {
            return if (clef === Clef.Treble) BottomTreble else BottomBass
        }
    }

    /** Create a new note with the given letter and octave.  */
    init {
        require(letter >= 0 && letter <= 6)
        this.letter = letter
        this.octave = octave
    }
}