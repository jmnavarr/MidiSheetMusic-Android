/*
 * Copyright (c) 2011-2012 Madhav Vaidyanathan
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
import android.net.Uri
import org.json.JSONException
import org.json.JSONObject
import java.io.FileInputStream
import java.io.InputStream
import java.util.*

/** @class FileUri
 * Represents a reference to a file.
 * The file could be either in the /assets directory,
 * the internal storage, or the external storage.
 */
class FileUri(uri: Uri, path: String?) : Comparator<FileUri?> {
    /** Return the uri  */
    val uri: Uri

    /** The URI path to the file  */
    private val displayName: String

    /** Return the display name  */
    override fun toString(): String {
        return displayName
    }

    /** Return true if this is a directory  */
    val isDirectory: Boolean
        get() {
            val path = uri.path
            return path != null && path.endsWith("/")
        }

    /** Compare two files by their display name  */
    override fun compare(f1: FileUri?, f2: FileUri?): Int {
        return f1!!.displayName.compareTo(f2!!.displayName, ignoreCase = true)
    }

    /** Return the file contents as a byte array.
     * If any IO error occurs, return null.
     */
    fun getData(activity: Activity): ByteArray? {
        return try {
            var data: ByteArray
            var totallen: Int
            var len: Int
            var offset: Int

            // First, determine the file length
            data = ByteArray(4096)
            var file: InputStream
            val uriString = uri.toString()
            file = if (uriString.startsWith("file:///android_asset/")) {
                val asset = activity.resources.assets
                val filepath = uriString.replace("file:///android_asset/", "")
                asset.open(filepath)
            } else if (uriString.startsWith("content://")) {
                val resolver = activity.contentResolver
                resolver.openInputStream(uri)
            } else {
                FileInputStream(uri.path)
            }
            totallen = 0
            len = file.read(data, 0, 4096)
            while (len > 0) {
                totallen += len
                len = file.read(data, 0, 4096)
            }
            file.close()

            // Now read in the data
            offset = 0
            data = ByteArray(totallen)
            file = if (uriString.startsWith("file:///android_asset/")) {
                val asset = activity.resources.assets
                val filepath = uriString.replace("file:///android_asset/", "")
                asset.open(filepath)
            } else if (uriString.startsWith("content://")) {
                val resolver = activity.contentResolver
                resolver.openInputStream(uri)
            } else {
                FileInputStream(uri.path)
            }
            while (offset < totallen) {
                len = file.read(data, offset, totallen - offset)
                if (len <= 0) {
                    throw MidiFileException("Error reading midi file", offset)
                }
                offset += len
            }
            data
        } catch (e: Exception) {
            null
        }
    }

    /* Convert this URI to a JSON string */
    fun toJson(): JSONObject? {
        return try {
            val json = JSONObject()
            json.put("uri", uri.toString())
            json.put("displayName", displayName)
            json
        } catch (e: JSONException) {
            null
        } catch (e: NullPointerException) {
            null
        }
    }

    companion object {
        /** Given a path name, return a display name  */
        fun displayNameFromPath(path: String?): String {
            var displayName = path
            displayName = displayName!!.replace("__", ": ")
            displayName = displayName.replace("_", " ")
            displayName = displayName.replace(".mid", "")
            return displayName
        }

        /* Initialize this URI from a json string */
        fun fromJson(obj: JSONObject, activity: Activity?): FileUri? {
            return try {
                val displayName = obj.optString("displayName", null)
                val uriString = obj.optString("uri", null)
                if (displayName == null || uriString == null) {
                    return null
                }
                val uri = Uri.parse(uriString)
                FileUri(uri, displayName)
            } catch (e: Exception) {
                null
            }
        }

        fun equalStrings(s1: String?, s2: String?): Boolean {
            if (s1 == null && s2 != null ||
                    s1 != null && s2 == null) {
                return false
            }
            return if (s1 == null && s2 == null) {
                true
            } else s1 == s2
        }

        /* Return true if the two FileUri json objects are equal */
        @JvmStatic
        fun equalJson(obj1: JSONObject, obj2: JSONObject): Boolean {
            val displayName1 = obj1.optString("displayName", null)
            val uriString1 = obj1.optString("uri", null)
            val displayName2 = obj2.optString("displayName", null)
            val uriString2 = obj2.optString("uri", null)
            return equalStrings(displayName1, displayName2) &&
                    equalStrings(uriString1, uriString2)
        }
    }
    /** The name to display  */
    /** Create a Uri with the given display name  */
    init {
        var path = path
        this.uri = uri
        if (path == null) {
            path = uri.lastPathSegment
        }
        displayName = displayNameFromPath(path)
    }
}