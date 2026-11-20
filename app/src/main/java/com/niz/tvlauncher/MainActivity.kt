package com.niz.tvlauncher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private val ui = Handler(Looper.getMainLooper())

    private lateinit var clock: TextView
    private lateinit var date: TextView
    private lateinit var dock: LinearLayout
    private lateinit var qr: ImageView
    private lateinit var remoteHint: TextView
    private lateinit var wakeOverlay: View
    private lateinit var wakeLabel: TextView

    private var ps5Dot: View? = null          // the status light on the PS5 tile
    private var firstTile: View? = null       // focus target on launch

    private val timeFmt = SimpleDateFormat("HH:mm", Locale.UK)
    private val dateFmt = SimpleDateFormat("EEEE d MMMM", Locale.UK)

    private var remote: RemoteServer? = null

    // Keep the clock in step with the system minute.
    private val timeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) = refreshClock()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        clock = findViewById(R.id.clock)
        date = findViewById(R.id.date)
        dock = findViewById(R.id.dock)
        qr = findViewById(R.id.qr)
        remoteHint = findViewById(R.id.remote_hint)
        wakeOverlay = findViewById(R.id.wake_overlay)
        wakeLabel = findViewById(R.id.wake_label)

        refreshClock()
        buildDock()
        startRemote()
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(timeReceiver, IntentFilter(Intent.ACTION_TIME_TICK))
        refreshClock()
        buildDock()                 // reflect any newly installed / pinned apps
        showQr()
        pollPs5()
        firstTile?.requestFocus()
    }

    override fun onPause() {
        try { unregisterReceiver(timeReceiver) } catch (_: Exception) {}
        super.onPause()
    }

    override fun onDestroy() {
        remote?.stop()
        super.onDestroy()
    }

    // ---- Home behaviour ---------------------------------------------------

    /** Swallow BACK on the home screen so it never leaves to a blank screen. */
    override fun onBackPressed() { /* stay home */ }

    // ---- Clock ------------------------------------------------------------

    private fun refreshClock() {
        val now = Date()
        clock.text = timeFmt.format(now)
        date.text = dateFmt.format(now)
    }

    // ---- Dock -------------------------------------------------------------

    private fun buildDock() {
        dock.removeAllViews()
        val inflater = LayoutInflater.from(this)

        // 1) PS5
        val ps5 = inflater.inflate(R.layout.item_ps5_tile, dock, false)
        ps5Dot = ps5.findViewById(R.id.ps5_dot)
        ps5.setOnClickListener { onPs5() }
        dock.addView(ps5)
        firstTile = ps5

        // 2) Shop
        val shop = inflater.inflate(R.layout.item_shop_tile, dock, false)
        shop.setOnClickListener { openShop() }
        dock.addView(shop)

        // 3) Favourite apps (skip any not installed)
        val installed = Apps.loadAll(this).associateBy { it.packageName }
        for (pkg in Prefs.favourites(this)) {
            val app = installed[pkg] ?: continue
            dock.addView(appTile(inflater, app))
        }

        // 4) All apps
        val all = inflater.inflate(R.layout.item_allapps_tile, dock, false)
        all.setOnClickListener { openAllApps() }
        dock.addView(all)
    }

    private fun appTile(inflater: LayoutInflater, app: AppInfo): View {
        val tile = inflater.inflate(R.layout.item_app_tile, dock, false)
        val icon = tile.findViewById<ImageView>(R.id.app_icon)
        val name = tile.findViewById<TextView>(R.id.app_name)
        icon.setImageDrawable(app.icon)
        name.text = app.label
        tile.setOnFocusChangeListener { _, focused ->
            name.setTextColor(resources.getColor(if (focused) R.color.cream_text else R.color.ink, theme))
        }
        tile.setOnClickListener { startActivity(app.launchIntent) }
        tile.setOnLongClickListener {
            Prefs.toggleFavourite(this, app.packageName)
            buildDock()
            true
        }
        return tile
    }

    // ---- Actions ----------------------------------------------------------

    private fun openShop() = startActivity(Intent(this, ShopActivity::class.java))

    private fun openAllApps() = startActivity(Intent(this, AllAppsActivity::class.java))

    private fun onPs5() {
        val host = Prefs.ps5Host(this)
        val cred = Prefs.ps5Cred(this)

        // Show the red "waking" screen immediately.
        wakeLabel.text = "WAKING UP · SWITCHING TO HDMI 2"
        wakeOverlay.visibility = View.VISIBLE

        thread {
            val ok = Ps5.wake(host, cred)
            ui.post {
                wakeLabel.text = when {
                    host.isBlank() -> "SET YOUR PS5 IN SETTINGS"
                    ok -> "WAKING UP · USE THE CONTROLLER IF IT DOESN'T SWITCH"
                    else -> "COULDN'T REACH PS5 · CHECK IT'S IN REST MODE"
                }
            }
            // Try to hand over to the console's HDMI input. On many TVs the PS5's own
            // HDMI Device Link wakes the TV to its port; this is the app-side nudge.
            switchToPs5Input()
            ui.postDelayed({ wakeOverlay.visibility = View.GONE }, 3500)
        }
    }

    /**
     * Best-effort input switch. A non-system launcher can't force an HDMI port on every TV,
     * so we rely on the PS5's HDMI Device Link to pull the TV over as it wakes. On a rooted
     * device this is where a CEC/input-switch command would go (see INSTALL.md).
     */
    private fun switchToPs5Input() {
        // Intentionally minimal: the wake packet + Device Link does the switching.
    }

    private fun pollPs5() {
        val host = Prefs.ps5Host(this)
        if (host.isBlank()) { ps5Dot?.setBackgroundResource(R.drawable.dot_off); return }
        thread {
            val status = Ps5.probe(host)
            ui.post {
                ps5Dot?.setBackgroundResource(
                    when (status) {
                        Ps5.Status.ON -> R.drawable.dot_green
                        Ps5.Status.STANDBY -> R.drawable.dot_amber
                        Ps5.Status.OFFLINE -> R.drawable.dot_off
                    }
                )
            }
        }
    }

    // ---- Phone remote -----------------------------------------------------

    private fun startRemote() {
        remote = RemoteServer(this) { cmd -> onRemoteCommand(cmd) }.also { it.start() }
    }

    private fun onRemoteCommand(cmd: String) {
        when (cmd) {
            "UP" -> move(View.FOCUS_UP)
            "DOWN" -> move(View.FOCUS_DOWN)
            "LEFT" -> move(View.FOCUS_LEFT)
            "RIGHT" -> move(View.FOCUS_RIGHT)
            "OK" -> currentFocus?.performClick()
            "BACK" -> { /* home stays put */ }
            "HOME" -> firstTile?.requestFocus()
            "PS5" -> onPs5()
            "SHOP" -> openShop()
            "APPS" -> openAllApps()
        }
    }

    private fun move(direction: Int) {
        val cur = currentFocus ?: firstTile
        val next = cur?.focusSearch(direction)
        next?.requestFocus()
    }

    private fun showQr() {
        val url = remote?.url() ?: return
        try {
            val size = 320
            val matrix = QRCodeWriter().encode(url, BarcodeFormat.QR_CODE, size, size)
            val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
            for (x in 0 until size) for (y in 0 until size) {
                bmp.setPixel(x, y, if (matrix[x, y]) Color.parseColor("#061D35") else Color.parseColor("#EFE6D2"))
            }
            qr.setImageBitmap(bmp)
            remoteHint.text = "Scan for\nphone remote"
        } catch (e: Exception) {
            remoteHint.text = "Remote:\n$url"
        }
    }
}
