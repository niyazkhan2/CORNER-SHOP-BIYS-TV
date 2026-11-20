package com.niz.tvlauncher

import android.content.Context

/**
 * Simple stored settings:
 *  - favourites: the app packages pinned to the dock, in order
 *  - PS5 wake details: host + the "user-credential" you get from registering (see INSTALL.md)
 */
object Prefs {
    private const val FILE = "csb"
    private const val KEY_FAVS = "favs"
    private const val KEY_PS5_HOST = "ps5_host"
    private const val KEY_PS5_CRED = "ps5_cred"

    // Sensible defaults for the dock. Any that aren't installed are skipped silently.
    private val DEFAULT_FAVS = listOf(
        "com.netflix.ninja",             // Netflix (Fire/AndroidTV)
        "com.amazon.firetv.youtube",     // YouTube on Fire TV
        "com.google.android.youtube.tv", // YouTube on Android TV
        "com.spotify.tv.android"         // Spotify
    )

    fun favourites(ctx: Context): List<String> {
        val raw = prefs(ctx).getString(KEY_FAVS, null) ?: return DEFAULT_FAVS
        if (raw.isBlank()) return emptyList()
        return raw.split("\n")
    }

    fun setFavourites(ctx: Context, pkgs: List<String>) {
        prefs(ctx).edit().putString(KEY_FAVS, pkgs.joinToString("\n")).apply()
    }

    fun toggleFavourite(ctx: Context, pkg: String) {
        val cur = favourites(ctx).toMutableList()
        if (cur.contains(pkg)) cur.remove(pkg) else cur.add(pkg)
        setFavourites(ctx, cur)
    }

    fun ps5Host(ctx: Context): String = prefs(ctx).getString(KEY_PS5_HOST, "") ?: ""
    fun ps5Cred(ctx: Context): String = prefs(ctx).getString(KEY_PS5_CRED, "") ?: ""

    fun setPs5(ctx: Context, host: String, cred: String) {
        prefs(ctx).edit().putString(KEY_PS5_HOST, host).putString(KEY_PS5_CRED, cred).apply()
    }

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)
}
