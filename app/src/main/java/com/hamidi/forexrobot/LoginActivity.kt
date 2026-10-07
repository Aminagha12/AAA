package com.hamidi.forexrobot

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {

    private var showPass = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)

        // Check if already logged in
        if (UserPreferences.isLoggedIn(this)) {
            go(MainActivity::class.java); return
        }
        // No account yet → go to Sign Up
        if (!UserPreferences.isAccountRegistered(this)) {
            go(SignUpActivity::class.java); return
        }

        setContentView(R.layout.activity_login)

        val etEmail  = findViewById<EditText>(R.id.etEmail)
        val etPass   = findViewById<EditText>(R.id.etPassword)
        val btnOK    = findViewById<Button>(R.id.btnLogin)
        val ivEye    = findViewById<ImageView>(R.id.ivEye)
        val tvSignUp = findViewById<TextView>(R.id.tvGoSignUp)

        ivEye.setOnClickListener {
            showPass = !showPass
            etPass.inputType = if (showPass) {
                ivEye.alpha = 1.0f; InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            } else {
                ivEye.alpha = 0.5f; InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
            etPass.setSelection(etPass.text.length)
        }

        btnOK.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val pass  = etPass.text.toString()
            if (email.isEmpty() || pass.isEmpty()) {
                toast("⚠ Email او License Key ولیکئ"); return@setOnClickListener
            }
            if (UserPreferences.login(this, email, pass)) {
                go(MainActivity::class.java)
            } else {
                toast("⛔ Email یا License Key غلط دی!"); etPass.text.clear()
            }
        }

        tvSignUp.setOnClickListener { go(SignUpActivity::class.java) }
    }

    private fun go(cls: Class<*>) {
        startActivity(Intent(this, cls))
        finish()
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
}
