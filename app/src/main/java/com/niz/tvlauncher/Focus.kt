package com.niz.tvlauncher

import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.TextView

/**
 * The tvOS-style focus motion: the focused card springs up slightly, lifts with a soft
 * shadow (translationZ), and its label fades in. Everything else sits flat. This single
 * behaviour is most of what makes the screen feel premium rather than like a plain app.
 */
object Focus {
    fun bind(container: View, card: View, label: TextView? = null, scale: Float = 1.12f) {
        container.onFocusChangeListener = View.OnFocusChangeListener { _, focused ->
            card.animate()
                .scaleX(if (focused) scale else 1f)
                .scaleY(if (focused) scale else 1f)
                .translationZ(if (focused) 28f else 0f)
                .setInterpolator(if (focused) OvershootInterpolator(2.0f) else DecelerateInterpolator())
                .setDuration(if (focused) 240 else 180)
                .start()
            label?.animate()?.alpha(if (focused) 1f else 0f)?.setDuration(160)?.start()
        }
    }
}
