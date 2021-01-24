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

import android.content.Context
import android.graphics.*
import com.midisheetmusic.sheets.MusicSymbol

/** @class TimeSigSymbol
 * A TimeSigSymbol represents the time signature at the beginning
 * of the staff. We use pre-made images for the numbers, instead of
 * drawing strings.
 */
class TimeSigSymbol(
        /** The images for each number  */
        private val numerator: Int,
        /** The numerator  */
        private var denominator: Int) : MusicSymbol {
    /** The denominator  */
    override var width = 0

    /** The width in pixels  */
    private val canDraw: Boolean = numerator >= 0 && numerator < images!!.size && images!![numerator] != null && denominator >= 0 && denominator < images!!.size && images!![numerator] != null

    override val startTime = -1

    /** Get the minimum width (in pixels) needed to draw this symbol  */
    override val minWidth = if (canDraw) images!![2]!!.width * SheetMusic.NoteHeight * 2 / images!![2]!!.height else 0

    /** Get the number of pixels this symbol extends above the staff. Used
     * to determine the minimum height needed for the staff (Staff.FindBounds).
     */
    override val aboveStaff = 0

    /** Get the number of pixels this symbol extends below the staff. Used
     * to determine the minimum height needed for the staff (Staff.FindBounds).
     */
    override val belowStaff = 0

    /** Draw the symbol.
     * @param ytop The ylocation (in pixels) where the top of the staff starts.
     */
    override fun Draw(canvas: Canvas?, paint: Paint?, ytop: Int) {
        if (!canDraw) return
        canvas!!.translate((width - minWidth).toFloat(), 0f)
        val numer = images!![numerator]
        val denom = images!![denominator]

        /* Scale the image width to match the height */
        val imgheight = SheetMusic.NoteHeight * 2
        val imgwidth = numer!!.width * imgheight / numer.height
        var src = Rect(0, 0, numer.width, numer.height)
        var dest = Rect(0, ytop, imgwidth, ytop + imgheight)
        canvas.drawBitmap(numer, src, dest, paint)
        src = Rect(0, 0, denom!!.width, denom.height)
        dest = Rect(0, ytop + SheetMusic.NoteHeight * 2, imgwidth, ytop + SheetMusic.NoteHeight * 2 + imgheight)
        canvas.drawBitmap(denom, src, dest, paint)
        canvas.translate(-(width - minWidth).toFloat(), 0f)
    }

    override fun toString(): String {
        return String.format("TimeSigSymbol numerator=%1\$s denominator=%2\$s",
                numerator, denominator)
    }

    companion object {
        private var images: Array<Bitmap?>? = null

        /** Load the images into memory.  */
        fun LoadImages(context: Context) {
            if (images != null) {
                return
            }
            images = arrayOfNulls(13)
            val res = context.resources
            images!![2] = BitmapFactory.decodeResource(res, R.drawable.two)
            images!![3] = BitmapFactory.decodeResource(res, R.drawable.three)
            images!![4] = BitmapFactory.decodeResource(res, R.drawable.four)
            images!![6] = BitmapFactory.decodeResource(res, R.drawable.six)
            images!![8] = BitmapFactory.decodeResource(res, R.drawable.eight)
            images!![9] = BitmapFactory.decodeResource(res, R.drawable.nine)
            images!![12] = BitmapFactory.decodeResource(res, R.drawable.twelve)
        }
    }
    /** True if we can draw the time signature  */
    /** Create a new TimeSigSymbol  */
    init {
        width = minWidth
    }
}