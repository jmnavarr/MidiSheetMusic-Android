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

import android.graphics.*
import com.midisheetmusic.KeySignature
import com.midisheetmusic.MidiOptions
import com.midisheetmusic.SheetMusic
import com.midisheetmusic.SheetMusic.Companion.KeySignatureWidth
import java.util.*

/* @class Staff
 * The Staff is used to draw a single Staff (a row of measures) in the 
 * SheetMusic Control. A Staff needs to draw
 * - The Clef
 * - The key signature
 * - The horizontal lines
 * - A list of MusicSymbols
 * - The left and right vertical lines
 *
 * The height of the Staff is determined by the number of pixels each
 * MusicSymbol extends above and below the staff.
 *
 * The vertical lines (left and right sides) of the staff are joined
 * with the staffs above and below it, with one exception.  
 * The last track is not joined with the first track.
 */
class Staff(symbols: ArrayList<MusicSymbol>, key: KeySignature,
            options: MidiOptions, tracknum: Int, totaltracks: Int) {
    private val symbols: ArrayList<MusicSymbol>

    /** The music symbols in this staff  */
    private var lyrics: ArrayList<LyricSymbol>? = null

    /** The lyrics to display (can be null)  */
    private var ytop = 0

    /** The y pixel of the top of the staff  */
    private val clefsym: ClefSymbol

    /** The left-side Clef symbol  */
    private val keys: Array<AccidSymbol?>

    /** The key signature symbols  */
    var showMeasures: Boolean

    /** If true, show the measure numbers  */
    private val keysigWidth: Int
    /** Return the width of the staff  */
    /** The width of the clef and key signature  */
    var width = 0
        private set
    /** Return the height of the staff  */
    /** The width of the staff in pixels  */
    var height = 0
        private set
    /** Return the track number of this staff (starting from 0  */
    /** The height of the staff in pixels  */
    val track: Int

    /** The track this staff represents  */
    private val totaltracks: Int
    /** Return the starting time of the staff, the start time of
     * the first symbol.  This is used during playback, to
     * automatically scroll the music while playing.
     */
    /** The total number of tracks  */
    var startTime = 0
        private set
    /** Return the ending time of the staff, the endtime of
     * the last symbol.  This is used during playback, to
     * automatically scroll the music while playing.
     */
    /** The time (in pulses) of first symbol  */
    var endTime = 0

    /** The time (in pulses) of last symbol  */
    private var measureLength = 0

    /** Find the initial clef to use for this staff.  Use the clef of
     * the first ChordSymbol.
     */
    private fun FindClef(list: ArrayList<MusicSymbol>): Clef {
        for (m in list) {
            if (m is ChordSymbol) {
                return m.clef
            }
        }
        return Clef.Treble
    }

    /** Calculate the height of this staff.  Each MusicSymbol contains the
     * number of pixels it needs above and below the staff.  Get the maximum
     * values above and below the staff.
     */
    fun CalculateHeight() {
        var above = 0
        var below = 0
        for (s in symbols) {
            above = Math.max(above, s.aboveStaff)
            below = Math.max(below, s.belowStaff)
        }
        above = Math.max(above, clefsym.aboveStaff)
        below = Math.max(below, clefsym.belowStaff)
        if (showMeasures) {
            above = Math.max(above, SheetMusic.NoteHeight * 3)
        }
        ytop = above + SheetMusic.NoteHeight
        height = SheetMusic.NoteHeight * 5 + ytop + below
        if (lyrics != null) {
            height += SheetMusic.NoteHeight * 3 / 2
        }

        /* Add some extra vertical space between the last track
         * and first track.
         */if (track == totaltracks - 1) height += SheetMusic.NoteHeight * 3
    }

    /** Calculate the width of this staff  */
    private fun CalculateWidth(scrollVert: Boolean) {
        if (scrollVert) {
            width = SheetMusic.PageWidth
            return
        }
        width = keysigWidth
        for (s in symbols) {
            width += s.width
        }
    }

    /** Calculate the start and end time of this staff.  */
    private fun CalculateStartEndTime() {
        endTime = 0
        startTime = endTime
        if (symbols.size == 0) {
            return
        }
        startTime = symbols[0].startTime
        for (m in symbols) {
            if (endTime < m.startTime) {
                endTime = m.startTime
            }
            if (m is ChordSymbol) {
                val c = m
                if (endTime < c.endTime) {
                    endTime = c.endTime
                }
            }
        }
    }

    /** Full-Justify the symbols, so that they expand to fill the whole staff.  */
    private fun FullJustify() {
        if (width != SheetMusic.PageWidth) return
        var totalwidth = keysigWidth
        var totalsymbols = 0
        var i = 0
        while (i < symbols.size) {
            val start = symbols[i].startTime
            totalsymbols++
            totalwidth += symbols[i].width
            i++
            while (i < symbols.size && symbols[i].startTime == start) {
                totalwidth += symbols[i].width
                i++
            }
        }
        var extrawidth = (SheetMusic.PageWidth - totalwidth - 1) / totalsymbols
        if (extrawidth > SheetMusic.NoteHeight * 2) {
            extrawidth = SheetMusic.NoteHeight * 2
        }
        i = 0
        while (i < symbols.size) {
            val start = symbols[i].startTime
            val newwidth = symbols[i].width + extrawidth
            symbols[i].width = newwidth
            i++
            while (i < symbols.size && symbols[i].startTime == start) {
                i++
            }
        }
    }

    /** Add the lyric symbols that occur within this staff.
     * Set the x-position of the lyric symbol.
     */
    fun AddLyrics(tracklyrics: ArrayList<LyricSymbol?>?) {
        if (tracklyrics == null || tracklyrics.size == 0) {
            return
        }
        lyrics = ArrayList()
        var xpos = 0
        var symbolindex = 0
        for (lyric in tracklyrics) {
            if (lyric!!.startTime < startTime) {
                continue
            }
            if (lyric.startTime > endTime) {
                break
            }
            /* Get the x-position of this lyric */while (symbolindex < symbols.size &&
                    symbols[symbolindex].startTime < lyric.startTime) {
                xpos += symbols[symbolindex].width
                symbolindex++
            }
            lyric.x = xpos
            if (symbolindex < symbols.size &&
                    symbols[symbolindex] is BarSymbol) {
                lyric.x = lyric.x + SheetMusic.NoteWidth
            }
            lyrics!!.add(lyric)
        }
        if (lyrics!!.size == 0) {
            lyrics = null
        }
    }

    /** Draw the lyrics  */
    private fun DrawLyrics(canvas: Canvas, paint: Paint) {
        /* Skip the left side Clef symbol and key signature */
        val xpos = keysigWidth
        val ypos = height - SheetMusic.NoteHeight * 3 / 2
        for (lyric in lyrics!!) {
            canvas.drawText(lyric.text, (
                    xpos + lyric.x).toFloat(),
                    ypos.toFloat(),
                    paint)
        }
    }

    /** Draw the measure numbers for each measure  */
    private fun DrawMeasureNumbers(canvas: Canvas, paint: Paint) {
        /* Skip the left side Clef symbol and key signature */
        var xpos = keysigWidth
        val ypos = ytop - SheetMusic.NoteHeight * 3
        for (s in symbols) {
            if (s is BarSymbol) {
                val measure = 1 + s.startTime / measureLength
                canvas.drawText("" + measure, (
                        xpos + SheetMusic.NoteWidth / 2).toFloat(),
                        ypos.toFloat(),
                        paint)
            }
            xpos += s.width
        }
    }

    /** Draw the five horizontal lines of the staff  */
    private fun DrawHorizLines(canvas: Canvas, paint: Paint) {
        var line = 1
        var y = ytop - SheetMusic.LineWidth
        paint.strokeWidth = 1f
        line = 1
        while (line <= 5) {
            canvas.drawLine(SheetMusic.LeftMargin.toFloat(), y.toFloat(), (width - 1).toFloat(), y.toFloat(), paint)
            y += SheetMusic.LineWidth + SheetMusic.LineSpace
            line++
        }
    }

    /** Draw the vertical lines at the far left and far right sides.  */
    private fun DrawEndLines(canvas: Canvas, paint: Paint) {
        paint.strokeWidth = 1f

        /* Draw the vertical lines from 0 to the height of this staff,
         * including the space above and below the staff, with two exceptions:
         * - If this is the first track, don't start above the staff.
         *   Start exactly at the top of the staff (ytop - LineWidth)
         * - If this is the last track, don't end below the staff.
         *   End exactly at the bottom of the staff.
         */
        val ystart: Int
        val yend: Int
        ystart = if (track == 0) ytop - SheetMusic.LineWidth else 0
        yend = if (track == totaltracks - 1) ytop + 4 * SheetMusic.NoteHeight else height
        canvas.drawLine(SheetMusic.LeftMargin.toFloat(), ystart.toFloat(), SheetMusic.LeftMargin.toFloat(), yend.toFloat(), paint)
        canvas.drawLine((width - 1).toFloat(), ystart.toFloat(), (width - 1).toFloat(), yend.toFloat(), paint)
    }

    /** Draw this staff. Only draw the symbols inside the clip area  */
    fun Draw(canvas: Canvas, clip: Rect, paint: Paint) {
        paint.color = Color.BLACK
        var xpos = SheetMusic.LeftMargin + 5

        /* Draw the left side Clef symbol */canvas.translate(xpos.toFloat(), 0f)
        clefsym.Draw(canvas, paint, ytop)
        canvas.translate(-xpos.toFloat(), 0f)
        xpos += clefsym.width

        /* Draw the key signature */for (a in keys) {
            canvas.translate(xpos.toFloat(), 0f)
            a!!.Draw(canvas, paint, ytop)
            canvas.translate(-xpos.toFloat(), 0f)
            xpos += a.width
        }

        /* Draw the actual notes, rests, bars.  Draw the symbols one 
         * after another, using the symbol width to determine the
         * x position of the next symbol.
         *
         * For fast performance, only draw symbols that are in the clip area.
         */for (s in symbols) {
            if (xpos <= clip.left + clip.width() + 50 && xpos + s.width + 50 >= clip.left) {
                canvas.translate(xpos.toFloat(), 0f)
                s.Draw(canvas, paint, ytop)
                canvas.translate(-xpos.toFloat(), 0f)
            }
            xpos += s.width
        }
        paint.color = Color.BLACK
        DrawHorizLines(canvas, paint)
        DrawEndLines(canvas, paint)
        if (showMeasures) {
            DrawMeasureNumbers(canvas, paint)
        }
        if (lyrics != null) {
            DrawLyrics(canvas, paint)
        }
    }

    fun getCurrentNote(currentPulseTime: Int): MusicSymbol? {
        for (i in symbols.indices) {
            val cur = symbols[i]
            if (cur is ChordSymbol) {
                if (cur.startTime >= currentPulseTime) {
                    return cur
                }
            }
        }
        return null
    }

    /** Shade all the chords played in the given time.
     * Un-shade any chords shaded in the previous pulse time.
     * Store the x coordinate location where the shade was drawn.
     */
    fun ShadeNotes(canvas: Canvas, paint: Paint, shade: Int,
                   currentPulseTime: Int, prevPulseTime: Int, x_shade: Int): Int {

        /* If there's nothing to unshade, or shade, return */
        var x_shade = x_shade
        if ((startTime > prevPulseTime || endTime < prevPulseTime) &&
                (startTime > currentPulseTime || endTime < currentPulseTime)) {
            return x_shade
        }

        /* Skip the left side Clef symbol and key signature */
        var xpos = keysigWidth
        var curr: MusicSymbol? = null
        var prevChord: ChordSymbol? = null
        var prev_xpos = 0

        /* Loop through the symbols. 
         * Unshade symbols where start <= prevPulseTime < end
         * Shade symbols where start <= currentPulseTime < end
         */for (i in symbols.indices) {
            curr = symbols[i]
            if (curr is BarSymbol) {
                xpos += curr.width
                continue
            }
            val start = curr.startTime
            var end = 0
            end = if (i + 2 < symbols.size && symbols[i + 1] is BarSymbol) {
                symbols[i + 2].startTime
            } else if (i + 1 < symbols.size) {
                symbols[i + 1].startTime
            } else {
                endTime
            }


            /* If we've past the previous and current times, we're done. */if (start > prevPulseTime && start > currentPulseTime) {
                if (x_shade == 0) {
                    x_shade = xpos
                }
                return x_shade
            }
            /* If shaded notes are the same, we're done */if (start <= currentPulseTime && currentPulseTime < end &&
                    start <= prevPulseTime && prevPulseTime < end) {
                x_shade = xpos
                return x_shade
            }
            var redrawLines = false

            /* If symbol is in the previous time, draw a white background */if (start <= prevPulseTime && prevPulseTime < end) {
                canvas.translate((xpos - 2).toFloat(), -2f)
                paint.style = Paint.Style.FILL
                paint.color = Color.WHITE
                canvas.drawRect(0f, 0f, (curr.width + 4).toFloat(), (height + 4).toFloat(), paint)
                paint.style = Paint.Style.STROKE
                paint.color = Color.BLACK
                canvas.translate(-(xpos - 2).toFloat(), 2f)
                canvas.translate(xpos.toFloat(), 0f)
                curr.Draw(canvas, paint, ytop)
                canvas.translate(-xpos.toFloat(), 0f)
                redrawLines = true
            }

            /* If symbol is in the current time, draw a shaded background */if (start <= currentPulseTime && currentPulseTime < end) {
                x_shade = xpos
                canvas.translate(xpos.toFloat(), 0f)
                paint.style = Paint.Style.FILL
                paint.color = shade
                canvas.drawRect(0f, 0f, curr.width.toFloat(), height.toFloat(), paint)
                paint.style = Paint.Style.STROKE
                paint.color = Color.BLACK
                curr.Draw(canvas, paint, ytop)
                canvas.translate(-xpos.toFloat(), 0f)
                redrawLines = true
            }

            /* If either a gray or white background was drawn, we need to redraw
             * the horizontal staff lines, and redraw the stem of the previous chord.
             */if (redrawLines) {
                var line = 1
                var y = ytop - SheetMusic.LineWidth
                paint.style = Paint.Style.STROKE
                paint.color = Color.BLACK
                paint.strokeWidth = 1f
                canvas.translate((xpos - 2).toFloat(), 0f)
                line = 1
                while (line <= 5) {
                    canvas.drawLine(0f, y.toFloat(), (curr.width + 4).toFloat(), y.toFloat(), paint)
                    y += SheetMusic.LineWidth + SheetMusic.LineSpace
                    line++
                }
                canvas.translate(-(xpos - 2).toFloat(), 0f)
                if (prevChord != null) {
                    canvas.translate(prev_xpos.toFloat(), 0f)
                    prevChord.Draw(canvas, paint, ytop)
                    canvas.translate(-prev_xpos.toFloat(), 0f)
                }
                if (showMeasures) {
                    DrawMeasureNumbers(canvas, paint)
                }
                if (lyrics != null) {
                    DrawLyrics(canvas, paint)
                }
            }
            if (curr is ChordSymbol) {
                val chord = curr
                if (chord.stem != null && !chord.stem!!.receiver) {
                    prevChord = curr
                    prev_xpos = xpos
                }
            }
            xpos += curr.width
        }
        return x_shade
    }

    /** Return the pulse time corresponding to the given point.
     * Find the notes/symbols corresponding to the x position,
     * and return the startTime (pulseTime) of the symbol.
     */
    fun PulseTimeForPoint(point: Point): Int {
        var xpos = keysigWidth
        var pulseTime = startTime
        for (sym in symbols) {
            pulseTime = sym.startTime
            if (point.x <= xpos + sym.width) {
                return pulseTime
            }
            xpos += sym.width
        }
        return pulseTime
    }

    override fun toString(): String {
        val result = StringBuilder("Staff clef=$clefsym\n")
        result.append("  Keys:\n")
        for (a in keys) {
            result.append("    ").append(a.toString()).append("\n")
        }
        result.append("  Symbols:\n")
        for (s in keys) {
            result.append("    ").append(s.toString()).append("\n")
        }
        for (m in symbols) {
            result.append("    ").append(m.toString()).append("\n")
        }
        result.append("End Staff\n")
        return result.toString()
    }
    /** The time (in pulses) of a measure  */
    /** Create a new staff with the given list of music symbols,
     * and the given key signature.  The clef is determined by
     * the clef of the first chord symbol. The track number is used
     * to determine whether to join this left/right vertical sides
     * with the staffs above and below. The MidiOptions are used
     * to check whether to display measure numbers or not.
     */
    init {
        keysigWidth = KeySignatureWidth(key)
        track = tracknum
        this.totaltracks = totaltracks
        showMeasures = options.showMeasures && tracknum == 0
        measureLength = if (options.time != null) {
            options.time!!.measure
        } else {
            options.defaultTime!!.measure
        }
        val clef = FindClef(symbols)
        clefsym = ClefSymbol(clef, 0, false)
        keys = key.GetSymbols(clef)
        this.symbols = symbols
        CalculateWidth(options.scrollVert)
        CalculateHeight()
        CalculateStartEndTime()
        FullJustify()
    }
}