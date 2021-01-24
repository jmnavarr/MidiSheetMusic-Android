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
package com.midisheetmusic.sheets

import java.util.*

/** @class SymbolWidths
 * The SymbolWidths class is used to vertically align notes in different
 * tracks that occur at the same time (that have the same starttime).
 * This is done by the following:
 * - Store a list of all the start times.
 * - Store the width of symbols for each start time, for each track.
 * - Store the maximum width for each start time, across all tracks.
 * - Get the extra width needed for each track to match the maximum
 * width for that start time.
 *
 * See method SheetMusic.AlignSymbols(), which uses this class.
 */
class SymbolWidths(tracks: ArrayList<ArrayList<MusicSymbol>>, tracklyrics: ArrayList<ArrayList<LyricSymbol?>?>?) {
    /** Array of maps (starttime -> symbol width), one per track  */
    private val widths: Array<DictInt?>

    /** Map of starttime -> maximum symbol width  */
    private val maxwidths: DictInt
    /** Return an array of all the start times in all the tracks  */
    /** An array of all the starttimes, in all tracks  */
    val startTimes: IntArray

    /** Given a track and a start time, return the extra width needed so that
     * the symbols for that start time align with the other tracks.
     */
    fun GetExtraWidth(track: Int, start: Int): Int {
        return if (!widths[track]!!.contains(start)) {
            maxwidths[start]
        } else {
            maxwidths[start] - widths[track]!![start]
        }
    }

    companion object {
        /** Create a table of the symbol widths for each starttime in the track.  */
        private fun GetTrackWidths(symbols: ArrayList<MusicSymbol>): DictInt {
            val widths = DictInt()
            for (m in symbols) {
                val start = m.startTime
                val w = m.minWidth
                if (m is BarSymbol) {
                    continue
                } else if (widths.contains(start)) {
                    widths[start] = widths[start] + w
                } else {
                    widths[start] = w
                }
            }
            return widths
        }
    }

    /** Initialize the symbol width maps, given all the symbols in
     * all the tracks.
     */
    init {

        /* Get the symbol widths for all the tracks */
        widths = arrayOfNulls(tracks.size)
        for (track in tracks.indices) {
            widths[track] = GetTrackWidths(tracks[track])
        }
        maxwidths = DictInt()

        /* Calculate the maximum symbol widths */for (dict in widths) {
            for (i in 0 until dict!!.count()) {
                val time = dict.getKey(i)
                if (!maxwidths.contains(time) ||
                        maxwidths[time] < dict[time]) {
                    maxwidths[time] = dict[time]
                }
            }
        }
        if (tracklyrics != null) {
            for (lyrics in tracklyrics) {
                if (lyrics == null) {
                    continue
                }
                for (lyric in lyrics) {
                    val width = lyric!!.minWidth
                    val time = lyric.startTime
                    if (!maxwidths.contains(time) ||
                            maxwidths[time] < width) {
                        maxwidths[time] = width
                    }
                }
            }
        }

        /* Store all the start times to the starttime array */startTimes = IntArray(maxwidths.count())
        for (i in 0 until maxwidths.count()) {
            val key = maxwidths.getKey(i)
            startTimes[i] = key
        }
        Arrays.sort(startTimes)
    }
}