/*
 * Copyright (c) 2009-2011 Madhav Vaidyanathan
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
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import java.util.*

/** @class Piano
 *
 * The Piano Control is the panel at the top that displays the
 * piano, and highlights the piano notes during playback.
 * The main methods are:
 *
 * SetMidiFile() - Set the Midi file to use for shading.  The Midi file
 * is needed to determine which notes to shade.
 *
 * ShadeNotes() - Shade notes on the piano that occur at a given pulse time.
 */
class Piano : SurfaceView, SurfaceHolder.Callback {
    /** The x pixles of the black keys  */ /* The colors for drawing black/gray lines */
    private var gray1: Int
    private var gray2: Int
    private var gray3: Int
    private var shade1: Int
    private var shade2: Int
    private var useTwoColors = false

    /** If true, use two colors for highlighting  */
    private var notes: ArrayList<MidiNote>? = null

    /** The Midi notes for shading  */
    private var maxShadeDuration = 0

    /** The maximum duration we'll shade a note for  */
    private var showNoteLetters: Int

    /** Display the letter for each piano note  */
    private var paint: Paint

    /** The paint options for drawing  */
    private var surfaceReady = false

    /** True if we can draw on the surface  */
    private var bufferBitmap: Bitmap? = null

    /** The bitmap for double-buffering  */
    private var bufferCanvas: Canvas? = null

    /** The canvas for double-buffering  */
    private var player: MidiPlayer? = null
    /** Used to pause the player  */
    /** Create a new Piano.  */
    constructor(context: Context?) : super(context) {
        WhiteKeyWidth = 0
        blackKeyOffsets = null
        paint = Paint()
        paint.isAntiAlias = false
        paint.textSize = 9.0f
        gray1 = Color.rgb(16, 16, 16)
        gray2 = Color.rgb(90, 90, 90)
        gray3 = Color.rgb(200, 200, 200)
        shade1 = Color.rgb(210, 205, 220)
        shade2 = Color.rgb(150, 200, 220)
        showNoteLetters = MidiOptions.NoteNameNone
        val holder = holder
        holder.addCallback(this)
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        WhiteKeyWidth = 0
        blackKeyOffsets = null
        paint = Paint()
        paint.textSize = 10.0f
        paint.isAntiAlias = false
        gray1 = Color.rgb(16, 16, 16)
        gray2 = Color.rgb(90, 90, 90)
        gray3 = Color.rgb(200, 200, 200)
        shade1 = Color.rgb(210, 205, 220)
        shade2 = Color.rgb(150, 200, 220)
        showNoteLetters = MidiOptions.NoteNameNone
        val holder = holder
        holder.addCallback(this)
    }

    /** Set the measured width and height  */
    override fun onMeasure(widthspec: Int, heightspec: Int) {
        ScreenWidth = MeasureSpec.getSize(widthspec)
        WhiteKeyWidth = (ScreenWidth / (2.0 + KeysPerOctave * MaxOctave)).toInt()
        if (WhiteKeyWidth % 2 != 0) WhiteKeyWidth--

        // margin = WhiteKeyWidth/2;
        margin = 0
        BlackBorder = WhiteKeyWidth / 2
        WhiteKeyHeight = WhiteKeyWidth * 5
        BlackKeyWidth = WhiteKeyWidth / 2
        BlackKeyHeight = WhiteKeyHeight * 5 / 9
        blackKeyOffsets = intArrayOf(
                WhiteKeyWidth - BlackKeyWidth / 2 - 1,
                WhiteKeyWidth + BlackKeyWidth / 2 - 1,
                2 * WhiteKeyWidth - BlackKeyWidth / 2,
                2 * WhiteKeyWidth + BlackKeyWidth / 2,
                4 * WhiteKeyWidth - BlackKeyWidth / 2 - 1,
                4 * WhiteKeyWidth + BlackKeyWidth / 2 - 1,
                5 * WhiteKeyWidth - BlackKeyWidth / 2,
                5 * WhiteKeyWidth + BlackKeyWidth / 2,
                6 * WhiteKeyWidth - BlackKeyWidth / 2,
                6 * WhiteKeyWidth + BlackKeyWidth / 2
        )

//        int width = margin*2 + BlackBorder*2 + WhiteKeyWidth * KeysPerOctave * MaxOctave;
        val width = ScreenWidth
        val height = margin * 2 + BlackBorder * 3 + WhiteKeyHeight
        setMeasuredDimension(width, height)
        bufferBitmap = Bitmap.createBitmap(ScreenWidth, height, Bitmap.Config.ARGB_8888)
        bufferCanvas = Canvas(bufferBitmap)
        this.invalidate()
        draw()
    }

