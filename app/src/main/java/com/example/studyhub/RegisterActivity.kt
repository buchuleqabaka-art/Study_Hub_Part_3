package com.example.studyhub

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.util.Patterns
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import com.example.studyhub.databinding.ActivityRegisterBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.OAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.text.SimpleDateFormat
import java.util.*

class RegisterActivity : AppCompatActivity() {
    private lateinit var b: ActivityRegisterBinding
    private val auth = FirebaseAuth.getInstance()
    private var dobMillis = 0L
    private var passVisible = false

    // result of the Google account picker
    private val googleLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        GoogleAuth.finish(it.data, { goHome() }, { msg -> showError(msg) })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.etPassword.doAfterTextChanged { updateStrength(it.toString()) }

        // show / hide password
        b.btnTogglePass.setOnClickListener {
            passVisible = !passVisible
            b.etPassword.transformationMethod =
                if (passVisible) HideReturnsTransformationMethod.getInstance()
                else PasswordTransformationMethod.getInstance()
            b.etPassword.setSelection(b.etPassword.text.length)
            b.btnTogglePass.setImageResource(if (passVisible) R.drawable.ic_eye_off else R.drawable.ic_eye)
        }

        b.tvGoToLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java)); finish()
        }

        b.etDob.setOnClickListener {
            Utils.pickDate(this, false) { ms ->
                dobMillis = ms
                b.etDob.setText(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(ms)))
                showDobError(!ageOk())
            }
        }

        b.btnSignUp.setOnClickListener { signUp() }

        b.btnGoogle.setOnClickListener {
            val client = GoogleAuth.client(this)
            client.signOut().addOnCompleteListener { googleLauncher.launch(client.signInIntent) }
        }

        // Apple sign-in through Firebase (needs Apple set up in Firebase console)
        b.btnApple.setOnClickListener {
            val provider = OAuthProvider.newBuilder("apple.com")
                .setScopes(listOf("email", "name"))
                .build()
            auth.startActivityForSignInWithProvider(this, provider)
                .addOnSuccessListener { res ->
                    val u = res.user!!
                    FirebaseFirestore.getInstance().collection("users").document(u.uid)
                        .set(mapOf("name" to (u.displayName ?: ""), "email" to (u.email ?: "")), SetOptions.merge())
                    goHome()
                }
                .addOnFailureListener { showError(it.message ?: "Apple sign-in failed") }
        }
    }

    private fun signUp() {
        val name = b.etName.text.toString().trim()
        val email = b.etEmail.text.toString().trim()
        val pass = b.etPassword.text.toString()
        b.tvError.visibility = View.GONE

        if (name.isEmpty()) { b.etName.error = "Enter your name"; return }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) { b.etEmail.error = "Enter a valid email"; return }
        if (pass.length < 8) { b.etPassword.error = "Use at least 8 characters"; return }
        if (dobMillis == 0L || !ageOk()) { showDobError(true); return }

        b.btnSignUp.isEnabled = false
        auth.createUserWithEmailAndPassword(email, pass)
            .addOnSuccessListener { res ->
                val user = res.user!!
                FirebaseFirestore.getInstance().collection("users").document(user.uid)
                    .set(mapOf("name" to name, "email" to email, "dob" to b.etDob.text.toString()))
                user.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(name).build())
                    .addOnCompleteListener { goHome() }
            }
            .addOnFailureListener { e ->
                b.btnSignUp.isEnabled = true
                if (e is FirebaseAuthUserCollisionException) {
                    b.etEmail.error = "This email is already registered"
                    showError("This email is already registered. Please log in.")
                } else showError(e.message ?: "Sign up failed")
            }
    }

    // age must be 13 or older
    private fun ageOk(): Boolean {
        val dob = Calendar.getInstance().apply { timeInMillis = dobMillis }
        val now = Calendar.getInstance()
        var age = now.get(Calendar.YEAR) - dob.get(Calendar.YEAR)
        if (now.get(Calendar.DAY_OF_YEAR) < dob.get(Calendar.DAY_OF_YEAR)) age--
        return age >= 13
    }

    // pink field + red text when the age is too low
    private fun showDobError(show: Boolean) {
        b.tvDobError.visibility = if (show) View.VISIBLE else View.GONE
        b.etDob.setBackgroundResource(if (show) R.drawable.bg_input_error else R.drawable.bg_input_field)
    }

    private fun updateStrength(p: String) {
        var s = 0
        if (p.length >= 8) s++
        if (p.any { it.isUpperCase() } && p.any { it.isLowerCase() }) s++
        if (p.any { it.isDigit() }) s++
        if (p.any { !it.isLetterOrDigit() }) s++
        val on = ContextCompat.getColor(this, if (s <= 1) R.color.error else R.color.primary)
        val off = ContextCompat.getColor(this, R.color.bar_off)
        listOf(b.bar1, b.bar2, b.bar3, b.bar4).forEachIndexed { i, v ->
            v.backgroundTintList = ColorStateList.valueOf(if (i < s) on else off)
        }
        b.tvStrength.text = "Password strength: " + listOf("Too weak", "Weak", "Fair", "Good", "Strong")[s]
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
