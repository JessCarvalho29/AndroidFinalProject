package com.example.week10

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException


private const val currentPage = "Feedback"

class Feedback : AppCompatActivity() {

    private val myViewModel: MyViewModel by viewModels()
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_feedback)

        db = FirebaseFirestore.getInstance()
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        val feedback = findViewById<EditText>(R.id.editTextTextMultiLine)
        feedback.setText(myViewModel.feedback)
        feedback.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { myViewModel.feedback = s.toString() }
            override fun afterTextChanged(s: Editable?) {}
        })

        findViewById<Button>(R.id.buttonSubmit).setOnClickListener {
            val feedbackText = feedback.text.toString()
            if (feedbackText.isBlank()){
                Log.d(currentPage, "Feedback cannot be empty")
                Toast.makeText(this, "Feedback name cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            analyzeSentiment(feedbackText) { analysis ->
                runOnUiThread {
                    val userFeedback = hashMapOf(
                        "feedback" to feedbackText.trim(),
                        "userSatisfactionLevel" to analysis
                    )
                    Log.d(currentPage, analysis)

                    if (userId != null) {
                        db.collection("feedbacks").document(userId).set(userFeedback)
                            .addOnSuccessListener {
                                Log.d(currentPage, "Data saved successfully!")
                                val intent = Intent(this, MainActivity::class.java)
                                startActivity(intent)
                                finish()
                            }
                            .addOnFailureListener {
                                    e -> Log.e(currentPage, "Failed to save data. ${e.message}")
                            }
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

    fun analyzeSentiment(text: String, callback: (String) -> Unit) {
        val applicationInfo = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
        val bundle = applicationInfo.metaData
        val apiKey = bundle?.getString("com.google.android.geo.API_KEY") ?: ""

        val url = "https://language.googleapis.com/v1/documents:analyzeSentiment?key=$apiKey"

        val json = """
    {
      "document": {
        "type": "PLAIN_TEXT",
        "content": "$text"
      },
      "encodingType": "UTF8"
    }
    """.trimIndent()

        val client = OkHttpClient()
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = json.toRequestBody(mediaType)

        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(currentPage, "Network call failed: ${e.message}")
                callback("Error")
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) {
                        val errorBody = response.body?.string()
                        Log.e(currentPage, "API error: $errorBody")
                        callback("Error")
                        return
                    }
                    val jsonResponse = JSONObject(response.body?.string() ?: "{}")
                    val score = jsonResponse.getJSONObject("documentSentiment").getDouble("score")
                    callback(score.toString())
                }
            }
        })
    }
}