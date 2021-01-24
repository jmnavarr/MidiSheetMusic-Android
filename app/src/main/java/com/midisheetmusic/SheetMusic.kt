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
package com.midisheetmusic

import android.app.Activity
import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.midisheetmusic.KeySignature.Companion.Guess
import com.midisheetmusic.NoteScale.FromNumber
import com.midisheetmusic.sheets.*
import java.nio.charset.StandardCharsets
import java.util.*

internal class BoxedInt {
    var value = 0
}

/**
 * The SheetMusic Control is the main class for displaying the sheet music.
 * The SheetMusic class has the following public methods:
 *
 *  *  SheetMusic():
 * Create a new SheetMusic control from the given midi file and options.
 *
 *  *  onDraw():
 * Method called to draw the SheetMusic
 *
 *  *  shadeNotes():
 * Shade all the notes played at a given pulse time.
 */
class SheetMusic(context: Context) : SurfaceView(context), SurfaceHolder.Callback, ScrollAnimationListener {
    private var staffs: ArrayList<Staff>? = null
    /** Get the main key signature  */
    /** The array of staffs to display (from top to bottom)  */
    var mainKey: KeySignature? = null
        private set

    /** The main key signature  */
    private var filename: String? = null

    /** The midi filename  */
    private var numtracks = 0

    /** The number of tracks  */
    private var zoom = 0f

    /** The zoom level to draw at (1.0 == 100%)  */
    private var scrollVert = false
    /** Get whether to show note letters or not  */
    /** Whether to scroll vertically or horizontally  */
    var showNoteLetters = 0
        private set

    /** Display the note letters  */
    private var useColors = false
    private var NoteColors: IntArray? = null
    /** Get the shade color  */
    /** The note colors to use  */
    var shade1 = 0
        private set
    /** Get the shade2 color  */
    /** The color for shading  */
    var shade2 = 0
        private set

    /** The color for shading left-hand piano  */
    private var paint: Paint? = null

    /** The paint for drawing  */
    private var surfaceReady = false

    /** True if we can draw on the surface  */
    private var bufferBitmap: Bitmap? = null

    /** The bitmap for drawing  */
    private var bufferCanvas: Canvas? = null

    /** The canvas for drawing  */
    private var player: MidiPlayer? = null

    /** For pausing the music  */
    private val playerHeight: Int

    /** Height of the midi player  */
    private val screenwidth: Int

    /** The screen width  */
    private val screenheight: Int

    /** The screen height  */ /* fields used for scrolling */
    private var sheetwidth = 0

    /** The sheet music width (excluding zoom)  */
    private var sheetheight = 0

    /** The sheet music height (excluding zoom)  */
    private var viewwidth = 0

    /** The width of this view.  */
    private var viewheight = 0

    /** The height of this view.  */
    private var bufferX: Int

    /** The (left,top) of the bufferCanvas  */
    private var bufferY: Int
    private var scrollXCoord: Int

    /** The (left,top) of the scroll clip  */
    private var scrollYCoord: Int
    private var scrollAnimation: ScrollAnimation? = null

    /** Create a new SheetMusic View.
     * MidiFile is the parsed midi file to display.
     * SheetMusic Options are the menu options that were selected.
     *
     * - Apply all the Menu Options to the MidiFile tracks.
     * - Calculate the key signature
     * - For each track, create a list of MusicSymbols (notes, rests, bars, etc)
     * - Vertically align the music symbols in all the tracks
     * - Partition the music notes into horizontal staffs
     */
    fun init(file: MidiFile, options: MidiOptions?) {
        var options = options
        if (options == null) {
            options = MidiOptions(file)
        }
        zoom = 1.0f
        filename = file.fileName
        SetColors(options.noteColors, options.useColors, options.shade1Color, options.shade2Color)
        paint = Paint()
        paint!!.textSize = 12.0f
        val typeface = Typeface.create(paint!!.typeface, Typeface.NORMAL)
        paint!!.typeface = typeface
        paint!!.color = Color.BLACK
        val tracks = file.ChangeMidiNotes(options)
        scrollVert = options.scrollVert
        showNoteLetters = options.showNoteLetters
        var time = file.time
        if (options.time != null) {
            time = options.time
        }
        if (options.key == -1) {
            mainKey = GetKeySignature(tracks)
        } else {
            mainKey = KeySignature(options.key)
        }
        numtracks = tracks.size
        val lastStart = file.EndTime() + options.shifttime

        /* Create all the music symbols (notes, rests, vertical bars, and
         * clef changes).  The symbols variable contains a list of music
         * symbols for each track.  The list does not include the left-side 
         * Clef and key signature symbols.  Those can only be calculated 
         * when we create the staffs.
         */
        val allsymbols = ArrayList<ArrayList<MusicSymbol>>(numtracks)
        for (tracknum in 0 until numtracks) {
            val track = tracks[tracknum]
            val clefs = ClefMeasures(track.notes, time!!.measure)
            val chords = CreateChords(track.notes, mainKey, time, clefs)
            allsymbols.add(CreateSymbols(chords, clefs, time, lastStart))
        }
        var lyrics: ArrayList<ArrayList<LyricSymbol?>?>? = null
        if (options.showLyrics) {
            lyrics = GetLyrics(tracks)
        }

        /* Vertically align the music symbols */
        val widths = SymbolWidths(allsymbols, lyrics)
        AlignSymbols(allsymbols, widths, options)
        staffs = CreateStaffs(allsymbols, mainKey, options, time!!.measure)
        CreateAllBeamedChords(allsymbols, time)
        if (lyrics != null) {
            AddLyricsToStaffs(staffs, lyrics)
        }

        /* After making chord pairs, the stem directions can change,
         * which affects the staff height.  Re-calculate the staff height.
         */for (staff in staffs!!) {
            staff.CalculateHeight()
        }
        zoom = 1.0f
        scrollAnimation = ScrollAnimation(this, scrollVert)
    }

