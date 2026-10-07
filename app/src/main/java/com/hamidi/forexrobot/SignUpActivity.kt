package com.hamidi.forexrobot

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class SignUpActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        setContentView(R.layout.activity_signup)

        val etEmail   = findViewById<EditText>(R.id.etSignUpEmail)
        val etPass    = findViewById<EditText>(R.id.etSignUpPassword)
        val etConfirm = findViewById<EditText>(R.id.etSignUpConfirm)
        val btnSignUp = findViewById<Button>(R.id.btnSignUp)
        val tvLogin   = findViewById<TextView>(R.id.tvGoLogin)

        btnSignUp.setOnClickListener {
            val email   = etEmail.text.toString().trim()
            val pass    = etPass.text.toString()
            val confirm = etConfirm.text.toString()
            when {
                email.isEmpty()        -> toast("⚠ Email ولیکئ")
                !email.contains("@")   -> toast("⚠ Email سم ولیکئ")
                pass.length < 6        -> toast("⚠ Password لږ تر لږه 6 حروف وي")
                pass != confirm        -> toast("⚠ Password سره نه ده برابر")
                else -> {
                    UserPreferences.register(this, email, pass)
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
            }
        }

        tvLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
}
