package com.example.studyhub

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.studyhub.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException

class LoginActivity : AppCompatActivity() {
    private lateinit var b: ActivityLoginBinding
    private val auth = FirebaseAuth.getInstance()

    private val googleLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        GoogleAuth.finish(it.data, { goHome() }, { msg -> showError(msg) })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.tvGoToRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java)); finish()
        }
        b.btnGoogle.setOnClickListener { googleLauncher.launch(GoogleAuth.client(this).signInIntent) }
        b.btnLogin.setOnClickListener { login() }

        b.tvForgot.setOnClickListener {
            val email = b.etEmail.text.toString().trim()
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                b.etEmail.error = "Enter your email first"; return@setOnClickListener
            }
            auth.sendPasswordResetEmail(email)
                .addOnSuccessListener { Utils.toast(this, "Reset email sent (if the account exists)") }
                .addOnFailureListener { showError(it.message ?: "Could not send reset email") }
        }
    }

    private fun login() {
        val email = b.etEmail.text.toString().trim()
        val pass = b.etPassword.text.toString()
        b.tvError.visibility = View.GONE

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) { b.etEmail.error = "Enter a valid email"; return }
        if (pass.isEmpty()) { b.etPassword.error = "Enter your password"; return }

        b.btnLogin.isEnabled = false
        auth.signInWithEmailAndPassword(email, pass)
            .addOnSuccessListener { goHome() }
            .addOnFailureListener { e ->
                b.btnLogin.isEnabled = true
                when (e) {
                    is FirebaseAuthInvalidUserException -> {
                        b.etEmail.error = "No account found with this email"
                        showError("No account found with this email.")
                    }
                    is FirebaseAuthInvalidCredentialsException -> showError("Wrong email or password.")
                    else -> showError(e.message ?: "Login failed")
                }
            }
    }

    private fun showError(msg: String) {
        b.tvError.text = msg
        b.tvError.visibility = View.VISIBLE
    }

    private fun goHome() {
        startActivity(Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
    }
}