    /** Calculate the size of the sheet music width and height
     * (without zoom scaling to fit the screen).  Store the result in
     * sheetwidth and sheetheight.
     */
    private fun calculateSize() {
        sheetwidth = 0
        sheetheight = 0
        for (staff in staffs!!) {
            sheetwidth = Math.max(sheetwidth, staff.width)
            sheetheight += staff.height
        }
        sheetwidth += 2
        sheetheight += LeftMargin
    }

    /* Adjust the zoom level so that the sheet music page (PageWidth)
     * fits within the width. If the heightspec is 0, return the screenheight.
     * Else, use the given view width/height. 
     */
    override fun onMeasure(widthspec: Int, heightspec: Int) {
        // First, calculate the zoom level
        val specwidth = MeasureSpec.getSize(widthspec)
        val specheight = MeasureSpec.getSize(heightspec)
        if (specwidth == 0 && specheight == 0) {
            setMeasuredDimension(screenwidth, screenheight)
        } else if (specwidth == 0) {
            setMeasuredDimension(screenwidth, specheight)
        } else if (specheight == 0) {
            setMeasuredDimension(specwidth, screenheight)
        } else {
            setMeasuredDimension(specwidth, specheight)
        }
    }

    /** If this is the first size change, calculate the zoom level,
     * and create the bufferCanvas.  Otherwise, do nothing.
     */
    override fun onSizeChanged(newwidth: Int, newheight: Int, oldwidth: Int, oldheight: Int) {
        viewwidth = newwidth
        viewheight = newheight
        if (bufferCanvas != null) {
            draw()
            return
        }
        calculateSize()
        zoom = if (scrollVert) {
            ((newwidth - 2) * 1.0 / PageWidth).toFloat()
        } else {
            // Zoom to fit the height assuming the piano is visible
            val pianoSize = Piano.getPreferredSize(newwidth, newheight)
            ((screenheight - pianoSize.y - playerHeight) * 1.0 / sheetheight).toFloat()
        }
        if (bufferCanvas == null) {
            createBufferCanvas()
        }
        draw()
    }

    /** Get the best key signature given the midi notes in all the tracks.  */
    private fun GetKeySignature(tracks: ArrayList<MidiTrack>): KeySignature {
        val notenums = ListInt()
        for (track in tracks) {
            for (note in track.notes) {
                notenums.add(note.number)
            }
        }
        return Guess(notenums)
    }

    /** Create the chord symbols for a single track.
     * @param midinotes  The Midinotes in the track.
     * @param key        The Key Signature, for determining sharps/flats.
     * @param time       The Time Signature, for determining the measures.
     * @param clefs      The clefs to use for each measure.
     * @return An array of ChordSymbols
     */
    private fun CreateChords(midinotes: ArrayList<MidiNote>,
                             key: KeySignature?,
                             time: TimeSignature?,
                             clefs: ClefMeasures): ArrayList<ChordSymbol> {
        var i = 0
        val chords = ArrayList<ChordSymbol>()
        val notegroup = ArrayList<MidiNote>(12)
        val len = midinotes.size
        while (i < len) {
            val starttime = midinotes[i].startTime
            val clef = clefs.GetClef(starttime)

            /* Group all the midi notes with the same start time
             * into the notes list.
             */notegroup.clear()
            notegroup.add(midinotes[i])
            i++
            while (i < len && midinotes[i].startTime == starttime) {
                notegroup.add(midinotes[i])
                i++
            }

            /* Create a single chord from the group of midi notes with
             * the same start time.
             */
            val chord = ChordSymbol(notegroup, key!!, time!!, clef, this)
            chords.add(chord)
        }
        return chords
    }

    /** Given the chord symbols for a track, create a new symbol list
     * that contains the chord symbols, vertical bars, rests, and
     * clef changes.
     * Return a list of symbols (ChordSymbol, BarSymbol, RestSymbol, ClefSymbol)
     */
    private fun CreateSymbols(chords: ArrayList<ChordSymbol>, clefs: ClefMeasures,
                              time: TimeSignature?, lastStart: Int): ArrayList<MusicSymbol> {
        var symbols = ArrayList<MusicSymbol>()
        symbols = AddBars(chords, time, lastStart)
        symbols = AddRests(symbols, time)
        symbols = AddClefChanges(symbols, clefs, time)
        return symbols
    }

    /** Add in the vertical bars delimiting measures.
     * Also, add the time signature symbols.
     */
    private fun AddBars(chords: ArrayList<ChordSymbol>, time: TimeSignature?, lastStart: Int): ArrayList<MusicSymbol> {
        val symbols = ArrayList<MusicSymbol>()
        val timesig = TimeSigSymbol(time!!.numerator, time.denominator)
        symbols.add(timesig)

        /* The starttime of the beginning of the measure */
        var measuretime = 0
        var i = 0
        while (i < chords.size) {
            if (measuretime <= chords[i].startTime) {
                symbols.add(BarSymbol(measuretime))
                measuretime += time.measure
            } else {
                symbols.add(chords[i])
                i++
            }
        }

        /* Keep adding bars until the last StartTime (the end of the song) */while (measuretime < lastStart) {
            symbols.add(BarSymbol(measuretime))
            measuretime += time.measure
        }

        /* Add the final vertical bar to the last measure */symbols.add(BarSymbol(measuretime))
        return symbols
    }

    /** Add rest symbols between notes.  All times below are
     * measured in pulses.
     */
    private fun AddRests(symbols: ArrayList<MusicSymbol>, time: TimeSignature?): ArrayList<MusicSymbol> {
        var prevtime = 0
        val result = ArrayList<MusicSymbol>(symbols.size)
        for (symbol in symbols) {
            val starttime = symbol.startTime
            val rests = GetRests(time, prevtime, starttime)
            if (rests != null) {
                result.addAll(Arrays.asList(*rests))
            }
            result.add(symbol)

            /* Set prevtime to the end time of the last note/symbol. */prevtime = if (symbol is ChordSymbol) {
                Math.max(symbol.endTime, prevtime)
            } else {
                Math.max(starttime, prevtime)
            }
        }
        return result
    }

