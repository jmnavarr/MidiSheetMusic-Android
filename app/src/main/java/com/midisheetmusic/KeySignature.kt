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

import com.midisheetmusic.NoteScale.FromNumber
import com.midisheetmusic.NoteScale.IsBlackKey
import com.midisheetmusic.sheets.Accid
import com.midisheetmusic.sheets.AccidSymbol
import com.midisheetmusic.sheets.Clef
import com.midisheetmusic.sheets.WhiteNote

/** @class KeySignature
 * The KeySignature class represents a key signature, like G Major
 * or B-flat Major.  For sheet music, we only care about the number
 * of sharps or flats in the key signature, not whether it is major
 * or minor.
 *
 * The main operations of this class are:
 * - Guessing the key signature, given the notes in a song.
 * - Generating the accidental symbols for the key signature.
 * - Determining whether a particular note requires an accidental
 * or not.
 */
class KeySignature {
    private var num_flats: Int

    /** The number of sharps in the key, 0 thru 6  */
    private var num_sharps: Int
    /** The number of flats in the key, 0 thru 6  */
    /** The accidental symbols that denote this key, in a treble clef  */
    private lateinit var treble: Array<AccidSymbol?>

    /** The accidental symbols that denote this key, in a bass clef  */
    private lateinit var bass: Array<AccidSymbol?>

    /** The key map for this key signature:
     * keymap[notenumber] -> Accidental
     */
    private var keymap: Array<Accid?>

    /** The measure used in the previous call to GetAccidental()  */
    private var prevmeasure = 0

    /** Create new key signature, with the given number of
     * sharps and flats.  One of the two must be 0, you can't
     * have both sharps and flats in the key signature.
     */
    constructor(num_sharps: Int, num_flats: Int) {
        require(num_sharps == 0 || num_flats == 0)
        this.num_sharps = num_sharps
        this.num_flats = num_flats
        CreateAccidentalMaps()
        keymap = arrayOfNulls(160)
        ResetKeyMap()
        CreateSymbols()
    }

    /** Create new key signature, with the given notescale.
     */
    constructor(notescale: Int) {
        num_flats = 0
        num_sharps = num_flats
        when (notescale) {
            NoteScale.A -> num_sharps = 3
            NoteScale.Bflat -> num_flats = 2
            NoteScale.B -> num_sharps = 5
            NoteScale.C -> {
            }
            NoteScale.Dflat -> num_flats = 5
            NoteScale.D -> num_sharps = 2
            NoteScale.Eflat -> num_flats = 3
            NoteScale.E -> num_sharps = 4
            NoteScale.F -> num_flats = 1
            NoteScale.Gflat -> num_flats = 6
            NoteScale.G -> num_sharps = 1
            NoteScale.Aflat -> num_flats = 4
            else -> throw IllegalArgumentException()
        }
        CreateAccidentalMaps()
        keymap = arrayOfNulls(160)
        ResetKeyMap()
        CreateSymbols()
    }

    /** The keymap tells what accidental symbol is needed for each
     * note in the scale.  Reset the keymap to the values of the
     * key signature.
     */
    private fun ResetKeyMap() {
        val key: Array<Accid?>
        key = if (num_flats > 0) flatkeys[num_flats]!! else sharpkeys[num_sharps]!!
        for (notenumber in keymap.indices) {
            keymap[notenumber] = key[FromNumber(notenumber)]
        }
    }

