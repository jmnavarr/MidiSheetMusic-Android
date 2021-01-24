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
import com.midisheetmusic.SheetMusic

/** @class BarSymbol
 * The BarSymbol represents the vertical bars which delimit measures.
 * The starttime of the symbol is the beginning of the new
 * measure.
 */
class BarSymbol(private val starttime: Int) : MusicSymbol {

    override val startTime = 0

    override var width = 0

    /** Get the minimum width (in pixels) needed to draw this symbol  */
    override val minWidth = 2 * SheetMusic.LineSpace

    /** Get the number of pixels this symbol extends above the staff. Used
     * to determine the minimum height needed for the staff (Staff.FindBounds).
     */
    override val aboveStaff = 0

    /** Get the number of pixels this symbol extends below the staff. Used
     * to determine the minimum height needed for the staff (Staff.FindBounds).
     */
    override val belowStaff = 0

    /** Draw a vertical bar.
     * @param ytop The ylocation (in pixels) where the top of the staff starts.
     */
    override fun Draw(canvas: Canvas?, paint: Paint?, ytop: Int) {
        val yend = ytop + SheetMusic.LineSpace * 4 + SheetMusic.LineWidth * 4
        paint!!.strokeWidth = 1f
        canvas!!.drawLine((SheetMusic.NoteWidth / 2).toFloat(), ytop.toFloat(), (SheetMusic.NoteWidth / 2).toFloat(), yend.toFloat(), paint)
    }

    override fun toString(): String {
        return String.format("BarSymbol starttime=%1\$s width=%2\$s",
                starttime, width)
    }

    /** Create a BarSymbol. The starttime should be the beginning of a measure.  */
    init {
        width = minWidth
    }
}