    /** Return the rest symbols needed to fill the time interval between
     * start and end.  If no rests are needed, return nil.
     */
    private fun GetRests(time: TimeSignature?, start: Int, end: Int): Array<RestSymbol>? {
        val result: Array<RestSymbol>
        val r1: RestSymbol
        val r2: RestSymbol
        if (end - start < 0) return null
        val dur = time!!.GetNoteDuration(end - start)
        return when (dur) {
            NoteDuration.Whole, NoteDuration.Half, NoteDuration.Quarter, NoteDuration.Eighth -> {
                r1 = RestSymbol(start, dur)
                result = arrayOf(r1)
                result
            }
            NoteDuration.DottedHalf -> {
                r1 = RestSymbol(start, NoteDuration.Half)
                r2 = RestSymbol(start + time.quarter * 2,
                        NoteDuration.Quarter)
                result = arrayOf(r1, r2)
                result
            }
            NoteDuration.DottedQuarter -> {
                r1 = RestSymbol(start, NoteDuration.Quarter)
                r2 = RestSymbol(start + time.quarter,
                        NoteDuration.Eighth)
                result = arrayOf(r1, r2)
                result
            }
            NoteDuration.DottedEighth -> {
                r1 = RestSymbol(start, NoteDuration.Eighth)
                r2 = RestSymbol(start + time.quarter / 2,
                        NoteDuration.Sixteenth)
                result = arrayOf(r1, r2)
                result
            }
            else -> null
        }
    }

    /** The current clef is always shown at the beginning of the staff, on
     * the left side.  However, the clef can also change from measure to
     * measure. When it does, a Clef symbol must be shown to indicate the
     * change in clef.  This function adds these Clef change symbols.
     * This function does not add the main Clef Symbol that begins each
     * staff.  That is done in the Staff() contructor.
     */
    private fun AddClefChanges(symbols: ArrayList<MusicSymbol>,
                               clefs: ClefMeasures,
                               time: TimeSignature?): ArrayList<MusicSymbol> {
        val result = ArrayList<MusicSymbol>(symbols.size)
        var prevclef = clefs.GetClef(0)
        for (symbol in symbols) {
            /* A BarSymbol indicates a new measure */
            if (symbol is BarSymbol) {
                val clef = clefs.GetClef(symbol.startTime)
                if (clef != prevclef) {
                    result.add(ClefSymbol(clef, symbol.startTime - 1, true))
                }
                prevclef = clef
            }
            result.add(symbol)
        }
        return result
    }

    /** Notes with the same start times in different staffs should be
     * vertically aligned.  The SymbolWidths class is used to help
     * vertically align symbols.
     *
     * First, each track should have a symbol for every starttime that
     * appears in the Midi File.  If a track doesn't have a symbol for a
     * particular starttime, then add a "blank" symbol for that time.
     *
     * Next, make sure the symbols for each start time all have the same
     * width, across all tracks.  The SymbolWidths class stores
     * - The symbol width for each starttime, for each track
     * - The maximum symbol width for a given starttime, across all tracks.
     *
     * The method SymbolWidths.GetExtraWidth() returns the extra width
     * needed for a track to match the maximum symbol width for a given
     * starttime.
     */
    private fun AlignSymbols(allsymbols: ArrayList<ArrayList<MusicSymbol>>, widths: SymbolWidths, options: MidiOptions) {

        // If we show measure numbers, increase bar symbol width
        if (options.showMeasures) {
            for (track in allsymbols.indices) {
                val symbols = allsymbols[track]
                for (sym in symbols) {
                    (sym as? BarSymbol)?.width = sym.width + NoteWidth
                }
            }
        }
        for (track in allsymbols.indices) {
            val symbols = allsymbols[track]
            val result = ArrayList<MusicSymbol>()
            var i = 0

            /* If a track doesn't have a symbol for a starttime,
             * add a blank symbol.
             */for (start in widths.startTimes) {

                /* BarSymbols are not included in the SymbolWidths calculations */
                while (i < symbols.size && symbols[i] is BarSymbol && symbols[i].startTime <= start) {
                    result.add(symbols[i])
                    i++
                }
                if (i < symbols.size && symbols[i].startTime == start) {
                    while (i < symbols.size &&
                            symbols[i].startTime == start) {
                        result.add(symbols[i])
                        i++
                    }
                } else {
                    result.add(BlankSymbol(start))
                }
            }

            /* For each starttime, increase the symbol width by
             * SymbolWidths.GetExtraWidth().
             */i = 0
            while (i < result.size) {
                if (result[i] is BarSymbol) {
                    i++
                    continue
                }
                val start = result[i].startTime
                val extra = widths.GetExtraWidth(track, start)
                val newwidth = result[i].width + extra
                result[i].width = newwidth

                /* Skip all remaining symbols with the same starttime. */while (i < result.size && result[i].startTime == start) {
                    i++
                }
            }
            allsymbols[track] = result
        }
    }

