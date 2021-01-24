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

import java.util.*

/** @class MidiEvent
 * A MidiEvent represents a single event (such as EventNoteOn) in the
 * Midi file. It includes the delta time of the event.
 */
class MidiEvent
/** The raw byte value, for Sysex and meta events  */
    : Comparator<MidiEvent?> {
    @JvmField
    var DeltaTime = 0

    /** The time between the previous event and this on  */
    @JvmField
    var StartTime = 0

    /** The absolute time this event occurs  */
    @JvmField
    var HasEventflag = false

    /** False if this is using the previous eventflag  */
    @JvmField
    var EventFlag: Byte = 0

    /** NoteOn, NoteOff, etc.  Full list is in class MidiFile  */
    @JvmField
    var Channel: Byte = 0

    /** The channel this event occurs on  */
    @JvmField
    var Notenumber: Byte = 0

    /** The note number   */
    @JvmField
    var Velocity: Byte = 0

    /** The volume of the note  */
    @JvmField
    var Instrument: Byte = 0

    /** The instrument  */
    @JvmField
    var KeyPressure: Byte = 0

    /** The key pressure  */
    @JvmField
    var ChanPressure: Byte = 0

    /** The channel pressure  */
    @JvmField
    var ControlNum: Byte = 0

    /** The controller number  */
    @JvmField
    var ControlValue: Byte = 0

    /** The controller value  */
    @JvmField
    var PitchBend: Short = 0

    /** The pitch bend value  */
    @JvmField
    var Numerator: Byte = 0

    /** The numerator, for TimeSignature meta events  */
    @JvmField
    var Denominator: Byte = 0

    /** The denominator, for TimeSignature meta events  */
    @JvmField
    var Tempo = 0

    /** The tempo, for Tempo meta events  */
    @JvmField
    var Metaevent: Byte = 0

    /** The metaevent, used if eventflag is MetaEvent  */
    @JvmField
    var Metalength = 0

    /** The metaevent length   */
    @JvmField
    var Value: ByteArray? = null

    /** Return a copy of this event  */
    fun Clone(): MidiEvent {
        val mevent = MidiEvent()
        mevent.DeltaTime = DeltaTime
        mevent.StartTime = StartTime
        mevent.HasEventflag = HasEventflag
        mevent.EventFlag = EventFlag
        mevent.Channel = Channel
        mevent.Notenumber = Notenumber
        mevent.Velocity = Velocity
        mevent.Instrument = Instrument
        mevent.KeyPressure = KeyPressure
        mevent.ChanPressure = ChanPressure
        mevent.ControlNum = ControlNum
        mevent.ControlValue = ControlValue
        mevent.PitchBend = PitchBend
        mevent.Numerator = Numerator
        mevent.Denominator = Denominator
        mevent.Tempo = Tempo
        mevent.Metaevent = Metaevent
        mevent.Metalength = Metalength
        mevent.Value = Value
        return mevent
    }

    /** Compare two MidiEvents based on their start times.  */
    override fun compare(x: MidiEvent?, y: MidiEvent?): Int {
        return if (x!!.StartTime == y!!.StartTime) {
            if (x.EventFlag == y.EventFlag) {
                x.Notenumber - y.Notenumber
            } else {
                x.EventFlag - y.EventFlag
            }
        } else {
            x.StartTime - y.StartTime
        }
    }
}