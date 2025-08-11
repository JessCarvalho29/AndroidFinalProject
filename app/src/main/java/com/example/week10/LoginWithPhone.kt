package com.example.week10

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.content.Intent
import android.util.Log
import android.widget.*
import com.google.firebase.FirebaseException
import com.google.firebase.auth.*
import java.util.concurrent.TimeUnit

private const val currentPage = "LoginWithPhone"

class LoginWithPhone : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var editTextPhone: EditText
    private lateinit var editTextCode: EditText
    private lateinit var btnSendCode: Button
    private lateinit var btnVerifyCode: Button
    private var storedVerificationId: String? = null
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login_with_phone)

        auth = FirebaseAuth.getInstance()

        editTextPhone = findViewById(R.id.editTextPhone)
        editTextCode = findViewById(R.id.editTextCode)
        btnSendCode = findViewById(R.id.btnSendCode)
        btnVerifyCode = findViewById(R.id.btnVerifyCode)

        btnSendCode.setOnClickListener {
            val phoneNumber = editTextPhone.text.toString().trim()
            if (phoneNumber.isEmpty()) {
                Toast.makeText(this, "Enter phone number", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            sendVerificationCode(phoneNumber)
        }

        btnVerifyCode.setOnClickListener {
            val code = editTextCode.text.toString().trim()
            if (code.isEmpty()) {
                Toast.makeText(this, "Enter verification code", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            verifyCode(code)
        }

        findViewById<ImageButton>(R.id.imageBtnPreviousPage).setOnClickListener {
            finish()
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun sendVerificationCode(phoneNumber: String) {
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    Log.d(currentPage, "Verification completed")
                    signInWithPhoneAuthCredential(credential)
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    Log.e(currentPage, "Verification failed: ${e.message}")
                    Toast.makeText(this@LoginWithPhone, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }

                override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                    Log.d(currentPage, "Code sent: $verificationId")
                    storedVerificationId = verificationId
                    resendToken = token
                    editTextCode.visibility = EditText.VISIBLE
                    btnVerifyCode.visibility = Button.VISIBLE
                    Toast.makeText(this@LoginWithPhone, "Code sent!", Toast.LENGTH_SHORT).show()
                }
            })
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private fun signInWithPhoneAuthCredential(credential : PhoneAuthCredential){
        auth.signInWithCredential(credential).addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {
                Log.w(currentPage, "Phone login successful")
                Toast.makeText(this, "Phone login successful", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, PersonalInformation::class.java)
                startActivity(intent)
            } else {
                Log.w(currentPage, "Phone login Failed: ${task.exception?.message}")
                Toast.makeText(this, "Phone login Failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun verifyCode(code: String) {
        val credential = PhoneAuthProvider.getCredential(storedVerificationId!!, code)
        signInWithPhoneAuthCredential(credential)
    }
}