package com.example.week10

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar


private const val currentPage = "QuestionAndMedia"

class QuestionAndMedia : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_question_and_media)

        db = FirebaseFirestore.getInstance()
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        val questionSpinner = findViewById<Spinner>(R.id.spinnerQuestions)
        val answerTextView = findViewById<TextView>(R.id.textViewAnswer)

        val questions = listOf("What is my name?", "What is my family name?", "What is my full name?", "What year was I born?", "What city am I living in?", "What country am I living in?")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, questions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        questionSpinner.adapter = adapter

        questionSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val selectedQuestion = questionSpinner.selectedItem.toString()
                if (userId != null) {
                    db.collection("users").document(userId).get()
                        .addOnSuccessListener { document ->
                            if (document.exists()) {
                                val firstName = document.getString("firstName")
                                val familyName = document.getString("familyName")
                                val age = document.getString("age")
                                val city = document.getString("city")
                                val country = document.getString("country")
                                val year = Calendar.getInstance().get(Calendar.YEAR)

                                when (selectedQuestion) {
                                    "What is my name?" -> answerTextView.text = firstName
                                    "What is my family name?" -> answerTextView.text = familyName
                                    "What is my full name?" -> answerTextView.text = buildString {
                                        append(firstName)
                                        append(" ")
                                        append(familyName)
                                    }
                                    "What year was I born?" -> answerTextView.text = (year - (age?.toInt()
                                        ?: 0)).toString()
                                    "What city am I living in?" -> answerTextView.text = city
                                    "What country am I living in?" -> answerTextView.text = country
                                    else -> answerTextView.text = ""
                                }

                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e(currentPage, "Failed to load user data", e)
                        }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        findViewById<Button>(R.id.buttonRecordVideo).setOnClickListener {
            val intent = Intent(this, Camera::class.java)
            startActivity(intent)
        }


        findViewById<Button>(R.id.buttonViewMap).setOnClickListener {
            val intent = Intent(this, Map::class.java)
            startActivity(intent)
        }

        findViewById<ImageButton>(R.id.imageBtnPreviousPage).setOnClickListener {
            finish()
        }

        findViewById<Button>(R.id.buttonNext).setOnClickListener {
            val intent = Intent(this, Map::class.java)
            startActivity(intent)
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}