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
import android.graphics.Color
import android.graphics.Point
import android.graphics.PorterDuff
import android.media.MediaPlayer
import android.os.Handler
import android.os.SystemClock
import android.util.AttributeSet
import android.view.View
import android.view.View.OnClickListener
import android.widget.*
import android.widget.SeekBar.OnSeekBarChangeListener
import com.midisheetmusic.sheets.ChordSymbol
import com.mikepenz.materialdrawer.Drawer
import java.io.IOException
import java.util.*

/**
 * The MidiPlayer is the panel at the top used to play the sound
 * of the midi file. It consists of:
 *
 *  *  The Rewind button
 *  *  The Play/Pause button
 *  *  The Stop button
 *  *  The Fast Forward button
 *  *  The Playback speed bar
 *
 *
 * The sound of the midi file depends on
 *
 *  *  The MidiOptions (taken from the menus)
 *  *  Which tracks are selected
 *  *  How much to transpose the keys by
 *  *  What instruments to use per track
 *  *  The tempo (from the Speed bar)
 *  *  The volume
 *
 *
 * The MidiFile.ChangeSound() method is used to create a new midi file
 * with these options.  The mciSendString() function is used for
 * playing, pausing, and stopping the sound.
 * <br></br><br></br>
 * For shading the notes during playback, the method
 * `SheetMusic.ShadeNotes()` is used.  It takes the current 'pulse time',
 * and determines which notes to shade.
 */
class MidiPlayer : LinearLayout {
    private var midiButton: Button? = null
    private var leftHandButton: Button? = null
    private var rightHandButton: Button? = null
    private var pianoButton: ImageButton? = null

    /** The "Speed %" label  */
    private var speedText: TextView? = null

    /** The seekbar for controlling playback speed  */
    private var speedBar: SeekBar? = null
    private var drawer: Drawer? = null
    var playstate = 0

    /** The playing state of the Midi Player  */
    val stopped = 1

    /** Currently stopped  */
    val playing = 2

    /** Currently playing music  */
    val paused = 3

    /** Currently paused  */
    val initStop = 4

    /** Transitioning from playing to stop  */
    val initPause = 5

    /** Transitioning from playing to pause  */
    val midi = 6
    val tempSoundFile = "playing.mid"
    /** The filename to play sound from  */
    /** For playing the audio  */
    var player: MediaPlayer? = null

    /** The midi file to play  */
    var midifile: MidiFile? = null

    /** The sound options for playing the midi file  */
    var options: MidiOptions? = null

    /** The sheet music to shade while playing  */
    var sheet: SheetMusic? = null

    /** The piano to shade while playing  */
    var piano: Piano? = null

    /** Timer used to update the sheet music while playing  */
    var timer: Handler? = null

    /** Absolute time when music started playing (msec)  */
    var startTime: Long = 0

    /** The number of pulses per millisec  */
    var pulsesPerMsec = 0.0

    /** Time (in pulses) when music started playing  */
    var startPulseTime = 0.0

    /** Time (in pulses) music is currently at  */
    var currentPulseTime = 0.0

    /** Time (in pulses) music was last at  */
    var prevPulseTime = 0.0

    /** The parent activity.  */
    var activity: Activity? = null

    /** A listener that allows us to send a request to update the sheet when needed  */
    private var mSheetUpdateRequestListener: SheetUpdateRequestListener? = null
    fun setSheetUpdateRequestListener(listener: SheetUpdateRequestListener?) {
        mSheetUpdateRequestListener = listener
    }

    /** Create a new MidiPlayer, displaying the play/stop buttons, and the
     * speed bar.  The midifile and sheetmusic are initially null.
     */
    constructor(activity: Activity?) : super(activity) {
        this.activity = activity
        midifile = null
        options = null
        sheet = null
        playstate = stopped
        startTime = SystemClock.uptimeMillis()
        startPulseTime = 0.0
        currentPulseTime = 0.0
        prevPulseTime = -10.0
        init()
        player = MediaPlayer()
        setBackgroundColor(Color.BLACK)
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle) {
        init()
    }

