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
import com.midisheetmusic.SheetMusic

/** @class AccidSymbol
 * An accidental (accid) symbol represents a sharp, flat, or natural
 * accidental that is displayed at a specific position (note and clef).
 */
class AccidSymbol(private val accid: Accid,
                  /** The accidental (sharp, flat, natural)  */
                  var note: WhiteNote, clef: Clef) : MusicSymbol {
    /** Return the white note this accidental is displayed at  */

    /** The white note where the symbol occurs  */
    private val clef: Clef

    /** Which clef the symbols is in  */
    override var width = 0

    /** Get the time (in pulses) this symbol occurs at.
     * Not used.  Instead, the StartTime of the ChordSymbol containing this
     * AccidSymbol is used.
     */
    override val startTime = -1

    /** Get the minimum width (in pixels) needed to draw this symbol  */
    override val minWidth = 3 * SheetMusic.NoteHeight / 2

    /** Get the number of pixels this symbol extends above the staff. Used
     * to determine the minimum height needed for the staff (Staff.FindBounds).
     */
    override val aboveStaff: Int
    get() {
        var dist = WhiteNote.Top(clef).Dist(note) *
                SheetMusic.NoteHeight / 2
        if (accid === Accid.Sharp || accid === Accid.Natural) dist -= SheetMusic.NoteHeight else if (accid === Accid.Flat) dist -= 3 * SheetMusic.NoteHeight / 2
        return if (dist < 0) -dist else 0
    }


    /** Get the number of pixels this symbol extends below the staff. Used
     * to determine the minimum height needed for the staff (Staff.FindBounds).
     */
    override val belowStaff: Int
    get() {
        var dist = WhiteNote.Bottom(clef).Dist(note) *
                SheetMusic.NoteHeight / 2 +
                SheetMusic.NoteHeight
        if (accid === Accid.Sharp || accid === Accid.Natural) dist += SheetMusic.NoteHeight
        return if (dist > 0) dist else 0
    }

    /** Draw the symbol.
     * @param ytop The ylocation (in pixels) where the top of the staff starts.
     */
    override fun Draw(canvas: Canvas?, paint: Paint?, ytop: Int) {
        /* Align the symbol to the right */
        canvas!!.translate((width - minWidth).toFloat(), 0f)

        /* Store the y-pixel value of the top of the whitenote in ynote. */
        val ynote = ytop + WhiteNote.Top(clef).Dist(note) *
                SheetMusic.NoteHeight / 2
        if (accid === Accid.Sharp) DrawSharp(canvas, paint!!, ynote) else if (accid === Accid.Flat) DrawFlat(canvas, paint!!, ynote) else if (accid === Accid.Natural) DrawNatural(canvas, paint!!, ynote)
        canvas.translate(-(width - minWidth).toFloat(), 0f)
    }

    /** Draw a sharp symbol.
     * @param ynote The pixel location of the top of the accidental's note.
     */
    fun DrawSharp(canvas: Canvas, paint: Paint, ynote: Int) {

        /* Draw the two vertical lines */
        var ystart = ynote - SheetMusic.NoteHeight
        var yend = ynote + 2 * SheetMusic.NoteHeight
        var x = SheetMusic.NoteHeight / 2
        paint.strokeWidth = 1f
        canvas.drawLine(x.toFloat(), (ystart + 2).toFloat(), x.toFloat(), yend.toFloat(), paint)
        x += SheetMusic.NoteHeight / 2
        canvas.drawLine(x.toFloat(), ystart.toFloat(), x.toFloat(), (yend - 2).toFloat(), paint)

        /* Draw the slightly upwards horizontal lines */
        val xstart = SheetMusic.NoteHeight / 2 - SheetMusic.NoteHeight / 4
        val xend = SheetMusic.NoteHeight + SheetMusic.NoteHeight / 4
        ystart = ynote + SheetMusic.LineWidth
        yend = ystart - SheetMusic.LineWidth - SheetMusic.LineSpace / 4
        paint.strokeWidth = (SheetMusic.LineSpace / 2).toFloat()
        canvas.drawLine(xstart.toFloat(), ystart.toFloat(), xend.toFloat(), yend.toFloat(), paint)
        ystart += SheetMusic.LineSpace
        yend += SheetMusic.LineSpace
        canvas.drawLine(xstart.toFloat(), ystart.toFloat(), xend.toFloat(), yend.toFloat(), paint)
        paint.strokeWidth = 1f
    }

    /** Draw a flat symbol.
     * @param ynote The pixel location of the top of the accidental's note.
     */
    fun DrawFlat(canvas: Canvas, paint: Paint, ynote: Int) {
        val x = SheetMusic.LineSpace / 4

        /* Draw the vertical line */paint.strokeWidth = 1f
        canvas.drawLine(x.toFloat(), (ynote - SheetMusic.NoteHeight - SheetMusic.NoteHeight / 2).toFloat(),
                x.toFloat(), (ynote + SheetMusic.NoteHeight).toFloat(), paint)

        /* Draw 3 bezier curves.
         * All 3 curves start and stop at the same points.
         * Each subsequent curve bulges more and more towards 
         * the topright corner, making the curve look thicker
         * towards the top-right.
         */
        var bezierPath = Path()
        bezierPath.moveTo(x.toFloat(), (ynote + SheetMusic.LineSpace / 4).toFloat())
        bezierPath.cubicTo((x + SheetMusic.LineSpace / 2).toFloat(), (ynote - SheetMusic.LineSpace / 2).toFloat(), (
                x + SheetMusic.LineSpace).toFloat(), (ynote + SheetMusic.LineSpace / 3).toFloat(),
                x.toFloat(), (ynote + SheetMusic.LineSpace + SheetMusic.LineWidth + 1).toFloat())
        canvas.drawPath(bezierPath, paint)
        bezierPath = Path()
        bezierPath.moveTo(x.toFloat(), (ynote + SheetMusic.LineSpace / 4).toFloat())
        bezierPath.cubicTo((x + SheetMusic.LineSpace / 2).toFloat(), (ynote - SheetMusic.LineSpace / 2).toFloat(), (
                x + SheetMusic.LineSpace + SheetMusic.LineSpace / 4).toFloat(), (
                ynote + SheetMusic.LineSpace / 3 - SheetMusic.LineSpace / 4).toFloat(),
                x.toFloat(), (ynote + SheetMusic.LineSpace + SheetMusic.LineWidth + 1).toFloat())
        canvas.drawPath(bezierPath, paint)
        bezierPath = Path()
        bezierPath.moveTo(x.toFloat(), (ynote + SheetMusic.LineSpace / 4).toFloat())
        bezierPath.cubicTo((x + SheetMusic.LineSpace / 2).toFloat(), (ynote - SheetMusic.LineSpace / 2).toFloat(), (
                x + SheetMusic.LineSpace + SheetMusic.LineSpace / 2).toFloat(), (
                ynote + SheetMusic.LineSpace / 3 - SheetMusic.LineSpace / 2).toFloat(),
                x.toFloat(), (ynote + SheetMusic.LineSpace + SheetMusic.LineWidth + 1).toFloat())
        canvas.drawPath(bezierPath, paint)
    }

    /** Draw a natural symbol.
     * @param ynote The pixel location of the top of the accidental's note.
     */
    fun DrawNatural(canvas: Canvas, paint: Paint, ynote: Int) {

        /* Draw the two vertical lines */
        var ystart = ynote - SheetMusic.LineSpace - SheetMusic.LineWidth
        var yend = ynote + SheetMusic.LineSpace + SheetMusic.LineWidth
        var x = SheetMusic.LineSpace / 2
        paint.strokeWidth = 1f
        canvas.drawLine(x.toFloat(), ystart.toFloat(), x.toFloat(), yend.toFloat(), paint)
        x += SheetMusic.LineSpace - SheetMusic.LineSpace / 4
        ystart = ynote - SheetMusic.LineSpace / 4
        yend = ynote + 2 * SheetMusic.LineSpace + SheetMusic.LineWidth -
                SheetMusic.LineSpace / 4
        canvas.drawLine(x.toFloat(), ystart.toFloat(), x.toFloat(), yend.toFloat(), paint)

        /* Draw the slightly upwards horizontal lines */
        val xstart = SheetMusic.LineSpace / 2
        val xend = xstart + SheetMusic.LineSpace - SheetMusic.LineSpace / 4
        ystart = ynote + SheetMusic.LineWidth
        yend = ystart - SheetMusic.LineWidth - SheetMusic.LineSpace / 4
        paint.strokeWidth = (SheetMusic.LineSpace / 2).toFloat()
        canvas.drawLine(xstart.toFloat(), ystart.toFloat(), xend.toFloat(), yend.toFloat(), paint)
        ystart += SheetMusic.LineSpace
        yend += SheetMusic.LineSpace
        canvas.drawLine(xstart.toFloat(), ystart.toFloat(), xend.toFloat(), yend.toFloat(), paint)
        paint.strokeWidth = 1f
    }

    override fun toString(): String {
        return String.format(
                "AccidSymbol accid=%1\$s whitenote=%2\$s clef=%3\$s width=%4\$s",
                accid, note, clef, width)
    }
    /** Width of symbol  */
    /**
     * Create a new AccidSymbol with the given accidental, that is
     * displayed at the given note in the given clef.
     */
    init {
        this.clef = clef
        width = minWidth
    }
}