package com.niz.tvlauncher

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

/**
 * Talks to a PS5 over the local network, the same way Sony's Remote Play / 2nd Screen apps do.
 *
 * Status check ("SRCH") and wake ("WAKEUP") both go to UDP 9302 on the console.
 *
 * WAKE REQUIRES A CREDENTIAL. The "user-credential" is produced once when you register this
 * launcher with your PSN account (PIN pairing), exactly like setting up Remote Play. Until you
 * paste that credential into the launcher (see INSTALL.md), the button will still switch the TV's
 * input but can't wake the console from Rest Mode.
 *
 * This only works when the PS5 is in Rest Mode with BOTH of these on:
 *   Settings > System > Power Saving > Features Available in Rest Mode
 *     - Stay Connected to the Internet
 *     - Enable Turning On PS5 from Network
 */
object Ps5 {

    enum class Status { ON, STANDBY, OFFLINE }

    private const val PORT = 9302
    private const val PROTO = "00030010"

    /** Quick status probe. Call off the main thread. */
    fun probe(host: String): Status {
        if (host.isBlank()) return Status.OFFLINE
        val reply = sendAndRead(host, "SRCH * HTTP/1.1\ndevice-discovery-protocol-version:$PROTO\n")
            ?: return Status.OFFLINE
        return when {
            reply.contains("620") -> Status.STANDBY   // "620 Server Standby"
            reply.contains("200") -> Status.ON         // "200 Ok"
            else -> Status.OFFLINE
        }
    }

    /** Send the wake packet. Call off the main thread. Returns true if the packet was sent. */
    fun wake(host: String, credential: String): Boolean {
        if (host.isBlank() || credential.isBlank()) return false
        val msg = buildString {
            append("WAKEUP * HTTP/1.1\n")
            append("client-type:vr\n")
            append("auth-type:R\n")
            append("model:w\n")
            append("app-type:r\n")
            append("user-credential:$credential\n")
            append("device-discovery-protocol-version:$PROTO\n")
        }
        return try {
            DatagramSocket().use { sock ->
                val data = msg.toByteArray()
                val addr = InetAddress.getByName(host)
                // Send a few times; UDP wake packets can be dropped.
                repeat(3) {
                    sock.send(DatagramPacket(data, data.size, addr, PORT))
                    Thread.sleep(120)
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun sendAndRead(host: String, message: String): String? {
        return try {
            DatagramSocket().use { sock ->
                sock.soTimeout = 1200
                val addr = InetAddress.getByName(host)
                val data = message.toByteArray()
                sock.send(DatagramPacket(data, data.size, addr, PORT))
                val buf = ByteArray(1024)
                val resp = DatagramPacket(buf, buf.size)
                sock.receive(resp)
                String(resp.data, 0, resp.length)
            }
        } catch (e: Exception) {
            null
        }
    }
}
