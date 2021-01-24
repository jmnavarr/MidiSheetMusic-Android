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
import android.graphics.RectF
import com.midisheetmusic.NoteDuration
import com.midisheetmusic.SheetMusic

/* @class RestSymbol
 * A Rest symbol represents a rest - whole, half, quarter, or eighth.
 * The Rest symbol has a starttime and a duration, just like a regular
 * note.
 */
class RestSymbol(
        /** Get the time (in pulses) this symbol occurs at.
         * This is used to determine the measure this symbol belongs to.
         */
        override val startTime: Int,
        /** The starttime of the rest  */
        private var duration: NoteDuration) : MusicSymbol {

    /** Get/Set the width (in pixels) of this symbol. The width is set
     * in SheetMusic.AlignSymbols() to vertically align symbols.
     */
    /** The rest duration (eighth, quarter, half, whole)  */
    override var width = 0

    /** Get the minimum width (in pixels) needed to draw this symbol  */
    override val minWidth: Int
        get() = 2 * SheetMusic.NoteHeight + SheetMusic.NoteHeight / 2

    /** Get the number of pixels this symbol extends above the staff. Used
     * to determine the minimum height needed for the staff (Staff.FindBounds).
     */
    override val aboveStaff: Int
        get() = 0

    /** Get the number of pixels this symbol extends below the staff. Used
     * to determine the minimum height needed for the staff (Staff.FindBounds).
     */
    override val belowStaff: Int
        get() = 0

    /** Draw the symbol.
     * @param ytop The ylocation (in pixels) where the top of the staff starts.
     */
    override fun Draw(canvas: Canvas?, paint: Paint?, ytop: Int) {
        /* Align the rest symbol to the right */
        canvas!!.translate((width - minWidth).toFloat(), 0f)
        canvas.translate((SheetMusic.NoteHeight / 2).toFloat(), 0f)
        if (duration === NoteDuration.Whole) {
            DrawWhole(canvas, paint, ytop)
        } else if (duration === NoteDuration.Half) {
            DrawHalf(canvas, paint, ytop)
        } else if (duration === NoteDuration.Quarter) {
            DrawQuarter(canvas, paint, ytop)
        } else if (duration === NoteDuration.Eighth) {
            DrawEighth(canvas, paint, ytop)
        }
        canvas.translate((-SheetMusic.NoteHeight / 2).toFloat(), 0f)
        canvas.translate(-(width - minWidth).toFloat(), 0f)
    }

    /** Draw a whole rest symbol, a rectangle below a staff line.
     * @param ytop The ylocation (in pixels) where the top of the staff starts.
     */
    fun DrawWhole(canvas: Canvas?, paint: Paint?, ytop: Int) {
        val y = ytop + SheetMusic.NoteHeight
        paint!!.style = Paint.Style.FILL
        canvas!!.drawRect(0f, y.toFloat(), SheetMusic.NoteWidth.toFloat(), (y + SheetMusic.NoteHeight / 2).toFloat(), paint)
        paint.style = Paint.Style.STROKE
    }

    /** Draw a half rest symbol, a rectangle above a staff line.
     * @param ytop The ylocation (in pixels) where the top of the staff starts.
     */
    fun DrawHalf(canvas: Canvas?, paint: Paint?, ytop: Int) {
        val y = ytop + SheetMusic.NoteHeight + SheetMusic.NoteHeight / 2
        paint!!.style = Paint.Style.FILL
        canvas!!.drawRect(0f, y.toFloat(), SheetMusic.NoteWidth.toFloat(), (y + SheetMusic.NoteHeight / 2).toFloat(), paint)
        paint.style = Paint.Style.STROKE
    }

    /** Draw a quarter rest symbol.
     * @param ytop The ylocation (in pixels) where the top of the staff starts.
     */
    fun DrawQuarter(canvas: Canvas?, paint: Paint?, ytop: Int) {
        paint!!.strokeCap = Paint.Cap.BUTT
        var y = ytop + SheetMusic.NoteHeight / 2
        val x = 2
        val xend = x + 2 * SheetMusic.NoteHeight / 3
        paint.strokeWidth = 1f
        canvas!!.drawLine(x.toFloat(), y.toFloat(), (xend - 1).toFloat(), (y + SheetMusic.NoteHeight - 1).toFloat(), paint)
        paint.strokeWidth = (SheetMusic.LineSpace / 2).toFloat()
        y = ytop + SheetMusic.NoteHeight + 1
        canvas.drawLine((xend - 2).toFloat(), y.toFloat(), x.toFloat(), (y + SheetMusic.NoteHeight).toFloat(), paint)
        paint.strokeWidth = 1f
        y = ytop + SheetMusic.NoteHeight * 2 - 1
        canvas.drawLine(0f, y.toFloat(), (xend + 2).toFloat(), (y + SheetMusic.NoteHeight).toFloat(), paint)
        paint.strokeWidth = (SheetMusic.LineSpace / 2).toFloat()
        if (SheetMusic.NoteHeight == 6) {
            canvas.drawLine(xend.toFloat(), (y + 1 + 3 * SheetMusic.NoteHeight / 4).toFloat(), (
                    x / 2).toFloat(), (y + 1 + 3 * SheetMusic.NoteHeight / 4).toFloat(), paint)
        } else {  /* NoteHeight == 8 */
            canvas.drawLine(xend.toFloat(), (y + 3 * SheetMusic.NoteHeight / 4).toFloat(), (
                    x / 2).toFloat(), (y + 3 * SheetMusic.NoteHeight / 4).toFloat(), paint)
        }
        paint.strokeWidth = 1f
        canvas.drawLine(0f, (y + 2 * SheetMusic.NoteHeight / 3 + 1).toFloat(), (
                xend - 1).toFloat(), (y + 3 * SheetMusic.NoteHeight / 2).toFloat(), paint)
    }

    /** Draw an eighth rest symbol.
     * @param ytop The ylocation (in pixels) where the top of the staff starts.
     */
    fun DrawEighth(canvas: Canvas?, paint: Paint?, ytop: Int) {
        val y = ytop + SheetMusic.NoteHeight - 1
        val rect = RectF(0 as Float, (y + 1).toFloat(),
                (SheetMusic.LineSpace - 1).toFloat(), (y + 1 + SheetMusic.LineSpace - 1).toFloat())
        paint!!.style = Paint.Style.FILL
        canvas!!.drawOval(rect, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        canvas.drawLine(((SheetMusic.LineSpace - 2) / 2).toFloat(), (y + SheetMusic.LineSpace - 1).toFloat(), (
                3 * SheetMusic.LineSpace / 2).toFloat(), (y + SheetMusic.LineSpace / 2).toFloat(), paint)
        canvas.drawLine((3 * SheetMusic.LineSpace / 2).toFloat(), (y + SheetMusic.LineSpace / 2).toFloat(), (
                3 * SheetMusic.LineSpace / 4).toFloat(), (y + SheetMusic.NoteHeight * 2).toFloat(), paint)
    }

    override fun toString(): String {
        return String.format("RestSymbol starttime=%1\$s duration=%2\$s width=%3\$s",
                startTime, duration, width)
    }
    /** The width in pixels  */
    /** Create a new rest symbol with the given start time and duration  */
    init {
        width = minWidth
    }
}