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

import java.io.FileOutputStream
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.*
import kotlin.experimental.and

/** @class Pair - A pair of ints
 */
internal class PairInt() {
    var low = 0
    var high = 0
} /* MIDI file format.
 *
 * The Midi File format is described below.  The description uses
 * the following abbreviations.
 *
 * u1     - One byte
 * u2     - Two bytes (big endian)
 * u4     - Four bytes (big endian)
 * varlen - A variable length integer, that can be 1 to 4 bytes. The 
 *          integer ends when you encounter a byte that doesn't have 
 *          the 8th bit set (a byte less than 0x80).
 * len?   - The length of the data depends on some code
 *          
 *
 * The Midi files begins with the main Midi header
 * u4 = The four ascii characters 'MThd'
 * u4 = The length of the MThd header = 6 bytes
 * u2 = 0 if the file contains a single track
 *      1 if the file contains one or more simultaneous tracks
 *      2 if the file contains one or more independent tracks
 * u2 = number of tracks
 * u2 = if >  0, the number of pulses per quarter note
 *      if <= 0, then ???
 *
 * Next come the individual Midi tracks.  The total number of Midi
 * tracks was given above, in the MThd header.  Each track starts
 * with a header:
 *
 * u4 = The four ascii characters 'MTrk'
 * u4 = Amount of track data, in bytes.
 * 
 * The track data consists of a series of Midi events.  Each Midi event
 * has the following format:
 *
 * varlen  - The time between the previous event and this event, measured
 *           in "pulses".  The number of pulses per quarter note is given
 *           in the MThd header.
 * u1      - The Event code, always betwee 0x80 and 0xFF
 * len?    - The event data.  The length of this data is determined by the
 *           event code.  The first byte of the event data is always < 0x80.
 *
 * The event code is optional.  If the event code is missing, then it
 * defaults to the previous event code.  For example:
 *
 *   varlen, eventcode1, eventdata,
 *   varlen, eventcode2, eventdata,
 *   varlen, eventdata,  // eventcode is eventcode2
 *   varlen, eventdata,  // eventcode is eventcode2
 *   varlen, eventcode3, eventdata,
 *   ....
 *
 *   How do you know if the eventcode is there or missing? Well:
 *   - All event codes are between 0x80 and 0xFF
 *   - The first byte of eventdata is always less than 0x80.
 *   So, after the varlen delta time, if the next byte is between 0x80
 *   and 0xFF, its an event code.  Otherwise, its event data.
 *
 * The Event codes and event data for each event code are shown below.
 *
 * Code:  u1 - 0x80 thru 0x8F - Note Off event.
 *             0x80 is for channel 1, 0x8F is for channel 16.
 * Data:  u1 - The note number, 0-127.  Middle C is 60 (0x3C)
 *        u1 - The note velocity.  This should be 0
 * 
 * Code:  u1 - 0x90 thru 0x9F - Note On event.
 *             0x90 is for channel 1, 0x9F is for channel 16.
 * Data:  u1 - The note number, 0-127.  Middle C is 60 (0x3C)
 *        u1 - The note velocity, from 0 (no sound) to 127 (loud).
 *             A value of 0 is equivalent to a Note Off.
 *
 * Code:  u1 - 0xA0 thru 0xAF - Key Pressure
 * Data:  u1 - The note number, 0-127.
 *        u1 - The pressure.
 *
 * Code:  u1 - 0xB0 thru 0xBF - Control Change
 * Data:  u1 - The controller number
 *        u1 - The value
 *
 * Code:  u1 - 0xC0 thru 0xCF - Program Change
 * Data:  u1 - The program number.
 *
 * Code:  u1 - 0xD0 thru 0xDF - Channel Pressure
 *        u1 - The pressure.
 *
 * Code:  u1 - 0xE0 thru 0xEF - Pitch Bend
 * Data:  u2 - Some data
 *
 * Code:  u1     - 0xFF - Meta Event
 * Data:  u1     - Metacode
 *        varlen - Length of meta event
 *        u1[varlen] - Meta event data.
 *
 *
 * The Meta Event codes are listed below:
 *
 * Metacode: u1         - 0x0  Sequence Number
 *           varlen     - 0 or 2
 *           u1[varlen] - Sequence number
 *
 * Metacode: u1         - 0x1  Text
 *           varlen     - Length of text
 *           u1[varlen] - Text
 *
 * Metacode: u1         - 0x2  Copyright
 *           varlen     - Length of text
 *           u1[varlen] - Text
 *
 * Metacode: u1         - 0x3  Track Name
 *           varlen     - Length of name
 *           u1[varlen] - Track Name
 *
 * Metacode: u1         - 0x58  Time Signature
 *           varlen     - 4 
 *           u1         - numerator
 *           u1         - log2(denominator)
 *           u1         - clocks in metronome click
 *           u1         - 32nd notes in quarter note (usually 8)
 *
 * Metacode: u1         - 0x59  Key Signature
 *           varlen     - 2
 *           u1         - if >= 0, then number of sharps
 *                        if < 0, then number of flats * -1
 *           u1         - 0 if major key
 *                        1 if minor key
 *
 * Metacode: u1         - 0x51  Tempo
 *           varlen     - 3  
 *           u3         - quarter note length in microseconds
 */

/** @class MidiFile
 *
 * The MidiFile class contains the parsed data from the Midi File.
 * It contains:
 * - All the tracks in the midi file, including all MidiNotes per track.
 * - The time signature (e.g. 4/4, 3/4, 6/8)
 * - The number of pulses per quarter note.
 * - The tempo (number of microseconds per quarter note).
 *
 * The constructor takes a filename as input, and upon returning,
 * contains the parsed data from the midi file.
 *
 * The methods ReadTrack() and ReadMetaEvent() are helper functions called
 * by the constructor during the parsing.
 *
 * After the MidiFile is parsed and created, the user can retrieve the
 * tracks and notes by using the property Tracks and Tracks.Notes.
 *
 * There are two methods for modifying the midi data based on the menu
 * options selected:
 *
 * - ChangeMidiNotes()
 * Apply the menu options to the parsed MidiFile.  This uses the helper functions:
 * SplitTrack()
 * CombineToTwoTracks()
 * ShiftTime()
 * Transpose()
 * RoundStartTimes()
 * RoundDurations()
 *
 * - ChangeSound()
 * Apply the menu options to the MIDI music data, and save the modified midi data
 * to a file, for playback.
 */
