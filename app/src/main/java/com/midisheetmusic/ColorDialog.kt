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
/* ColorPickerDialog code
 *
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.midisheetmusic

import android.app.Dialog
import android.content.Context
import android.graphics.*
import android.os.Bundle
import android.view.MotionEvent
import android.view.View

/** @class ColorDialog
 * Display a dialog for choosing a color.
 * The initial color and a callback are passed as arguments.
 * When the user selects a color, the dialog is dismissed, and
 * the callback listener is invoked.
 */
class ColorDialog(context: Context?, private val listener: ColorChangedListener, private val selectedColor: Int) : Dialog(context) {
    override fun onCreate(state: Bundle) {
        super.onCreate(state)
        val listen: ColorChangedListener = object : ColorChangedListener {
            override fun colorChanged(color: Int) {
                listener.colorChanged(color)
                dismiss()
            }
        }
        setContentView(ColorView(context, listen, selectedColor))
        setTitle("Pick a Color")
    }
}

/** @class ColorView
 * Display a circle showing various colors to choose from.
 * On the top left corner, display a preview of the selected color.
 */
internal class ColorView(context: Context?, private val listener: ColorChangedListener, /* Currently selected color */private var selectedColor: Int) : View(context) {
    private lateinit var colorRings /* Rings of color to display */: Array<Paint?>
    private var colorPreview /* Small circle showing preview of color */: Paint? = null
    private var center /* The center of the circle */ = 100
    private var circleRadius /* The radius of the circle */ = 90

    /* Return the color wheel colors, for the given percent.
     * Percent is from 0.0 to 1.0, from center to outer-rim.
     * 0.0 is white
     * 1.0 is the brighest main color (pure red, pure green, etc)
     */
    private fun colorsForRing(percent: Float): IntArray {
        var percent = percent
        if (percent < 0) percent = 0f
        if (percent > 1) percent = 1f
        percent = 1 - percent
        val colors = IntArray(7)
        colors[0] = Color.rgb(255, (255 * percent).toInt(), (255 * percent).toInt())
        colors[1] = Color.rgb(255, (255 * percent).toInt(), 255)
        colors[2] = Color.rgb((255 * percent).toInt(), (255 * percent).toInt(), 255)
        colors[3] = Color.rgb((255 * percent).toInt(), 255, 255)
        colors[4] = Color.rgb((255 * percent).toInt(), 255, (255 * percent).toInt())
        colors[5] = Color.rgb(255, 255, (255 * percent).toInt())
        colors[6] = Color.rgb(255, (255 * percent).toInt(), (255 * percent).toInt())
        return colors
    }

    /* Create the color wheel.
     * Create 64 color rings, where each rings displays a rainbow gradient.
     */
    private fun initColorRings() {
        colorRings = arrayOfNulls(64)
        for (i in 0..63) {
            colorRings[i] = Paint(Paint.ANTI_ALIAS_FLAG)
            val s: Shader = SweepGradient(0 as Float, 0 as Float, colorsForRing(i / 64.0f), null)
            colorRings[i]!!.shader = s
            colorRings[i]!!.style = Paint.Style.STROKE
            colorRings[i]!!.strokeWidth = circleRadius / 64.0f + 0.5f
        }
        colorPreview = Paint(Paint.ANTI_ALIAS_FLAG)
        colorPreview!!.color = selectedColor
    }

    /** Draw a preview of the selected color in the top-left corner.
     * Draw the full color circle, by drawing concentric ovals
     * with increasing radius, using the colorRing gradients.
     */
    override fun onDraw(canvas: Canvas) {
        // TODO: Avoid object allocations during draw to enhance performance
        canvas.drawRoundRect(RectF((center / 10).toFloat(), (center / 10).toFloat(), (center / 4).toFloat(), (center / 4).toFloat()), 5f, 5f, colorPreview)
        canvas.translate(center.toFloat(), center.toFloat())
        for (i in 1 until colorRings.size) {
            val radius = circleRadius * i * 1.0f / (colorRings.size - 1)
            // radius -= colorRings[i].getStrokeWidth()/2.0;
            canvas.drawOval(RectF(-radius, -radius, radius, radius), colorRings[i])
        }
    }

    /** Set the circle's center, based on the available width/height  */
    override fun onMeasure(widthspec: Int, heightspec: Int) {
        val specwidth = MeasureSpec.getSize(widthspec)
        val specheight = MeasureSpec.getSize(heightspec)
        center = specwidth / 2
        if (specheight > 0 && specheight < specwidth) {
            center = specheight / 2
        }
        if (center <= 0) {
            center = 100
        }
        circleRadius = center - 10
        setMeasuredDimension(center * 2, center * 2)
        initColorRings()
    }

    /* Return the averagerage of the two colors, using the given percent */
    private fun average(color1: Int, color2: Int, percent: Float): Int {
        return color1 + Math.round(percent * (color2 - color1))
    }

    /* Given the radius and angle (from 0 to 1) determine the color selected.  */
    private fun calculateColor(radius: Float, angleUnit: Float): Int {
        val colors = colorsForRing(radius / circleRadius)
        if (angleUnit <= 0) {
            return colors[0]
        }
        if (angleUnit >= 1) {
            return colors[colors.size - 1]
        }
        var p = angleUnit * (colors.size - 1)
        val i = p.toInt()
        p -= i.toFloat()

        // now p is just the fractional part [0...1) and i is the index
        val c0 = colors[i]
        val c1 = colors[i + 1]
        val a = average(Color.alpha(c0), Color.alpha(c1), p)
        val r = average(Color.red(c0), Color.red(c1), p)
        val g = average(Color.green(c0), Color.green(c1), p)
        val b = average(Color.blue(c0), Color.blue(c1), p)
        return Color.argb(a, r, g, b)
    }

    /** When the user clicks on the color wheel, update
     * the selected color, and the preview pane.
     *
     * When they click outside the wheel, dismiss the dialog.
     */
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x - center
        val y = event.y - center
        val radius = Math.sqrt((x * x + y * y).toDouble()).toFloat()
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                if (radius > circleRadius) {
                    // break
                }
                val angle = Math.atan2(y.toDouble(), x.toDouble()).toFloat()
                // need to turn angle [-PI ... PI] into unit [0....1]
                var angleUnit = angle / (2 * PI)
                if (angleUnit < 0) {
                    angleUnit += 1f
                }
                selectedColor = calculateColor(radius, angleUnit)
                colorPreview!!.color = selectedColor
                invalidate()
            }
            MotionEvent.ACTION_UP -> if (radius > circleRadius) {
                listener.colorChanged(colorPreview!!.color)
            }
        }
        return true
    }

    companion object {
        private const val PI = 3.1415926f
    }
}