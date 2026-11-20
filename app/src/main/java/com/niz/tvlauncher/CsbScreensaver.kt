package com.niz.tvlauncher

import android.os.Handler
import android.os.Looper
import android.service.dreams.DreamService
import android.view.View
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * The screensaver: logo in ecru on black, big clock, date with a red dot.
 * The whole block drifts slowly each minute so nothing burns into the panel.
 */
class CsbScreensaver : DreamService() {

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var clock: TextView
    private lateinit var date: TextView
    private lateinit var block: View

    private val timeFmt = SimpleDateFormat("HH:mm", Locale.UK)
    private val dateFmt = SimpleDateFormat("EEEE d MMMM", Locale.UK)

    private var step = 0

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        isFullscreen = true
        isInteractive = false
        setContentView(R.layout.screensaver)
        clock = findViewById(R.id.saver_clock)
        date = findViewById(R.id.saver_date)
        block = findViewById(R.id.saver_block)
        tick()
    }

    private val ticker = object : Runnable {
        override fun run() {
            tick()
            handler.postDelayed(this, 30_000)   // refresh twice a minute
        }
    }

    override fun onDreamingStarted() {
        super.onDreamingStarted()
        handler.post(ticker)
    }

    override fun onDreamingStopped() {
        handler.removeCallbacksAndMessages(null)
        super.onDreamingStopped()
    }

    private fun tick() {
        val now = Date()
        clock.text = timeFmt.format(now)
        date.text = dateFmt.format(now).uppercase(Locale.UK)

        // Gentle drift: walk a small loop around the centre.
        step = (step + 1) % 8
        val dx = intArrayOf(0, 14, 20, 14, 0, -14, -20, -14)[step]
        val dy = intArrayOf(-18, -12, 0, 12, 18, 12, 0, -12)[step]
        block.animate().translationX(dx.toFloat()).translationY(dy.toFloat())
            .setDuration(6000).start()
    }
}
