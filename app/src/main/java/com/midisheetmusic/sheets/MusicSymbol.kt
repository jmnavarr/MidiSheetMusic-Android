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

/** @class MusicSymbol
 * The MusicSymbol class represents music symbols that can be displayed
 * on a staff.  This includes:
 * - Accidental symbols: sharp, flat, natural
 * - Chord symbols: single notes or chords
 * - Rest symbols: whole, half, quarter, eighth
 * - Bar symbols, the vertical bars which delimit measures.
 * - Treble and Bass clef symbols
 * - Blank symbols, used for aligning notes in different staffs
 */
interface MusicSymbol {
    /** Get the time (in pulses) this symbol occurs at.
     * This is used to determine the measure this symbol belongs to.
     */
    val startTime: Int

    /** Get the minimum width (in pixels) needed to draw this symbol  */
    val minWidth: Int

    /** Get/Set the width (in pixels) of this symbol. The width is set
     * in SheetMusic.AlignSymbols() to vertically align symbols.
     */
    var width: Int

    /** Get the number of pixels this symbol extends above the staff. Used
     * to determine the minimum height needed for the staff (Staff.FindBounds).
     */
    val aboveStaff: Int

    /** Get the number of pixels this symbol extends below the staff. Used
     * to determine the minimum height needed for the staff (Staff.FindBounds).
     */
    val belowStaff: Int

    /** Draw the symbol.
     * @param ytop The ylocation (in pixels) where the top of the staff starts.
     */
    fun Draw(canvas: Canvas?, paint: Paint?, ytop: Int)
}