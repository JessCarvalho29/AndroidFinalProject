package com.example.week10

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginResult
import com.facebook.login.widget.LoginButton
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

private lateinit var googleSignInClient : GoogleSignInClient
private lateinit var callbackManager: CallbackManager
private lateinit var auth : FirebaseAuth
private const val trackingNumberForGoogleSignUp = 123
private const val currentPage = "MainActivity"

class MainActivity : AppCompatActivity() {

    private var isLoginMode = false
    private lateinit var emailView: EditText
    private lateinit var passwordView: EditText
    private lateinit var confirmPasswordView: EditText
    private lateinit var executeAction: Button
    private lateinit var switchMode: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()

        switchMode = findViewById(R.id.SwitchToLogin)
        switchMode.setOnClickListener {
            isLoginMode = !isLoginMode
            changeBetweenSignInAndUp()
        }

        emailView = findViewById(R.id.editTextEmailAddress)
        passwordView = findViewById(R.id.editTextPassword)
        confirmPasswordView = findViewById(R.id.editTextConfirmPassword)
        executeAction = findViewById(R.id.btnExecuteAction)

        // GOOGLE
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.defaultWebClientId))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)
        findViewById<com.google.android.gms.common.SignInButton>(R.id.btnSignUpGoogle).setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            startActivityForResult(signInIntent, trackingNumberForGoogleSignUp)
        }

        // FACEBOOK
        callbackManager = CallbackManager.Factory.create()
        val loginButton = findViewById<LoginButton>(R.id.btnSignUpFacebook)
        loginButton.setPermissions("email", "public_profile")
        loginButton.registerCallback(callbackManager, object : FacebookCallback<LoginResult> {
                override fun onSuccess(loginResult: LoginResult) {
                    Log.d(currentPage, "Facebook Sign In Successful: ${loginResult.accessToken}")
                    handleFacebookAccessToken(loginResult.accessToken)
                }
                override fun onCancel() {
                    Log.w(currentPage, "Facebook Sign In Canceled")
                }
                override fun onError(error: FacebookException) {
                    Log.e(currentPage, "Facebook Sign In Failed. Error: $error")
                }
            })

        // EMAIL + PASSWORD
        executeAction.setOnClickListener {
            val email = emailView.text.toString().lowercase().trim()
            val password = passwordView.text.toString().trim()
            val confirmPassword = confirmPasswordView.text.toString().trim()

            if (email.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Log.d(currentPage, "Invalid email")
                Toast.makeText(this, "Invalid email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (password.isBlank()) {
                Log.d(currentPage, "Password is blank")
                Toast.makeText(this, "Password cannot be empty.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (isLoginMode) {
                auth.signInWithEmailAndPassword(email, password).addOnCompleteListener {
                    if (it.isSuccessful){
                        Log.d("MainActivity", "Login successful")
                        Toast.makeText(this, "Logged in successfully", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this, PersonalInformation::class.java)
                        startActivity(intent)
                    }
                    else {
                        Log.w("MainActivity", "Login failed", it.exception)
                        Toast.makeText(this, "Login failed. Please review your email and password.", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {

                if (confirmPassword.isBlank()) {
                    Log.d(currentPage, "Passwords do not match")
                    Toast.makeText(this, "Confirm password cannot be empty.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (password.length < 6) {
                    Toast.makeText(
                        this, "Password must be at least 6 characters.", Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }
                if (password != confirmPassword) {
                    Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                if (email.isNotEmpty() && password.isNotEmpty()) {
                    auth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(this) { task ->
                            if (task.isSuccessful) {
                                val user = auth.currentUser
                                Log.d("MainActivity", "createUserWithEmail:success")
                                Toast.makeText(
                                    this,
                                    "Account created successfully: ${user?.email}",
                                    Toast.LENGTH_SHORT
                                ).show()
                                val intent = Intent(this, PersonalInformation::class.java)
                                startActivity(intent)
                            } else {
                                Log.w("MainActivity", "createUserWithEmail:failure", task.exception)
                                Toast.makeText(this, "Authentication failed.", Toast.LENGTH_SHORT)
                                    .show()
                            }
                        }
                }
            }
        }

        // PHONE
        findViewById<Button>(R.id.btnSignUpPhone).setOnClickListener {
            val intent = Intent(this, LoginWithPhone::class.java)
            startActivity(intent)
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun handleFacebookAccessToken(token: AccessToken?) {
        val credential = FacebookAuthProvider.getCredential(token!!.token)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    Log.d(currentPage, "Facebook Sign In Successful: ${user?.email}")
                    Toast.makeText(this, "Facebook Sign In Successful: ${user?.email}", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, PersonalInformation::class.java)
                    startActivity(intent)
                } else {
                    Log.w(currentPage, "Facebook Sign In Failed: ${task.exception?.message}")
                    Toast.makeText(this, "Facebook Sign In Failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun changeBetweenSignInAndUp() {
        if (isLoginMode) {
            findViewById<TextView>(R.id.pageTitle).text = "Log In"
            confirmPasswordView.visibility = View.GONE
            executeAction.text = "Log In"
            switchMode.text = "Don't have an account? Sign up"
        } else {
            findViewById<TextView>(R.id.pageTitle).text = "Sign Up"
            confirmPasswordView.visibility = View.VISIBLE
            executeAction.text = "Create Account"
            switchMode.text = "Already have an account? Log in"
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == trackingNumberForGoogleSignUp) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            if (task.isSuccessful) {
                val account = task.result
                val idToken = account.idToken // The id, name, email and profile
                val credentials = GoogleAuthProvider.getCredential(idToken, null)
                Log.d(currentPage, "Google Sign In Successful: ${account?.email}")
                Toast.makeText(this, "Google Sign In Successful: ${account?.email}", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, PersonalInformation::class.java)
                startActivity(intent)

            } else {
                Log.w(currentPage, "Google Sign In Failed: ${task.exception?.message}")
                Toast.makeText(this, "Google Sign In Failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
            }
        }
        callbackManager.onActivityResult(requestCode, resultCode, data)
    }

}