    override fun onSizeChanged(newwidth: Int, newheight: Int, oldwidth: Int, oldheight: Int) {
        super.onSizeChanged(newwidth, newheight, oldwidth, oldheight)
    }

    /** Set the MidiFile to use.
     * Save the list of midi notes. Each midi note includes the note Number
     * and StartTime (in pulses), so we know which notes to shade given the
     * current pulse time.
     */
    fun SetMidiFile(midifile: MidiFile?, options: MidiOptions,
                    player: MidiPlayer?) {
        if (midifile == null) {
            notes = null
            useTwoColors = false
            return
        }
        this.player = player
        val tracks = midifile.ChangeMidiNotes(options)
        val track = MidiFile.CombineToSingleTrack(tracks)
        notes = track.notes
        maxShadeDuration = midifile.time!!.quarter * 2

        /* We want to know which track the note came from.
         * Use the 'channel' field to store the track.
         */for (tracknum in tracks.indices) {
            for (note in tracks[tracknum].notes) {
                note.channel = tracknum
            }
        }

        /* When we have exactly two tracks, we assume this is a piano song,
         * and we use different colors for highlighting the left hand and
         * right hand notes.
         */useTwoColors = tracks.size == 2
        showNoteLetters = options.showNoteLetters
        this.invalidate()
    }

    /** Set the colors to use for shading  */
    fun SetShadeColors(c1: Int, c2: Int) {
        shade1 = c1
        shade2 = c2
    }

    /** Draw the outline of a 12-note (7 white note) piano octave  */
    private fun DrawOctaveOutline(canvas: Canvas?) {
        val right = WhiteKeyWidth * KeysPerOctave

        // Draw the bounding rectangle, from C to B
        paint.color = gray1
        canvas!!.drawLine(0f, 0f, 0f, WhiteKeyHeight.toFloat(), paint)
        canvas.drawLine(right.toFloat(), 0f, right.toFloat(), WhiteKeyHeight.toFloat(), paint)
        // canvas.drawLine(0, 0, right, 0, paint);
        canvas.drawLine(0f, WhiteKeyHeight.toFloat(), right.toFloat(), WhiteKeyHeight.toFloat(), paint)
        paint.color = gray3
        canvas.drawLine((right - 1).toFloat(), 0f, (right - 1).toFloat(), WhiteKeyHeight.toFloat(), paint)
        canvas.drawLine(1f, 0f, 1f, WhiteKeyHeight.toFloat(), paint)

        // Draw the line between E and F
        paint.color = gray1
        canvas.drawLine((3 * WhiteKeyWidth).toFloat(), 0f, (3 * WhiteKeyWidth).toFloat(), WhiteKeyHeight.toFloat(), paint)
        paint.color = gray3
        canvas.drawLine((3 * WhiteKeyWidth - 1).toFloat(), 0f, (3 * WhiteKeyWidth - 1).toFloat(), WhiteKeyHeight.toFloat(), paint)
        canvas.drawLine((3 * WhiteKeyWidth + 1).toFloat(), 0f, (3 * WhiteKeyWidth + 1).toFloat(), WhiteKeyHeight.toFloat(), paint)

        // Draw the sides/bottom of the black keys
        run {
            var i = 0
            while (i < 10) {
                val x1 = blackKeyOffsets!![i]
                val x2 = blackKeyOffsets!![i + 1]
                paint.color = gray1
                canvas.drawLine(x1.toFloat(), 0f, x1.toFloat(), BlackKeyHeight.toFloat(), paint)
                canvas.drawLine(x2.toFloat(), 0f, x2.toFloat(), BlackKeyHeight.toFloat(), paint)
                canvas.drawLine(x1.toFloat(), BlackKeyHeight.toFloat(), x2.toFloat(), BlackKeyHeight.toFloat(), paint)
                paint.color = gray2
                canvas.drawLine((x1 - 1).toFloat(), 0f, (x1 - 1).toFloat(), (BlackKeyHeight + 1).toFloat(), paint)
                canvas.drawLine((x2 + 1).toFloat(), 0f, (x2 + 1).toFloat(), (BlackKeyHeight + 1).toFloat(), paint)
                canvas.drawLine((x1 - 1).toFloat(), (BlackKeyHeight + 1).toFloat(), (x2 + 1).toFloat(), (BlackKeyHeight + 1).toFloat(), paint)
                paint.color = gray3
                canvas.drawLine((x1 - 2).toFloat(), 0f, (x1 - 2).toFloat(), (BlackKeyHeight + 2).toFloat(), paint)
                canvas.drawLine((x2 + 2).toFloat(), 0f, (x2 + 2).toFloat(), (BlackKeyHeight + 2).toFloat(), paint)
                canvas.drawLine((x1 - 2).toFloat(), (BlackKeyHeight + 2).toFloat(), (x2 + 2).toFloat(), (BlackKeyHeight + 2).toFloat(), paint)
                i += 2
            }
        }

        // Draw the bottom-half of the white keys
        for (i in 1 until KeysPerOctave) {
            if (i == 3) {
                continue  // we draw the line between E and F above
            }
            paint.color = gray1
            canvas.drawLine((i * WhiteKeyWidth).toFloat(), BlackKeyHeight.toFloat(), (
                    i * WhiteKeyWidth).toFloat(), WhiteKeyHeight.toFloat(), paint)
            paint.color = gray2
            canvas.drawLine((i * WhiteKeyWidth - 1).toFloat(), (BlackKeyHeight + 1).toFloat(), (
                    i * WhiteKeyWidth - 1).toFloat(), WhiteKeyHeight.toFloat(), paint)
            paint.color = gray3
            canvas.drawLine((i * WhiteKeyWidth + 1).toFloat(), (BlackKeyHeight + 1).toFloat(), (
                    i * WhiteKeyWidth + 1).toFloat(), WhiteKeyHeight.toFloat(), paint)
        }
    }