    /** Given MusicSymbols for a track, create the staffs for that track.
     * Each Staff has a maxmimum width of PageWidth (800 pixels).
     * Also, measures should not span multiple Staffs.
     */
    private fun CreateStaffsForTrack(symbols: ArrayList<MusicSymbol>, measurelen: Int,
                                     key: KeySignature?, options: MidiOptions,
                                     track: Int, totaltracks: Int): ArrayList<Staff> {
        val keysigWidth = KeySignatureWidth(key)
        var startindex = 0
        val thestaffs = ArrayList<Staff>(symbols.size / 50)
        while (startindex < symbols.size) {
            /* startindex is the index of the first symbol in the staff.
             * endindex is the index of the last symbol in the staff.
             */
            var endindex = startindex
            var width = keysigWidth
            var maxwidth: Int

            /* If we're scrolling vertically, the maximum width is PageWidth. */maxwidth = if (scrollVert) {
                PageWidth
            } else {
                2000000
            }
            while (endindex < symbols.size &&
                    width + symbols[endindex].width < maxwidth) {
                width += symbols[endindex].width
                endindex++
            }
            endindex--

            /* There's 3 possibilities at this point:
             * 1. We have all the symbols in the track.
             *    The endindex stays the same.
             *
             * 2. We have symbols for less than one measure.
             *    The endindex stays the same.
             *
             * 3. We have symbols for 1 or more measures.
             *    Since measures cannot span multiple staffs, we must
             *    make sure endindex does not occur in the middle of a
             *    measure.  We count backwards until we come to the end
             *    of a measure.
             */if (endindex == symbols.size - 1) {
                /* endindex stays the same */
            } else if (symbols[startindex].startTime / measurelen ==
                    symbols[endindex].startTime / measurelen) {
                /* endindex stays the same */
            } else {
                val endmeasure = symbols[endindex + 1].startTime / measurelen
                while (symbols[endindex].startTime / measurelen ==
                        endmeasure) {
                    endindex--
                }
            }
            if (scrollVert) {
                width = PageWidth
            }
            // int range = endindex + 1 - startindex;
            val staffSymbols = ArrayList<MusicSymbol>()
            for (i in startindex..endindex) {
                staffSymbols.add(symbols[i])
            }
            val staff = Staff(staffSymbols, key!!, options, track, totaltracks)
            thestaffs.add(staff)
            startindex = endindex + 1
        }
        return thestaffs
    }

    /** Given all the MusicSymbols for every track, create the staffs
     * for the sheet music.  There are two parts to this:
     *
     * - Get the list of staffs for each track.
     * The staffs will be stored in trackstaffs as:
     *
     * trackstaffs[0] = { Staff0, Staff1, Staff2, ... } for track 0
     * trackstaffs[1] = { Staff0, Staff1, Staff2, ... } for track 1
     * trackstaffs[2] = { Staff0, Staff1, Staff2, ... } for track 2
     *
     * - Store the Staffs in the staffs list, but interleave the
     * tracks as follows:
     *
     * staffs = { Staff0 for track 0, Staff0 for track1, Staff0 for track2,
     * Staff1 for track 0, Staff1 for track1, Staff1 for track2,
     * Staff2 for track 0, Staff2 for track1, Staff2 for track2,
     * ... }
     */
    private fun CreateStaffs(allsymbols: ArrayList<ArrayList<MusicSymbol>>, key: KeySignature?,
                             options: MidiOptions, measurelen: Int): ArrayList<Staff> {
        val trackstaffs = ArrayList<ArrayList<Staff>>(allsymbols.size)
        val totaltracks = allsymbols.size
        for (track in 0 until totaltracks) {
            val symbols = allsymbols[track]
            trackstaffs.add(CreateStaffsForTrack(symbols, measurelen, key,
                    options, track, totaltracks))
        }

        /* Update the EndTime of each Staff. EndTime is used for playback */for (list in trackstaffs) {
            for (i in 0 until list.size - 1) {
                list[i].endTime = list[i + 1].startTime
            }
        }

        /* Interleave the staffs of each track into the result array. */
        var maxstaffs = 0
        for (i in trackstaffs.indices) {
            if (maxstaffs < trackstaffs[i].size) {
                maxstaffs = trackstaffs[i].size
            }
        }
        val result = ArrayList<Staff>(maxstaffs * trackstaffs.size)
        for (i in 0 until maxstaffs) {
            for (list in trackstaffs) {
                if (i < list.size) {
                    result.add(list[i])
                }
            }
        }
        return result
    }

    /** Change the note colors for the sheet music, and redraw.  */
    fun SetColors(newcolors: IntArray?, shouldUseColors: Boolean, newshade1: Int, newshade2: Int) {
        useColors = shouldUseColors
        if (NoteColors == null) {
            NoteColors = IntArray(12)
            for (i in 0..11) {
                NoteColors!![i] = Color.BLACK
            }
        }
        if (shouldUseColors && newcolors != null) {
            System.arraycopy(newcolors, 0, NoteColors, 0, newcolors.size)
        }
        shade1 = newshade1
        shade2 = newshade2
    }

    /** Get the color for a given note number. Not currently used.  */
    fun NoteColor(number: Int): Int {
        return NoteColors!![FromNumber(number)]
    }

    /** Create a bitmap/canvas to use for double-buffered drawing.
     * This is needed for shading the notes quickly.
     * Instead of redrawing the entire sheet music on every shade call,
     * we draw the sheet music to this bitmap canvas.  On subsequent
     * calls to ShadeNotes(), we only need to draw the delta (the
     * new notes to shade/unshade) onto the bitmap, and then draw the bitmap.
     *
     * We include the MidiPlayer height (since we hide the MidiPlayer
     * once the music starts playing). Also, we make the bitmap twice as
     * large as the scroll viewable area, so that we don't need to
     * refresh the bufferCanvas on every scroll change.
     */
    fun createBufferCanvas() {
        if (bufferBitmap != null) {
            bufferCanvas = null
            bufferBitmap!!.recycle()
            bufferBitmap = null
        }
        bufferBitmap = if (scrollVert) {
            Bitmap.createBitmap(viewwidth,
                    (viewheight + playerHeight) * 2,
                    Bitmap.Config.ARGB_8888)
        } else {
            Bitmap.createBitmap(viewwidth * 2,
                    (viewheight + playerHeight) * 2,
                    Bitmap.Config.ARGB_8888)
        }
        bufferCanvas = Canvas(bufferBitmap)
        drawToBuffer(scrollXCoord, scrollYCoord)
    }

    /** Obtain the drawing canvas and call onDraw()  */
    fun draw() {
        Thread(label@ Runnable {
            if (!surfaceReady) {
                return@Runnable
            }
            val holder = holder
            val canvas = holder.lockCanvas() ?: return@Runnable
            doDraw(canvas)
            holder.unlockCanvasAndPost(canvas)
        }).start()
    }

