package com.hamidi.forexrobot

import android.content.Context
import java.security.MessageDigest

object UserPreferences {
    private const val PREF = "forex_user_prefs"
    private const val K_EMAIL   = "email"
    private const val K_HASH    = "pass_hash"
    private const val K_NAME    = "name"
    private const val K_PHOTO   = "photo_path"
    private const val K_LOGGED  = "logged_in"
    private const val K_ACCOUNT = "account_exists"

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    private fun hash(s: String): String {
        val b = MessageDigest.getInstance("SHA-256").digest(s.toByteArray())
        return b.joinToString("") { "%02x".format(it) }
    }

    fun isAccountRegistered(ctx: Context) = prefs(ctx).getBoolean(K_ACCOUNT, false)
    fun isLoggedIn(ctx: Context)          = prefs(ctx).getBoolean(K_LOGGED, false)
    fun getEmail(ctx: Context)            = prefs(ctx).getString(K_EMAIL, "") ?: ""
    fun getName(ctx: Context)             = prefs(ctx).getString(K_NAME, "") ?: ""
    fun getPhotoPath(ctx: Context)        = prefs(ctx).getString(K_PHOTO, null)

    fun register(ctx: Context, email: String, password: String) {
        prefs(ctx).edit()
            .putString(K_EMAIL, email.trim().lowercase())
            .putString(K_HASH, hash(password))
            .putBoolean(K_ACCOUNT, true)
            .putBoolean(K_LOGGED, true)
            .apply()
    }

    fun login(ctx: Context, email: String, password: String): Boolean {
        val ok = email.trim().lowercase() == getEmail(ctx) &&
                 hash(password) == (prefs(ctx).getString(K_HASH, "") ?: "")
        if (ok) prefs(ctx).edit().putBoolean(K_LOGGED, true).apply()
        return ok
    }

    fun changePassword(ctx: Context, newPass: String) =
        prefs(ctx).edit().putString(K_HASH, hash(newPass)).apply()

    fun setName(ctx: Context, name: String) =
        prefs(ctx).edit().putString(K_NAME, name).apply()

    fun setPhotoPath(ctx: Context, path: String) =
        prefs(ctx).edit().putString(K_PHOTO, path).apply()

    fun logout(ctx: Context) =
        prefs(ctx).edit().putBoolean(K_LOGGED, false).apply()
}
