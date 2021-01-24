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

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import com.midisheetmusic.NoteDuration
import com.midisheetmusic.SheetMusic

/** @class Stem
 * The Stem class is used by ChordSymbol to draw the stem portion of
 * the chord.  The stem has the following fields:
 *
 * duration  - The duration of the stem.
 * direction - Either Up or Down
 * side      - Either left or right
 * top       - The topmost note in the chord
 * bottom    - The bottommost note in the chord
 * end       - The note position where the stem ends.  This is usually
 * six notes past the last note in the chord.  For 8th/16th
 * notes, the stem must extend even more.
 *
 * The SheetMusic class can change the direction of a stem after it
 * has been created.  The side and end fields may also change due to
 * the direction change.  But other fields will not change.
 */
class Stem(
        /** Topmost note in chord  */
        var bottom: WhiteNote,
        /** Up, Down, or None  */
        val top: WhiteNote,
        duration: NoteDuration, direction: Int, overlap: Boolean) {
    /** Get the duration of the stem (Eigth, Sixteenth, ThirtySecond)  */
    val duration: NoteDuration

    /** Duration of the stem.  */
    /** This stem is the receiver of a horizontal
     * beam stem from another chord.  */
    /** Get/Set the direction of the stem (Up or Down)  */
    var direction: Int
    get() {
        return direction
    }
    set(value) {
        ChangeDirection(value)
    }
    /** Get the top note in the chord. This is needed to determine the stem direction  */
    /** Get the bottom note in the chord. This is needed to determine the stem direction  */
    /** Get/Set the location where the stem ends.  This is usually six notes
     * past the last note in the chord. See method CalculateEnd.
     */
    /** Bottommost note in chord  */
    var end: WhiteNote?

    /** Location of end of the stem  */
    private val notesoverlap: Boolean

    /** Do the chord notes overlap  */
    private var side = 0

    /** Left side or right side of note  */
    private var pair: Stem?

    /** If pair != null, this is a horizontal
     * beam stem to another chord  */
    private var width_to_pair: Int
    /** Set this Stem to be the receiver of a horizontal beam, as part
     * of a chord pair.  In Draw(), if this stem is a receiver, we
     * don't draw a curvy stem, we only draw the vertical line.
     */
    /** The width (in pixels) to the chord pair  */
    var receiver: Boolean


    /** Calculate the vertical position (white note key) where
     * the stem ends
     */
    fun CalculateEnd(): WhiteNote? {
        return if (direction == Up) {
            var w = top
            w = w.Add(6)
            if (duration === NoteDuration.Sixteenth) {
                w = w.Add(2)
            } else if (duration === NoteDuration.ThirtySecond) {
                w = w.Add(4)
            }
            w
        } else if (direction == Down) {
            var w = bottom
            w = w.Add(-6)
            if (duration === NoteDuration.Sixteenth) {
                w = w.Add(-2)
            } else if (duration === NoteDuration.ThirtySecond) {
                w = w.Add(-4)
            }
            w
        } else {
            null /* Shouldn't happen */
        }
    }

    /** Change the direction of the stem.  This function is called by
     * ChordSymbol.MakePair().  When two chords are joined by a horizontal
     * beam, their stems must point in the same direction (up or down).
     */
    fun ChangeDirection(newdirection: Int) {
        direction = newdirection
        side = if (direction == Up || notesoverlap) RightSide else LeftSide
        end = CalculateEnd()
    }

    /** Pair this stem with another Chord.  Instead of drawing a curvy tail,
     * this stem will now have to draw a beam to the given stem pair.  The
     * width (in pixels) to this stem pair is passed as argument.
     */
    fun SetPair(pair: Stem?, width_to_pair: Int) {
        this.pair = pair
        this.width_to_pair = width_to_pair
    }

    /** Return true if this Stem is part of a horizontal beam.  */
    fun IsBeam(): Boolean {
        return receiver || pair != null
    }

    /** Draw this stem.
     * @param ytop The y location (in pixels) where the top of the staff starts.
     * @param topstaff  The note at the top of the staff.
     */
    fun Draw(canvas: Canvas, paint: Paint, ytop: Int, topstaff: WhiteNote) {
        if (duration === NoteDuration.Whole) return
        DrawVerticalLine(canvas, paint, ytop, topstaff)
        if (duration === NoteDuration.Quarter || duration === NoteDuration.DottedQuarter || duration === NoteDuration.Half || duration === NoteDuration.DottedHalf ||
                receiver) {
            return
        }
        if (pair != null) DrawHorizBarStem(canvas, paint, ytop, topstaff) else DrawCurvyStem(canvas, paint, ytop, topstaff)
    }

    /** Draw the vertical line of the stem
     * @param ytop The y location (in pixels) where the top of the staff starts.
     * @param topstaff  The note at the top of the staff.
     */
    private fun DrawVerticalLine(canvas: Canvas, paint: Paint, ytop: Int, topstaff: WhiteNote) {
        val xstart: Int
        xstart = if (side == LeftSide) SheetMusic.LineSpace / 4 + 1 else SheetMusic.LineSpace / 4 + SheetMusic.NoteWidth
        if (direction == Up) {
            val y1 = ytop + topstaff.Dist(bottom) * SheetMusic.NoteHeight / 2 + SheetMusic.NoteHeight / 4
            val ystem = ytop + topstaff.Dist(end!!) * SheetMusic.NoteHeight / 2
            canvas.drawLine(xstart.toFloat(), y1.toFloat(), xstart.toFloat(), ystem.toFloat(), paint)
        } else if (direction == Down) {
            var y1 = ytop + topstaff.Dist(top) * SheetMusic.NoteHeight / 2 + SheetMusic.NoteHeight
            y1 = if (side == LeftSide) y1 - SheetMusic.NoteHeight / 4 else y1 - SheetMusic.NoteHeight / 2
            val ystem = ytop + topstaff.Dist(end!!) * SheetMusic.NoteHeight / 2 + SheetMusic.NoteHeight
            canvas.drawLine(xstart.toFloat(), y1.toFloat(), xstart.toFloat(), ystem.toFloat(), paint)
        }
    }

    /** Draw a curvy stem tail.  This is only used for single chords, not chord pairs.
     * @param ytop The y location (in pixels) where the top of the staff starts.
     * @param topstaff  The note at the top of the staff.
     */
    private fun DrawCurvyStem(canvas: Canvas, paint: Paint, ytop: Int, topstaff: WhiteNote) {
        var bezierPath: Path
        paint.strokeWidth = 2f
        var xstart = 0
        xstart = if (side == LeftSide) SheetMusic.LineSpace / 4 + 1 else SheetMusic.LineSpace / 4 + SheetMusic.NoteWidth
        if (direction == Up) {
            var ystem = ytop + topstaff.Dist(end!!) * SheetMusic.NoteHeight / 2
            if (duration === NoteDuration.Eighth || duration === NoteDuration.DottedEighth || duration === NoteDuration.Triplet || duration === NoteDuration.Sixteenth || duration === NoteDuration.ThirtySecond) {
                bezierPath = Path()
                bezierPath.moveTo(xstart.toFloat(), ystem.toFloat())
                bezierPath.cubicTo(xstart.toFloat(), (ystem + 3 * SheetMusic.LineSpace / 2).toFloat(), (
                        xstart + SheetMusic.LineSpace * 2).toFloat(), (ystem + SheetMusic.NoteHeight * 2).toFloat(), (
                        xstart + SheetMusic.LineSpace / 2).toFloat(), (ystem + SheetMusic.NoteHeight * 3).toFloat())
                canvas.drawPath(bezierPath, paint)
            }
            ystem += SheetMusic.NoteHeight
            if (duration === NoteDuration.Sixteenth ||
                    duration === NoteDuration.ThirtySecond) {
                bezierPath = Path()
                bezierPath.moveTo(xstart.toFloat(), ystem.toFloat())
                bezierPath.cubicTo(xstart.toFloat(), (ystem + 3 * SheetMusic.LineSpace / 2).toFloat(), (
                        xstart + SheetMusic.LineSpace * 2).toFloat(), (ystem + SheetMusic.NoteHeight * 2).toFloat(), (
                        xstart + SheetMusic.LineSpace / 2).toFloat(), (ystem + SheetMusic.NoteHeight * 3).toFloat())
                canvas.drawPath(bezierPath, paint)
            }
            ystem += SheetMusic.NoteHeight
            if (duration === NoteDuration.ThirtySecond) {
                bezierPath = Path()
                bezierPath.moveTo(xstart.toFloat(), ystem.toFloat())
                bezierPath.cubicTo(xstart.toFloat(), (ystem + 3 * SheetMusic.LineSpace / 2).toFloat(), (
                        xstart + SheetMusic.LineSpace * 2).toFloat(), (ystem + SheetMusic.NoteHeight * 2).toFloat(), (
                        xstart + SheetMusic.LineSpace / 2).toFloat(), (ystem + SheetMusic.NoteHeight * 3).toFloat())
                canvas.drawPath(bezierPath, paint)
            }
        } else if (direction == Down) {
            var ystem = ytop + topstaff.Dist(end!!) * SheetMusic.NoteHeight / 2 +
                    SheetMusic.NoteHeight
            if (duration === NoteDuration.Eighth || duration === NoteDuration.DottedEighth || duration === NoteDuration.Triplet || duration === NoteDuration.Sixteenth || duration === NoteDuration.ThirtySecond) {
                bezierPath = Path()
                bezierPath.moveTo(xstart.toFloat(), ystem.toFloat())
                bezierPath.cubicTo(xstart.toFloat(), (ystem - SheetMusic.LineSpace).toFloat(), (
                        xstart + SheetMusic.LineSpace * 2).toFloat(), (ystem - SheetMusic.NoteHeight * 2).toFloat(), (
                        xstart + SheetMusic.LineSpace).toFloat(), (ystem - SheetMusic.NoteHeight * 2 - SheetMusic.LineSpace / 2).toFloat())
                canvas.drawPath(bezierPath, paint)
            }
            ystem -= SheetMusic.NoteHeight
            if (duration === NoteDuration.Sixteenth ||
                    duration === NoteDuration.ThirtySecond) {
                bezierPath = Path()
                bezierPath.moveTo(xstart.toFloat(), ystem.toFloat())
                bezierPath.cubicTo(xstart.toFloat(), (ystem - SheetMusic.LineSpace).toFloat(), (
                        xstart + SheetMusic.LineSpace * 2).toFloat(), (ystem - SheetMusic.NoteHeight * 2).toFloat(), (
                        xstart + SheetMusic.LineSpace).toFloat(), (ystem - SheetMusic.NoteHeight * 2 - SheetMusic.LineSpace / 2).toFloat())
                canvas.drawPath(bezierPath, paint)
            }
            ystem -= SheetMusic.NoteHeight
            if (duration === NoteDuration.ThirtySecond) {
                bezierPath = Path()
                bezierPath.moveTo(xstart.toFloat(), ystem.toFloat())
                bezierPath.cubicTo(xstart.toFloat(), (ystem - SheetMusic.LineSpace).toFloat(), (
                        xstart + SheetMusic.LineSpace * 2).toFloat(), (ystem - SheetMusic.NoteHeight * 2).toFloat(), (
                        xstart + SheetMusic.LineSpace).toFloat(), (ystem - SheetMusic.NoteHeight * 2 - SheetMusic.LineSpace / 2).toFloat())
                canvas.drawPath(bezierPath, paint)
            }
        }
        paint.strokeWidth = 1f
    }

    /* Draw a horizontal beam stem, connecting this stem with the Stem pair.
     * @param ytop The y location (in pixels) where the top of the staff starts.
     * @param topstaff  The note at the top of the staff.
     */
    private fun DrawHorizBarStem(canvas: Canvas, paint: Paint, ytop: Int, topstaff: WhiteNote) {
        paint.strokeWidth = (SheetMusic.NoteHeight / 2).toFloat()
        paint.strokeCap = Paint.Cap.BUTT
        var xstart = 0
        var xstart2 = 0
        if (side == LeftSide) xstart = SheetMusic.LineSpace / 4 + 1 else if (side == RightSide) xstart = SheetMusic.LineSpace / 4 + SheetMusic.NoteWidth
        if (pair!!.side == LeftSide) xstart2 = SheetMusic.LineSpace / 4 + 1 else if (pair!!.side == RightSide) xstart2 = SheetMusic.LineSpace / 4 + SheetMusic.NoteWidth
        if (direction == Up) {
            val xend = width_to_pair + xstart2
            var ystart = ytop + topstaff.Dist(end!!) * SheetMusic.NoteHeight / 2
            var yend = ytop + topstaff.Dist(pair!!.end!!) * SheetMusic.NoteHeight / 2
            if (duration === NoteDuration.Eighth || duration === NoteDuration.DottedEighth || duration === NoteDuration.Triplet || duration === NoteDuration.Sixteenth || duration === NoteDuration.ThirtySecond) {
                canvas.drawLine(xstart.toFloat(), ystart.toFloat(), xend.toFloat(), yend.toFloat(), paint)
            }
            ystart += SheetMusic.NoteHeight
            yend += SheetMusic.NoteHeight

            /* A dotted eighth will connect to a 16th note. */if (duration === NoteDuration.DottedEighth) {
                val x = xend - SheetMusic.NoteHeight
                val slope = (yend - ystart) * 1.0 / (xend - xstart)
                val y = (slope * (x - xend) + yend).toInt()
                canvas.drawLine(x.toFloat(), y.toFloat(), xend.toFloat(), yend.toFloat(), paint)
            }
            if (duration === NoteDuration.Sixteenth ||
                    duration === NoteDuration.ThirtySecond) {
                canvas.drawLine(xstart.toFloat(), ystart.toFloat(), xend.toFloat(), yend.toFloat(), paint)
            }
            ystart += SheetMusic.NoteHeight
            yend += SheetMusic.NoteHeight
            if (duration === NoteDuration.ThirtySecond) {
                canvas.drawLine(xstart.toFloat(), ystart.toFloat(), xend.toFloat(), yend.toFloat(), paint)
            }
        } else {
            val xend = width_to_pair + xstart2
            var ystart = ytop + topstaff.Dist(end!!) * SheetMusic.NoteHeight / 2 +
                    SheetMusic.NoteHeight
            var yend = ytop + topstaff.Dist(pair!!.end!!) * SheetMusic.NoteHeight / 2 + SheetMusic.NoteHeight
            if (duration === NoteDuration.Eighth || duration === NoteDuration.DottedEighth || duration === NoteDuration.Triplet || duration === NoteDuration.Sixteenth || duration === NoteDuration.ThirtySecond) {
                canvas.drawLine(xstart.toFloat(), ystart.toFloat(), xend.toFloat(), yend.toFloat(), paint)
            }
            ystart -= SheetMusic.NoteHeight
            yend -= SheetMusic.NoteHeight

            /* A dotted eighth will connect to a 16th note. */if (duration === NoteDuration.DottedEighth) {
                val x = xend - SheetMusic.NoteHeight
                val slope = (yend - ystart) * 1.0 / (xend - xstart)
                val y = (slope * (x - xend) + yend).toInt()
                canvas.drawLine(x.toFloat(), y.toFloat(), xend.toFloat(), yend.toFloat(), paint)
            }
            if (duration === NoteDuration.Sixteenth ||
                    duration === NoteDuration.ThirtySecond) {
                canvas.drawLine(xstart.toFloat(), ystart.toFloat(), xend.toFloat(), yend.toFloat(), paint)
            }
            ystart -= SheetMusic.NoteHeight
            yend -= SheetMusic.NoteHeight
            if (duration === NoteDuration.ThirtySecond) {
                canvas.drawLine(xstart.toFloat(), ystart.toFloat(), xend.toFloat(), yend.toFloat(), paint)
            }
        }
        paint.strokeWidth = 1f
    }

    override fun toString(): String {
        return String.format("Stem duration=%1\$s direction=%2\$s top=%3\$s bottom=%4\$s end=%5\$s" +
                " overlap=%6\$s side=%7\$s width_to_pair=%8\$s receiver_in_pair=%9\$s",
                duration, direction, top.toString(), bottom.toString(),
                end.toString(), notesoverlap, side, width_to_pair, receiver)
    }

    companion object {
        const val Up = 1 /* The stem points up */
        const val Down = 2 /* The stem points down */
        const val LeftSide = 1 /* The stem is to the left of the note */
        const val RightSide = 2 /* The stem is to the right of the note */
    }

    /** Create a new stem.  The top note, bottom note, and direction are
     * needed for drawing the vertical line of the stem.  The duration is
     * needed to draw the tail of the stem.  The overlap boolean is true
     * if the notes in the chord overlap.  If the notes overlap, the
     * stem must be drawn on the right side.
     */
    init {
        this.duration = duration
        this.direction = direction
        notesoverlap = overlap
        side = if (direction == Up || notesoverlap) RightSide else LeftSide
        end = CalculateEnd()
        pair = null
        width_to_pair = 0
        receiver = false
    }
}