    /** Draw the SheetMusic.  */
    fun doDraw(canvas: Canvas) {
        if (bufferBitmap == null) {
            createBufferCanvas()
        }
        if (!isScrollPositionInBuffer) {
            drawToBuffer(scrollXCoord, scrollYCoord)
        }

        // We want (scrollX - bufferX, scrollY - bufferY) 
        // to be (0,0) on the canvas 
        canvas.translate(-(scrollXCoord - bufferX).toFloat(), -(scrollYCoord - bufferY).toFloat())
        canvas.drawBitmap(bufferBitmap, 0f, 0f, paint)
        canvas.translate((scrollXCoord - bufferX).toFloat(), (scrollYCoord - bufferY).toFloat())
    }

    /** Return true if the scrollX/scrollY is in the bufferBitmap  */
    private val isScrollPositionInBuffer: Boolean
        private get() = if (scrollYCoord < bufferY ||
                scrollXCoord < bufferX ||
                scrollYCoord > bufferY + bufferBitmap!!.height / 3 ||
                scrollXCoord > bufferX + bufferBitmap!!.width / 3) {
            false
        } else {
            true
        }

    /** Draw the SheetMusic to the bufferCanvas, with the
     * given (left,top) corner.
     *
     * Scale the graphics by the current zoom factor.
     * Only draw Staffs which lie inside the buffer area.
     */
    private fun drawToBuffer(left: Int, top: Int) {
        if (staffs == null) {
            return
        }
        bufferX = left
        bufferY = top
        bufferCanvas!!.translate(-bufferX.toFloat(), -bufferY.toFloat())
        val clip = Rect(bufferX, bufferY,
                bufferX + bufferBitmap!!.width,
                bufferY + bufferBitmap!!.height)

        // Scale both the canvas and the clip by the zoom factor
        clip.left = (clip.left / zoom).toInt()
        clip.top = (clip.top / zoom).toInt()
        clip.right = (clip.right / zoom).toInt()
        clip.bottom = (clip.bottom / zoom).toInt()
        bufferCanvas!!.scale(zoom, zoom)

        // Draw a white background
        paint!!.isAntiAlias = true
        paint!!.style = Paint.Style.FILL
        paint!!.color = Color.WHITE
        bufferCanvas!!.drawRect(clip.left.toFloat(), clip.top.toFloat(), clip.right.toFloat(), clip.bottom.toFloat(), paint)
        paint!!.style = Paint.Style.STROKE
        paint!!.color = Color.BLACK

        // Draw the staffs in the clip area
        var ypos = 0
        for (staff in staffs!!) {
            if (ypos + staff.height < clip.top || ypos > clip.bottom) {
                /* Staff is not in the clip, don't need to draw it */
            } else {
                bufferCanvas!!.translate(0f, ypos.toFloat())
                staff.Draw((bufferCanvas as Canvas), clip, (paint as Paint))
                bufferCanvas!!.translate(0f, -ypos.toFloat())
            }
            ypos += staff.height
        }
        bufferCanvas!!.scale(1.0f / zoom, 1.0f / zoom)
        bufferCanvas!!.translate(bufferX.toFloat(), bufferY.toFloat())
    }

    /** Write the MIDI filename at the top of the page  */
    private fun DrawTitle(canvas: Canvas) {
        val leftmargin = 20
        val topmargin = 20
        var title = filename
        title = title!!.replace(".mid", "").replace("_", " ")
        canvas.translate(leftmargin.toFloat(), topmargin.toFloat())
        canvas.drawText(title, 0f, 0f, paint)
        canvas.translate(-leftmargin.toFloat(), -topmargin.toFloat())
    }

    /**
     * Return the number of pages needed to print this sheet music.
     * A staff should fit within a single page, not be split across two pages.
     * If the sheet music has exactly 2 tracks, then two staffs should
     * fit within a single page, and not be split across two pages.
     */
    fun GetTotalPages(): Int {
        var num = 1
        var currheight = TitleHeight
        if (numtracks == 2 && staffs!!.size % 2 == 0) {
            var i = 0
            while (i < staffs!!.size) {
                val heights = staffs!![i].height + staffs!![i + 1].height
                if (currheight + heights > PageHeight) {
                    num++
                    currheight = heights
                } else {
                    currheight += heights
                }
                i += 2
            }
        } else {
            for (staff in staffs!!) {
                if (currheight + staff.height > PageHeight) {
                    num++
                    currheight = staff.height
                } else {
                    currheight += staff.height
                }
            }
        }
        return num
    }