    /** Draw an outline of the piano for 6 octaves  */
    private fun DrawOutline(canvas: Canvas?) {
        for (octave in 0 until MaxOctave) {
            canvas!!.translate((octave * WhiteKeyWidth * KeysPerOctave).toFloat(), 0f)
            DrawOctaveOutline(canvas)
            canvas.translate(-(octave * WhiteKeyWidth * KeysPerOctave).toFloat(), 0f)
        }
    }

    /* Draw the Black keys */
    private fun DrawBlackKeys(canvas: Canvas?) {
        paint.style = Paint.Style.FILL
        for (octave in 0 until MaxOctave) {
            canvas!!.translate((octave * WhiteKeyWidth * KeysPerOctave).toFloat(), 0f)
            var i = 0
            while (i < 10) {
                val x1 = blackKeyOffsets!![i]
                val x2 = blackKeyOffsets!![i + 1]
                paint.color = gray1
                canvas.drawRect(x1.toFloat(), 0f, (x1 + BlackKeyWidth).toFloat(), BlackKeyHeight.toFloat(), paint)
                paint.color = gray2
                canvas.drawRect((x1 + 1).toFloat(), (BlackKeyHeight - BlackKeyHeight / 8).toFloat(), (
                        x1 + 1 + BlackKeyWidth - 2).toFloat(), (
                        BlackKeyHeight - BlackKeyHeight / 8 + BlackKeyHeight / 8).toFloat(),
                        paint)
                i += 2
            }
            canvas.translate(-(octave * WhiteKeyWidth * KeysPerOctave).toFloat(), 0f)
        }
        paint.style = Paint.Style.STROKE
    }

