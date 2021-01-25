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

import java.nio.charset.StandardCharsets
import kotlin.experimental.and

/** @class MidiFileReader
 * The MidiFileReader is used to read low-level binary data from a file.
 * This class can do the following:
 *
 * - Peek at the next byte in the file.
 * - Read a byte
 * - Read a 16-bit big endian short
 * - Read a 32-bit big endian int
 * - Read a fixed length ascii string (not null terminated)
 * - Read a "variable length" integer.  The format of the variable length
 * int is described at the top of this file.
 * - Skip ahead a given number of bytes
 * - Return the current offset.
 */
class MidiFileReader
/** Create a new MidiFileReader from the given data  */(private val data: ByteArray) {
    /** The entire midi file data  */
    private var parse_offset = 0

    /** Check that the given number of bytes doesn't exceed the file size  */
    private fun checkRead(amount: Int) {
        if (parse_offset + amount > data.size) {
            throw MidiFileException("File is truncated", parse_offset)
        }
    }

    /** Read the next byte in the file, but don't increment the parse offset  */
    fun Peek(): Byte {
        checkRead(1)
        return data[parse_offset]
    }

    /** Read a byte from the file  */
    fun ReadByte(): Byte {
        checkRead(1)
        val x = data[parse_offset]
        parse_offset++
        return x
    }

    /** Read the given number of bytes from the file  */
    fun ReadBytes(amount: Int): ByteArray {
        checkRead(amount)
        val result = ByteArray(amount)
        for (i in 0 until amount) {
            result[i] = data[i + parse_offset]
        }
        parse_offset += amount
        return result
    }

    /** Read a 16-bit short from the file  */
    fun ReadShort(): Int {
        checkRead(2)
        val x: Int = ((data[parse_offset] and 0xFF.toByte()).toInt() shl 8) or
                (data[parse_offset + 1] and 0xFF.toByte()).toInt()
        parse_offset += 2
        return x
    }

    /** Read a 32-bit int from the file  */
    fun ReadInt(): Int {
        checkRead(4)
        val x: Int = (data[parse_offset] and 0xFF.toByte()).toInt() shl 24 or
                ((data[parse_offset + 1] and 0xFF.toByte()).toInt() shl 16) or
                ((data[parse_offset + 2] and 0xFF.toByte()).toInt() shl 8) or
                (data[parse_offset + 3] and 0xFF.toByte()).toInt()
        parse_offset += 4
        return x
    }

    /** Read an ascii String with the given length  */
    fun ReadAscii(len: Int): String {
        checkRead(len)
        var s = ""
        s = String(data, parse_offset, len, StandardCharsets.US_ASCII)
        parse_offset += len
        return s
    }

    /** Read a variable-length integer (1 to 4 bytes). The integer ends
     * when you encounter a byte that doesn't have the 8th bit set
     * (a byte less than 0x80).
     */
    fun ReadVarlen(): Int {
        var result = 0
        var b: Byte
        b = ReadByte()
        result = (b and 0x7f).toInt()
        for (i in 0..2) {
            if ((b and 0x80.toByte()).toInt() != 0) {
                b = ReadByte()
                result = ((result shl 7) + (b and 0x7f))
            } else {
                break
            }
        }
        return result
    }

    /** Skip over the given number of bytes  */
    fun Skip(amount: Int) {
        checkRead(amount)
        parse_offset += amount
    }

    /** Return the current parse offset  */
    fun GetOffset(): Int {
        return parse_offset
    }

    /** Return the raw midi file byte data  */
    fun GetData(): ByteArray {
        return data
    }
    /** The current offset while parsing  */
    /** Create a new MidiFileReader for the given filename  */ /* Not used
    public MidiFileReader(String filename) {
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

            parse_offset = 0;
        }
        catch (IOException e) {
            throw new MidiFileException("Cannot open file " + filename, 0);
        }
    }
    */
}