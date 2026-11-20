package com.niz.tvlauncher

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable

/** A launchable app on the device. */
data class AppInfo(
    val label: String,
    val packageName: String,
    val launchIntent: Intent,
    val icon: Drawable
)

object Apps {

    /** Our own package, so we never list the launcher itself. */
    private const val SELF = "com.niz.tvlauncher"

    /**
     * All launchable apps. We prefer the TV (LEANBACK) launcher list; if that is empty
     * (some sideloaded phone apps only declare the normal LAUNCHER category) we fall back
     * to the standard launcher list so nothing is hidden.
     */
    fun loadAll(ctx: Context): List<AppInfo> {
        val pm = ctx.packageManager
        val tv = query(pm, Intent.CATEGORY_LEANBACK_LAUNCHER)
        val phone = query(pm, Intent.CATEGORY_LAUNCHER)

        // Merge, TV entries first, de-duped by package.
        val seen = HashSet<String>()
        val out = ArrayList<AppInfo>()
        for (a in tv + phone) {
            if (a.packageName == SELF) continue
            if (seen.add(a.packageName)) out.add(a)
        }
        out.sortBy { it.label.lowercase() }
        return out
    }

    private fun query(pm: PackageManager, category: String): List<AppInfo> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(category)
        val list = ArrayList<AppInfo>()
        for (ri in pm.queryIntentActivities(intent, 0)) {
            val pkg = ri.activityInfo.packageName ?: continue
            val launch = Intent(Intent.ACTION_MAIN)
                .addCategory(category)
                .setClassName(pkg, ri.activityInfo.name)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            list.add(
                AppInfo(
                    label = ri.loadLabel(pm).toString(),
                    packageName = pkg,
                    launchIntent = launch,
                    icon = ri.loadIcon(pm)
                )
            )
        }
        return list
    }

    /** Launch an app by package, if present. Returns false if it isn't installed/launchable. */
    fun launch(ctx: Context, pkg: String): Boolean {
        val all = loadAll(ctx)
        val app = all.firstOrNull { it.packageName == pkg } ?: return false
        ctx.startActivity(app.launchIntent)
        return true
    }
}