    /* Draw the black border area surrounding the piano keys.
     * Also, draw gray outlines at the bottom of the white keys.
     */
    private fun DrawBlackBorder(canvas: Canvas?) {
        val PianoWidth = WhiteKeyWidth * KeysPerOctave * MaxOctave
        paint.style = Paint.Style.FILL
        paint.color = gray1
        canvas!!.drawRect(margin.toFloat(), margin.toFloat(), (margin + PianoWidth + BlackBorder * 2).toFloat(), (
                margin + BlackBorder - 2).toFloat(), paint)
        canvas.drawRect(margin.toFloat(), margin.toFloat(), (margin + BlackBorder).toFloat(), (
                margin + WhiteKeyHeight + BlackBorder * 3).toFloat(), paint)
        canvas.drawRect(margin.toFloat(), (margin + BlackBorder + WhiteKeyHeight).toFloat(), (
                margin + BlackBorder * 2 + PianoWidth).toFloat(), (
                margin + BlackBorder + WhiteKeyHeight + BlackBorder * 2).toFloat(), paint)
        canvas.drawRect((margin + BlackBorder + PianoWidth).toFloat(), margin.toFloat(), (
                margin + BlackBorder + PianoWidth + BlackBorder).toFloat(), (
                margin + WhiteKeyHeight + BlackBorder * 3).toFloat(), paint)
        paint.color = gray2
        canvas.drawLine((margin + BlackBorder).toFloat(), (margin + BlackBorder - 1).toFloat(), (
                margin + BlackBorder + PianoWidth).toFloat(), (
                margin + BlackBorder - 1).toFloat(), paint)
        canvas.translate((margin + BlackBorder).toFloat(), (margin + BlackBorder).toFloat())

        // Draw the gray bottoms of the white keys  
        for (i in 0 until KeysPerOctave * MaxOctave) {
            canvas.drawRect((i * WhiteKeyWidth + 1).toFloat(), (WhiteKeyHeight + 2).toFloat(), (
                    i * WhiteKeyWidth + 1 + WhiteKeyWidth - 2).toFloat(), (
                    WhiteKeyHeight + 2 + BlackBorder / 2).toFloat(), paint)
        }
        canvas.translate(-(margin + BlackBorder).toFloat(), -(margin + BlackBorder).toFloat())
    }

