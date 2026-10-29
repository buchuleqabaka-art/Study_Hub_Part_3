package com.example.studyhub

import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.example.studyhub.databinding.ActivityWelcomeBinding
import com.example.studyhub.databinding.BottomSheetSsoBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.FirebaseAuth

class WelcomeActivity : AppCompatActivity() {
    private lateinit var b: ActivityWelcomeBinding

    // receives the result of the Google account picker
    private val googleLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        GoogleAuth.finish(it.data, { goHome() }, { msg -> Utils.toast(this, msg) })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // apply saved dark mode choice
        val dark = getSharedPreferences("prefs", MODE_PRIVATE).getBoolean("dark", false)
        AppCompatDelegate.setDefaultNightMode(
            if (dark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )
        super.onCreate(savedInstanceState)

        // already signed in -> skip the welcome screen
        if (FirebaseAuth.getInstance().currentUser != null) {
            goHome()
            return
        }

        b = ActivityWelcomeBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.btnGetStarted.setOnClickListener { showSsoSheet() }
        b.tvLogin.setOnClickListener { startActivity(Intent(this, LoginActivity::class.java)) }
    }

    // bottom sheet with the two sign-in options
    private fun showSsoSheet() {
        val sheet = BottomSheetDialog(this)
        val s = BottomSheetSsoBinding.inflate(layoutInflater)
        sheet.setContentView(s.root)

        s.btnGoogle.setOnClickListener {
            sheet.dismiss()
            // sign out first so the account picker always shows
            val client = GoogleAuth.client(this)
            client.signOut().addOnCompleteListener { googleLauncher.launch(client.signInIntent) }
        }
        s.btnEmail.setOnClickListener {
            sheet.dismiss()
            startActivity(Intent(this, RegisterActivity::class.java))
        }
        sheet.show()
    }

    private fun goHome() {
        startActivity(
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        )
    }
}