    /** Draw the given page of the sheet music.
     * Page numbers start from 1.
     * A staff should fit within a single page, not be split across two pages.
     * If the sheet music has exactly 2 tracks, then two staffs should
     * fit within a single page, and not be split across two pages.
     */
    fun DrawPage(canvas: Canvas, pagenumber: Int) {
        val leftmargin = 20
        val topmargin = 20
        //int rightmargin = 20;
        //int bottommargin = 20;

        //float scale = 1.0f;
        val clip = Rect(0, 0, PageWidth + 40, PageHeight + 40)
        paint!!.isAntiAlias = true
        paint!!.style = Paint.Style.FILL
        paint!!.color = Color.WHITE
        canvas.drawRect(clip.left.toFloat(), clip.top.toFloat(), clip.right.toFloat(), clip.bottom.toFloat(), paint)
        paint!!.style = Paint.Style.STROKE
        paint!!.color = Color.BLACK
        var ypos = TitleHeight
        var pagenum = 1
        var staffnum = 0
        if (numtracks == 2 && staffs!!.size % 2 == 0) {
            /* Skip the staffs until we reach the given page number */
            while (staffnum + 1 < staffs!!.size && pagenum < pagenumber) {
                val heights = staffs!![staffnum].height +
                        staffs!![staffnum + 1].height
                if (ypos + heights >= PageHeight) {
                    pagenum++
                    ypos = 0
                } else {
                    ypos += heights
                    staffnum += 2
                }
            }
            /* Print the staffs until the height reaches PageHeight */ypos = if (pagenum == 1) {
                DrawTitle(canvas)
                TitleHeight
            } else {
                0
            }
            while (staffnum + 1 < staffs!!.size) {
                val heights = staffs!![staffnum].height +
                        staffs!![staffnum + 1].height
                if (ypos + heights >= PageHeight) break
                canvas.translate(leftmargin.toFloat(), (topmargin + ypos).toFloat())
                staffs!![staffnum].Draw(canvas, clip, (paint as Paint))
                canvas.translate(-leftmargin.toFloat(), -(topmargin + ypos).toFloat())
                ypos += staffs!![staffnum].height
                canvas.translate(leftmargin.toFloat(), (topmargin + ypos).toFloat())
                staffs!![staffnum + 1].Draw(canvas, clip, (paint as Paint))
                canvas.translate(-leftmargin.toFloat(), -(topmargin + ypos).toFloat())
                ypos += staffs!![staffnum + 1].height
                staffnum += 2
            }
        } else {
            /* Skip the staffs until we reach the given page number */
            while (staffnum < staffs!!.size && pagenum < pagenumber) {
                if (ypos + staffs!![staffnum].height >= PageHeight) {
                    pagenum++
                    ypos = 0
                } else {
                    ypos += staffs!![staffnum].height
                    staffnum++
                }
            }

            /* Print the staffs until the height reaches viewPageHeight */ypos = if (pagenum == 1) {
                DrawTitle(canvas)
                TitleHeight
            } else {
                0
            }
            while (staffnum < staffs!!.size) {
                if (ypos + staffs!![staffnum].height >= PageHeight) break
                canvas.translate(leftmargin.toFloat(), (topmargin + ypos).toFloat())
                staffs!![staffnum].Draw(canvas, clip, (paint as Paint))
                canvas.translate(-leftmargin.toFloat(), -(topmargin + ypos).toFloat())
                ypos += staffs!![staffnum].height
                staffnum++
            }
        }

        /* Draw the page number */canvas.drawText("" + pagenumber, (
                PageWidth - leftmargin).toFloat(), (
                topmargin + PageHeight - 12).toFloat(),
                paint)
    }

    /** Shade all the chords played at the given pulse time.
     * First, make sure the current scroll position is in the bufferBitmap.
     * Loop through all the staffs and call staff.Shade().
     * If scrollGradually is true, scroll gradually (smooth scrolling)
     * to the shaded notes.
     */
    fun ShadeNotes(currentPulseTime: Int, prevPulseTime: Int, scrollType: Int) {
        if (!surfaceReady || staffs == null) {
            return
        }
        if (bufferCanvas == null) {
            createBufferCanvas()
        }

        /* If the scroll position is not in the bufferCanvas,
         * we need to redraw the sheet music into the bufferCanvas
         */if (!isScrollPositionInBuffer) {
            drawToBuffer(scrollXCoord, scrollYCoord)
        }

        /* We're going to draw the shaded notes into the bufferCanvas.
         * Translate, so that (bufferX, bufferY) maps to (0,0) on the canvas
         */bufferCanvas!!.translate(-bufferX.toFloat(), -bufferY.toFloat())

        /* Loop through each staff.  Each staff will shade any notes that 
         * start at currentPulseTime, and unshade notes at prevPulseTime.
         */
        var x_shade = 0
        var y_shade = 0
        paint!!.isAntiAlias = true
        bufferCanvas!!.scale(zoom, zoom)
        var ypos = 0
        for (staff in staffs!!) {
            bufferCanvas!!.translate(0f, ypos.toFloat())
            x_shade = staff.ShadeNotes((bufferCanvas as Canvas), (paint as Paint), shade1,
                    currentPulseTime, prevPulseTime, x_shade)
            bufferCanvas!!.translate(0f, -ypos.toFloat())
            ypos += staff.height
            if (currentPulseTime >= staff.endTime) {
                y_shade += staff.height
            }
        }
        bufferCanvas!!.scale(1.0f / zoom, 1.0f / zoom)
        bufferCanvas!!.translate(bufferX.toFloat(), bufferY.toFloat())

        /* We have the (x,y) position of the shaded notes.
         * Calculate the new scroll position.
         */if (currentPulseTime >= 0) {
            x_shade = (x_shade * zoom).toInt()
            y_shade -= NoteHeight
            y_shade = (y_shade * zoom).toInt()
            if (scrollType == ImmediateScroll) {
                ScrollToShadedNotes(x_shade, y_shade, false)
            } else if (scrollType == GradualScroll) {
                ScrollToShadedNotes(x_shade, y_shade, true)
            } else if (scrollType == DontScroll) {
            }
        }

        /* If the new scrollX, scrollY is not in the buffer,
         * we have to call this method again.
         */if (scrollXCoord < bufferX || scrollYCoord < bufferY) {
            ShadeNotes(currentPulseTime, prevPulseTime, scrollType)
            return
        }

        /* Draw the buffer canvas to the real canvas.        
         * Translate canvas such that (scrollX,scrollY) within the 
         * bufferCanvas maps to (0,0) on the real canvas.
         */
        val holder = holder
        val canvas = holder.lockCanvas() ?: return
        canvas.translate(-(scrollXCoord - bufferX).toFloat(), -(scrollYCoord - bufferY).toFloat())
        canvas.drawBitmap(bufferBitmap, 0f, 0f, paint)
        canvas.translate((scrollXCoord - bufferX).toFloat(), (scrollYCoord - bufferY).toFloat())
        holder.unlockCanvasAndPost(canvas)
    }