    /** Create the Accidental symbols for this key, for
     * the treble and bass clefs.
     */
    private fun CreateSymbols() {
        val count = Math.max(num_sharps, num_flats)
        treble = arrayOfNulls(count)
        bass = arrayOfNulls(count)
        if (count == 0) {
            return
        }
        var treblenotes: Array<WhiteNote>? = null
        var bassnotes: Array<WhiteNote>? = null
        if (num_sharps > 0) {
            treblenotes = arrayOf(
                    WhiteNote(WhiteNote.F, 5),
                    WhiteNote(WhiteNote.C, 5),
                    WhiteNote(WhiteNote.G, 5),
                    WhiteNote(WhiteNote.D, 5),
                    WhiteNote(WhiteNote.A, 6),
                    WhiteNote(WhiteNote.E, 5)
            )
            bassnotes = arrayOf(
                    WhiteNote(WhiteNote.F, 3),
                    WhiteNote(WhiteNote.C, 3),
                    WhiteNote(WhiteNote.G, 3),
                    WhiteNote(WhiteNote.D, 3),
                    WhiteNote(WhiteNote.A, 4),
                    WhiteNote(WhiteNote.E, 3)
            )
        } else if (num_flats > 0) {
            treblenotes = arrayOf(
                    WhiteNote(WhiteNote.B, 5),
                    WhiteNote(WhiteNote.E, 5),
                    WhiteNote(WhiteNote.A, 5),
                    WhiteNote(WhiteNote.D, 5),
                    WhiteNote(WhiteNote.G, 4),
                    WhiteNote(WhiteNote.C, 5)
            )
            bassnotes = arrayOf(
                    WhiteNote(WhiteNote.B, 3),
                    WhiteNote(WhiteNote.E, 3),
                    WhiteNote(WhiteNote.A, 3),
                    WhiteNote(WhiteNote.D, 3),
                    WhiteNote(WhiteNote.G, 2),
                    WhiteNote(WhiteNote.C, 3)
            )
        }
        var a = Accid.None
        a = if (num_sharps > 0) Accid.Sharp else Accid.Flat
        for (i in 0 until count) {
            treble[i] = AccidSymbol(a, treblenotes!![i], Clef.Treble)
            bass[i] = AccidSymbol(a, bassnotes!![i], Clef.Bass)
        }
    }

    /** Return the Accidental symbols for displaying this key signature
     * for the given clef.
     */
    fun GetSymbols(clef: Clef): Array<AccidSymbol?> {
        return if (clef == Clef.Treble) treble else bass
    }

    /** Given a midi note number, return the accidental (if any)
     * that should be used when displaying the note in this key signature.
     *
     * The current measure is also required.  Once we return an
     * accidental for a measure, the accidental remains for the
     * rest of the measure. So we must update the current keymap
     * with any new accidentals that we return.  When we move to another
     * measure, we reset the keymap back to the key signature.
     */
    fun GetAccidental(notenumber: Int, measure: Int): Accid? {
        if (measure != prevmeasure) {
            ResetKeyMap()
            prevmeasure = measure
        }
        if (notenumber <= 1 || notenumber >= 127) {
            return Accid.None
        }
        val result = keymap[notenumber]
        if (result == Accid.Sharp) {
            keymap[notenumber] = Accid.None
            keymap[notenumber - 1] = Accid.Natural
        } else if (result == Accid.Flat) {
            keymap[notenumber] = Accid.None
            keymap[notenumber + 1] = Accid.Natural
        } else if (result == Accid.Natural) {
            keymap[notenumber] = Accid.None
            val nextkey = FromNumber(notenumber + 1)
            val prevkey = FromNumber(notenumber - 1)

            /* If we insert a natural, then either:
             * - the next key must go back to sharp,
             * - the previous key must go back to flat.
             */if (keymap[notenumber - 1] == Accid.None && keymap[notenumber + 1] == Accid.None &&
                    IsBlackKey(nextkey) && IsBlackKey(prevkey)) {
                if (num_flats == 0) {
                    keymap[notenumber + 1] = Accid.Sharp
                } else {
                    keymap[notenumber - 1] = Accid.Flat
                }
            } else if (keymap[notenumber - 1] == Accid.None && IsBlackKey(prevkey)) {
                keymap[notenumber - 1] = Accid.Flat
            } else if (keymap[notenumber + 1] == Accid.None && IsBlackKey(nextkey)) {
                keymap[notenumber + 1] = Accid.Sharp
            } else {
                /* Shouldn't get here */
            }
        }
        return result
    }