class MidiFile(rawdata: ByteArray,
               /** The file reference  */
               val fileName: String) {
    private val fileuri: FileUri? = null
    /** Get the file name  */

    /** The Midi file name  */
    private var allevents: ArrayList<ArrayList<MidiEvent>>? = null
    /** Get the list of tracks  */
    /** The raw MidiEvents, one list per track  */
    var tracks: ArrayList<MidiTrack>? = null
        private set

    /** The tracks of the midifile that have notes  */
    private var trackmode: Short = 0
    /** Get the time signature  */
    /** 0 (single track), 1 (simultaneous tracks) 2 (independent tracks)  */
    var time: TimeSignature? = null
        private set

    /** The time signature  */
    private var quarternote = 0
    /** Get the total length (in pulses) of the song  */
    /** The number of pulses per quarter note  */
    var totalPulses = 0
        private set

    /** The total length of the song, in pulses  */
    private var trackPerChannel = false
    /* End Instruments */
    /** Return a String representation of a Midi event  */
    private fun EventName(ev: Int): String {
        if (ev >= EventNoteOff && ev < EventNoteOff + 16) return "NoteOff" else if (ev >= EventNoteOn && ev < EventNoteOn + 16) return "NoteOn" else if (ev >= EventKeyPressure && ev < EventKeyPressure + 16) return "KeyPressure" else if (ev >= EventControlChange && ev < EventControlChange + 16) return "ControlChange" else if (ev >= EventProgramChange && ev < EventProgramChange + 16) return "ProgramChange" else if (ev >= EventChannelPressure && ev < EventChannelPressure + 16) return "ChannelPressure" else if (ev >= EventPitchBend && ev < EventPitchBend + 16) return "PitchBend" else if (ev == MetaEvent.toInt()) return "MetaEvent" else return if (ev == SysexEvent1.toInt() || ev == SysexEvent2.toInt()) "SysexEvent" else "Unknown"
    }

    /** Return a String representation of a meta-event  */
    private fun MetaName(ev: Int): String {
        if (ev == MetaEventSequence.toInt()) return "MetaEventSequence" else if (ev == MetaEventText.toInt()) return "MetaEventText" else if (ev == MetaEventCopyright.toInt()) return "MetaEventCopyright" else if (ev == MetaEventSequenceName.toInt()) return "MetaEventSequenceName" else if (ev == MetaEventInstrument.toInt()) return "MetaEventInstrument" else if (ev == MetaEventLyric.toInt()) return "MetaEventLyric" else if (ev == MetaEventMarker.toInt()) return "MetaEventMarker" else if (ev == MetaEventEndOfTrack.toInt()) return "MetaEventEndOfTrack" else if (ev == MetaEventTempo.toInt()) return "MetaEventTempo" else if (ev == MetaEventSMPTEOffset.toInt()) return "MetaEventSMPTEOffset" else if (ev == MetaEventTimeSignature.toInt()) return "MetaEventTimeSignature" else return if (ev == MetaEventKeySignature.toInt()) "MetaEventKeySignature" else "Unknown"
    }

    /** Parse the given Midi file, and return an instance of this MidiFile
     * class.  After reading the midi file, this object will contain:
     * - The raw list of midi events
     * - The Time Signature of the song
     * - All the tracks in the song which contain notes.
     * - The number, starttime, and duration of each note.
     */
    private fun parse(rawdata: ByteArray) {
        val id: String
        val len: Int
        tracks = ArrayList()
        trackPerChannel = false
        val file = MidiFileReader(rawdata)
        id = file.ReadAscii(4)
        if (id != "MThd") {
            throw MidiFileException("Doesn't start with MThd", 0)
        }
        len = file.ReadInt()
        if (len != 6) {
            throw MidiFileException("Bad MThd header", 4)
        }
        trackmode = file.ReadShort().toShort()
        val num_tracks = file.ReadShort()
        quarternote = file.ReadShort()
        allevents = ArrayList()
        for (tracknum in 0 until num_tracks) {
            allevents!!.add(ReadTrack(file))
            val track = MidiTrack(allevents!![tracknum], tracknum)
            if (track.notes.size > 0) {
                tracks!!.add(track)
            }
        }

        /* Get the length of the song in pulses */for (track: MidiTrack in tracks!!) {
            val last = track.notes[track.notes.size - 1]
            if (totalPulses < last.startTime + last.duration) {
                totalPulses = last.startTime + last.duration
            }
        }

        /* If we only have one track with multiple channels, then treat
         * each channel as a separate track.
         */if (tracks!!.size == 1 && HasMultipleChannels(tracks!![0])) {
            tracks = SplitChannels(tracks!![0], allevents!![tracks!![0].trackNumber()])
            trackPerChannel = true
        }
        CheckStartTimes(tracks)

        /* Determine the time signature */
        var tempoCount = 0
        var tempo: Long = 0
        var numer = 0
        var denom = 0
        for (list: ArrayList<MidiEvent> in allevents!!) {
            for (mevent: MidiEvent in list) {
                if (mevent.Metaevent == MetaEventTempo) {
                    // Take average of all tempos
                    tempo += mevent.Tempo
                    tempoCount++
                }
                if (mevent.Metaevent == MetaEventTimeSignature && numer == 0) {
                    numer = mevent.Numerator.toInt()
                    denom = mevent.Denominator.toInt()
                }
            }
        }
        if (tempo == 0L) {
            tempo = 500000 /* 500,000 microseconds = 0.05 sec */
        } else {
            tempo = tempo / tempoCount
        }
        if (numer == 0) {
            numer = 4
            denom = 4
        }
        time = TimeSignature(numer, denom, quarternote, tempo.toInt())
    }

    /** Parse a single Midi track into a list of MidiEvents.
     * Entering this function, the file offset should be at the start of
     * the MTrk header.  Upon exiting, the file offset should be at the
     * start of the next MTrk header.
     */
    private fun ReadTrack(file: MidiFileReader): ArrayList<MidiEvent> {
        val result = ArrayList<MidiEvent>(20)
        var starttime = 0
        val id = file.ReadAscii(4)
        if (id != "MTrk") {
            throw MidiFileException("Bad MTrk header", file.GetOffset() - 4)
        }
        val tracklen = file.ReadInt()
        val trackend = tracklen + file.GetOffset()
        var eventflag: Byte = 0
        while (file.GetOffset() < trackend) {

            // If the midi file is truncated here, we can still recover.
            // Just return what we've parsed so far.
            var startoffset: Int
            var deltatime: Int
            var peekevent: Byte
            try {
                startoffset = file.GetOffset()
                deltatime = file.ReadVarlen()
                starttime += deltatime
                peekevent = file.Peek()
            } catch (e: MidiFileException) {
                return result
            }
            val mevent = MidiEvent()
            result.add(mevent)
            mevent.DeltaTime = deltatime
            mevent.StartTime = starttime

            // if (peekevent >= EventNoteOff) { 
            if (peekevent < 0) {
                mevent.HasEventflag = true
                eventflag = file.ReadByte()
            }

            //Log.e("debug",  "offset " + startoffset + 
            //                " event " + eventflag + " " + EventName(eventflag) +
            //                " start " + starttime + " delta " + mevent.DeltaTime);
            if (eventflag >= EventNoteOn && eventflag < EventNoteOn + 16) {
                mevent.EventFlag = EventNoteOn
                mevent.Channel = (eventflag - EventNoteOn).toByte()
                mevent.Notenumber = file.ReadByte()
                mevent.Velocity = file.ReadByte()
            } else if (eventflag >= EventNoteOff && eventflag < EventNoteOff + 16) {
                mevent.EventFlag = EventNoteOff
                mevent.Channel = (eventflag - EventNoteOff).toByte()
                mevent.Notenumber = file.ReadByte()
                mevent.Velocity = file.ReadByte()
            } else if (eventflag >= EventKeyPressure &&
                    eventflag < EventKeyPressure + 16) {
                mevent.EventFlag = EventKeyPressure
                mevent.Channel = (eventflag - EventKeyPressure).toByte()
                mevent.Notenumber = file.ReadByte()
                mevent.KeyPressure = file.ReadByte()
            } else if (eventflag >= EventControlChange &&
                    eventflag < EventControlChange + 16) {
                mevent.EventFlag = EventControlChange
                mevent.Channel = (eventflag - EventControlChange).toByte()
                mevent.ControlNum = file.ReadByte()
                mevent.ControlValue = file.ReadByte()
            } else if (eventflag >= EventProgramChange &&
                    eventflag < EventProgramChange + 16) {
                mevent.EventFlag = EventProgramChange
                mevent.Channel = (eventflag - EventProgramChange).toByte()
                mevent.Instrument = file.ReadByte()
            } else if (eventflag >= EventChannelPressure &&
                    eventflag < EventChannelPressure + 16) {
                mevent.EventFlag = EventChannelPressure
                mevent.Channel = (eventflag - EventChannelPressure).toByte()
                mevent.ChanPressure = file.ReadByte()
            } else if (eventflag >= EventPitchBend &&
                    eventflag < EventPitchBend + 16) {
                mevent.EventFlag = EventPitchBend
                mevent.Channel = (eventflag - EventPitchBend).toByte()
                mevent.PitchBend = file.ReadShort().toShort()
            } else if (eventflag == SysexEvent1) {
                mevent.EventFlag = SysexEvent1
                mevent.Metalength = file.ReadVarlen()
                mevent.Value = file.ReadBytes(mevent.Metalength)
            } else if (eventflag == SysexEvent2) {
                mevent.EventFlag = SysexEvent2
                mevent.Metalength = file.ReadVarlen()
                mevent.Value = file.ReadBytes(mevent.Metalength)
            } else if (eventflag == MetaEvent) {
                mevent.EventFlag = MetaEvent
                mevent.Metaevent = file.ReadByte()
                mevent.Metalength = file.ReadVarlen()
                mevent.Value = file.ReadBytes(mevent.Metalength)
                if (mevent.Metaevent == MetaEventTimeSignature) {
                    if (mevent.Metalength < 2) {
                        throw MidiFileException(
                                "Meta Event Time Signature len == " + mevent.Metalength +
                                        " != 4", file.GetOffset())
                    } else {
                        mevent.Numerator = mevent.Value!![0]
                        mevent.Denominator = (Math.pow(2.0, mevent.Value!![1].toDouble()).toByte())
                    }
                } else if (mevent.Metaevent == MetaEventTempo) {
                    if (mevent.Metalength != 3) {
                        throw MidiFileException(
                                ("Meta Event Tempo len == " + mevent.Metalength +
                                        " != 3"), file.GetOffset())
                    }
                    mevent.Tempo = ((((mevent.Value!![0]).toInt() and 0xFF) shl 16) or
                            (((mevent.Value!![1]).toInt() and 0xFF) shl 8) or
                            ((mevent.Value!![2]).toInt() and 0xFF))
                } else if (mevent.Metaevent == MetaEventEndOfTrack) {
                    /* break;  */
                }
            } else {
                throw MidiFileException("Unknown event " + mevent.EventFlag,
                        file.GetOffset() - 1)
            }
        }
        return result
    }

    /** Write this Midi file to the given file.
     * If options is not null, apply those options to the midi events
     * before performing the write.
     * Return true if the file was saved successfully, else false.
     */
    @Throws(IOException::class)
    fun ChangeSound(destfile: FileOutputStream, options: MidiOptions?) {
        Write(destfile, options)
    }

    @Throws(IOException::class)
    fun Write(destfile: FileOutputStream, options: MidiOptions?) {
        var newevents = allevents
        if (options != null) {
            newevents = ApplyOptionsToEvents(options)
        }
        WriteEvents(destfile, newevents, trackmode.toInt(), quarternote)
    }

    /** Apply the following sound options to the midi events:
     * - The tempo (the microseconds per pulse)
     * - The instruments per track
     * - The note number (transpose value)
     * - The tracks to include
     * Return the modified list of midi events.
     */
    fun ApplyOptionsToEvents(options: MidiOptions): ArrayList<ArrayList<MidiEvent>> {
        var i: Int
        if (trackPerChannel) {
            return ApplyOptionsPerChannel(options)
        }

        /* A midifile can contain tracks with notes and tracks without notes.
         * The options.tracks and options.instruments are for tracks with notes.
         * So the track numbers in 'options' may not match correctly if the
         * midi file has tracks without notes. Re-compute the instruments, and 
         * tracks to keep.
         */
        val num_tracks = allevents!!.size
        val instruments = IntArray(num_tracks)
        val keeptracks = BooleanArray(num_tracks)
        i = 0
        while (i < num_tracks) {
            instruments[i] = 0
            keeptracks[i] = true
            i++
        }
        for (tracknum in tracks!!.indices) {
            val track = tracks!![tracknum]
            val realtrack = track.trackNumber()
            instruments[realtrack] = options.instruments[tracknum]
            if (!options.tracks[tracknum] || options.mute[tracknum]) {
                keeptracks[realtrack] = false
            }
        }
        var newevents = CloneMidiEvents(allevents)

        /* Set the tempo at the beginning of each track */for (tracknum in newevents.indices) {
            val mevent = CreateTempoEvent(options.tempo)
            newevents[tracknum].add(0, mevent)
        }

        /* Change the note number (transpose), instrument, and tempo */for (tracknum in newevents.indices) {
            for (mevent: MidiEvent in newevents[tracknum]) {
                var num = mevent.Notenumber + options.transpose
                if (num < 0) num = 0
                if (num > 127) num = 127
                mevent.Notenumber = num.toByte()
                if (!options.useDefaultInstruments) {
                    mevent.Instrument = instruments[tracknum].toByte()
                }
                mevent.Tempo = options.tempo
            }
        }
        if (options.pauseTime != 0) {
            newevents = StartAtPauseTime(newevents, options.pauseTime)
        }

        /* Change the tracks to include */
        var count = 0
        for (keeptrack: Boolean in keeptracks) {
            if (keeptrack) {
                count++
            }
        }
        val result = ArrayList<ArrayList<MidiEvent>>(count)
        i = 0
        for (tracknum in keeptracks.indices) {
            if (keeptracks[tracknum]) {
                result.add(newevents[tracknum])
                i++
            }
        }
        return result
    }

    /** Apply the following sound options to the midi events:
     * - The tempo (the microseconds per pulse)
     * - The instruments per track
     * - The note number (transpose value)
     * - The tracks to include
     * Return the modified list of midi events.
     *
     * This Midi file only has one actual track, but we've split that
     * into multiple fake tracks, one per channel, and displayed that
     * to the end-user.  So changing the instrument, and tracks to
     * include, is implemented differently than the ApplyOptionsToEvents() method:
     *
     * - We change the instrument based on the channel, not the track.
     * - We include/exclude channels, not tracks.
     * - We exclude a channel by setting the note volume/velocity to 0.
     */
    fun ApplyOptionsPerChannel(options: MidiOptions): ArrayList<ArrayList<MidiEvent>> {
        /* Determine which channels to include/exclude.
         * Also, determine the instruments for each channel.
         */
        val instruments = IntArray(16)
        val keepchannel = BooleanArray(16)
        for (i in 0..15) {
            instruments[i] = 0
            keepchannel[i] = true
        }
        for (tracknum in tracks!!.indices) {
            val track = tracks!![tracknum]
            val channel = track.notes[0].channel
            instruments[channel] = options.instruments[tracknum]
            if (!options.tracks[tracknum] || options.mute[tracknum]) {
                keepchannel[channel] = false
            }
        }
        var newevents = CloneMidiEvents(allevents)

        /* Set the tempo at the beginning of each track */for (tracknum in newevents.indices) {
            val mevent = CreateTempoEvent(options.tempo)
            newevents[tracknum].add(0, mevent)
        }

        /* Change the note number (transpose), instrument, and tempo */for (tracknum in newevents.indices) {
            for (mevent: MidiEvent in newevents[tracknum]) {
                var num = mevent.Notenumber + options.transpose
                if (num < 0) num = 0
                if (num > 127) num = 127
                mevent.Notenumber = num.toByte()
                if (!keepchannel[mevent.Channel.toInt()]) {
                    mevent.Velocity = 0
                }
                if (!options.useDefaultInstruments) {
                    mevent.Instrument = instruments[mevent.Channel.toInt()].toByte()
                }
                mevent.Tempo = options.tempo
            }
        }
        if (options.pauseTime != 0) {
            newevents = StartAtPauseTime(newevents, options.pauseTime)
        }
        return newevents
    }

    /** Apply the given sheet music options to the MidiNotes.
     * Return the midi tracks with the changes applied.
     */
    fun ChangeMidiNotes(options: MidiOptions): ArrayList<MidiTrack> {
        var newtracks = ArrayList<MidiTrack>()
        for (track in tracks!!.indices) {
            if (options.tracks[track]) {
                newtracks.add(tracks!![track].Clone())
            }
        }

        /* To make the sheet music look nicer, we round the start times
         * so that notes close together appear as a single chord.  We
         * also extend the note durations, so that we have longer notes
         * and fewer rest symbols.
         */
        var time = time
        if (options.time != null) {
            time = options.time
        }
        RoundStartTimes(newtracks, options.combineInterval, this.time)
        RoundDurations(newtracks, time!!.quarter)
        if (options.twoStaffs) {
            newtracks = CombineToTwoTracks(newtracks, time.measure)
        }
        if (options.shifttime != 0) {
            ShiftTime(newtracks, options.shifttime)
        }
        if (options.transpose != 0) {
            Transpose(newtracks, options.transpose)
        }
        return newtracks
    }

    /** Guess the measure length.  We assume that the measure
     * length must be between 0.5 seconds and 4 seconds.
     * Take all the note start times that fall between 0.5 and
     * 4 seconds, and return the starttimes.
     */
    fun GuessMeasureLength(): ListInt {
        val result = ListInt()
        val pulses_per_second = (1000000.0 / time!!.tempo * time!!.quarter).toInt()
        val minmeasure = pulses_per_second / 2 /* The minimum measure length in pulses */
        val maxmeasure = pulses_per_second * 4 /* The maximum measure length in pulses */

        /* Get the start time of the first note in the midi file. */
        var firstnote = time!!.measure * 5
        for (track: MidiTrack in tracks!!) {
            if (firstnote > track.notes[0].startTime) {
                firstnote = track.notes[0].startTime
            }
        }

        /* interval = 0.06 seconds, converted into pulses */
        val interval = time!!.quarter * 60000 / time!!.tempo
        for (track: MidiTrack in tracks!!) {
            var prevtime = 0
            for (note: MidiNote in track.notes) {
                if (note.startTime - prevtime <= interval) continue
                prevtime = note.startTime
                var time_from_firstnote = note.startTime - firstnote

                /* Round the time down to a multiple of 4 */time_from_firstnote = time_from_firstnote / 4 * 4
                if (time_from_firstnote < minmeasure) continue
                if (time_from_firstnote > maxmeasure) break
                if (!result.contains(time_from_firstnote)) {
                    result.add(time_from_firstnote)
                }
            }
        }
        result.sort()
        return result
    }

    /** Return the last start time  */
    fun EndTime(): Int {
        var lastStart = 0
        for (track: MidiTrack in tracks!!) {
            if (track.notes.size == 0) {
                continue
            }
            val last = track.notes[track.notes.size - 1].startTime
            lastStart = Math.max(last, lastStart)
        }
        return lastStart
    }

    /** Return true if this midi file has lyrics  */
    fun hasLyrics(): Boolean {
        for (track: MidiTrack in tracks!!) {
            if (track.getLyrics() != null) {
                return true
            }
        }
        return false
    }

    override fun toString(): String {
        val result = StringBuilder(
                "Midi File tracks=" + tracks!!.size + " quarter=" + quarternote + "\n")
        result.append(time.toString()).append("\n")
        for (track: MidiTrack in tracks!!) {
            result.append(track.toString())
        }
        return result.toString()
    }

    companion object {
        /** True if we've split each channel into a track  */ /* The list of Midi Events */
        val EventNoteOff = 0x80.toByte()
        val EventNoteOn = 0x90.toByte()
        val EventKeyPressure = 0xA0.toByte()
        val EventControlChange = 0xB0.toByte()
        val EventProgramChange = 0xC0.toByte()
        val EventChannelPressure = 0xD0.toByte()
        val EventPitchBend = 0xE0.toByte()
        val SysexEvent1 = 0xF0.toByte()
        val SysexEvent2 = 0xF7.toByte()
        val MetaEvent = 0xFF.toByte()

        /* The list of Meta Events */
        val MetaEventSequence = 0x0.toByte()
        val MetaEventText = 0x1.toByte()
        val MetaEventCopyright = 0x2.toByte()
        val MetaEventSequenceName = 0x3.toByte()
        val MetaEventInstrument = 0x4.toByte()
        val MetaEventLyric = 0x5.toByte()
        val MetaEventMarker = 0x6.toByte()
        val MetaEventEndOfTrack = 0x2F.toByte()
        val MetaEventTempo = 0x51.toByte()
        val MetaEventSMPTEOffset = 0x54.toByte()
        val MetaEventTimeSignature = 0x58.toByte()
        val MetaEventKeySignature = 0x59.toByte()

        /* The Program Change event gives the instrument that should
     * be used for a particular channel.  The following table
     * maps each instrument number (0 thru 128) to an instrument
     * name.
     */
        var Instruments = arrayOf(
                "Acoustic Grand Piano",
                "Bright Acoustic Piano",
                "Electric Grand Piano",
                "Honky-tonk Piano",
                "Electric Piano 1",
                "Electric Piano 2",
                "Harpsichord",
                "Clavi",
                "Celesta",
                "Glockenspiel",
                "Music Box",
                "Vibraphone",
                "Marimba",
                "Xylophone",
                "Tubular Bells",
                "Dulcimer",
                "Drawbar Organ",
                "Percussive Organ",
                "Rock Organ",
                "Church Organ",
                "Reed Organ",
                "Accordion",
                "Harmonica",
                "Tango Accordion",
                "Acoustic Guitar (nylon)",
                "Acoustic Guitar (steel)",
                "Electric Guitar (jazz)",
                "Electric Guitar (clean)",
                "Electric Guitar (muted)",
                "Overdriven Guitar",
                "Distortion Guitar",
                "Guitar harmonics",
                "Acoustic Bass",
                "Electric Bass (finger)",
                "Electric Bass (pick)",
                "Fretless Bass",
                "Slap Bass 1",
                "Slap Bass 2",
                "Synth Bass 1",
                "Synth Bass 2",
                "Violin",
                "Viola",
                "Cello",
                "Contrabass",
                "Tremolo Strings",
                "Pizzicato Strings",
                "Orchestral Harp",
                "Timpani",
                "String Ensemble 1",
                "String Ensemble 2",
                "SynthStrings 1",
                "SynthStrings 2",
                "Choir Aahs",
                "Voice Oohs",
                "Synth Voice",
                "Orchestra Hit",
                "Trumpet",
                "Trombone",
                "Tuba",
                "Muted Trumpet",
                "French Horn",
                "Brass Section",
                "SynthBrass 1",
                "SynthBrass 2",
                "Soprano Sax",
                "Alto Sax",
                "Tenor Sax",
                "Baritone Sax",
                "Oboe",
                "English Horn",
                "Bassoon",
                "Clarinet",
                "Piccolo",
                "Flute",
                "Recorder",
                "Pan Flute",
                "Blown Bottle",
                "Shakuhachi",
                "Whistle",
                "Ocarina",
                "Lead 1 (square)",
                "Lead 2 (sawtooth)",
                "Lead 3 (calliope)",
                "Lead 4 (chiff)",
                "Lead 5 (charang)",
                "Lead 6 (voice)",
                "Lead 7 (fifths)",
                "Lead 8 (bass + lead)",
                "Pad 1 (new age)",
                "Pad 2 (warm)",
                "Pad 3 (polysynth)",
                "Pad 4 (choir)",
                "Pad 5 (bowed)",
                "Pad 6 (metallic)",
                "Pad 7 (halo)",
                "Pad 8 (sweep)",
                "FX 1 (rain)",
                "FX 2 (soundtrack)",
                "FX 3 (crystal)",
                "FX 4 (atmosphere)",
                "FX 5 (brightness)",
                "FX 6 (goblins)",
                "FX 7 (echoes)",
                "FX 8 (sci-fi)",
                "Sitar",
                "Banjo",
                "Shamisen",
                "Koto",
                "Kalimba",
                "Bag pipe",
                "Fiddle",
                "Shanai",
                "Tinkle Bell",
                "Agogo",
                "Steel Drums",
                "Woodblock",
                "Taiko Drum",
                "Melodic Tom",
                "Synth Drum",
                "Reverse Cymbal",
                "Guitar Fret Noise",
                "Breath Noise",
                "Seashore",
                "Bird Tweet",
                "Telephone Ring",
                "Helicopter",
                "Applause",
                "Gunshot",
                "Percussion"
        )

        /** Return true if this track contains multiple channels.
         * If a MidiFile contains only one track, and it has multiple channels,
         * then we treat each channel as a separate track.
         */
        fun HasMultipleChannels(track: MidiTrack): Boolean {
            val channel = track.notes[0].channel
            for (note: MidiNote in track.notes) {
                if (note.channel != channel) {
                    return true
                }
            }
            return false
        }

        /** Write a variable length number to the buffer at the given offset.
         * Return the number of bytes written.
         */
        fun VarlenToBytes(num: Int, buf: ByteArray, offset: Int): Int {
            val b1 = ((num shr 21) and 0x7F).toByte()
            val b2 = ((num shr 14) and 0x7F).toByte()
            val b3 = ((num shr 7) and 0x7F).toByte()
            val b4 = (num and 0x7F).toByte()
            if (b1 > 0) {
                buf[offset] = (b1.toInt() or 0x80) as Byte
                buf[offset + 1] = (b2.toInt() or 0x80) as Byte
                buf[offset + 2] = (b3.toInt() or 0x80) as Byte
                buf[offset + 3] = b4
                return 4
            } else if (b2 > 0) {
                buf[offset] = (b2.toInt() or 0x80) as Byte
                buf[offset + 1] = (b3.toInt() or 0x80) as Byte
                buf[offset + 2] = b4
                return 3
            } else if (b3 > 0) {
                buf[offset] = (b3.toInt() or 0x80) as Byte
                buf[offset + 1] = b4
                return 2
            } else {
                buf[offset] = b4
                return 1
            }
        }

        /** Write a 4-byte integer to data[offset : offset+4]  */
        private fun IntToBytes(value: Int, data: ByteArray, offset: Int) {
            data[offset] = ((value shr 24) and 0xFF).toByte()
            data[offset + 1] = ((value shr 16) and 0xFF).toByte()
            data[offset + 2] = ((value shr 8) and 0xFF).toByte()
            data[offset + 3] = (value and 0xFF).toByte()
        }

        /** Calculate the track length (in bytes) given a list of Midi events  */
        private fun GetTrackLength(events: ArrayList<MidiEvent>): Int {
            var len = 0
            val buf = ByteArray(1024)
            for (mevent: MidiEvent in events) {
                len += VarlenToBytes(mevent.DeltaTime, buf, 0)
                len += 1 /* for eventflag */
                when (mevent.EventFlag) {
                    EventNoteOn -> len += 2
                    EventNoteOff -> len += 2
                    EventKeyPressure -> len += 2
                    EventControlChange -> len += 2
                    EventProgramChange -> len += 1
                    EventChannelPressure -> len += 1
                    EventPitchBend -> len += 2
                    SysexEvent1, SysexEvent2 -> {
                        len += VarlenToBytes(mevent.Metalength, buf, 0)
                        len += mevent.Metalength
                    }
                    MetaEvent -> {
                        len += 1
                        len += VarlenToBytes(mevent.Metalength, buf, 0)
                        len += mevent.Metalength
                    }
                    else -> {
                    }
                }
            }
            return len
        }

        /** Copy len bytes from src to dest, at the given offsets  */
        private fun ArrayCopy(src: ByteArray?, srcoffset: Int, dest: ByteArray, destoffset: Int, len: Int) {
            if (len >= 0) System.arraycopy(src, srcoffset, dest, destoffset, len)
        }

        /** Write the given list of Midi events to a stream/file.
         * This method is used for sound playback, for creating new Midi files
         * with the tempo, transpose, etc changed.
         *
         * Return true on success, and false on error.
         */
        @Throws(IOException::class)
        private fun WriteEvents(file: FileOutputStream, allevents: ArrayList<ArrayList<MidiEvent>>?,
                                trackmode: Int, quarter: Int) {
            val buf = ByteArray(16384)

            /* Write the MThd, len = 6, track mode, number tracks, quarter note */file.write("MThd".toByteArray(StandardCharsets.US_ASCII), 0, 4)
            IntToBytes(6, buf, 0)
            file.write(buf, 0, 4)
            buf[0] = (trackmode shr 8).toByte()
            buf[1] = (trackmode and 0xFF).toByte()
            file.write(buf, 0, 2)
            buf[0] = 0
            buf[1] = allevents!!.size.toByte()
            file.write(buf, 0, 2)
            buf[0] = (quarter shr 8).toByte()
            buf[1] = (quarter and 0xFF).toByte()
            file.write(buf, 0, 2)
            for (list: ArrayList<MidiEvent> in allevents) {
                /* Write the MTrk header and track length */
                file.write("MTrk".toByteArray(StandardCharsets.US_ASCII), 0, 4)
                val len = GetTrackLength(list)
                IntToBytes(len, buf, 0)
                file.write(buf, 0, 4)
                for (mevent: MidiEvent in list) {
                    val varlen = VarlenToBytes(mevent.DeltaTime, buf, 0)
                    file.write(buf, 0, varlen)
                    if ((mevent.EventFlag == SysexEvent1) || (
                                    mevent.EventFlag == SysexEvent2) || (
                                    mevent.EventFlag == MetaEvent)) {
                        buf[0] = mevent.EventFlag
                    } else {
                        buf[0] = (mevent.EventFlag + mevent.Channel).toByte()
                    }
                    file.write(buf, 0, 1)
                    if (mevent.EventFlag == EventNoteOn) {
                        buf[0] = mevent.Notenumber
                        buf[1] = mevent.Velocity
                        file.write(buf, 0, 2)
                    } else if (mevent.EventFlag == EventNoteOff) {
                        buf[0] = mevent.Notenumber
                        buf[1] = mevent.Velocity
                        file.write(buf, 0, 2)
                    } else if (mevent.EventFlag == EventKeyPressure) {
                        buf[0] = mevent.Notenumber
                        buf[1] = mevent.KeyPressure
                        file.write(buf, 0, 2)
                    } else if (mevent.EventFlag == EventControlChange) {
                        buf[0] = mevent.ControlNum
                        buf[1] = mevent.ControlValue
                        file.write(buf, 0, 2)
                    } else if (mevent.EventFlag == EventProgramChange) {
                        buf[0] = mevent.Instrument
                        file.write(buf, 0, 1)
                    } else if (mevent.EventFlag == EventChannelPressure) {
                        buf[0] = mevent.ChanPressure
                        file.write(buf, 0, 1)
                    } else if (mevent.EventFlag == EventPitchBend) {
                        buf[0] = ((mevent.PitchBend).toInt() shr 8) as Byte
                        buf[1] = (mevent.PitchBend and 0xFF) as Byte
                        file.write(buf, 0, 2)
                    } else if (mevent.EventFlag == SysexEvent1) {
                        val offset = VarlenToBytes(mevent.Metalength, buf, 0)
                        ArrayCopy(mevent.Value, 0, buf, offset, mevent.Value!!.size)
                        file.write(buf, 0, offset + mevent.Value!!.size)
                    } else if (mevent.EventFlag == SysexEvent2) {
                        val offset = VarlenToBytes(mevent.Metalength, buf, 0)
                        ArrayCopy(mevent.Value, 0, buf, offset, mevent.Value!!.size)
                        file.write(buf, 0, offset + mevent.Value!!.size)
                    } else if (mevent.EventFlag == MetaEvent && mevent.Metaevent == MetaEventTempo) {
                        buf[0] = mevent.Metaevent
                        buf[1] = 3
                        buf[2] = ((mevent.Tempo shr 16) and 0xFF).toByte()
                        buf[3] = ((mevent.Tempo shr 8) and 0xFF).toByte()
                        buf[4] = (mevent.Tempo and 0xFF).toByte()
                        file.write(buf, 0, 5)
                    } else if (mevent.EventFlag == MetaEvent) {
                        buf[0] = mevent.Metaevent
                        val offset = VarlenToBytes(mevent.Metalength, buf, 1) + 1
                        ArrayCopy(mevent.Value, 0, buf, offset, mevent.Value!!.size)
                        file.write(buf, 0, offset + mevent.Value!!.size)
                    }
                }
            }
            file.close()
        }

        /** Clone the list of MidiEvents  */
        private fun CloneMidiEvents(origlist: ArrayList<ArrayList<MidiEvent>>?): ArrayList<ArrayList<MidiEvent>> {
            val newlist = ArrayList<ArrayList<MidiEvent>>(origlist!!.size)
            for (tracknum in origlist.indices) {
                val origevents = origlist[tracknum]
                val newevents = ArrayList<MidiEvent>(origevents.size)
                newlist.add(newevents)
                for (mevent: MidiEvent in origevents) {
                    newevents.add(mevent.Clone())
                }
            }
            return newlist
        }

        /** Create a new Midi tempo event, with the given tempo   */
        private fun CreateTempoEvent(tempo: Int): MidiEvent {
            val mevent = MidiEvent()
            mevent.DeltaTime = 0
            mevent.StartTime = 0
            mevent.HasEventflag = true
            mevent.EventFlag = MetaEvent
            mevent.Metaevent = MetaEventTempo
            mevent.Metalength = 3
            mevent.Tempo = tempo
            return mevent
        }

        /** Search the events for a ControlChange event with the same
         * channel and control number.  If a matching event is found,
         * update the control value.  Else, add a new ControlChange event.
         */
        private fun UpdateControlChange(newevents: ArrayList<MidiEvent>, changeEvent: MidiEvent) {
            for (mevent: MidiEvent in newevents) {
                if (((mevent.EventFlag == changeEvent.EventFlag) &&
                                (mevent.Channel == changeEvent.Channel) &&
                                (mevent.ControlNum == changeEvent.ControlNum))) {
                    mevent.ControlValue = changeEvent.ControlValue
                    return
                }
            }
            newevents.add(changeEvent)
        }

        /** Start the Midi music at the given pause time (in pulses).
         * Remove any NoteOn/NoteOff events that occur before the pause time.
         * For other events, change the delta-time to 0 if they occur
         * before the pause time.  Return the modified Midi Events.
         */
        private fun StartAtPauseTime(list: ArrayList<ArrayList<MidiEvent>>, pauseTime: Int): ArrayList<ArrayList<MidiEvent>> {
            val newlist = ArrayList<ArrayList<MidiEvent>>(list.size)
            for (tracknum in list.indices) {
                val events = list[tracknum]
                val newevents = ArrayList<MidiEvent>(events.size)
                newlist.add(newevents)
                var foundEventAfterPause = false
                for (mevent: MidiEvent in events) {
                    if (mevent.StartTime < pauseTime) {
                        if (mevent.EventFlag == EventNoteOn ||
                                mevent.EventFlag == EventNoteOff) {

                            /* Skip NoteOn/NoteOff event */
                        } else if (mevent.EventFlag == EventControlChange) {
                            mevent.DeltaTime = 0
                            UpdateControlChange(newevents, mevent)
                        } else {
                            mevent.DeltaTime = 0
                            newevents.add(mevent)
                        }
                    } else if (!foundEventAfterPause) {
                        mevent.DeltaTime = (mevent.StartTime - pauseTime)
                        newevents.add(mevent)
                        foundEventAfterPause = true
                    } else {
                        newevents.add(mevent)
                    }
                }
            }
            return newlist
        }

        /** Shift the starttime of the notes by the given amount.
         * This is used by the Shift Notes menu to shift notes left/right.
         */
        fun ShiftTime(tracks: ArrayList<MidiTrack>, amount: Int) {
            for (track: MidiTrack in tracks) {
                for (note: MidiNote in track.notes) {
                    note.startTime = note.startTime + amount
                }
            }
        }

        /** Shift the note keys up/down by the given amount  */
        fun Transpose(tracks: ArrayList<MidiTrack>, amount: Int) {
            for (track: MidiTrack in tracks) {
                for (note: MidiNote in track.notes) {
                    note.number = note.number + amount
                    if (note.number < 0) {
                        note.number = 0
                    }
                }
            }
        }

        /* Find the highest and lowest notes that overlap this interval (starttime to endtime).
     * This method is used by SplitTrack to determine which staff (top or bottom) a note
     * should go to.
     *
     * For more accurate SplitTrack() results, we limit the interval/duration of this note 
     * (and other notes) to one measure. We care only about high/low notes that are
     * reasonably close to this note.
     */
        private fun FindHighLowNotes(notes: ArrayList<MidiNote>, measurelen: Int, startindex: Int,
                                     starttime: Int, endtime: Int, pair: PairInt) {
            var endtime = endtime
            var i = startindex
            if (starttime + measurelen < endtime) {
                endtime = starttime + measurelen
            }
            while (i < notes.size && notes[i].startTime < endtime) {
                if (notes[i].endTime < starttime) {
                    i++
                    continue
                }
                if (notes[i].startTime + measurelen < starttime) {
                    i++
                    continue
                }
                if (pair.high < notes[i].number) {
                    pair.high = notes[i].number
                }
                if (pair.low > notes[i].number) {
                    pair.low = notes[i].number
                }
                i++
            }
        }

        /* Find the highest and lowest notes that start at this exact start time */
        private fun FindExactHighLowNotes(notes: ArrayList<MidiNote>, startindex: Int, starttime: Int,
                                          pair: PairInt) {
            var i = startindex
            while (notes[i].startTime < starttime) {
                i++
            }
            while (i < notes.size && notes[i].startTime == starttime) {
                if (pair.high < notes[i].number) {
                    pair.high = notes[i].number
                }
                if (pair.low > notes[i].number) {
                    pair.low = notes[i].number
                }
                i++
            }
        }

        /* Split the given MidiTrack into two tracks, top and bottom.
     * The highest notes will go into top, the lowest into bottom.
     * This function is used to split piano songs into left-hand (bottom)
     * and right-hand (top) tracks.
     */
        fun SplitTrack(track: MidiTrack, measurelen: Int): ArrayList<MidiTrack> {
            val notes = track.notes
            val count = notes.size
            val top = MidiTrack(1)
            val bottom = MidiTrack(2)
            val result = ArrayList<MidiTrack>(2)
            result.add(top)
            result.add(bottom)
            if (count == 0) return result
            var prevhigh = 76 /* E5, top of treble staff */
            var prevlow = 45 /* A3, bottom of bass staff */
            var startindex = 0
            for (note: MidiNote in notes) {
                var high: Int
                var low: Int
                var highExact: Int
                var lowExact: Int
                val number = note.number
                lowExact = number
                highExact = lowExact
                low = highExact
                high = low
                while (notes[startindex].endTime < note.startTime) {
                    startindex++
                }

                /* I've tried several algorithms for splitting a track in two,
             * and the one below seems to work the best:
             * - If this note is more than an octave from the high/low notes
             *   (that start exactly at this start time), choose the closest one.
             * - If this note is more than an octave from the high/low notes
             *   (in this note's time duration), choose the closest one.
             * - If the high and low notes (that start exactly at this starttime)
             *   are more than an octave apart, choose the closest note.
             * - If the high and low notes (that overlap this starttime)
             *   are more than an octave apart, choose the closest note.
             * - Else, look at the previous high/low notes that were more than an 
             *   octave apart.  Choose the closeset note.
             */
                val pair = PairInt()
                pair.high = high
                pair.low = low
                val pairExact = PairInt()
                pairExact.high = highExact
                pairExact.low = lowExact
                FindHighLowNotes(notes, measurelen, startindex, note.startTime, note.endTime, pair)
                FindExactHighLowNotes(notes, startindex, note.startTime, pairExact)
                high = pair.high
                low = pair.low
                highExact = pairExact.high
                lowExact = pairExact.low
                if (highExact - number > 12 || number - lowExact > 12) {
                    if (highExact - number <= number - lowExact) {
                        top.AddNote(note)
                    } else {
                        bottom.AddNote(note)
                    }
                } else if (high - number > 12 || number - low > 12) {
                    if (high - number <= number - low) {
                        top.AddNote(note)
                    } else {
                        bottom.AddNote(note)
                    }
                } else if (highExact - lowExact > 12) {
                    if (highExact - number <= number - lowExact) {
                        top.AddNote(note)
                    } else {
                        bottom.AddNote(note)
                    }
                } else if (high - low > 12) {
                    if (high - number <= number - low) {
                        top.AddNote(note)
                    } else {
                        bottom.AddNote(note)
                    }
                } else {
                    if (prevhigh - number <= number - prevlow) {
                        top.AddNote(note)
                    } else {
                        bottom.AddNote(note)
                    }
                }

                /* The prevhigh/prevlow are set to the last high/low
             * that are more than an octave apart.
             */if (high - low > 12) {
                    prevhigh = high
                    prevlow = low
                }
            }
            Collections.sort(top.notes, track.notes[0])
            Collections.sort(bottom.notes, track.notes[0])
            return result
        }

        /** Combine the notes in the given tracks into a single MidiTrack.
         * The individual tracks are already sorted.  To merge them, we
         * use a mergesort-like algorithm.
         */
        fun CombineToSingleTrack(tracks: ArrayList<MidiTrack>): MidiTrack {
            /* Add all notes into one track */
            val result = MidiTrack(1)
            if (tracks.size == 0) {
                return result
            } else if (tracks.size == 1) {
                val track = tracks[0]
                for (note: MidiNote? in track.notes) {
                    result.AddNote((note)!!)
                }
                return result
            }
            val noteindex = IntArray(tracks.size + 1)
            val notecount = IntArray(tracks.size + 1)
            for (tracknum in tracks.indices) {
                noteindex[tracknum] = 0
                notecount[tracknum] = tracks[tracknum].notes.size
            }
            var prevnote: MidiNote? = null
            while (true) {
                var lowestnote: MidiNote? = null
                var lowestTrack = -1
                for (tracknum in tracks.indices) {
                    val track = tracks[tracknum]
                    if (noteindex[tracknum] >= notecount[tracknum]) {
                        continue
                    }
                    val note = track.notes[noteindex[tracknum]]
                    if (lowestnote == null) {
                        lowestnote = note
                        lowestTrack = tracknum
                    } else if (note.startTime < lowestnote.startTime) {
                        lowestnote = note
                        lowestTrack = tracknum
                    } else if (note.startTime == lowestnote.startTime && note.number < lowestnote.number) {
                        lowestnote = note
                        lowestTrack = tracknum
                    }
                }
                if (lowestnote == null) {
                    /* We've finished the merge */
                    break
                }
                noteindex[lowestTrack]++
                if (((prevnote != null) && (prevnote.startTime == lowestnote.startTime) &&
                                (prevnote.number == lowestnote.number))) {

                    /* Don't add duplicate notes, with the same start time and number */
                    if (lowestnote.duration > prevnote.duration) {
                        prevnote.duration = lowestnote.duration
                    }
                } else {
                    result.AddNote(lowestnote)
                    prevnote = lowestnote
                }
            }
            return result
        }

        /** Combine the notes in all the tracks given into two MidiTracks,
         * and return them.
         *
         * This function is intended for piano songs, when we want to display
         * a left-hand track and a right-hand track.  The lower notes go into
         * the left-hand track, and the higher notes go into the right hand
         * track.
         */
        fun CombineToTwoTracks(tracks: ArrayList<MidiTrack>, measurelen: Int): ArrayList<MidiTrack> {
            val single = CombineToSingleTrack(tracks)
            val result = SplitTrack(single, measurelen)
            val lyrics = ArrayList<MidiEvent>()
            for (track: MidiTrack in tracks) {
                if (track.getLyrics() != null) {
                    lyrics.addAll(track.getLyrics()!!)
                }
            }
            if (lyrics.size > 0) {
                Collections.sort(lyrics, lyrics[0])
                result[0].setLyrics(lyrics)
            }
            return result
        }

        /** Check that the MidiNote start times are in increasing order.
         * This is for debugging purposes.
         */
        private fun CheckStartTimes(tracks: ArrayList<MidiTrack>?) {
            for (track: MidiTrack in tracks!!) {
                var prevtime = -1
                for (note: MidiNote in track.notes) {
                    if (note.startTime < prevtime) {
                        throw MidiFileException("Internal parsing error", 0)
                    }
                    prevtime = note.startTime
                }
            }
        }

        /** In Midi Files, time is measured in pulses.  Notes that have
         * pulse times that are close together (like within 10 pulses)
         * will sound like they're the same chord.  We want to draw
         * these notes as a single chord, it makes the sheet music much
         * easier to read.  We don't want to draw notes that are close
         * together as two separate chords.
         *
         * The SymbolSpacing class only aligns notes that have exactly the same
         * start times.  Notes with slightly different start times will
         * appear in separate vertical columns.  This isn't what we want.
         * We want to align notes with approximately the same start times.
         * So, this function is used to assign the same starttime for notes
         * that are close together (timewise).
         */
        fun RoundStartTimes(tracks: ArrayList<MidiTrack>, millisec: Int, time: TimeSignature?) {
            /* Get all the starttimes in all tracks, in sorted order */
            val starttimes = ListInt()
            for (track: MidiTrack in tracks) {
                for (note: MidiNote in track.notes) {
                    starttimes.add(note.startTime)
                }
            }
            starttimes.sort()

            /* Notes within "millisec" milliseconds apart will be combined. */
            val interval = time!!.quarter * millisec * 1000 / time.tempo

            /* If two starttimes are within interval millisec, make them the same */for (i in 0 until starttimes.size() - 1) {
                if (starttimes[i + 1] - starttimes[i] <= interval) {
                    starttimes[i + 1] = starttimes[i]
                }
            }
            CheckStartTimes(tracks)

            /* Adjust the note starttimes, so that it matches one of the starttimes values */for (track: MidiTrack in tracks) {
                var i = 0
                for (note: MidiNote in track.notes) {
                    while (i < starttimes.size() &&
                            note.startTime - interval > starttimes[i]) {
                        i++
                    }
                    if (note.startTime > starttimes[i] &&
                            note.startTime - starttimes[i] <= interval) {
                        note.startTime = starttimes[i]
                    }
                }
                Collections.sort(track.notes, track.notes[0])
            }
        }

        /** We want note durations to span up to the next note in general.
         * The sheet music looks nicer that way.  In contrast, sheet music
         * with lots of 16th/32nd notes separated by small rests doesn't
         * look as nice.  Having nice looking sheet music is more important
         * than faithfully representing the Midi File data.
         *
         * Therefore, this function rounds the duration of MidiNotes up to
         * the next note where possible.
         */
        fun RoundDurations(tracks: ArrayList<MidiTrack>, quarternote: Int) {
            for (track: MidiTrack in tracks) {
                var prevNote: MidiNote? = null
                for (i in 0 until track.notes.size - 1) {
                    val note1 = track.notes[i]
                    if (prevNote == null) {
                        prevNote = note1
                    }

                    /* Get the next note that has a different start time */
                    var note2 = note1
                    for (j in i + 1 until track.notes.size) {
                        note2 = track.notes[j]
                        if (note1.startTime < note2.startTime) {
                            break
                        }
                    }
                    val maxduration = note2.startTime - note1.startTime
                    var dur = 0
                    if (quarternote <= maxduration) dur = quarternote else if (quarternote / 2 <= maxduration) dur = quarternote / 2 else if (quarternote / 3 <= maxduration) dur = quarternote / 3 else if (quarternote / 4 <= maxduration) dur = quarternote / 4
                    if (dur < note1.duration) {
                        dur = note1.duration
                    }

                    /* Special case: If the previous note's duration
                 * matches this note's duration, we can make a notepair.
                 * So don't expand the duration in that case.
                 */if ((prevNote.startTime + prevNote.duration == note1.startTime) &&
                            (prevNote.duration == note1.duration)) {
                        dur = note1.duration
                    }
                    note1.duration = dur
                    if (track.notes[i + 1].startTime != note1.startTime) {
                        prevNote = note1
                    }
                }
            }
        }

        /** Split the given track into multiple tracks, separating each
         * channel into a separate track.
         */
        private fun SplitChannels(origtrack: MidiTrack, events: ArrayList<MidiEvent>): ArrayList<MidiTrack> {

            /* Find the instrument used for each channel */
            val channelInstruments = IntArray(16)
            for (mevent: MidiEvent in events) {
                if (mevent.EventFlag == EventProgramChange) {
                    channelInstruments[mevent.Channel.toInt()] = mevent.Instrument.toInt()
                }
            }
            channelInstruments[9] = 128 /* Channel 9 = Percussion */
            val result = ArrayList<MidiTrack>()
            for (note: MidiNote in origtrack.notes) {
                var foundchannel = false
                for (track: MidiTrack in result) {
                    if (note.channel == track.notes[0].channel) {
                        foundchannel = true
                        track.AddNote(note)
                    }
                }
                if (!foundchannel) {
                    val track = MidiTrack(result.size + 1)
                    track.AddNote(note)
                    track.instrument = channelInstruments[note.channel]
                    result.add(track)
                }
            }
            val lyrics = origtrack.getLyrics()
            if (lyrics != null) {
                for (lyricEvent: MidiEvent in lyrics) {
                    for (track: MidiTrack in result) {
                        if (lyricEvent.Channel.toInt() == track.notes[0].channel) {
                            track.AddLyric(lyricEvent)
                        }
                    }
                }
            }
            return result
        }

        /** Return true if the data starts with the header MTrk  */
        fun hasMidiHeader(data: ByteArray?): Boolean {
            val s: String
            s = String((data)!!, 0, 4, StandardCharsets.US_ASCII)
            return (s == "MThd")
        }

        /* Command-line program to print out a parsed Midi file. Used for debugging.
     * To run:
     * - Change main2 to main
     * - javac MidiFile.java
     * - java MidiFile file.mid
     *
     */
        fun main2(args: Array<String?>?) {
            /*
        if (args.length == 0) {
            System.out.println("Usage: MidiFile <filename>");
            return;
        }
        String filename = args[0];
        byte[] data;
        try {
            File info = new File(filename);
            FileInputStream file = new FileInputStream(filename);

            data = new byte[ (int)info.length() ];
            int offset = 0;
            int len = (int)info.length();
            while (true) {
                if (offset == len)
                    break;
                int n = file.read(data, offset, len- offset);
                if (n <= 0)
                    break;
                offset += n;
            }
            file.close();
        }
        catch(IOException e) {
            return;
        }

        MidiFile f = new MidiFile(data, "");
        System.out.print(f.toString());
        */
        }
    }

    /** Create a new MidiFile from the byte[]  */
    init {
        parse(rawdata)
    }
} /* End class MidiFile */