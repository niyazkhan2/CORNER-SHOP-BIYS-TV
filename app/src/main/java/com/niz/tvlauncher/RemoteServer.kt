package com.niz.tvlauncher

import android.content.Context
import android.net.wifi.WifiManager
import android.os.Handler
import android.os.Looper
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.ServerSocket
import java.net.URLDecoder

/**
 * Phone remote. A tiny HTTP server (no libraries) runs on the TV. Scan the QR on the home
 * screen and your phone opens a web remote — no app to install. The buttons drive the launcher:
 * move focus, open apps, open the Shop, wake the PS5.
 *
 * Note: this controls the launcher itself and launches apps. Acting as a universal remote
 * *inside* other apps (e.g. scrubbing in Netflix) needs system-level key injection, which is a
 * separate step on a rooted device — see INSTALL.md.
 */
class RemoteServer(
    private val ctx: Context,
    private val onCommand: (String) -> Unit
) {
    private val main = Handler(Looper.getMainLooper())
    private var server: ServerSocket? = null
    private var thread: Thread? = null
    @Volatile private var running = false

    val port = 8080

    fun start() {
        if (running) return
        running = true
        thread = Thread {
            try {
                server = ServerSocket(port)
                while (running) {
                    val client = server!!.accept()
                    try {
                        handle(client.getInputStream(), client.getOutputStream())
                    } catch (_: Exception) {
                    } finally {
                        try { client.close() } catch (_: Exception) {}
                    }
                }
            } catch (_: Exception) {
            }
        }.also { it.isDaemon = true; it.start() }
    }

    fun stop() {
        running = false
        try { server?.close() } catch (_: Exception) {}
    }

    /** http://<ip>:8080 — the address the QR encodes. */
    fun url(): String = "http://${ipAddress()}:$port"

    private fun handle(input: java.io.InputStream, output: OutputStream) {
        val reader = BufferedReader(InputStreamReader(input))
        val line = reader.readLine() ?: return           // e.g. "GET /cmd?c=UP HTTP/1.1"
        val path = line.split(" ").getOrNull(1) ?: "/"

        if (path.startsWith("/cmd")) {
            val cmd = param(path, "c")
            if (cmd != null) main.post { onCommand(cmd) }
            respond(output, "text/plain", "ok")
        } else {
            respond(output, "text/html", page())
        }
    }

    private fun param(path: String, key: String): String? {
        val q = path.substringAfter('?', "")
        for (pair in q.split("&")) {
            val kv = pair.split("=")
            if (kv.size == 2 && kv[0] == key) return URLDecoder.decode(kv[1], "UTF-8")
        }
        return null
    }

    private fun respond(out: OutputStream, type: String, body: String) {
        val bytes = body.toByteArray()
        val header = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: $type; charset=utf-8\r\n" +
                "Content-Length: ${bytes.size}\r\n" +
                "Connection: close\r\n\r\n"
        out.write(header.toByteArray())
        out.write(bytes)
        out.flush()
    }

    private fun ipAddress(): String {
        return try {
            val wifi = ctx.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val ip = wifi.connectionInfo.ipAddress
            if (ip == 0) "0.0.0.0"
            else "${ip and 0xff}.${ip shr 8 and 0xff}.${ip shr 16 and 0xff}.${ip shr 24 and 0xff}"
        } catch (e: Exception) {
            "0.0.0.0"
        }
    }

    /** The remote page, ecru-themed to match the brand. */
    private fun page(): String = """
<!doctype html><html lang="en"><head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no">
<title>Corner Shop Boys — Remote</title>
<style>
  :root{color-scheme:light}
  *{box-sizing:border-box;-webkit-tap-highlight-color:transparent}
  body{margin:0;background:#EFE6D2;color:#061D35;font-family:-apple-system,system-ui,sans-serif;
       display:flex;flex-direction:column;align-items:center;gap:18px;padding:24px;min-height:100vh}
  h1{font-size:20px;letter-spacing:3px;margin:8px 0 0;font-weight:800}
  .pad{display:grid;grid-template-columns:repeat(3,84px);grid-template-rows:repeat(3,84px);gap:12px}
  button{border:none;border-radius:18px;background:#E2D6BC;color:#061D35;font-size:18px;font-weight:600;
         touch-action:manipulation}
  button:active{background:#061D35;color:#EFE6D2}
  .ok{background:#A20503;color:#EFE6D2;font-size:20px}
  .wide{width:100%;height:60px;border-radius:16px;font-size:17px}
  .row{display:flex;gap:12px;width:264px}
  .red{background:#A20503;color:#EFE6D2}
  .navy{background:#061D35;color:#EFE6D2}
  .sp{visibility:hidden}
</style></head><body>
<h1>CORNER SHOP BOYS</h1>
<div class="pad">
  <button class="sp"></button>
  <button onpointerdown="k('UP')">▲</button>
  <button class="sp"></button>
  <button onpointerdown="k('LEFT')">◀</button>
  <button class="ok" onpointerdown="k('OK')">OK</button>
  <button onpointerdown="k('RIGHT')">▶</button>
  <button class="sp"></button>
  <button onpointerdown="k('DOWN')">▼</button>
  <button class="sp"></button>
</div>
<div class="row">
  <button class="wide" onpointerdown="k('BACK')">Back</button>
  <button class="wide" onpointerdown="k('HOME')">Home</button>
</div>
<div class="row">
  <button class="wide red" onpointerdown="k('PS5')">PS5</button>
  <button class="wide navy" onpointerdown="k('SHOP')">Shop</button>
</div>
<button class="wide" style="width:264px" onpointerdown="k('APPS')">All apps</button>
<script>
  function k(c){fetch('/cmd?c='+c).catch(function(){});}
</script>
</body></html>
""".trimIndent()
}