    /** Draw the note letters (A, A#, Bb, etc) underneath each white note  */
    private fun DrawNoteLetters(canvas: Canvas) {
        val letters = arrayOf("C", "D", "E", "F", "G", "A", "B")
        val numbers = arrayOf("1", "3", "5", "6", "8", "10", "12")
        val names: Array<String>
        names = if (showNoteLetters == MidiOptions.NoteNameLetter) {
            letters
        } else if (showNoteLetters == MidiOptions.NoteNameFixedNumber) {
            numbers
        } else {
            return
        }
        canvas.translate((margin + BlackBorder).toFloat(), (margin + BlackBorder).toFloat())
        paint.color = Color.WHITE
        for (octave in 0 until MaxOctave) {
            for (i in 0 until KeysPerOctave) {
                canvas.drawText(names[i], (
                        (octave * KeysPerOctave + i) * WhiteKeyWidth + WhiteKeyWidth / 3).toFloat(), (
                        WhiteKeyHeight + BlackBorder + 4).toFloat(), paint)
            }
        }
        canvas.translate(-(margin + BlackBorder).toFloat(), -(margin + BlackBorder).toFloat())
        paint.color = Color.BLACK
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

    /** Draw the Piano.  */
    fun doDraw(canvas: Canvas) {
        if (!surfaceReady || bufferBitmap == null) {
            return
        }
        if (WhiteKeyWidth == 0) {
            return
        }
        paint.isAntiAlias = false
        paint.style = Paint.Style.FILL

        // Draw the black frame of the piano
        val height = margin * 2 + BlackBorder * 3 + WhiteKeyHeight
        paint.color = gray1
        bufferCanvas!!.drawRect(0f, 0f, ScreenWidth.toFloat(), height.toFloat(), paint)
        bufferCanvas!!.translate((margin + BlackBorder).toFloat(), (margin + BlackBorder).toFloat())
        paint.color = Color.WHITE
        bufferCanvas!!.drawRect(0f, 0f, (WhiteKeyWidth * KeysPerOctave * MaxOctave).toFloat(),
                WhiteKeyHeight.toFloat(), paint)
        paint.style = Paint.Style.STROKE
        paint.color = gray1
        DrawBlackKeys(bufferCanvas)
        DrawOutline(bufferCanvas)
        bufferCanvas!!.translate(-(margin + BlackBorder).toFloat(), -(margin + BlackBorder).toFloat())
        DrawBlackBorder(bufferCanvas)
        canvas.drawBitmap(bufferBitmap, 0f, 0f, paint)
        if (showNoteLetters != MidiOptions.NoteNameNone) {
            DrawNoteLetters(canvas)
        }
    }

    fun ShadeOneNote(noteNumber: Int, color: Int) {
        val holder = holder
        val canvas = holder.lockCanvas() ?: return
        bufferCanvas!!.translate((margin + BlackBorder).toFloat(), (margin + BlackBorder).toFloat())
        ShadeOneNote(bufferCanvas, noteNumber, color)
        bufferCanvas!!.translate(-(margin + BlackBorder).toFloat(), -(margin + BlackBorder).toFloat())
        canvas.drawBitmap(bufferBitmap, 0f, 0f, paint)
        holder.unlockCanvasAndPost(canvas)
    }

    /* Shade the given note with the given brush.
     * We only draw notes from notenumber 24 to 96.
     * (Middle-C is 60).
     */
    private fun ShadeOneNote(canvas: Canvas?, notenumber: Int, color: Int) {
        var octave = notenumber / 12
        val notescale = notenumber % 12
        octave -= 2
        if (octave < 0 || octave >= MaxOctave) return
        paint.color = color
        paint.style = Paint.Style.FILL
        canvas!!.translate((octave * WhiteKeyWidth * KeysPerOctave).toFloat(), 0f)
        val x1: Int
        val x2: Int
        val x3: Int
        val bottomHalfHeight = WhiteKeyHeight - (BlackKeyHeight + 3)
        when (notescale) {
            0 -> {
                x1 = 2
                x2 = blackKeyOffsets!![0] - 2
                canvas.drawRect(x1.toFloat(), 0f, (x1 + x2 - x1).toFloat(), (0 + BlackKeyHeight + 3).toFloat(), paint)
                canvas.drawRect(x1.toFloat(), (BlackKeyHeight + 3).toFloat(), (x1 + WhiteKeyWidth - 3).toFloat(), (
                        BlackKeyHeight + 3 + bottomHalfHeight).toFloat(), paint)
            }
            1 -> {
                x1 = blackKeyOffsets!![0]
                x2 = blackKeyOffsets!![1]
                canvas.drawRect(x1.toFloat(), 0f, (x1 + x2 - x1).toFloat(), (0 + BlackKeyHeight).toFloat(), paint)
                if (color == gray1) {
                    paint.color = gray2
                    canvas.drawRect((x1 + 1).toFloat(), (BlackKeyHeight - BlackKeyHeight / 8).toFloat(), (
                            x1 + 1 + BlackKeyWidth - 2).toFloat(), (
                            BlackKeyHeight - BlackKeyHeight / 8 + BlackKeyHeight / 8).toFloat(),
                            paint)
                }
            }
            2 -> {
                x1 = WhiteKeyWidth + 2
                x2 = blackKeyOffsets!![1] + 3
                x3 = blackKeyOffsets!![2] - 2
                canvas.drawRect(x2.toFloat(), 0f, (x2 + x3 - x2).toFloat(), (0 + BlackKeyHeight + 3).toFloat(), paint)
                canvas.drawRect(x1.toFloat(), (BlackKeyHeight + 3).toFloat(), (x1 + WhiteKeyWidth - 3).toFloat(), (
                        BlackKeyHeight + 3 + bottomHalfHeight).toFloat(), paint)
            }
            3 -> {
                x1 = blackKeyOffsets!![2]
                x2 = blackKeyOffsets!![3]
                canvas.drawRect(x1.toFloat(), 0f, (x1 + BlackKeyWidth).toFloat(), (0 + BlackKeyHeight).toFloat(), paint)
                if (color == gray1) {
                    paint.color = gray2
                    canvas.drawRect((x1 + 1).toFloat(), (BlackKeyHeight - BlackKeyHeight / 8).toFloat(), (
                            x1 + 1 + BlackKeyWidth - 2).toFloat(), (
                            BlackKeyHeight - BlackKeyHeight / 8 + BlackKeyHeight / 8).toFloat(),
                            paint)
                }
            }
            4 -> {
                x1 = WhiteKeyWidth * 2 + 2
                x2 = blackKeyOffsets!![3] + 3
                x3 = WhiteKeyWidth * 3 - 1
                canvas.drawRect(x2.toFloat(), 0f, (x2 + x3 - x2).toFloat(), (0 + BlackKeyHeight + 3).toFloat(), paint)
                canvas.drawRect(x1.toFloat(), (BlackKeyHeight + 3).toFloat(), (x1 + WhiteKeyWidth - 3).toFloat(), (
                        BlackKeyHeight + 3 + bottomHalfHeight).toFloat(), paint)
            }
            5 -> {
                x1 = WhiteKeyWidth * 3 + 2
                x2 = blackKeyOffsets!![4] - 2
                x3 = WhiteKeyWidth * 4 - 2
                canvas.drawRect(x1.toFloat(), 0f, (x1 + x2 - x1).toFloat(), (0 + BlackKeyHeight + 3).toFloat(), paint)
                canvas.drawRect(x1.toFloat(), (BlackKeyHeight + 3).toFloat(), (x1 + WhiteKeyWidth - 3).toFloat(), (
                        BlackKeyHeight + 3 + bottomHalfHeight).toFloat(), paint)
            }
            6 -> {
                x1 = blackKeyOffsets!![4]
                x2 = blackKeyOffsets!![5]
                canvas.drawRect(x1.toFloat(), 0f, (x1 + BlackKeyWidth).toFloat(), (0 + BlackKeyHeight).toFloat(), paint)
                if (color == gray1) {
                    paint.color = gray2
                    canvas.drawRect((x1 + 1).toFloat(), (BlackKeyHeight - BlackKeyHeight / 8).toFloat(), (
                            x1 + 1 + BlackKeyWidth - 2).toFloat(), (
                            BlackKeyHeight - BlackKeyHeight / 8 + BlackKeyHeight / 8).toFloat(),
                            paint)
                }
            }
            7 -> {
                x1 = WhiteKeyWidth * 4 + 2
                x2 = blackKeyOffsets!![5] + 3
                x3 = blackKeyOffsets!![6] - 2
                canvas.drawRect(x2.toFloat(), 0f, (x2 + x3 - x2).toFloat(), (0 + BlackKeyHeight + 3).toFloat(), paint)
                canvas.drawRect(x1.toFloat(), (BlackKeyHeight + 3).toFloat(), (x1 + WhiteKeyWidth - 3).toFloat(), (
                        BlackKeyHeight + 3 + bottomHalfHeight).toFloat(), paint)
            }
            8 -> {
                x1 = blackKeyOffsets!![6]
                x2 = blackKeyOffsets!![7]
                canvas.drawRect(x1.toFloat(), 0f, (x1 + BlackKeyWidth).toFloat(), (0 + BlackKeyHeight).toFloat(), paint)
                if (color == gray1) {
                    paint.color = gray2
                    canvas.drawRect((x1 + 1).toFloat(), (BlackKeyHeight - BlackKeyHeight / 8).toFloat(), (
                            x1 + 1 + BlackKeyWidth - 2).toFloat(), (
                            BlackKeyHeight - BlackKeyHeight / 8 + BlackKeyHeight / 8).toFloat(),
                            paint)
                }
            }
            9 -> {
                x1 = WhiteKeyWidth * 5 + 2
                x2 = blackKeyOffsets!![7] + 3
                x3 = blackKeyOffsets!![8] - 2
                canvas.drawRect(x2.toFloat(), 0f, (x2 + x3 - x2).toFloat(), (0 + BlackKeyHeight + 3).toFloat(), paint)
                canvas.drawRect(x1.toFloat(), (BlackKeyHeight + 3).toFloat(), (x1 + WhiteKeyWidth - 3).toFloat(), (
                        BlackKeyHeight + 3 + bottomHalfHeight).toFloat(), paint)
            }
            10 -> {
                x1 = blackKeyOffsets!![8]
                x2 = blackKeyOffsets!![9]
                canvas.drawRect(x1.toFloat(), 0f, (x1 + BlackKeyWidth).toFloat(), (0 + BlackKeyHeight).toFloat(), paint)
                if (color == gray1) {
                    paint.color = gray2
                    canvas.drawRect((x1 + 1).toFloat(), (BlackKeyHeight - BlackKeyHeight / 8).toFloat(), (
                            x1 + 1 + BlackKeyWidth - 2).toFloat(), (
                            BlackKeyHeight - BlackKeyHeight / 8 + BlackKeyHeight / 8).toFloat(),
                            paint)
                }
            }
            11 -> {
                x1 = WhiteKeyWidth * 6 + 2
                x2 = blackKeyOffsets!![9] + 3
                x3 = WhiteKeyWidth * KeysPerOctave - 1
                canvas.drawRect(x2.toFloat(), 0f, (x2 + x3 - x2).toFloat(), (0 + BlackKeyHeight + 3).toFloat(), paint)
                canvas.drawRect(x1.toFloat(), (BlackKeyHeight + 3).toFloat(), (x1 + WhiteKeyWidth - 3).toFloat(), (
                        BlackKeyHeight + 3 + bottomHalfHeight).toFloat(), paint)
            }
            else -> {
            }
        }
        canvas.translate(-(octave * WhiteKeyWidth * KeysPerOctave).toFloat(), 0f)
    }

    /** Find the MidiNote with the startTime closest to the given time.
     * Return the index of the note.  Use a binary search method.
     */
    private fun FindClosestStartTime(pulseTime: Int): Int {
        var left = 0
        var right = notes!!.size - 1
        while (right - left > 1) {
            val i = (right + left) / 2
            if (notes!![left].startTime == pulseTime) break else if (notes!![i].startTime <= pulseTime) left = i else right = i
        }
        while (left >= 1 &&
                notes!![left - 1].startTime == notes!![left].startTime) {
            left--
        }
        return left
    }

    /** Return the next StartTime that occurs after the MidiNote
     * at offset i, that is also in the same track/channel.
     */
    private fun NextStartTimeSameTrack(i: Int): Int {
        var i = i
        val start = notes!![i].startTime
        var end = notes!![i].endTime
        val track = notes!![i].channel
        while (i < notes!!.size) {
            if (notes!![i].channel != track) {
                i++
                continue
            }
            if (notes!![i].startTime > start) {
                return notes!![i].startTime
            }
            end = Math.max(end, notes!![i].endTime)
            i++
        }
        return end
    }

    /** Return the next StartTime that occurs after the MidiNote
     * at offset i.  If all the subsequent notes have the same
     * StartTime, then return the largest EndTime.
     */
    private fun NextStartTime(i: Int): Int {
        var i = i
        val start = notes!![i].startTime
        var end = notes!![i].endTime
        while (i < notes!!.size) {
            if (notes!![i].startTime > start) {
                return notes!![i].startTime
            }
            end = Math.max(end, notes!![i].endTime)
            i++
        }
        return end
    }

    /** Find the Midi notes that occur in the current time.
     * Shade those notes on the piano displayed.
     * Un-shade the those notes played in the previous time.
     */
    fun ShadeNotes(currentPulseTime: Int, prevPulseTime: Int) {
        if (notes == null || notes!!.size == 0 || !surfaceReady || bufferBitmap == null) {
            return
        }
        val holder = holder
        val canvas = holder.lockCanvas() ?: return
        bufferCanvas!!.translate((margin + BlackBorder).toFloat(), (margin + BlackBorder).toFloat())

        /* Loop through the Midi notes.
         * Unshade notes where StartTime <= prevPulseTime < next StartTime
         * Shade notes where StartTime <= currentPulseTime < next StartTime
         */
        val lastShadedIndex = FindClosestStartTime(prevPulseTime - maxShadeDuration * 2)
        for (i in lastShadedIndex until notes!!.size) {
            val start = notes!![i].startTime
            var end = notes!![i].endTime
            val notenumber = notes!![i].number
            val nextStart = NextStartTime(i)
            val nextStartTrack = NextStartTimeSameTrack(i)
            end = Math.max(end, nextStartTrack)
            end = Math.min(end, start + maxShadeDuration - 1)

            /* If we've past the previous and current times, we're done. */if (start > prevPulseTime && start > currentPulseTime) {
                break
            }

            /* If shaded notes are the same, we're done */if (start <= currentPulseTime && currentPulseTime < nextStart &&
                    currentPulseTime < end &&
                    start <= prevPulseTime && prevPulseTime < nextStart &&
                    prevPulseTime < end) {
                break
            }

            /* If the note is in the current time, shade it */if (start <= currentPulseTime && currentPulseTime < end) {
                if (useTwoColors) {
                    if (notes!![i].channel == 1) {
                        ShadeOneNote(bufferCanvas, notenumber, shade2)
                    } else {
                        ShadeOneNote(bufferCanvas, notenumber, shade1)
                    }
                } else {
                    ShadeOneNote(bufferCanvas, notenumber, shade1)
                }
            } else if (start <= prevPulseTime && prevPulseTime < end) {
                val num = notenumber % 12
                if (num == 1 || num == 3 || num == 6 || num == 8 || num == 10) {
                    ShadeOneNote(bufferCanvas, notenumber, gray1)
                } else {
                    ShadeOneNote(bufferCanvas, notenumber, Color.WHITE)
                }
            }
        }
        bufferCanvas!!.translate(-(margin + BlackBorder).toFloat(), -(margin + BlackBorder).toFloat())
        canvas.drawBitmap(bufferBitmap, 0f, 0f, paint)
        holder.unlockCanvasAndPost(canvas)
    }

    /** TODO ??  */
    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        draw()
    }

