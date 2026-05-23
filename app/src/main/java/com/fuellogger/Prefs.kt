package com.fuellogger

import android.content.Context
import android.content.SharedPreferences

object Prefs {
    private const val NAME = "fuel_logger_prefs"
    private const val KEY_WEBHOOK = "webhook_url"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    fun getWebhookUrl(context: Context): String =
        prefs(context).getString(KEY_WEBHOOK, "") ?: ""

    fun setWebhookUrl(context: Context, url: String) =
        prefs(context).edit().putString(KEY_WEBHOOK, url).apply()
}