    /** Given a midi note number, return the white note (the
     * non-sharp/non-flat note) that should be used when displaying
     * this note in this key signature.  This should be called
     * before calling GetAccidental().
     */
    fun GetWhiteNote(notenumber: Int): WhiteNote {
        val notescale = FromNumber(notenumber)
        var octave = (notenumber + 3) / 12 - 1
        var letter = 0
        val whole_sharps = intArrayOf(
                WhiteNote.A, WhiteNote.A,
                WhiteNote.B,
                WhiteNote.C, WhiteNote.C,
                WhiteNote.D, WhiteNote.D,
                WhiteNote.E,
                WhiteNote.F, WhiteNote.F,
                WhiteNote.G, WhiteNote.G
        )
        val whole_flats = intArrayOf(
                WhiteNote.A,
                WhiteNote.B, WhiteNote.B,
                WhiteNote.C,
                WhiteNote.D, WhiteNote.D,
                WhiteNote.E, WhiteNote.E,
                WhiteNote.F,
                WhiteNote.G, WhiteNote.G,
                WhiteNote.A
        )
        val accid = keymap[notenumber]
        if (accid == Accid.Flat) {
            letter = whole_flats[notescale]
        } else if (accid == Accid.Sharp) {
            letter = whole_sharps[notescale]
        } else if (accid == Accid.Natural) {
            letter = whole_sharps[notescale]
        } else if (accid == Accid.None) {
            letter = whole_sharps[notescale]

            /* If the note number is a sharp/flat, and there's no accidental,
             * determine the white note by seeing whether the previous or next note
             * is a natural.
             */if (IsBlackKey(notescale)) {
                if (keymap[notenumber - 1] == Accid.Natural &&
                        keymap[notenumber + 1] == Accid.Natural) {
                    letter = if (num_flats > 0) {
                        whole_flats[notescale]
                    } else {
                        whole_sharps[notescale]
                    }
                } else if (keymap[notenumber - 1] == Accid.Natural) {
                    letter = whole_sharps[notescale]
                } else if (keymap[notenumber + 1] == Accid.Natural) {
                    letter = whole_flats[notescale]
                }
            }
        }

        /* The above algorithm doesn't quite work for G-flat major.
         * Handle it here.
         */if (num_flats == Gflat && notescale == NoteScale.B) {
            letter = WhiteNote.C
        }
        if (num_flats == Gflat && notescale == NoteScale.Bflat) {
            letter = WhiteNote.B
        }
        if (num_flats > 0 && notescale == NoteScale.Aflat) {
            octave++
        }
        return WhiteNote(letter, octave)
    }

    /** Return true if this key signature is equal to key signature k  */
    fun equals(k: KeySignature): Boolean {
        return if (k.num_sharps == num_sharps && k.num_flats == num_flats) true else false
    }

    /* Return the Major Key of this Key Signature */
    fun Notescale(): Int {
        val flatmajor = intArrayOf(
                NoteScale.C, NoteScale.F, NoteScale.Bflat, NoteScale.Eflat,
                NoteScale.Aflat, NoteScale.Dflat, NoteScale.Gflat, NoteScale.B
        )
        val sharpmajor = intArrayOf(
                NoteScale.C, NoteScale.G, NoteScale.D, NoteScale.A, NoteScale.E,
                NoteScale.B, NoteScale.Fsharp, NoteScale.Csharp, NoteScale.Gsharp,
                NoteScale.Dsharp
        )
        return if (num_flats > 0) flatmajor[num_flats] else sharpmajor[num_sharps]
    }

    /* Return a string representation of this key signature.
     * We only return the major key signature, not the minor one.
     */
    override fun toString(): String {
        return KeyToString(Notescale())
    }

