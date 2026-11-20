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
import android.view.LayoutInflater
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
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
    private lateinit var dockWrap: View
    private lateinit var logo: View
    private lateinit var qr: ImageView
    private lateinit var remoteHint: TextView
    private lateinit var wakeOverlay: View
    private lateinit var wakeLabel: TextView

    private var ps5Dot: View? = null
    private var firstTile: View? = null

    private val timeFmt = SimpleDateFormat("HH:mm", Locale.UK)
    private val dateFmt = SimpleDateFormat("EEEE d MMMM", Locale.UK)

    private var remote: RemoteServer? = null

    private val timeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) = refreshClock()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        clock = findViewById(R.id.clock)
        date = findViewById(R.id.date)
        dock = findViewById(R.id.dock)
        dockWrap = findViewById(R.id.dock_wrap)
        logo = findViewById(R.id.logo)
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
        buildDock()
        showQr()
        pollPs5()
        entrance()
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

    override fun onBackPressed() { /* stay home */ }

    // ---- Entrance motion ----
    private fun entrance() {
        logo.alpha = 0f
        logo.scaleX = 0.96f
        logo.scaleY = 0.96f
        logo.animate().alpha(1f).scaleX(1f).scaleY(1f)
            .setDuration(520).setInterpolator(DecelerateInterpolator()).start()

        dockWrap.alpha = 0f
        dockWrap.translationY = 26f
        dockWrap.animate().alpha(1f).translationY(0f)
            .setStartDelay(120).setDuration(480).setInterpolator(DecelerateInterpolator()).start()
    }

    // ---- Clock ----
    private fun refreshClock() {
        val now = Date()
        clock.text = timeFmt.format(now)
        date.text = dateFmt.format(now)
    }

    // ---- Dock ----
    private fun buildDock() {
        dock.removeAllViews()
        val inflater = LayoutInflater.from(this)

        val ps5 = inflater.inflate(R.layout.item_ps5_tile, dock, false)
        ps5Dot = ps5.findViewById(R.id.ps5_dot)
        Focus.bind(ps5, ps5.findViewById(R.id.card))
        ps5.setOnClickListener { onPs5() }
        dock.addView(ps5)
        firstTile = ps5

        val shop = inflater.inflate(R.layout.item_shop_tile, dock, false)
        Focus.bind(shop, shop.findViewById(R.id.card))
        shop.setOnClickListener { openShop() }
        dock.addView(shop)

        val installed = Apps.loadAll(this).associateBy { it.packageName }
        for (pkg in Prefs.favourites(this)) {
            val app = installed[pkg] ?: continue
            dock.addView(appTile(inflater, app))
        }

        val all = inflater.inflate(R.layout.item_allapps_tile, dock, false)
        Focus.bind(all, all.findViewById(R.id.card), all.findViewById(R.id.allapps_label))
        all.setOnClickListener { openAllApps() }
        dock.addView(all)
    }

    private fun appTile(inflater: LayoutInflater, app: AppInfo): View {
        val tile = inflater.inflate(R.layout.item_app_tile, dock, false)
        val icon = tile.findViewById<ImageView>(R.id.app_icon)
        val name = tile.findViewById<TextView>(R.id.app_name)
        icon.setImageDrawable(app.icon)
        name.text = app.label
        Focus.bind(tile, tile.findViewById(R.id.card), name)
        tile.setOnClickListener { launchWithFade(app.launchIntent) }
        tile.setOnLongClickListener {
            Prefs.toggleFavourite(this, app.packageName)
            buildDock()
            firstTile?.requestFocus()
            true
        }
        return tile
    }

    // ---- Actions ----
    private fun launchWithFade(intent: Intent) {
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    private fun openShop() {
        startActivity(Intent(this, ShopActivity::class.java))
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    private fun openAllApps() {
        startActivity(Intent(this, AllAppsActivity::class.java))
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    private fun onPs5() {
        val host = Prefs.ps5Host(this)
        val cred = Prefs.ps5Cred(this)
        wakeLabel.text = "WAKING UP · SWITCHING TO HDMI 2"
        wakeOverlay.alpha = 0f
        wakeOverlay.visibility = View.VISIBLE
        wakeOverlay.animate().alpha(1f).setDuration(220).start()

        thread {
            val ok = Ps5.wake(host, cred)
            ui.post {
                wakeLabel.text = when {
                    host.isBlank() -> "SET YOUR PS5 IN SETTINGS"
                    ok -> "WAKING UP · USE THE CONTROLLER IF IT DOESN'T SWITCH"
                    else -> "COULDN'T REACH PS5 · CHECK IT'S IN REST MODE"
                }
            }
            ui.postDelayed({
                wakeOverlay.animate().alpha(0f).setDuration(260)
                    .withEndAction { wakeOverlay.visibility = View.GONE }.start()
            }, 3500)
        }
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

    // ---- Phone remote ----
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
            "BACK" -> {}
            "HOME" -> firstTile?.requestFocus()
            "PS5" -> onPs5()
            "SHOP" -> openShop()
            "APPS" -> openAllApps()
        }
    }

    private fun move(direction: Int) {
        val cur = currentFocus ?: firstTile
        cur?.focusSearch(direction)?.requestFocus()
    }

    private fun showQr() {
        val url = remote?.url() ?: return
        try {
            val size = 320
            val matrix = QRCodeWriter().encode(url, BarcodeFormat.QR_CODE, size, size)
            val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
            val fg = Color.parseColor("#0B1E33")
            val bg = Color.parseColor("#EFE6D2")
            for (x in 0 until size) for (y in 0 until size) {
                bmp.setPixel(x, y, if (matrix[x, y]) fg else bg)
            }
            qr.setImageBitmap(bmp)
            remoteHint.text = "Scan for\nphone remote"
        } catch (e: Exception) {
            remoteHint.text = "Remote:\n$url"
        }
    }
}
