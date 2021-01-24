package com.midisheetmusic

/**
 * A listener that allows [MidiPlayer] to send a request
 * to [SheetMusicActivity] to update the sheet when it
 * changes the settings
 */
interface SheetUpdateRequestListener {
    fun onSheetUpdateRequest()
}