    companion object {
        /** The number of sharps in each key signature  */
        const val C = 0
        const val G = 1
        const val D = 2
        const val A = 3
        const val E = 4
        const val B = 5

        /** The number of flats in each key signature  */
        const val F = 1
        const val Bflat = 2
        const val Eflat = 3
        const val Aflat = 4
        const val Dflat = 5
        const val Gflat = 6

        /** The two arrays below are key maps.  They take a major key
         * (like G major, B-flat major) and a note in the scale, and
         * return the Accidental required to display that note in the
         * given key.  In a nutshel, the map is
         *
         * map[Key][NoteScale] -> Accidental
         */
        private lateinit var sharpkeys: Array<Array<Accid?>?>
        private lateinit var flatkeys: Array<Array<Accid?>?>

        /** Iniitalize the sharpkeys and flatkeys maps  */
        private fun CreateAccidentalMaps() {
            sharpkeys = arrayOfNulls(8)
            flatkeys = arrayOfNulls(8)
            for (i in 0..7) {
                sharpkeys[i] = arrayOfNulls(12)
                flatkeys[i] = arrayOfNulls(12)
            }
            var map: Array<Accid?> = sharpkeys[C]!!
            map[NoteScale.A] = Accid.None
            map[NoteScale.Asharp] = Accid.Flat
            map[NoteScale.B] = Accid.None
            map[NoteScale.C] = Accid.None
            map[NoteScale.Csharp] = Accid.Sharp
            map[NoteScale.D] = Accid.None
            map[NoteScale.Dsharp] = Accid.Sharp
            map[NoteScale.E] = Accid.None
            map[NoteScale.F] = Accid.None
            map[NoteScale.Fsharp] = Accid.Sharp
            map[NoteScale.G] = Accid.None
            map[NoteScale.Gsharp] = Accid.Sharp
            map = sharpkeys[G]!!
            map[NoteScale.A] = Accid.None
            map[NoteScale.Asharp] = Accid.Flat
            map[NoteScale.B] = Accid.None
            map[NoteScale.C] = Accid.None
            map[NoteScale.Csharp] = Accid.Sharp
            map[NoteScale.D] = Accid.None
            map[NoteScale.Dsharp] = Accid.Sharp
            map[NoteScale.E] = Accid.None
            map[NoteScale.F] = Accid.Natural
            map[NoteScale.Fsharp] = Accid.None
            map[NoteScale.G] = Accid.None
            map[NoteScale.Gsharp] = Accid.Sharp
            map = sharpkeys[D]!!
            map[NoteScale.A] = Accid.None
            map[NoteScale.Asharp] = Accid.Flat
            map[NoteScale.B] = Accid.None
            map[NoteScale.C] = Accid.Natural
            map[NoteScale.Csharp] = Accid.None
            map[NoteScale.D] = Accid.None
            map[NoteScale.Dsharp] = Accid.Sharp
            map[NoteScale.E] = Accid.None
            map[NoteScale.F] = Accid.Natural
            map[NoteScale.Fsharp] = Accid.None
            map[NoteScale.G] = Accid.None
            map[NoteScale.Gsharp] = Accid.Sharp
            map = sharpkeys[A]!!
            map[NoteScale.A] = Accid.None
            map[NoteScale.Asharp] = Accid.Flat
            map[NoteScale.B] = Accid.None
            map[NoteScale.C] = Accid.Natural
            map[NoteScale.Csharp] = Accid.None
            map[NoteScale.D] = Accid.None
            map[NoteScale.Dsharp] = Accid.Sharp
            map[NoteScale.E] = Accid.None
            map[NoteScale.F] = Accid.Natural
            map[NoteScale.Fsharp] = Accid.None
            map[NoteScale.G] = Accid.Natural
            map[NoteScale.Gsharp] = Accid.None
            map = sharpkeys[E]!!
            map[NoteScale.A] = Accid.None
            map[NoteScale.Asharp] = Accid.Flat
            map[NoteScale.B] = Accid.None
            map[NoteScale.C] = Accid.Natural
            map[NoteScale.Csharp] = Accid.None
            map[NoteScale.D] = Accid.Natural
            map[NoteScale.Dsharp] = Accid.None
            map[NoteScale.E] = Accid.None
            map[NoteScale.F] = Accid.Natural
            map[NoteScale.Fsharp] = Accid.None
            map[NoteScale.G] = Accid.Natural
            map[NoteScale.Gsharp] = Accid.None
            map = sharpkeys[B]!!
            map[NoteScale.A] = Accid.Natural
            map[NoteScale.Asharp] = Accid.None
            map[NoteScale.B] = Accid.None
            map[NoteScale.C] = Accid.Natural
            map[NoteScale.Csharp] = Accid.None
            map[NoteScale.D] = Accid.Natural
            map[NoteScale.Dsharp] = Accid.None
            map[NoteScale.E] = Accid.None
            map[NoteScale.F] = Accid.Natural
            map[NoteScale.Fsharp] = Accid.None
            map[NoteScale.G] = Accid.Natural
            map[NoteScale.Gsharp] = Accid.None

            /* Flat keys */map = flatkeys[C]!!
            map[NoteScale.A] = Accid.None
            map[NoteScale.Asharp] = Accid.Flat
            map[NoteScale.B] = Accid.None
            map[NoteScale.C] = Accid.None
            map[NoteScale.Csharp] = Accid.Sharp
            map[NoteScale.D] = Accid.None
            map[NoteScale.Dsharp] = Accid.Sharp
            map[NoteScale.E] = Accid.None
            map[NoteScale.F] = Accid.None
            map[NoteScale.Fsharp] = Accid.Sharp
            map[NoteScale.G] = Accid.None
            map[NoteScale.Gsharp] = Accid.Sharp
            map = flatkeys[F]!!
            map[NoteScale.A] = Accid.None
            map[NoteScale.Bflat] = Accid.None
            map[NoteScale.B] = Accid.Natural
            map[NoteScale.C] = Accid.None
            map[NoteScale.Csharp] = Accid.Sharp
            map[NoteScale.D] = Accid.None
            map[NoteScale.Eflat] = Accid.Flat
            map[NoteScale.E] = Accid.None
            map[NoteScale.F] = Accid.None
            map[NoteScale.Fsharp] = Accid.Sharp
            map[NoteScale.G] = Accid.None
            map[NoteScale.Aflat] = Accid.Flat
            map = flatkeys[Bflat]!!
            map[NoteScale.A] = Accid.None
            map[NoteScale.Bflat] = Accid.None
            map[NoteScale.B] = Accid.Natural
            map[NoteScale.C] = Accid.None
            map[NoteScale.Csharp] = Accid.Sharp
            map[NoteScale.D] = Accid.None
            map[NoteScale.Eflat] = Accid.None
            map[NoteScale.E] = Accid.Natural
            map[NoteScale.F] = Accid.None
            map[NoteScale.Fsharp] = Accid.Sharp
            map[NoteScale.G] = Accid.None
            map[NoteScale.Aflat] = Accid.Flat
            map = flatkeys[Eflat]!!
            map[NoteScale.A] = Accid.Natural
            map[NoteScale.Bflat] = Accid.None
            map[NoteScale.B] = Accid.Natural
            map[NoteScale.C] = Accid.None
            map[NoteScale.Dflat] = Accid.Flat
            map[NoteScale.D] = Accid.None
            map[NoteScale.Eflat] = Accid.None
            map[NoteScale.E] = Accid.Natural
            map[NoteScale.F] = Accid.None
            map[NoteScale.Fsharp] = Accid.Sharp
            map[NoteScale.G] = Accid.None
            map[NoteScale.Aflat] = Accid.None
            map = flatkeys[Aflat]!!
            map[NoteScale.A] = Accid.Natural
            map[NoteScale.Bflat] = Accid.None
            map[NoteScale.B] = Accid.Natural
            map[NoteScale.C] = Accid.None
            map[NoteScale.Dflat] = Accid.None
            map[NoteScale.D] = Accid.Natural
            map[NoteScale.Eflat] = Accid.None
            map[NoteScale.E] = Accid.Natural
            map[NoteScale.F] = Accid.None
            map[NoteScale.Fsharp] = Accid.Sharp
            map[NoteScale.G] = Accid.None
            map[NoteScale.Aflat] = Accid.None
            map = flatkeys[Dflat]!!
            map[NoteScale.A] = Accid.Natural
            map[NoteScale.Bflat] = Accid.None
            map[NoteScale.B] = Accid.Natural
            map[NoteScale.C] = Accid.None
            map[NoteScale.Dflat] = Accid.None
            map[NoteScale.D] = Accid.Natural
            map[NoteScale.Eflat] = Accid.None
            map[NoteScale.E] = Accid.Natural
            map[NoteScale.F] = Accid.None
            map[NoteScale.Gflat] = Accid.None
            map[NoteScale.G] = Accid.Natural
            map[NoteScale.Aflat] = Accid.None
            map = flatkeys[Gflat]!!
            map[NoteScale.A] = Accid.Natural
            map[NoteScale.Bflat] = Accid.None
            map[NoteScale.B] = Accid.None
            map[NoteScale.C] = Accid.Natural
            map[NoteScale.Dflat] = Accid.None
            map[NoteScale.D] = Accid.Natural
            map[NoteScale.Eflat] = Accid.None
            map[NoteScale.E] = Accid.Natural
            map[NoteScale.F] = Accid.None
            map[NoteScale.Gflat] = Accid.None
            map[NoteScale.G] = Accid.Natural
            map[NoteScale.Aflat] = Accid.None
        }

        /** Guess the key signature, given the midi note numbers used in
         * the song.
         */
        @JvmStatic
        fun Guess(notes: ListInt): KeySignature {
            CreateAccidentalMaps()

            /* Get the frequency count of each note in the 12-note scale */
            val notecount = IntArray(12)
            for (i in 0 until notes.size()) {
                val notenumber = notes[i]
                val notescale = (notenumber + 3) % 12
                notecount[notescale] += 1
            }

            /* For each key signature, count the total number of accidentals
         * needed to display all the notes.  Choose the key signature
         * with the fewest accidentals.
         */
            var bestkey = 0
            var is_best_sharp = true
            var smallest_accid_count = notes.size()
            var key: Int
            key = 0
            while (key < 6) {
                var accid_count = 0
                for (n in 0..11) {
                    if (sharpkeys[key]!![n] != Accid.None) {
                        accid_count += notecount[n]
                    }
                }
                if (accid_count < smallest_accid_count) {
                    smallest_accid_count = accid_count
                    bestkey = key
                    is_best_sharp = true
                }
                key++
            }
            key = 0
            while (key < 7) {
                var accid_count = 0
                for (n in 0..11) {
                    if (flatkeys[key]!![n] != Accid.None) {
                        accid_count += notecount[n]
                    }
                }
                if (accid_count < smallest_accid_count) {
                    smallest_accid_count = accid_count
                    bestkey = key
                    is_best_sharp = false
                }
                key++
            }
            return if (is_best_sharp) {
                KeySignature(bestkey, 0)
            } else {
                KeySignature(0, bestkey)
            }
        }

        /* Convert a Major Key into a String */
        fun KeyToString(notescale: Int): String {
            return when (notescale) {
                NoteScale.A -> "A major"
                NoteScale.Bflat -> "B-flat major"
                NoteScale.B -> "B major"
                NoteScale.C -> "C major"
                NoteScale.Dflat -> "D-flat major"
                NoteScale.D -> "D major"
                NoteScale.Eflat -> "E-flat major"
                NoteScale.E -> "E major"
                NoteScale.F -> "F major"
                NoteScale.Gflat -> "G-flat major"
                NoteScale.G -> "G major"
                NoteScale.Aflat -> "A-flat major"
                else -> ""
            }
        }
    }
}