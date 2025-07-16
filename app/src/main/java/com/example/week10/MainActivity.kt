package com.example.week10

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()

        // GOOGLE
        // report in a variable --> google object
        // create the report with default parameters: name, email and profile picture
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN) // creating the report
            .requestIdToken(getString(R.string.defaultWebClientId)) // Requesting the id
            .build() // Build these two actions for me

        // gso --> google object
        // getClient will apply the parameters to the object
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Check is user clicks, then google object should create the report
        findViewById<com.google.android.gms.common.SignInButton>(R.id.btnSignUpGoogle).setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent

            // Include a tracking id in the intent
            startActivityForResult(signInIntent, trackingNumberForGoogleSignUp)
        }

        // FACEBOOK
        // Adding callback manager to manage the login responses
        callbackManager = CallbackManager.Factory.create()

        val loginButton = findViewById<LoginButton>(R.id.login_button)
        loginButton.setPermissions("email", "public_profile")

        loginButton.registerCallback(
            callbackManager,
            object : FacebookCallback<LoginResult> {
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
        findViewById<Button>(R.id.button2).setOnClickListener {
            val email = findViewById<EditText>(R.id.editTextEmailAddress).text.toString().lowercase().trim()
            val password = findViewById<EditText>(R.id.editTextPassword).text.toString().lowercase().trim()
            val confirmPassword = findViewById<EditText>(R.id.editTextPassword2).text.toString().lowercase().trim()

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

            // Create account with email and password
            if (email.isNotEmpty() && password.isNotEmpty()){
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            val user = auth.currentUser
                            Log.d("MainActivity", "createUserWithEmail:success")
                            Toast.makeText(this, "Account created successfully: ${user?.email}", Toast.LENGTH_SHORT).show()
                            val intent = Intent(this, Login::class.java)
                            startActivity(intent)
                        } else {
                            Log.w("MainActivity", "createUserWithEmail:failure", task.exception)
                            Toast.makeText(this, "Authentication failed.", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    // This function will be called on itself when signing up with google, at the end of the process
    // requestCode: Tracking
    // resultCode: result (successful, fail, etc)
    // data: report created
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // adding new logic to the method
        if (requestCode == trackingNumberForGoogleSignUp) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            if (task.isSuccessful) {
                val account = task.result
                // val idToken = account.idToken // The id, name, email and profile
                // val credentials = GoogleAuthProvider.getCredential(idToken, null)
                Log.d(currentPage, "Google Sign In Successful: ${account?.email}")
                Toast.makeText(this, "Google Sign In Successful: ${account?.email}", Toast.LENGTH_SHORT).show()
            } else {
                Log.w(currentPage, "Google Sign In Failed: ${task.exception?.message}")
                Toast.makeText(this, "Google Sign In Failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
            }
        }
        callbackManager.onActivityResult(requestCode, resultCode, data)
    }

    private fun handleFacebookAccessToken(token: AccessToken?) {
        val credential = FacebookAuthProvider.getCredential(token!!.token)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    Log.d(currentPage, "Facebook Sign In Successful: ${user?.email}")
                    Toast.makeText(this, "Facebook Sign In Successful: ${user?.email}", Toast.LENGTH_SHORT).show()
                } else {
                    Log.w(currentPage, "Facebook Sign In Failed: ${task.exception?.message}")
                    Toast.makeText(this, "Facebook Sign In Failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }
}