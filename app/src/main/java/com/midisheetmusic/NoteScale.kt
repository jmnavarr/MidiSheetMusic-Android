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

/** Enumeration of the notes in a scale (A, A#, ... G#)  */
object NoteScale {
    const val A = 0
    const val Asharp = 1
    const val Bflat = 1
    const val B = 2
    const val C = 3
    const val Csharp = 4
    const val Dflat = 4
    const val D = 5
    const val Dsharp = 6
    const val Eflat = 6
    const val E = 7
    const val F = 8
    const val Fsharp = 9
    const val Gflat = 9
    const val G = 10
    const val Gsharp = 11
    const val Aflat = 11

    /** Convert a note (A, A#, B, etc) and octave into a
     * Midi Note number.
     */
    @JvmStatic
    fun ToNumber(notescale: Int, octave: Int): Int {
        return 9 + notescale + octave * 12
    }

    /** Convert a Midi note number into a notescale (A, A#, B)  */
    @JvmStatic
    fun FromNumber(number: Int): Int {
        return (number + 3) % 12
    }

    /** Return true if this notescale number is a black key  */
    @JvmStatic
    fun IsBlackKey(notescale: Int): Boolean {
        return notescale == Asharp || notescale == Csharp || notescale == Dsharp || notescale == Fsharp || notescale == Gsharp
    }
}