    /** Scroll the sheet music so that the shaded notes are visible.
     * If scrollGradually is true, scroll gradually (smooth scrolling)
     * to the shaded notes. Update the scrollX/scrollY fields.
     */
    fun ScrollToShadedNotes(x_shade: Int, y_shade: Int, scrollGradually: Boolean) {
        if (scrollVert) {
            var scrollDist = y_shade - scrollYCoord
            if (scrollGradually) {
                if (scrollDist > zoom * StaffHeight * 8) scrollDist = scrollDist / 2 else if (scrollDist > NoteHeight * 4 * zoom) scrollDist = (NoteHeight * 4 * zoom).toInt()
            }
            scrollYCoord += scrollDist
        } else {
            val x_view = scrollXCoord + viewwidth * 40 / 100
            val xmax = scrollXCoord + viewwidth * 65 / 100
            var scrollDist = x_shade - x_view
            if (scrollGradually) {
                if (x_shade > xmax) scrollDist = (x_shade - x_view) / 3 else if (x_shade > x_view) scrollDist = (x_shade - x_view) / 6
            }
            scrollXCoord += scrollDist
        }
        checkScrollBounds()
    }

    /** Return the pulseTime corresponding to the given point on the SheetMusic.
     * First, find the staff corresponding to the point.
     * Then, within the staff, find the notes/symbols corresponding to the point,
     * and return the StartTime (pulseTime) of the symbols.
     */
    fun PulseTimeForPoint(point: Point): Int {
        val scaledPoint = Point((point.x / zoom).toInt(), (point.y / zoom).toInt())
        var y = 0
        for (staff in staffs!!) {
            if (scaledPoint.y >= y && scaledPoint.y <= y + staff.height) {
                return staff.PulseTimeForPoint(scaledPoint)
            }
            y += staff.height
        }
        return -1
    }

    /** Check that the scrollX/scrollY position does not exceed
     * the bounds of the sheet music.
     */
    private fun checkScrollBounds() {
        // Get the width/height of the scrollable area
        val scrollwidth = (sheetwidth * zoom).toInt()
        val scrollheight = (sheetheight * zoom).toInt()
        if (scrollXCoord < 0) {
            scrollXCoord = 0
        }
        if (scrollXCoord > scrollwidth - viewwidth / 2) {
            scrollXCoord = scrollwidth - viewwidth / 2
        }
        if (scrollYCoord < 0) {
            scrollYCoord = 0
        }
        if (scrollYCoord > scrollheight - viewheight / 2) {
            scrollYCoord = scrollheight - viewheight / 2
        }
    }