    fun OnMidiDeviceStatus(connected: Boolean) {
        midiButton!!.isEnabled = connected
        midiButton!!.setTextColor(if (connected) Color.BLUE else Color.RED)
    }

    var prevWrongMidi = 0
    fun OnMidiNote(note: Int, pressed: Boolean) {
        var note = note
        if (!pressed) return
        val nextNote = sheet!!.getCurrentNote(currentPulseTime.toInt())
        val midiNote: Int = ((nextNote as ChordSymbol).notedata[0] as NoteData).number
        note += options!!.midiShift
        if (midiNote != note) {
            piano!!.UnShadeOneNote(prevWrongMidi)
            piano!!.ShadeOneNote(note, Color.RED)
            prevWrongMidi = note
        } else {
            prevPulseTime = currentPulseTime
            currentPulseTime = sheet!!.getCurrentNote(nextNote.startTime + 1)!!.startTime.toDouble()
            piano!!.UnShadeOneNote(prevWrongMidi)
        }
        sheet!!.ShadeNotes(currentPulseTime.toInt(), prevPulseTime.toInt(), SheetMusic.ImmediateScroll)
        piano!!.ShadeNotes(currentPulseTime.toInt(), prevPulseTime.toInt())
    }

    /** Create the rewind, play, stop, and fast forward buttons  */
    fun init() {
        inflate(activity, R.layout.player_toolbar, this)
        val backButton = findViewById<ImageButton>(R.id.btn_back)
        val rewindButton = findViewById<ImageButton>(R.id.btn_rewind)
        val resetButton = findViewById<ImageButton>(R.id.btn_replay)
        val playButton = findViewById<ImageButton>(R.id.btn_play)
        val fastFwdButton = findViewById<ImageButton>(R.id.btn_forward)
        val settingsButton = findViewById<ImageButton>(R.id.btn_settings)
        leftHandButton = findViewById(R.id.btn_left)
        rightHandButton = findViewById(R.id.btn_right)
        midiButton = findViewById(R.id.btn_midi)
        pianoButton = findViewById(R.id.btn_piano)
        speedText = findViewById(R.id.txt_speed)
        speedBar = findViewById(R.id.speed_bar)
        backButton.setOnClickListener { v: View? -> activity!!.onBackPressed() }
        rewindButton.setOnClickListener { v: View? -> Rewind() }
        resetButton.setOnClickListener { v: View? -> Reset() }
        playButton.setOnClickListener { v: View? -> Play() }
        fastFwdButton.setOnClickListener { v: View? -> FastForward() }
        settingsButton.setOnClickListener { v: View? ->
            drawer!!.deselect()
            drawer!!.openDrawer()
        }
        (midiButton as Button).setOnClickListener { toggleMidi() }
        (leftHandButton as Button).setOnClickListener { toggleTrack(LEFT_TRACK) }
        (rightHandButton as Button).setOnClickListener { toggleTrack(RIGHT_TRACK) }
        (pianoButton as ImageButton).setOnClickListener({ togglePiano() })

        // Resize the speedBar so all toolbar icons fit on the screen
        (speedBar as SeekBar).post(
                Runnable {
                    val iconsWidth = backButton.width + resetButton.width + playButton.width +
                            rewindButton.width + fastFwdButton.width + (midiButton as Button).getWidth() +
                            (leftHandButton as Button).getWidth() + (rightHandButton as Button).getWidth() + (pianoButton as ImageButton).getWidth() +
                            settingsButton.width
                    val screenwidth = activity!!.windowManager.defaultDisplay.width
                    (speedBar as SeekBar).setLayoutParams(
                            LayoutParams(screenwidth - iconsWidth - 16, (speedBar as SeekBar).getHeight()))
                }
        )
        (speedBar as SeekBar).getProgressDrawable().setColorFilter(Color.parseColor("#00BB87"), PorterDuff.Mode.SRC_IN)
        (speedBar as SeekBar).setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(bar: SeekBar, progress: Int, fromUser: Boolean) {
                // If speed bar is between 97 and 103 approximate it to 100
                var progress = progress
                if (97 < progress && progress < 103) {
                    progress = 100
                    bar.progress = progress
                }
                (speedText as TextView).setText(String.format(Locale.US, "%3d", progress) + "%")
            }

            override fun onStartTrackingTouch(bar: SeekBar) {}
            override fun onStopTrackingTouch(bar: SeekBar) {}
        })

        /* Initialize the timer used for playback, but don't start
         * the timer yet (enabled = false).
         */timer = Handler()
    }

    private fun toggleMidi() {
        sheet!!.ShadeNotes(-10, prevPulseTime.toInt(), SheetMusic.DontScroll)
        sheet!!.ShadeNotes(-10, currentPulseTime.toInt(), SheetMusic.DontScroll)
        piano!!.ShadeNotes(-10, prevPulseTime.toInt())
        piano!!.ShadeNotes(-10, currentPulseTime.toInt())
        piano!!.UnShadeOneNote(prevWrongMidi)
        if (playstate != midi) {
            playstate = midi
            currentPulseTime = 0.0
            prevPulseTime = 0.0
        } else {
            playstate = paused
        }
        this.visibility = GONE
        timer!!.removeCallbacks(TimerCallback)
        timer!!.postDelayed(ReShade, 500)
    }

    /** Show/hide treble and bass clefs  */
    private fun toggleTrack(track: Int) {
        if (track < options!!.tracks.size) {
            options!!.tracks[track] = !options!!.tracks[track]
            options!!.mute[track] = !options!!.tracks[track]
            if (mSheetUpdateRequestListener != null) {
                mSheetUpdateRequestListener!!.onSheetUpdateRequest()
            }
            updateToolbarButtons()
        }
    }

    /** Show/hide the piano  */
    private fun togglePiano() {
        options!!.showPiano = !options!!.showPiano
        piano!!.visibility = if (options!!.showPiano) VISIBLE else GONE
        updateToolbarButtons()
    }

    /** Update the status of the toolbar buttons (show, hide, opacity, etc.)  */
    fun updateToolbarButtons() {
        pianoButton!!.alpha = if (options!!.showPiano) 1.0.toFloat() else 0.5.toFloat()
        var leftAlpha = 0.5.toFloat()
        var rightAlpha = 0.5.toFloat()
        if (LEFT_TRACK < options!!.tracks.size) {
            leftAlpha = if (options!!.tracks[LEFT_TRACK]) 1.0.toFloat() else 0.5.toFloat()
        }
        if (RIGHT_TRACK < options!!.tracks.size) {
            rightAlpha = if (options!!.tracks[RIGHT_TRACK]) 1.0.toFloat() else 0.5.toFloat()
        }
        leftHandButton!!.visibility = if (LEFT_TRACK < options!!.tracks.size) VISIBLE else GONE
        rightHandButton!!.visibility = if (RIGHT_TRACK < options!!.tracks.size) VISIBLE else GONE
        leftHandButton!!.alpha = leftAlpha
        rightHandButton!!.alpha = rightAlpha
    }

    /** Determine the measured width and height.
     * Resize the individual buttons according to the new width/height.
     */
    override fun onMeasure(widthspec: Int, heightspec: Int) {
        super.onMeasure(widthspec, heightspec)
        val screenwidth = MeasureSpec.getSize(widthspec)
        /* Make the button height 2/3 the piano WhiteKeyHeight */
        var height = (5.0 * screenwidth / (2 + Piano.KeysPerOctave * Piano.MaxOctave)).toInt()
        height = height * 2 / 3
        setMeasuredDimension(screenwidth, height)
    }

    fun SetPiano(p: Piano?) {
        piano = p
    }

    /** The MidiFile and/or SheetMusic has changed. Stop any playback sound,
     * and store the current midifile and sheet music.
     */
    fun SetMidiFile(file: MidiFile, opt: MidiOptions?, s: SheetMusic?) {

        /* If we're paused, and using the same midi file, redraw the
         * highlighted notes.
         */
        if (file === midifile && midifile != null && playstate == paused) {
            options = opt
            sheet = s
            sheet!!.ShadeNotes(currentPulseTime.toInt(), -1, SheetMusic.DontScroll)

            /* We have to wait some time (200 msec) for the sheet music
             * to scroll and redraw, before we can re-shade.
             */timer!!.removeCallbacks(TimerCallback)
            timer!!.postDelayed(ReShade, 500)
        } else {
            Reset()
            midifile = file
            options = opt
            sheet = s
            ScrollToStart()
        }
    }

    /** If we're paused, reshade the sheet music and piano.  */
    var ReShade = Runnable {
        if (playstate == paused || playstate == stopped) {
            sheet!!.ShadeNotes(currentPulseTime.toInt(), -10, SheetMusic.ImmediateScroll)
            piano!!.ShadeNotes(currentPulseTime.toInt(), prevPulseTime.toInt())
        }
    }

    /** Return the number of tracks selected in the MidiOptions.
     * If the number of tracks is 0, there is no sound to play.
     */
    private fun numberTracks(): Int {
        var count = 0
        for (i in options!!.tracks.indices) {
            if (options!!.tracks[i] && !options!!.mute[i]) {
                count += 1
            }
        }
        return count
    }

    /** Create a new midi file with all the MidiOptions incorporated.
     * Save the new file to playing.mid, and store
     * this temporary filename in tempSoundFile.
     */
    private fun CreateMidiFile() {
        val inverse_tempo = 1.0 / midifile!!.time!!.tempo
        val inverse_tempo_scaled = inverse_tempo * speedBar!!.progress / 100.0
        // double inverse_tempo_scaled = inverse_tempo * 100.0 / 100.0;
        options!!.tempo = (1.0 / inverse_tempo_scaled).toInt()
        pulsesPerMsec = midifile!!.time!!.quarter * (1000.0 / options!!.tempo)
        try {
            val dest = activity!!.openFileOutput(tempSoundFile, Context.MODE_PRIVATE)
            midifile!!.ChangeSound(dest, options)
            dest.close()
            // checkFile(tempSoundFile);
        } catch (e: IOException) {
            val toast = Toast.makeText(activity, "Error: Unable to create MIDI file for playing.", Toast.LENGTH_LONG)
            toast.show()
        }
    }

    private fun checkFile(name: String) {
        try {
            var `in` = activity!!.openFileInput(name)
            var data = ByteArray(4096)
            var total = 0
            var len = 0
            while (true) {
                len = `in`.read(data, 0, 4096)
                total += if (len > 0) len else break
            }
            `in`.close()
            data = ByteArray(total)
            `in` = activity!!.openFileInput(name)
            var offset = 0
            while (offset < total) {
                len = `in`.read(data, offset, total - offset)
                if (len > 0) offset += len
            }
            `in`.close()
        } catch (e: IOException) {
            val toast = Toast.makeText(activity, "CheckFile: $e", Toast.LENGTH_LONG)
            toast.show()
        } catch (e: MidiFileException) {
            val toast = Toast.makeText(activity, "CheckFile midi: $e", Toast.LENGTH_LONG)
            toast.show()
        }
    }

    /** Play the sound for the given MIDI file  */
    private fun PlaySound(filename: String) {
        if (player == null) return
        try {
            val input = activity!!.openFileInput(filename)
            player!!.reset()
            player!!.setDataSource(input.fd)
            input.close()
            player!!.prepare()
            player!!.start()
        } catch (e: IOException) {
            val toast = Toast.makeText(activity, "Error: Unable to play MIDI sound", Toast.LENGTH_LONG)
            toast.show()
        }
    }

    /** Stop playing the MIDI music  */
    private fun StopSound() {
        if (player == null) return
        player!!.stop()
        player!!.reset()
    }

    /** The callback for the play button.
     * If we're stopped or pause, then play the midi file.
     */
    private fun Play() {
        if (midifile == null || sheet == null || numberTracks() == 0) {
            return
        } else if (playstate == initStop || playstate == initPause || playstate == playing) {
            return
        }
        // playstate is stopped or paused

        // Hide the midi player, wait a little for the view
        // to refresh, and then start playing
        this.visibility = GONE
        RemoveShading()
        timer!!.removeCallbacks(TimerCallback)
        timer!!.postDelayed(DoPlay, 1000)
    }

    var DoPlay = Runnable { /* The startPulseTime is the pulse time of the midi file when
       * we first start playing the music.  It's used during shading.
       */
        if (options!!.playMeasuresInLoop) {
            /* If we're playing measures in a loop, make sure the
           * currentPulseTime is somewhere inside the loop measures.
           */
            val measure = (currentPulseTime / midifile!!.time!!.measure).toInt()
            if (measure < options!!.playMeasuresInLoopStart ||
                    measure > options!!.playMeasuresInLoopEnd) {
                currentPulseTime = (options!!.playMeasuresInLoopStart * midifile!!.time!!.measure).toDouble()
            }
            startPulseTime = currentPulseTime
            options!!.pauseTime = (currentPulseTime - options!!.shifttime).toInt()
        } else if (playstate == paused) {
            startPulseTime = currentPulseTime
            options!!.pauseTime = (currentPulseTime - options!!.shifttime).toInt()
        } else {
            options!!.pauseTime = 0
            startPulseTime = options!!.shifttime.toDouble()
            currentPulseTime = options!!.shifttime.toDouble()
            prevPulseTime = (options!!.shifttime - midifile!!.time!!.quarter).toDouble()
        }
        CreateMidiFile()
        playstate = playing
        PlaySound(tempSoundFile)
        startTime = SystemClock.uptimeMillis()
        timer!!.removeCallbacks(TimerCallback)
        timer!!.removeCallbacks(ReShade)
        timer!!.postDelayed(TimerCallback, 100)
        sheet!!.ShadeNotes(currentPulseTime.toInt(), prevPulseTime.toInt(), SheetMusic.GradualScroll)
        piano!!.ShadeNotes(currentPulseTime.toInt(), prevPulseTime.toInt())
    }

    /** The callback for pausing playback.
     * If we're currently playing, pause the music.
     * The actual pause is done when the timer is invoked.
     */
    fun Pause() {
        this.visibility = VISIBLE
        val layout = this.parent as LinearLayout
        layout.requestLayout()
        requestLayout()
        this.invalidate()
        if (midifile == null || sheet == null || numberTracks() == 0) {
            return
        }

        // Cancel pending play events
        timer!!.removeCallbacks(DoPlay)
        if (playstate == playing) {
            playstate = initPause
        } else if (playstate == midi) {
            playstate = paused
        }
    }

    /** The callback for the Reset button.
     * If playing, initiate a stop and wait for the timer to finish.
     * Then do the actual stop.
     */
    fun Reset() {
        if (midifile == null || sheet == null) {
            return
        }
        if (playstate == stopped) {
            RemoveShading()
            ScrollToStart()
        } else if (playstate == initPause || playstate == initStop || playstate == playing) {
            /* Wait for timer to finish */
            playstate = initStop
            DoStop()
        } else if (playstate == paused) {
            DoStop()
        }
    }

    /** Perform the actual stop, by stopping the sound,
     * removing any shading, and clearing the state.
     */
    fun DoStop() {
        playstate = stopped
        timer!!.removeCallbacks(TimerCallback)
        RemoveShading()
        ScrollToStart()
        visibility = VISIBLE
        StopSound()
    }

    fun RemoveShading() {
        sheet!!.ShadeNotes(-10, prevPulseTime.toInt(), SheetMusic.DontScroll)
        sheet!!.ShadeNotes(-10, currentPulseTime.toInt(), SheetMusic.DontScroll)
        piano!!.ShadeNotes(-10, prevPulseTime.toInt())
        piano!!.ShadeNotes(-10, currentPulseTime.toInt())
    }

    /**
     * Scroll to the beginning of the sheet or to options.playMeasuresInLoopStart if enabled
     */
    fun ScrollToStart() {
        startPulseTime = if (options!!.playMeasuresInLoop) (options!!.playMeasuresInLoopStart * midifile!!.time!!.measure).toDouble() else 0.toDouble()
        currentPulseTime = startPulseTime
        prevPulseTime = -10.0
        sheet!!.ShadeNotes(currentPulseTime.toInt(), prevPulseTime.toInt(), SheetMusic.ImmediateScroll)
        piano!!.ShadeNotes(currentPulseTime.toInt(), prevPulseTime.toInt())
    }

    /** Rewind the midi music back one measure.
     * The music must be in the paused/stopped state.
     * When we resume in playPause, we start at the currentPulseTime.
     * So to rewind, just decrease the currentPulseTime,
     * and re-shade the sheet music.
     */
    fun Rewind() {
        if (midifile == null || sheet == null) {
            return
        }
        if (playstate != paused && playstate != stopped) {
            return
        }

        /* Remove any highlighted notes */sheet!!.ShadeNotes(-10, currentPulseTime.toInt(), SheetMusic.DontScroll)
        piano!!.ShadeNotes(-10, currentPulseTime.toInt())
        prevPulseTime = currentPulseTime
        currentPulseTime -= midifile!!.time!!.measure.toDouble()
        if (currentPulseTime < 0) {
            currentPulseTime = 0.0
            prevPulseTime = -10.0
        } else if (currentPulseTime < options!!.shifttime) {
            currentPulseTime = options!!.shifttime.toDouble()
        }
        sheet!!.ShadeNotes(currentPulseTime.toInt(), prevPulseTime.toInt(), SheetMusic.ImmediateScroll)
        piano!!.ShadeNotes(currentPulseTime.toInt(), prevPulseTime.toInt())
    }

    /** Fast forward the midi music by one measure.
     * The music must be in the paused/stopped state.
     * When we resume in playPause, we start at the currentPulseTime.
     * So to fast forward, just increase the currentPulseTime,
     * and re-shade the sheet music.
     */
    fun FastForward() {
        if (midifile == null || sheet == null) {
            return
        }
        if (playstate != paused && playstate != stopped) {
            return
        }
        playstate = paused

        /* Remove any highlighted notes */sheet!!.ShadeNotes(-10, currentPulseTime.toInt(), SheetMusic.DontScroll)
        piano!!.ShadeNotes(-10, currentPulseTime.toInt())
        prevPulseTime = currentPulseTime
        currentPulseTime += midifile!!.time!!.measure.toDouble()
        if (currentPulseTime > midifile!!.totalPulses) {
            currentPulseTime -= midifile!!.time!!.measure.toDouble()
        }
        sheet!!.ShadeNotes(currentPulseTime.toInt(), prevPulseTime.toInt(), SheetMusic.ImmediateScroll)
        piano!!.ShadeNotes(currentPulseTime.toInt(), prevPulseTime.toInt())
    }

    /** Move the current position to the location clicked.
     * The music must be in the paused/stopped state.
     * When we resume in playPause, we start at the currentPulseTime.
     * So, set the currentPulseTime to the position clicked.
     */
    fun MoveToClicked(x: Int, y: Int) {
        if (midifile == null || sheet == null) {
            return
        }
        if (playstate != paused && playstate != stopped && playstate != midi) {
            return
        }
        if (playstate != midi) {
            playstate = paused
        }

        /* Remove any highlighted notes */sheet!!.ShadeNotes(-10, currentPulseTime.toInt(), SheetMusic.DontScroll)
        piano!!.ShadeNotes(-10, currentPulseTime.toInt())
        currentPulseTime = sheet!!.PulseTimeForPoint(Point(x, y)).toDouble()
        prevPulseTime = currentPulseTime - midifile!!.time!!.measure
        if (currentPulseTime > midifile!!.totalPulses) {
            currentPulseTime -= midifile!!.time!!.measure.toDouble()
        }
        sheet!!.ShadeNotes(currentPulseTime.toInt(), prevPulseTime.toInt(), SheetMusic.DontScroll)
        piano!!.ShadeNotes(currentPulseTime.toInt(), prevPulseTime.toInt())
    }

    /** The callback for the timer. If the midi is still playing,
     * update the currentPulseTime and shade the sheet music.
     * If a stop or pause has been initiated (by someone clicking
     * the stop or pause button), then stop the timer.
     */
    var TimerCallback: Runnable = object : Runnable {
        override fun run() {
            if (midifile == null || sheet == null) {
                playstate = stopped
            } else if (playstate == stopped || playstate == paused) {
                /* This case should never happen */
                return
            } else if (playstate == initStop) {
                return
            } else if (playstate == playing) {
                val msec = SystemClock.uptimeMillis() - startTime
                prevPulseTime = currentPulseTime
                currentPulseTime = startPulseTime + msec * pulsesPerMsec

                /* If we're playing in a loop, stop and restart */if (options!!.playMeasuresInLoop) {
                    val nearEndTime = currentPulseTime + pulsesPerMsec * 10
                    val measure = (nearEndTime / midifile!!.time!!.measure).toInt()
                    if (measure > options!!.playMeasuresInLoopEnd) {
                        RestartPlayMeasuresInLoop()
                        return
                    }
                }

                /* Stop if we've reached the end of the song */if (currentPulseTime > midifile!!.totalPulses) {
                    DoStop()
                    return
                }
                sheet!!.ShadeNotes(currentPulseTime.toInt(), prevPulseTime.toInt(), SheetMusic.GradualScroll)
                piano!!.ShadeNotes(currentPulseTime.toInt(), prevPulseTime.toInt())
                timer!!.postDelayed(this, 100)
            } else if (playstate == initPause) {
                val msec = SystemClock.uptimeMillis() - startTime
                StopSound()
                prevPulseTime = currentPulseTime
                currentPulseTime = startPulseTime + msec * pulsesPerMsec
                sheet!!.ShadeNotes(currentPulseTime.toInt(), prevPulseTime.toInt(), SheetMusic.ImmediateScroll)
                piano!!.ShadeNotes(currentPulseTime.toInt(), prevPulseTime.toInt())
                playstate = paused
                timer!!.postDelayed(ReShade, 1000)
            }
        }
    }

    /** The "Play Measures in a Loop" feature is enabled, and we've reached
     * the last measure. Stop the sound, unshade the music, and then
     * start playing again.
     */
    private fun RestartPlayMeasuresInLoop() {
        playstate = stopped
        piano!!.ShadeNotes(-10, prevPulseTime.toInt())
        sheet!!.ShadeNotes(-10, prevPulseTime.toInt(), SheetMusic.DontScroll)
        currentPulseTime = 0.0
        prevPulseTime = -1.0
        StopSound()
        timer!!.postDelayed(DoPlay, 300)
    }

    val isInMidiMode: Boolean
        get() = playstate == midi

    fun setDrawer(drawer: Drawer?) {
        this.drawer = drawer
    }

    companion object {
        /** The index corresponding to left/right hand in the track list  */
        private const val LEFT_TRACK = 1
        private const val RIGHT_TRACK = 0

        /** Get the preferred width/height given the screen width/height  */
        @JvmStatic
        fun getPreferredSize(screenwidth: Int, screenheight: Int): Point {
            var height = (5.0 * screenwidth / (2 + Piano.KeysPerOctave * Piano.MaxOctave)).toInt()
            height = height * 2 / 3
            return Point(screenwidth, height)
        }
    }
}