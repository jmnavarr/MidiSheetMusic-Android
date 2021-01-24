/*
 * Copyright (c) 2013 Madhav Vaidyanathan
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

import android.os.Handler
import android.view.MotionEvent
import android.view.animation.AnimationUtils

/** @class ScrollAnimation
 */
class ScrollAnimation(
        /** Fling lasts 3 sec  */
        var listener: ScrollAnimationListener,
        /** Callback to invoke when scroll position changes  */
        private val scrollVert: Boolean)
{
    private val timerInterval = 50.0f

    /** Call timer every 50 msec  */
    private val totalFlingTime = 3000.0f

    /** True if we're scrolling vertically  */
    private var inMotion = false

    /** True if we're in a motion event  */
    private var downX = 0f

    /** x-pixel when down touch occurs  */
    private var downY = 0f

    /** y-pixel when down touch occurs  */
    private var moveX = 0f

    /** x-pixel when move touch occurs  */
    private var moveY = 0f

    /** y-pixel when move touch occurs  */
    private var prevMoveX = 0f

    /** x-pixel when previous move touch occurs  */
    private var prevMoveY = 0f

    /** y-pixel when previous move touch occurs  */
    private var upX = 0f

    /** x-pixel when up touch occurs  */
    private var upY = 0f

    /** y-pixel when up touch occurs  */
    private var deltaX = 0f

    /** change in x-pixel from move touch  */
    private var deltaY = 0f

    /** change in y-pixel from move touch  */
    private var downTime: Long = 0

    /** Time (millisec) when down touch occurs  */
    private var moveTime: Long = 0

    /** Time (millisec) when move touch occurs  */
    private var prevMoveTime: Long = 0

    /** Time (millisec) when previous move touch occurs  */
    private var upTime: Long = 0

    /** Time (millisec) when up touch occurs  */
    private var flingStartTime: Long = 0

    /** Time (millisec) when up fling started  */
    private var flingVelocity = 0f

    /** Initial fling velocity (pixels/sec)  */
    private var velocityX = 0f

    /** Velocity of move (pixels/sec)  */
    private var velocityY = 0f

    /** Velocity of move (pixels/sec)  */
    // timer for doing 'fling' scrolling
    private val scrollTimer: Handler = Handler()

    /* Motion has stopped */
    fun stopMotion() {
        inMotion = false
        velocityY = 0f
        velocityX = velocityY
        deltaY = velocityX
        deltaX = deltaY
        upY = deltaX
        upX = upY
        prevMoveY = upX
        prevMoveX = prevMoveY
        moveY = prevMoveX
        moveX = moveY
        downY = moveX
        downX = downY
        flingStartTime = 0
        upTime = flingStartTime
        moveTime = upTime
        prevMoveTime = moveTime
        downTime = prevMoveTime
        flingVelocity = 0f
        scrollTimer.removeCallbacks(flingScroll)
    }

    /** Handle touch/motion events to implement scrolling the sheet music.
     * - On down touch, store the (x,y) of the touch
     * - On a motion event, calculate the delta (change) in x, y.
     * Update the scrolX, scrollY and redraw the sheet music.
     * - On a up touch, implement a 'fling'.  Call flingScroll
     * every 50 msec for the next 2 seconds.
     */
    fun onTouchEvent(event: MotionEvent): Boolean {
        val action = event.action and MotionEvent.ACTION_MASK
        val currentTime: Long
        return when (action) {
            MotionEvent.ACTION_DOWN -> {
                stopMotion()
                inMotion = true
                run {
                    downX = event.x
                    moveX = downX
                    prevMoveX = moveX
                }
                run {
                    downY = event.y
                    moveY = downY
                    prevMoveY = moveY
                }
                run {
                    downTime = AnimationUtils.currentAnimationTimeMillis()
                    moveTime = downTime
                    prevMoveTime = moveTime
                }
                true
            }
            MotionEvent.ACTION_MOVE -> {
                if (!inMotion) return false
                currentTime = AnimationUtils.currentAnimationTimeMillis()
                velocityX = (prevMoveX - event.x) * 1000.0f / (currentTime - prevMoveTime)
                velocityY = (prevMoveY - event.y) * 1000.0f / (currentTime - prevMoveTime)
                deltaX = moveX - event.x
                deltaY = moveY - event.y
                prevMoveX = moveX
                prevMoveY = moveY
                prevMoveTime = moveTime
                moveX = event.x
                moveY = event.y
                moveTime = currentTime

                // If this is a tap, do nothing.
                if (currentTime - downTime < 500 && Math.abs(moveX - downX) <= 20 && Math.abs(moveY - downY) <= 20) {
                    return true
                }
                if (scrollVert) {
                    listener.scrollUpdate(0, deltaY.toInt())
                } else {
                    if (Math.abs(deltaY) > Math.abs(deltaX) || Math.abs(deltaY) > 4) {
                        listener.scrollUpdate(deltaX.toInt(), deltaY.toInt())
                    } else {
                        listener.scrollUpdate(deltaX.toInt(), 0)
                    }
                }
                true
            }
            MotionEvent.ACTION_UP -> {
                if (!inMotion) return false
                inMotion = false
                upTime = AnimationUtils.currentAnimationTimeMillis()
                upX = event.x
                upY = event.y
                val overallDeltaX = upX - downX
                val overallDeltaY = upY - downY

                // If this is a tap, inform the listener.
                if (upTime - downTime < 500 && Math.abs(overallDeltaX) <= 20 && Math.abs(overallDeltaY) <= 20) {
                    listener.scrollTapped(downX.toInt(), downY.toInt())
                    return true
                }
                if (scrollVert) {
                    if (Math.abs(overallDeltaY) <= 5) {
                        return true
                    } else if (Math.abs(velocityY) < 20) {
                        return true
                    }
                } else {
                    if (Math.abs(overallDeltaX) <= 5) {
                        return true
                    } else if (Math.abs(velocityX) < 20) {
                        return true
                    }
                }

                /* Keep scrolling for several seconds (fling). */flingStartTime = upTime
                val scale = 0.95f
                flingVelocity = if (scrollVert) {
                    scale * velocityY
                } else {
                    scale * velocityX
                }
                scrollTimer.postDelayed(flingScroll, timerInterval.toLong())
                true
            }
            else -> false
        }
    }

    /** The timer callback for doing 'fling' scrolling.
     * Adjust the scrollX/scrollY using the last delta.
     * Redraw the sheet music.
     * Then, schedule this timer again.
     */
    var flingScroll: Runnable = object : Runnable {
        override fun run() {
            val currentTime = AnimationUtils.currentAnimationTimeMillis()
            val percentDone = (currentTime - flingStartTime) / totalFlingTime
            if (percentDone >= 1.0f) {
                return
            }
            val exp = -2.0f + percentDone * 2.0f // -2 to 0
            val scale = (1.0f - Math.pow(Math.E, exp.toDouble())).toFloat() // 0.99 to 0
            val velocity = flingVelocity * scale
            val delta = velocity * 1.0f / timerInterval
            if (scrollVert) {
                listener.scrollUpdate(0, delta.toInt())
            } else {
                listener.scrollUpdate(delta.toInt(), 0)
            }
            if (percentDone < 1.0) {
                scrollTimer.postDelayed(this, timerInterval.toLong())
            }
        }
    }

}