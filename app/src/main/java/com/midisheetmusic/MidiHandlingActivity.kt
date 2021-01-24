package com.midisheetmusic

import android.graphics.Color
import android.hardware.usb.UsbDevice
import android.view.View
import android.widget.Button
import android.widget.Toast
import jp.kshoji.driver.midi.activity.AbstractSingleMidiActivity
import jp.kshoji.driver.midi.device.MidiInputDevice
import jp.kshoji.driver.midi.device.MidiOutputDevice

abstract class MidiHandlingActivity : AbstractSingleMidiActivity() {
    fun log(s: String?) {
        runOnUiThread { Toast.makeText(this@MidiHandlingActivity, s, Toast.LENGTH_SHORT).show() }
    }

    abstract fun OnMidiDeviceStatus(connected: Boolean)
    abstract fun OnMidiNote(note: Int, pressed: Boolean)
    override fun onDeviceAttached(usbDevice: UsbDevice) {
        //deprecated
    }

    override fun onMidiInputDeviceAttached(midiInputDevice: MidiInputDevice) {
        OnMidiDeviceStatus(true)
        (findViewById<View>(R.id.btn_midi) as Button).setTextColor(Color.BLUE)
        log("MIDI Input device connected: " + midiInputDevice.manufacturerName + " - " + midiInputDevice.productName)
    }

    override fun onMidiOutputDeviceAttached(midiOutputDevice: MidiOutputDevice) {}
    override fun onDeviceDetached(usbDevice: UsbDevice) {
        //deprecated
    }

    override fun onMidiInputDeviceDetached(midiInputDevice: MidiInputDevice) {
        OnMidiDeviceStatus(false)
        log("MIDI Input device disconnected")
    }

    override fun onMidiOutputDeviceDetached(midiOutputDevice: MidiOutputDevice) {}
    override fun onMidiMiscellaneousFunctionCodes(midiInputDevice: MidiInputDevice, i: Int, i1: Int, i2: Int, i3: Int) {}
    override fun onMidiCableEvents(midiInputDevice: MidiInputDevice, i: Int, i1: Int, i2: Int, i3: Int) {}
    override fun onMidiSystemCommonMessage(midiInputDevice: MidiInputDevice, i: Int, bytes: ByteArray) {}
    override fun onMidiSystemExclusive(midiInputDevice: MidiInputDevice, i: Int, bytes: ByteArray) {}
    override fun onMidiNoteOff(midiInputDevice: MidiInputDevice, i: Int, i1: Int, note: Int, velocity: Int) {
        //OnMidiNote(note, false);
    }

    override fun onMidiNoteOn(midiInputDevice: MidiInputDevice, i: Int, i1: Int, note: Int, velocity: Int) {
        OnMidiNote(note, true)
    }

    override fun onMidiPolyphonicAftertouch(midiInputDevice: MidiInputDevice, i: Int, i1: Int, i2: Int, i3: Int) {}
    override fun onMidiControlChange(midiInputDevice: MidiInputDevice, i: Int, i1: Int, i2: Int, i3: Int) {}
    override fun onMidiProgramChange(midiInputDevice: MidiInputDevice, i: Int, i1: Int, i2: Int) {}
    override fun onMidiChannelAftertouch(midiInputDevice: MidiInputDevice, i: Int, i1: Int, i2: Int) {}
    override fun onMidiPitchWheel(midiInputDevice: MidiInputDevice, i: Int, i1: Int, i2: Int) {}
    override fun onMidiSingleByte(midiInputDevice: MidiInputDevice, i: Int, i1: Int) {}
    override fun onMidiTimeCodeQuarterFrame(midiInputDevice: MidiInputDevice, i: Int, i1: Int) {}
    override fun onMidiSongSelect(midiInputDevice: MidiInputDevice, i: Int, i1: Int) {}
    override fun onMidiSongPositionPointer(midiInputDevice: MidiInputDevice, i: Int, i1: Int) {}
    override fun onMidiTuneRequest(midiInputDevice: MidiInputDevice, i: Int) {}
    override fun onMidiTimingClock(midiInputDevice: MidiInputDevice, i: Int) {}
    override fun onMidiStart(midiInputDevice: MidiInputDevice, i: Int) {}
    override fun onMidiContinue(midiInputDevice: MidiInputDevice, i: Int) {}
    override fun onMidiStop(midiInputDevice: MidiInputDevice, i: Int) {}
    override fun onMidiActiveSensing(midiInputDevice: MidiInputDevice, i: Int) {}
    override fun onMidiReset(midiInputDevice: MidiInputDevice, i: Int) {}
}