    /** Surface is ready for shading the notes  */
    override fun surfaceCreated(holder: SurfaceHolder) {
        surfaceReady = true
        // Disabling this allows the DrawerLayout to draw over the this view
        setWillNotDraw(false)
        draw()
    }

    /** Surface has been destroyed  */
    override fun surfaceDestroyed(holder: SurfaceHolder) {
        surfaceReady = false
    }

    /** When the Piano is touched, pause the midi player  */
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            if (player != null) {
                player!!.Pause()
            }
        }
        return true
    }

    fun UnShadeOneNote(notenumber: Int) {
        val holder = holder
        val canvas = holder.lockCanvas() ?: return
        bufferCanvas!!.translate((margin + BlackBorder).toFloat(), (margin + BlackBorder).toFloat())
        val num = notenumber % 12
        if (num == 1 || num == 3 || num == 6 || num == 8 || num == 10) {
            ShadeOneNote(bufferCanvas, notenumber, gray1)
        } else {
            ShadeOneNote(bufferCanvas, notenumber, Color.WHITE)
        }
        bufferCanvas!!.translate(-(margin + BlackBorder).toFloat(), -(margin + BlackBorder).toFloat())
        canvas.drawBitmap(bufferBitmap, 0f, 0f, paint)
        holder.unlockCanvasAndPost(canvas)
    }

    companion object {
        const val KeysPerOctave = 7
        const val MaxOctave = 6
        private var WhiteKeyWidth: Int = 0

        /** Width of a single white key  */
        private var WhiteKeyHeight = 0

        /** Height of a single white key  */
        private var BlackKeyWidth = 0

        /** Width of a single black key  */
        private var BlackKeyHeight = 0

        /** Height of a single black key  */
        private var margin = 0

        /** The top/left margin to the piano (0)  */
        private var BlackBorder = 0
        /** The width of the black border around the keys  */
        /** The width of the device screen  */
        private var ScreenWidth = 0
        private var blackKeyOffsets: IntArray? = null

        /** Get the preferred width/height, given the screen width/height  */
        @JvmStatic
        fun getPreferredSize(screenwidth: Int, screenheight: Int): Point {
            var keywidth = (screenwidth / (2.0 + KeysPerOctave * MaxOctave)).toInt()
            if (keywidth % 2 != 0) {
                keywidth--
            }
            //int margin = keywidth/2;
            val margin = 0
            val border = keywidth / 2
            val result = Point()
            result.x = margin * 2 + border * 2 + keywidth * KeysPerOctave * MaxOctave
            result.y = margin * 2 + border * 3 + WhiteKeyHeight
            return result
        }
    }
}