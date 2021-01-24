/*
 * Copyright (c) 2007-2012 Madhav Vaidyanathan
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

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.midisheetmusic.*
import com.midisheetmusic.NoteScale.FromNumber
import com.midisheetmusic.SheetMusic.Companion.getTextColor
import java.util.*

/** @class ChordSymbol
 * A chord symbol represents a group of notes that are played at the same
 * time.  A chord includes the notes, the accidental symbols for each
 * note, and the stem (or stems) to use.  A single chord may have two
 * stems if the notes have different durations (e.g. if one note is a
 * quarter note, and another is an eighth note).
 */
class ChordSymbol(midinotes: ArrayList<MidiNote>, key: KeySignature,
                  time: TimeSignature, c: Clef, sheet: SheetMusic?) : MusicSymbol {
    /** Return the clef this chord is drawn in.  */
    val clef: Clef

    /** Which clef the chord is being drawn in  */
    override var startTime = 0
    /** Get the end time (in pulses) of the longest note in the chord.
     * Used to determine whether two adjacent chords can be joined
     * by a stem.
     */
    /** The time (in pulses) the notes occurs at  */
    var endTime: Int

    /** The starttime plus the longest note duration  */
    val notedata: Array<NoteData?>

    /** The notes to draw  */
    private val accidsymbols: Array<AccidSymbol?>

    /** The accidental symbols to draw  */
    override var width = 0

    /** The width of the chord  */
    private var stem1: Stem? = null

    /** The stem of the chord. Can be null.  */
    private var stem2: Stem? = null
    /** Return true if this chord has two stems  */
    /** The second stem of the chord. Can be null  */
    var hasTwoStems: Boolean

    /** True if this chord has two stems  */
    private val sheetmusic: SheetMusic?

    /* Return the stem will the smallest duration.  This property
     * is used when making chord pairs (chords joined by a horizontal
     * beam stem). The stem durations must match in order to make
     * a chord pair.  If a chord has two stems, we always return
     * the one with a smaller duration, because it has a better
     * chance of making a pair.
     */
    val stem: Stem?
        get() = if (stem1 == null) {
            stem2
        } else if (stem2 == null) {
            stem1
        } else if ((stem1 as Stem).duration.ordinal < (stem2 as Stem).duration.ordinal) {
            stem1
        } else {
            stem2
        }

    /* Return the minimum width needed to display this chord.
     *
     * The accidental symbols can be drawn above one another as long
     * as they don't overlap (they must be at least 6 notes apart).
     * If two accidental symbols do overlap, the accidental symbol
     * on top must be shifted to the right.  So the width needed for
     * accidental symbols depends on whether they overlap or not.
     *
     * If we are also displaying the letters, include extra width.
     */
    override val minWidth: Int
    get() {
        /* The width needed for the note circles */
        var result = 2 * SheetMusic.NoteHeight + SheetMusic.NoteHeight * 3 / 4
        if (accidsymbols.size > 0) {
            result += accidsymbols[0]!!.minWidth
            for (i in 1 until accidsymbols.size) {
                val accid = accidsymbols[i]
                val prev = accidsymbols[i - 1]
                if (accid!!.note.Dist(prev!!.note) < 6) {
                    result += accid.minWidth
                }
            }
        }
        if (sheetmusic != null && sheetmusic.showNoteLetters != MidiOptions.NoteNameNone) {
            result += 8
        }
        return result
    }

    /** Get the number of pixels this symbol extends above the staff. Used
     * to determine the minimum height needed for the staff (Staff.FindBounds).
     */
    override val aboveStaff: Int
    get() {
        /* Find the topmost note in the chord */
        var topnote = notedata[notedata.size - 1]!!.whitenote

        /* The stem.End is the note position where the stem ends.
         * Check if the stem end is higher than the top note.
         */if (stem1 != null) topnote = WhiteNote.Max(topnote!!, (stem1 as Stem).end!!)
        if (stem2 != null) topnote = WhiteNote.Max(topnote!!, (stem2 as Stem).end!!)
        val dist = topnote!!.Dist(WhiteNote.Top(clef)) * SheetMusic.NoteHeight / 2
        var result = 0
        if (dist > 0) result = dist

        /* Check if any accidental symbols extend above the staff */for (symbol in accidsymbols) {
            if (symbol!!.aboveStaff > result) {
                result = symbol.aboveStaff
            }
        }
        return result
    }

    /** Get the number of pixels this symbol extends below the staff. Used
     * to determine the minimum height needed for the staff (Staff.FindBounds).
     */
    override val belowStaff: Int
    get() {
        /* Find the bottom note in the chord */
        var bottomnote = notedata[0]!!.whitenote!!

        /* The stem.End is the note position where the stem ends.
         * Check if the stem end is lower than the bottom note.
         */if (stem1 != null) bottomnote = WhiteNote.Min(bottomnote, (stem1 as Stem).end!!)
        if (stem2 != null) bottomnote = WhiteNote.Min(bottomnote, (stem2 as Stem).end!!)
        val dist = WhiteNote.Bottom(clef).Dist(bottomnote) *
                SheetMusic.NoteHeight / 2
        var result = 0
        if (dist > 0) result = dist

        /* Check if any accidental symbols extend below the staff */for (symbol in accidsymbols) {
            if (symbol!!.belowStaff > result) {
                result = symbol.belowStaff
            }
        }
        return result
    }

    /** Get the name for this note  */
    private fun NoteName(notenumber: Int, whitenote: WhiteNote?): String {
        var notenumber = notenumber
        return if (sheetmusic!!.showNoteLetters == MidiOptions.NoteNameLetter) {
            Letter(notenumber, whitenote)
        } else if (sheetmusic.showNoteLetters == MidiOptions.NoteNameFixedDoReMi) {
            val fixedDoReMi = arrayOf(
                    "La", "Li", "Ti", "Do", "Di", "Re", "Ri", "Mi", "Fa", "Fi", "So", "Si"
            )
            val notescale = FromNumber(notenumber)
            fixedDoReMi[notescale]
        } else if (sheetmusic.showNoteLetters == MidiOptions.NoteNameMovableDoReMi) {
            val fixedDoReMi = arrayOf(
                    "La", "Li", "Ti", "Do", "Di", "Re", "Ri", "Mi", "Fa", "Fi", "So", "Si"
            )
            val mainscale = sheetmusic.mainKey!!.Notescale()
            val diff = NoteScale.C - mainscale
            notenumber += diff
            if (notenumber < 0) {
                notenumber += 12
            }
            val notescale = FromNumber(notenumber)
            fixedDoReMi[notescale]
        } else if (sheetmusic.showNoteLetters == MidiOptions.NoteNameFixedNumber) {
            val num = arrayOf(
                    "10", "11", "12", "1", "2", "3", "4", "5", "6", "7", "8", "9"
            )
            val notescale = FromNumber(notenumber)
            num[notescale]
        } else if (sheetmusic.showNoteLetters == MidiOptions.NoteNameMovableNumber) {
            val num = arrayOf(
                    "10", "11", "12", "1", "2", "3", "4", "5", "6", "7", "8", "9"
            )
            val mainscale = sheetmusic.mainKey!!.Notescale()
            val diff = NoteScale.C - mainscale
            notenumber += diff
            if (notenumber < 0) {
                notenumber += 12
            }
            val notescale = FromNumber(notenumber)
            num[notescale]
        } else {
            ""
        }
    }

    /** Get the letter (A, A#, Bb) representing this note  */
    private fun Letter(notenumber: Int, whitenote: WhiteNote?): String {
        val notescale = FromNumber(notenumber)
        return when (notescale) {
            NoteScale.A -> "A"
            NoteScale.B -> "B"
            NoteScale.C -> "C"
            NoteScale.D -> "D"
            NoteScale.E -> "E"
            NoteScale.F -> "F"
            NoteScale.G -> "G"
            NoteScale.Asharp -> if (whitenote!!.letter == WhiteNote.A) "A#" else "Bb"
            NoteScale.Csharp -> if (whitenote!!.letter == WhiteNote.C) "C#" else "Db"
            NoteScale.Dsharp -> if (whitenote!!.letter == WhiteNote.D) "D#" else "Eb"
            NoteScale.Fsharp -> if (whitenote!!.letter == WhiteNote.F) "F#" else "Gb"
            NoteScale.Gsharp -> if (whitenote!!.letter == WhiteNote.G) "G#" else "Ab"
            else -> ""
        }
    }

    /** Draw the Chord Symbol:
     * - Draw the accidental symbols.
     * - Draw the black circle notes.
     * - Draw the stems.
     * @param ytop The ylocation (in pixels) where the top of the staff starts.
     */
    override fun Draw(canvas: Canvas?, paint: Paint?, ytop: Int) {
        paint!!.style = Paint.Style.STROKE

        /* Align the chord to the right */canvas!!.translate((width - minWidth).toFloat(), 0f)

        /* Draw the accidentals. */
        val topstaff = WhiteNote.Top(clef)
        val xpos = DrawAccid(canvas, paint, ytop)

        /* Draw the notes */canvas.translate(xpos.toFloat(), 0f)
        DrawNotes(canvas, paint, ytop, topstaff)
        if (sheetmusic != null && sheetmusic.showNoteLetters != 0) {
            DrawNoteLetters(canvas, paint, ytop, topstaff)
        }

        /* Draw the stems */if (stem1 != null) (stem1 as Stem).Draw(canvas, paint, ytop, topstaff)
        if (stem2 != null) (stem2 as Stem).Draw(canvas, paint, ytop, topstaff)
        canvas.translate(-xpos.toFloat(), 0f)
        canvas.translate(-(width - minWidth).toFloat(), 0f)
    }

    /* Draw the accidental symbols.  If two symbols overlap (if they
     * are less than 6 notes apart), we cannot draw the symbol directly
     * above the previous one.  Instead, we must shift it to the right.
     * @param ytop The ylocation (in pixels) where the top of the staff starts.
     * @return The x pixel width used by all the accidentals.
     */
    fun DrawAccid(canvas: Canvas, paint: Paint?, ytop: Int): Int {
        var xpos = 0
        var prev: AccidSymbol? = null
        for (symbol in accidsymbols) {
            if (prev != null && symbol!!.note.Dist(prev.note) < 6) {
                xpos += symbol.width
            }
            canvas.translate(xpos.toFloat(), 0f)
            symbol!!.Draw(canvas, paint!!, ytop)
            canvas.translate(-xpos.toFloat(), 0f)
            prev = symbol
        }
        if (prev != null) {
            xpos += prev.width
        }
        return xpos
    }

    /** Draw the black circle notes.
     * @param ytop The ylocation (in pixels) where the top of the staff starts.
     * @param topstaff The white note of the top of the staff.
     */
    fun DrawNotes(canvas: Canvas, paint: Paint, ytop: Int, topstaff: WhiteNote) {
        paint.strokeWidth = 1f
        for (note in notedata) {
            /* Get the x,y position to draw the note */
            val ynote = ytop + topstaff.Dist(note!!.whitenote!!) *
                    SheetMusic.NoteHeight / 2
            var xnote = SheetMusic.LineSpace / 4
            if (!note.leftside) xnote += SheetMusic.NoteWidth

            /* Draw rotated ellipse.  You must first translate (0,0)
             * to the center of the ellipse.
             */canvas.translate((xnote + SheetMusic.NoteWidth / 2 + 1).toFloat(), (
                    ynote - SheetMusic.LineWidth + SheetMusic.NoteHeight / 2).toFloat())
            canvas.rotate(-45f)
            if (sheetmusic != null) {
                paint.color = sheetmusic.NoteColor(note.number)
            } else {
                paint.color = Color.BLACK
            }
            if (note.duration === NoteDuration.Whole || note.duration === NoteDuration.Half || note.duration === NoteDuration.DottedHalf) {
                var rect = RectF((-SheetMusic.NoteWidth / 2).toFloat(), (-SheetMusic.NoteHeight / 2).toFloat(),
                        (-SheetMusic.NoteWidth / 2 + SheetMusic.NoteWidth).toFloat(),
                        (-SheetMusic.NoteHeight / 2 + SheetMusic.NoteHeight - 1).toFloat())
                canvas.drawOval(rect, paint)
                rect = RectF((-SheetMusic.NoteWidth / 2).toFloat(), (-SheetMusic.NoteHeight / 2 + 1).toFloat(),
                        (-SheetMusic.NoteWidth / 2 + SheetMusic.NoteWidth).toFloat(),
                        (-SheetMusic.NoteHeight / 2 + 1 + SheetMusic.NoteHeight - 2).toFloat())
                canvas.drawOval(rect, paint)
                rect = RectF((-SheetMusic.NoteWidth / 2).toFloat(), (-SheetMusic.NoteHeight / 2 + 1).toFloat(),
                        (-SheetMusic.NoteWidth / 2 + SheetMusic.NoteWidth).toFloat(),
                        (-SheetMusic.NoteHeight / 2 + 1 + SheetMusic.NoteHeight - 3).toFloat())
                canvas.drawOval(rect, paint)
            } else {
                paint.style = Paint.Style.FILL
                val rect = RectF((-SheetMusic.NoteWidth / 2).toFloat(), (-SheetMusic.NoteHeight / 2).toFloat(),
                        (-SheetMusic.NoteWidth / 2 + SheetMusic.NoteWidth).toFloat(),
                        (-SheetMusic.NoteHeight / 2 + SheetMusic.NoteHeight - 1).toFloat())
                canvas.drawOval(rect, paint)
                paint.style = Paint.Style.STROKE
            }
            paint.color = Color.BLACK
            canvas.rotate(45f)
            canvas.translate(-(xnote + SheetMusic.NoteWidth / 2 + 1).toFloat(), -(ynote - SheetMusic.LineWidth + SheetMusic.NoteHeight / 2).toFloat())

            /* Draw a dot if this is a dotted duration. */if (note.duration === NoteDuration.DottedHalf || note.duration === NoteDuration.DottedQuarter || note.duration === NoteDuration.DottedEighth) {
                val rect = RectF((xnote + SheetMusic.NoteWidth + SheetMusic.LineSpace / 3).toFloat(),
                        (ynote + SheetMusic.LineSpace / 3).toFloat(),
                        (xnote + SheetMusic.NoteWidth + SheetMusic.LineSpace / 3 + 4).toFloat(),
                        (ynote + SheetMusic.LineSpace / 3 + 4).toFloat())
                paint.style = Paint.Style.FILL
                canvas.drawOval(rect, paint)
                paint.style = Paint.Style.STROKE
            }

            /* Draw horizontal lines if note is above/below the staff */
            val top = topstaff.Add(1)
            var dist = note.whitenote!!.Dist(top)
            var y = ytop - SheetMusic.LineWidth
            if (dist >= 2) {
                var i = 2
                while (i <= dist) {
                    y -= SheetMusic.NoteHeight
                    canvas.drawLine((xnote - SheetMusic.LineSpace / 4).toFloat(), y.toFloat(), (
                            xnote + SheetMusic.NoteWidth + SheetMusic.LineSpace / 4).toFloat(),
                            y.toFloat(), paint)
                    i += 2
                }
            }
            val bottom = top.Add(-8)
            y = ytop + (SheetMusic.LineSpace + SheetMusic.LineWidth) * 4 - 1
            dist = bottom.Dist(note.whitenote!!)
            if (dist >= 2) {
                var i = 2
                while (i <= dist) {
                    y += SheetMusic.NoteHeight
                    canvas.drawLine((xnote - SheetMusic.LineSpace / 4).toFloat(), y.toFloat(), (
                            xnote + SheetMusic.NoteWidth + SheetMusic.LineSpace / 4).toFloat(),
                            y.toFloat(), paint)
                    i += 2
                }
            }
            /* End drawing horizontal lines */
        }
    }

    /** Draw the note letters (A, A#, Bb, etc) next to the note circles.
     * @param ytop The y location (in pixels) where the top of the staff starts.
     * @param topstaff The white note of the top of the staff.
     */
    fun DrawNoteLetters(canvas: Canvas, paint: Paint, ytop: Int, topstaff: WhiteNote) {
        val overlap = NotesOverlap(notedata, 0, notedata.size)
        paint.strokeWidth = 1f
        paint.color = getTextColor()
        for (note in notedata) {
            if (!note!!.leftside) {
                // There's not enough pixel room to show the letter
                continue
            }

            // Get the x,y position to draw the note 
            val ynote = ytop + topstaff.Dist(note.whitenote!!) *
                    SheetMusic.NoteHeight / 2

            // Draw the letter to the right side of the note 
            var xnote = SheetMusic.NoteWidth + SheetMusic.NoteWidth / 2
            if (note.duration === NoteDuration.DottedHalf || note.duration === NoteDuration.DottedQuarter || note.duration === NoteDuration.DottedEighth || overlap) {
                xnote += SheetMusic.NoteWidth / 2
            }
            canvas.drawText(NoteName(note.number, note.whitenote),
                    xnote.toFloat(), (
                    ynote + SheetMusic.NoteHeight / 2).toFloat(), paint)
        }
    }

    override fun toString(): String {
        val result = StringBuilder(String.format(
                "ChordSymbol clef=%1\$s start=%2\$s end=%3\$s width=%4\$s hastwostems=%5\$s ",
                clef, startTime, endTime, width, hasTwoStems))
        for (symbol in accidsymbols) {
            result.append(symbol.toString()).append(" ")
        }
        for (note in notedata) {
            result.append(String.format("Note whitenote=%1\$s duration=%2\$s leftside=%3\$s ",
                    note!!.whitenote, note.duration, note.leftside))
        }
        if (stem1 != null) {
            result.append(stem1.toString()).append(" ")
        }
        if (stem2 != null) {
            result.append(stem2.toString()).append(" ")
        }
        return result.toString()
    }

    companion object {
        /** Given the raw midi notes (the note number and duration in pulses),
         * calculate the following note data:
         * - The white key
         * - The accidental (if any)
         * - The note duration (half, quarter, eighth, etc)
         * - The side it should be drawn (left or side)
         * By default, notes are drawn on the left side.  However, if two notes
         * overlap (like A and B) you cannot draw the next note directly above it.
         * Instead you must shift one of the notes to the right.
         *
         * The KeySignature is used to determine the white key and accidental.
         * The TimeSignature is used to determine the duration.
         */
        private fun CreateNoteData(midinotes: ArrayList<MidiNote>, key: KeySignature,
                                   time: TimeSignature): Array<NoteData?> {
            val len = midinotes.size
            val notedata = arrayOfNulls<NoteData>(len)
            for (i in 0 until len) {
                val midi = midinotes[i]
                notedata[i] = NoteData()
                notedata[i]!!.number = midi.number
                notedata[i]!!.leftside = true
                notedata[i]!!.whitenote = key.GetWhiteNote(midi.number)
                notedata[i]!!.duration = time.GetNoteDuration(midi.endTime - midi.startTime)
                notedata[i]!!.accid = key.GetAccidental(midi.number, midi.startTime / time.measure)
                if (i > 0 && notedata[i]!!.whitenote!!.Dist(notedata[i - 1]!!.whitenote!!) == 1) {
                    /* This note (notedata[i]) overlaps with the previous note.
                 * Change the side of this note.
                 */
                    notedata[i]!!.leftside = !notedata[i - 1]!!.leftside
                } else {
                    notedata[i]!!.leftside = true
                }
            }
            return notedata
        }

        /** Given the note data (the white keys and accidentals), create
         * the Accidental Symbols and return them.
         */
        private fun CreateAccidSymbols(notedata: Array<NoteData?>, clef: Clef): Array<AccidSymbol?> {
            var count = 0
            for (n in notedata) {
                if (n!!.accid !== Accid.None) {
                    count++
                }
            }
            val accidsymbols = arrayOfNulls<AccidSymbol>(count)
            var i = 0
            for (n in notedata) {
                if (n!!.accid !== Accid.None) {
                    accidsymbols[i] = AccidSymbol(n!!.accid!!, n.whitenote!!, clef)
                    i++
                }
            }
            return accidsymbols
        }

        /** Calculate the stem direction (Up or down) based on the top and
         * bottom note in the chord.  If the average of the notes is above
         * the middle of the staff, the direction is down.  Else, the
         * direction is up.
         */
        private fun StemDirection(bottom: WhiteNote?, top: WhiteNote?, clef: Clef): Int {
            val middle: WhiteNote
            middle = if (clef == Clef.Treble) WhiteNote(WhiteNote.B, 5) else WhiteNote(WhiteNote.D, 3)
            val dist = middle.Dist(bottom!!) + middle.Dist(top!!)
            return if (dist >= 0) Stem.Up else Stem.Down
        }

        /** Return whether any of the notes in notedata (between start and
         * end indexes) overlap.  This is needed by the Stem class to
         * determine the position of the stem (left or right of notes).
         */
        private fun NotesOverlap(notedata: Array<NoteData?>, start: Int, end: Int): Boolean {
            for (i in start until end) {
                if (!notedata[i]!!.leftside) {
                    return true
                }
            }
            return false
        }

        /** Return true if the chords can be connected, where their stems are
         * joined by a horizontal beam. In order to create the beam:
         *
         * - The chords must be in the same measure.
         * - The chord stems should not be a dotted duration.
         * - The chord stems must be the same duration, with one exception
         * (Dotted Eighth to Sixteenth).
         * - The stems must all point in the same direction (up or down).
         * - The chord cannot already be part of a beam.
         *
         * - 6-chord beams must be 8th notes in 3/4, 6/8, or 6/4 time
         * - 3-chord beams must be either triplets, or 8th notes (12/8 time signature)
         * - 4-chord beams are ok for 2/2, 2/4 or 4/4 time, any duration
         * - 4-chord beams are ok for other times if the duration is 16th
         * - 2-chord beams are ok for any duration
         *
         * If startQuarter is true, the first note should start on a quarter note
         * (only applies to 2-chord beams).
         */
        fun CanCreateBeam(chords: Array<ChordSymbol?>, time: TimeSignature, startQuarter: Boolean): Boolean {
            val numChords = chords.size
            val firstStem = chords[0]!!.stem
            val lastStem = chords[chords.size - 1]!!.stem
            if (firstStem == null || lastStem == null) {
                return false
            }
            val measure = chords[0]!!.startTime / time.measure
            val dur = firstStem.duration
            val dur2 = lastStem.duration
            var dotted8_to_16 = false
            if (chords.size == 2 && dur === NoteDuration.DottedEighth && dur2 === NoteDuration.Sixteenth) {
                dotted8_to_16 = true
            }
            if (dur === NoteDuration.Whole || dur === NoteDuration.Half || dur === NoteDuration.DottedHalf || dur === NoteDuration.Quarter || dur === NoteDuration.DottedQuarter ||
                    dur === NoteDuration.DottedEighth && !dotted8_to_16) {
                return false
            }
            if (numChords == 6) {
                if (dur !== NoteDuration.Eighth) {
                    return false
                }
                val correctTime = time.numerator == 3 && time.denominator == 4 ||
                        time.numerator == 6 && time.denominator == 8 ||
                        time.numerator == 6 && time.denominator == 4
                if (!correctTime) {
                    return false
                }
                if (time.numerator == 6 && time.denominator == 4) {
                    /* first chord must start at 1st or 4th quarter note */
                    val beat = time.quarter * 3
                    if (chords[0]!!.startTime % beat > time.quarter / 6) {
                        return false
                    }
                }
            } else if (numChords == 4) {
                if (time.numerator == 3 && time.denominator == 8) {
                    return false
                }
                val correctTime = time.numerator == 2 || time.numerator == 4 || time.numerator == 8
                if (!correctTime && dur !== NoteDuration.Sixteenth) {
                    return false
                }

                /* chord must start on quarter note */
                var beat = time.quarter
                if (dur === NoteDuration.Eighth) {
                    /* 8th note chord must start on 1st or 3rd quarter beat */
                    beat = time.quarter * 2
                } else if (dur === NoteDuration.ThirtySecond) {
                    /* 32nd note must start on an 8th beat */
                    beat = time.quarter / 2
                }
                if (chords[0]!!.startTime % beat > time.quarter / 6) {
                    return false
                }
            } else if (numChords == 3) {
                val valid = dur === NoteDuration.Triplet ||
                        dur === NoteDuration.Eighth && time.numerator == 12 && time.denominator == 8
                if (!valid) {
                    return false
                }

                /* chord must start on quarter note */
                var beat = time.quarter
                if (time.numerator == 12 && time.denominator == 8) {
                    /* In 12/8 time, chord must start on 3*8th beat */
                    beat = time.quarter / 2 * 3
                }
                if (chords[0]!!.startTime % beat > time.quarter / 6) {
                    return false
                }
            } else if (numChords == 2) {
                if (startQuarter) {
                    val beat = time.quarter
                    if (chords[0]!!.startTime % beat > time.quarter / 6) {
                        return false
                    }
                }
            }
            for (chord in chords) {
                if (chord!!.startTime / time.measure != measure) return false
                if (chord.stem == null) return false
                if (chord.stem!!.duration !== dur && !dotted8_to_16) return false
                if (chord.stem!!.IsBeam()) return false
            }

            /* Check that all stems can point in same direction */
            var hasTwoStems = false
            var direction = Stem.Up
            for (chord in chords) {
                if (chord!!.hasTwoStems) {
                    if (hasTwoStems && chord.stem!!.direction != direction) {
                        return false
                    }
                    hasTwoStems = true
                    direction = chord.stem!!.direction
                }
            }

            /* Get the final stem direction */if (!hasTwoStems) {
                val note1: WhiteNote
                val note2: WhiteNote
                note1 = if (firstStem.direction == Stem.Up) firstStem.top else firstStem.bottom
                note2 = if (lastStem.direction == Stem.Up) lastStem.top else lastStem.bottom
                direction = StemDirection(note1, note2, chords[0]!!.clef)
            }

            /* If the notes are too far apart, don't use a beam */return if (direction == Stem.Up) {
                Math.abs(firstStem.top.Dist(lastStem.top)) < 11
            } else {
                Math.abs(firstStem.bottom.Dist(lastStem.bottom)) < 11
            }
        }

        /** Connect the chords using a horizontal beam.
         *
         * spacing is the horizontal distance (in pixels) between the right side
         * of the first chord, and the right side of the last chord.
         *
         * To make the beam:
         * - Change the stem directions for each chord, so they match.
         * - In the first chord, pass the stem location of the last chord, and
         * the horizontal spacing to that last stem.
         * - Mark all chords (except the first) as "receiver" pairs, so that
         * they don't draw a curvy stem.
         */
        fun CreateBeam(chords: Array<ChordSymbol?>, spacing: Int) {
            val firstStem = chords[0]!!.stem
            val lastStem = chords[chords.size - 1]!!.stem

            /* Calculate the new stem direction */
            var newdirection = -1
            for (chord in chords) {
                if (chord!!.hasTwoStems) {
                    newdirection = chord.stem!!.direction
                    break
                }
            }
            if (newdirection == -1) {
                val note1: WhiteNote
                val note2: WhiteNote
                note1 = if (firstStem!!.direction == Stem.Up) firstStem.top else firstStem.bottom
                note2 = if (lastStem!!.direction == Stem.Up) lastStem.top else lastStem.bottom
                newdirection = StemDirection(note1, note2, chords[0]!!.clef)
            }
            for (chord in chords) {
                chord!!.stem!!.direction = newdirection
            }
            if (chords.size == 2) {
                BringStemsCloser(chords)
            } else {
                LineUpStemEnds(chords)
            }
            firstStem!!.SetPair(lastStem, spacing)
            for (i in 1 until chords.size) {
                chords[i]!!.stem!!.receiver = true
            }
        }

        /** We're connecting the stems of two chords using a horizontal beam.
         * Adjust the vertical endpoint of the stems, so that they're closer
         * together.  For a dotted 8th to 16th beam, increase the stem of the
         * dotted eighth, so that it's as long as a 16th stem.
         */
        fun BringStemsCloser(chords: Array<ChordSymbol?>) {
            val firstStem = chords[0]!!.stem
            val lastStem = chords[1]!!.stem

            /* If we're connecting a dotted 8th to a 16th, increase
         * the stem end of the dotted eighth.
         */if (firstStem!!.duration === NoteDuration.DottedEighth &&
                    lastStem!!.duration === NoteDuration.Sixteenth) {
                if (firstStem!!.direction == Stem.Up) {
                    firstStem.end = firstStem.end!!.Add(2)
                } else {
                    firstStem.end = firstStem.end!!.Add(-2)
                }
            }

            /* Bring the stem ends closer together */
            val distance = Math.abs(firstStem!!.end!!.Dist(lastStem!!.end!!))
            if (firstStem.direction == Stem.Up) {
                if (WhiteNote.Max(firstStem.end!!, lastStem.end!!) === firstStem.end) lastStem.end = lastStem.end!!.Add(distance / 2) else firstStem.end = firstStem.end!!.Add(distance / 2)
            } else {
                if (WhiteNote.Min(firstStem.end!!, lastStem.end!!) === firstStem.end) lastStem.end = lastStem.end!!.Add(-distance / 2) else firstStem.end = firstStem.end!!.Add(-distance / 2)
            }
        }

        /** We're connecting the stems of three or more chords using a horizontal beam.
         * Adjust the vertical endpoint of the stems, so that the middle chord stems
         * are vertically in between the first and last stem.
         */
        fun LineUpStemEnds(chords: Array<ChordSymbol?>) {
            val firstStem = chords[0]!!.stem
            val lastStem = chords[chords.size - 1]!!.stem
            val middleStem = chords[1]!!.stem
            if (firstStem!!.direction == Stem.Up) {
                /* Find the highest stem. The beam will either:
             * - Slant downwards (first stem is highest)
             * - Slant upwards (last stem is highest)
             * - Be straight (middle stem is highest)
             */
                var top = firstStem.end
                for (chord in chords) {
                    top = WhiteNote.Max(top!!, chord!!.stem!!.end!!)
                }
                if (top === firstStem.end && top!!.Dist(lastStem!!.end!!) >= 2) {
                    firstStem.end = top
                    middleStem!!.end = top.Add(-1)
                    lastStem.end = top.Add(-2)
                } else if (top === lastStem!!.end && top!!.Dist(firstStem.end!!) >= 2) {
                    firstStem.end = top!!.Add(-2)
                    middleStem!!.end = top!!.Add(-1)
                    lastStem!!.end = top
                } else {
                    firstStem.end = top
                    middleStem!!.end = top
                    lastStem!!.end = top
                }
            } else {
                /* Find the bottommost stem. The beam will either:
             * - Slant upwards (first stem is lowest)
             * - Slant downwards (last stem is lowest)
             * - Be straight (middle stem is highest)
             */
                var bottom = firstStem.end
                for (chord in chords) {
                    bottom = WhiteNote.Min(bottom!!, chord!!.stem!!.end!!)
                }
                if (bottom === firstStem.end && lastStem!!.end!!.Dist(bottom!!) >= 2) {
                    middleStem!!.end = bottom.Add(1)
                    lastStem.end = bottom.Add(2)
                } else if (bottom === lastStem!!.end && firstStem.end!!.Dist(bottom!!) >= 2) {
                    middleStem!!.end = bottom.Add(1)
                    firstStem.end = bottom.Add(2)
                } else {
                    firstStem.end = bottom
                    middleStem!!.end = bottom
                    lastStem!!.end = bottom
                }
            }

            /* All middle stems have the same end */for (i in 1 until chords.size - 1) {
                val stem = chords[i]!!.stem
                stem!!.end = middleStem.end
            }
        }
    }
    /** Used to get colors and other options  */
    /** Create a new Chord Symbol from the given list of midi notes.
     * All the midi notes will have the same start time.  Use the
     * key signature to get the white key and accidental symbol for
     * each note.  Use the time signature to calculate the duration
     * of the notes. Use the clef when drawing the chord.
     */
    init {
        val len = midinotes.size
        var i: Int
        hasTwoStems = false
        clef = c
        sheetmusic = sheet
        startTime = midinotes[0].startTime
        endTime = midinotes[0].endTime
        i = 0
        while (i < len) {
            if (i > 1) {
                require(midinotes[i].number >= midinotes[i - 1].number)
            }
            endTime = Math.max(endTime, midinotes[i].endTime)
            i++
        }
        notedata = CreateNoteData(midinotes, key, time)
        accidsymbols = CreateAccidSymbols(notedata, clef)


        /* Find out how many stems we need (1 or 2) */
        val dur1 = notedata[0]!!.duration
        var dur2 = dur1
        var change = -1
        i = 0
        while (i < notedata.size) {
            dur2 = notedata[i]!!.duration
            if (dur1 !== dur2) {
                change = i
                break
            }
            i++
        }
        if (dur1 !== dur2) {
            /* We have notes with different durations.  So we will need
             * two stems.  The first stem points down, and contains the
             * bottom note up to the note with the different duration.
             *
             * The second stem points up, and contains the note with the
             * different duration up to the top note.
             */
            hasTwoStems = true
            stem1 = Stem(notedata[0]!!.whitenote!!,
                    notedata[change - 1]!!.whitenote!!,
                    dur1!!,
                    Stem.Down,
                    NotesOverlap(notedata, 0, change)
            )
            stem2 = Stem(notedata[change]!!.whitenote!!,
                    notedata[notedata.size - 1]!!.whitenote!!,
                    dur2!!,
                    Stem.Up,
                    NotesOverlap(notedata, change, notedata.size)
            )
        } else {
            /* All notes have the same duration, so we only need one stem. */
            val direction = StemDirection(notedata[0]!!.whitenote,
                    notedata[notedata.size - 1]!!.whitenote,
                    clef)
            stem1 = Stem(notedata[0]!!.whitenote!!,
                    notedata[notedata.size - 1]!!.whitenote!!,
                    dur1!!,
                    direction,
                    NotesOverlap(notedata, 0, notedata.size)
            )
            stem2 = null
        }

        /* For whole notes, no stem is drawn. */if (dur1 === NoteDuration.Whole) stem1 = null
        if (dur2 === NoteDuration.Whole) stem2 = null
        width = minWidth
    }
}