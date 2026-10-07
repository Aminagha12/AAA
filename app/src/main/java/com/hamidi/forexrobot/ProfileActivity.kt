package com.hamidi.forexrobot

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.WindowManager
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.FileOutputStream

class ProfileActivity : AppCompatActivity() {

    private lateinit var ivPhoto: ImageView
    private lateinit var etName: EditText
    private lateinit var tvEmail: TextView

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            try {
                val stream = contentResolver.openInputStream(it) ?: return@let
                val file   = File(filesDir, "profile_photo.jpg")
                FileOutputStream(file).use { out -> stream.copyTo(out) }
                stream.close()
                UserPreferences.setPhotoPath(this, file.absolutePath)
                ivPhoto.setImageBitmap(BitmapFactory.decodeFile(file.absolutePath))
            } catch (_: Exception) { toast("⚠ Photo load failed") }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        setContentView(R.layout.activity_profile)

        ivPhoto           = findViewById(R.id.ivProfilePhoto)
        etName            = findViewById(R.id.etProfileName)
        tvEmail           = findViewById(R.id.tvProfileEmail)
        val btnPhoto      = findViewById<Button>(R.id.btnChangePhoto)
        val btnChangePw   = findViewById<Button>(R.id.btnChangePw)
        val btnSave       = findViewById<Button>(R.id.btnSaveProfile)
        val btnLogout     = findViewById<Button>(R.id.btnLogout)
        val tvBack        = findViewById<TextView>(R.id.tvBack)

        // Load saved data
        etName.setText(UserPreferences.getName(this))
        tvEmail.text = "📧  " + UserPreferences.getEmail(this)
        UserPreferences.getPhotoPath(this)?.let { path ->
            val f = File(path)
            if (f.exists()) ivPhoto.setImageBitmap(BitmapFactory.decodeFile(path))
        }

        tvBack.setOnClickListener { finish() }
        btnPhoto.setOnClickListener { pickImage.launch("image/*") }

        btnSave.setOnClickListener {
            UserPreferences.setName(this, etName.text.toString().trim())
            toast("✅ Profile saved!"); finish()
        }

        btnChangePw.setOnClickListener { showChangePwDialog() }

        btnLogout.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Log Out")
                .setMessage("ایا تاسو غواړئ چې Log Out شئ؟")
                .setPositiveButton("Yes") { _, _ ->
                    UserPreferences.logout(this)
                    startActivity(
                        Intent(this, LoginActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    )
                }
                .setNegativeButton("No", null)
                .show()
        }
    }

    private fun showChangePwDialog() {
        val v       = layoutInflater.inflate(R.layout.dialog_change_password, null)
        val etNew   = v.findViewById<EditText>(R.id.etNewPassword)
        val etConf  = v.findViewById<EditText>(R.id.etConfirmNewPassword)
        AlertDialog.Builder(this)
            .setTitle("🔑  Change License Key")
            .setView(v)
            .setPositiveButton("Save") { _, _ ->
                val nw = etNew.text.toString(); val cn = etConf.text.toString()
                when {
                    nw.length < 6  -> toast("⚠ Minimum 6 characters")
                    nw != cn       -> toast("⚠ Passwords don't match")
                    else -> { UserPreferences.changePassword(this, nw); toast("✅ License Key changed!") }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun toast(m: String) = Toast.makeText(this, m, Toast.LENGTH_LONG).show()
}
