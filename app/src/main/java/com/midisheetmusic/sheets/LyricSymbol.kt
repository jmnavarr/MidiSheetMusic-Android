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

/** @class LyricSymbol
 * A lyric contains the lyric to display, the start time the lyric occurs at,
 * the the x-coordinate where it will be displayed.
 */
class LyricSymbol(var startTime: Int,
                  /** The start time, in pulses  */
                  var text: String) {

    /** The lyric text  */
    var x = 0

    /* Return the minimum width in pixels needed to display this lyric.
   * This is an estimation, not exact.
   */
    val minWidth: Int
        get() {
            val widthPerChar = 10.0f * 2.0f / 3.0f
            var width = text.length * widthPerChar
            if (text.contains("i")) {
                width -= widthPerChar / 2.0f
            }
            if (text.contains("j")) {
                width -= widthPerChar / 2.0f
            }
            if (text.contains("l")) {
                width -= widthPerChar / 2.0f
            }
            return width.toInt()
        }

    override fun toString(): String {
        return String.format("Lyric start=%1\$s x=%2\$s text=%3\$s",
                startTime, x, text)
    }

    /** The x (horizontal) position within the staff  */
    init {
        text = text
    }
}