    /** Handle touch/motion events to implement scrolling the sheet music.  */
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val action = event.action and MotionEvent.ACTION_MASK
        val result = scrollAnimation!!.onTouchEvent(event)
        return when (action) {
            MotionEvent.ACTION_DOWN -> {
                // If we touch while music is playing, stop the midi player
                if (player != null && player!!.visibility == GONE && !player!!.isInMidiMode) {
                    player!!.Pause()
                    scrollAnimation!!.stopMotion()
                }
                result
            }
            MotionEvent.ACTION_MOVE -> result
            MotionEvent.ACTION_UP -> result
            else -> false
        }
    }

    /** Update the scroll position. Callback by ScrollAnimation  */
    override fun scrollUpdate(deltaX: Int, deltaY: Int) {
        scrollXCoord += deltaX
        // Only scroll vertically in vertical mode
        if (scrollVert) {
            scrollYCoord += deltaY
        }
        checkScrollBounds()
        draw()
    }

    /** When the scroll is tapped, highlight the position tapped  */
    override fun scrollTapped(x: Int, y: Int) {
        if (player != null) {
            player!!.MoveToClicked(scrollXCoord + x, scrollYCoord + y)
        }
    }

    fun setPlayer(p: MidiPlayer?) {
        player = p
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        draw()
    }

    /** Surface is ready for shading the notes  */
    override fun surfaceCreated(holder: SurfaceHolder) {
        surfaceReady = true
        // Disabling this allows the DrawerLayout to draw over the this view
        setWillNotDraw(false)
    }

    /** Surface has been destroyed  */
    override fun surfaceDestroyed(holder: SurfaceHolder) {
        surfaceReady = false
    }

    fun getCurrentNote(currentTime: Int): MusicSymbol? {
        for (i in staffs!!.indices) {
            val note = staffs!![i].getCurrentNote(currentTime)
            if (note != null) return note
        }
        return null
    }

    override fun toString(): String {
        var result = """
            SheetMusic staffs=${staffs!!.size}
            
            """.trimIndent()
        for (staff in staffs!!) {
            result += staff.toString()
        }
        result += "End SheetMusic\n"
        return result
    }

    companion object {
        /* Measurements used when drawing.  All measurements are in pixels. */
        const val LineWidth = 1

        /** The width of a line  */
        const val LeftMargin = 4

        /** The left margin  */
        const val LineSpace = 7

        /** The space between lines in the staff  */
        const val StaffHeight = LineSpace * 4 + LineWidth * 5

        /** The height between the 5 horizontal lines of the staff  */
        const val NoteHeight = LineSpace + LineWidth

        /** The height of a whole note  */
        const val NoteWidth = 3 * LineSpace / 2

        /** The width of a whole note  */
        const val PageWidth = 800

        /** The width of each page  */
        const val PageHeight = 1050

        /** The height of each page (when printing)  */
        const val TitleHeight = 14

        /** Height of title on first page  */
        const val ImmediateScroll = 1
        const val GradualScroll = 2
        const val DontScroll = 3

        /** Find 2, 3, 4, or 6 chord symbols that occur consecutively (without any
         * rests or bars in between).  There can be BlankSymbols in between.
         *
         * The startIndex is the index in the symbols to start looking from.
         *
         * Store the indexes of the consecutive chords in chordIndexes.
         * Store the horizontal distance (pixels) between the first and last chord.
         * If we failed to find consecutive chords, return false.
         */
        private fun FindConsecutiveChords(symbols: ArrayList<MusicSymbol>, time: TimeSignature?,
                                          startIndex: Int, chordIndexes: IntArray,
                                          horizDistance: BoxedInt): Boolean {
            var i = startIndex
            val numChords = chordIndexes.size
            while (true) {
                horizDistance.value = 0

                /* Find the starting chord */while (i < symbols.size - numChords) {
                    if (symbols[i] is ChordSymbol) {
                        val c = symbols[i] as ChordSymbol
                        if (c.stem != null) {
                            break
                        }
                    }
                    i++
                }
                if (i >= symbols.size - numChords) {
                    chordIndexes[0] = -1
                    return false
                }
                chordIndexes[0] = i
                var foundChords = true
                for (chordIndex in 1 until numChords) {
                    i++
                    val remaining = numChords - 1 - chordIndex
                    while (i < symbols.size - remaining &&
                            symbols[i] is BlankSymbol) {
                        horizDistance.value += symbols[i].width
                        i++
                    }
                    if (i >= symbols.size - remaining) {
                        return false
                    }
                    if (symbols[i] !is ChordSymbol) {
                        foundChords = false
                        break
                    }
                    chordIndexes[chordIndex] = i
                    horizDistance.value += symbols[i].width
                }
                if (foundChords) {
                    return true
                }

                /* Else, start searching again from index i */
            }
        }

        /** Connect chords of the same duration with a horizontal beam.
         * numChords is the number of chords per beam (2, 3, 4, or 6).
         * if startBeat is true, the first chord must start on a quarter note beat.
         */
        private fun CreateBeamedChords(allsymbols: ArrayList<ArrayList<MusicSymbol>>, time: TimeSignature?,
                                       numChords: Int, startBeat: Boolean) {
            val chordIndexes = IntArray(numChords)
            val chords = arrayOfNulls<ChordSymbol>(numChords)
            for (symbols in allsymbols) {
                var startIndex = 0
                while (true) {
                    val horizDistance = BoxedInt()
                    horizDistance.value = 0
                    val found = FindConsecutiveChords(symbols, time,
                            startIndex,
                            chordIndexes,
                            horizDistance)
                    if (!found) {
                        break
                    }
                    for (i in 0 until numChords) {
                        chords[i] = symbols[chordIndexes[i]] as ChordSymbol
                    }
                    startIndex = if (ChordSymbol.CanCreateBeam(chords, time!!, startBeat)) {
                        ChordSymbol.CreateBeam(chords, horizDistance.value)
                        chordIndexes[numChords - 1] + 1
                    } else {
                        chordIndexes[0] + 1
                    }

                    /* What is the value of startIndex here?
                 * If we created a beam, we start after the last chord.
                 * If we failed to create a beam, we start after the first chord.
                 */
                }
            }
        }

        /** Connect chords of the same duration with a horizontal beam.
         *
         * We create beams in the following order:
         * - 6 connected 8th note chords, in 3/4, 6/8, or 6/4 time
         * - Triplets that start on quarter note beats
         * - 3 connected chords that start on quarter note beats (12/8 time only)
         * - 4 connected chords that start on quarter note beats (4/4 or 2/4 time only)
         * - 2 connected chords that start on quarter note beats
         * - 2 connected chords that start on any beat
         */
        private fun CreateAllBeamedChords(allsymbols: ArrayList<ArrayList<MusicSymbol>>, time: TimeSignature?) {
            if (time!!.numerator == 3 && time.denominator == 4 ||
                    time.numerator == 6 && time.denominator == 8 ||
                    time.numerator == 6 && time.denominator == 4) {
                CreateBeamedChords(allsymbols, time, 6, true)
            }
            CreateBeamedChords(allsymbols, time, 3, true)
            CreateBeamedChords(allsymbols, time, 4, true)
            CreateBeamedChords(allsymbols, time, 2, true)
            CreateBeamedChords(allsymbols, time, 2, false)
        }

        /** Get the width (in pixels) needed to display the key signature  */
        @JvmStatic
        fun KeySignatureWidth(key: KeySignature?): Int {
            val clefsym = ClefSymbol(Clef.Treble, 0, false)
            var result = clefsym.minWidth
            val keys = key!!.GetSymbols(Clef.Treble)
            for (symbol in keys) {
                result += symbol!!.minWidth
            }
            return result + LeftMargin + 5
        }

        @JvmStatic
        fun getTextColor(): Int = Color.rgb(70, 70, 70)

        /** Get the lyrics for each track  */
        private fun GetLyrics(tracks: ArrayList<MidiTrack>): ArrayList<ArrayList<LyricSymbol?>?>? {
            var hasLyrics = false
            val result = ArrayList<ArrayList<LyricSymbol?>?>()
            for (tracknum in tracks.indices) {
                val lyrics = ArrayList<LyricSymbol?>()
                result.add(lyrics)
                val track = tracks[tracknum]
                if (track.getLyrics() == null) {
                    continue
                }
                hasLyrics = true
                for (ev in track.getLyrics()!!) {
                    val text = String(ev.Value!!, 0, ev.Value!!.size, StandardCharsets.UTF_8)
                    val sym = LyricSymbol(ev.StartTime, text)
                    lyrics.add(sym)
                }
            }
            return if (!hasLyrics) {
                null
            } else {
                result
            }
        }

        /** Add the lyric symbols to the corresponding staffs  */
        fun AddLyricsToStaffs(staffs: ArrayList<Staff>?, tracklyrics: ArrayList<ArrayList<LyricSymbol?>?>) {
            for (staff in staffs!!) {
                val lyrics = tracklyrics[staff.track]
                staff.AddLyrics(lyrics)
            }
        }
    }

    init {
        val holder = holder
        holder.addCallback(this)
        scrollYCoord = 0
        scrollXCoord = scrollYCoord
        bufferY = scrollXCoord
        bufferX = bufferY
        val activity = context as Activity
        screenwidth = activity.windowManager.defaultDisplay.width
        screenheight = activity.windowManager.defaultDisplay.height
        playerHeight = MidiPlayer.getPreferredSize(screenwidth, screenheight).y
    }
}