package com.example.week10

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.content.Intent
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

private const val currentPage = "PersonalInformation"

class PersonalInformation : AppCompatActivity() {

    private val myViewModel: MyViewModel by viewModels()
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_personal_information)

        db = FirebaseFirestore.getInstance()

        val firstNameEdit = findViewById<EditText>(R.id.editFirstName)
        val familyNameEdit = findViewById<EditText>(R.id.editFamilyName)

        firstNameEdit.setText(myViewModel.firstName)
        bindEditTextToViewModel(firstNameEdit) { text -> myViewModel.firstName = text }

        familyNameEdit.setText(myViewModel.familyName)
        bindEditTextToViewModel(familyNameEdit) { text -> myViewModel.familyName = text }

        val ageSpinner = findViewById<Spinner>(R.id.spinnerAge)
        val citySpinner = findViewById<Spinner>(R.id.spinnerCity)
        val countrySpinner = findViewById<Spinner>(R.id.spinnerCountry)

        setupSpinner(ageSpinner, (18..99).map { it.toString() })
        setupSpinner(citySpinner, listOf("Toronto", "Vancouver", "Calgary", "Montreal"))
        setupSpinner(countrySpinner, listOf("Canada", "USA", "UK", "Brazil"))

        val userId = FirebaseAuth.getInstance().currentUser?.uid

        if (userId != null) {
            db.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        firstNameEdit.setText(document.getString("firstName") ?: "")
                        familyNameEdit.setText(document.getString("familyName") ?: "")

                        val age = document.getString("age")
                        val city = document.getString("city")
                        val country = document.getString("country")

                        setSpinnerSelection(ageSpinner, age)
                        setSpinnerSelection(citySpinner, city)
                        setSpinnerSelection(countrySpinner, country)
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(currentPage, "Failed to load user data", e)
                }
        }

        findViewById<Button>(R.id.buttonSubmit).setOnClickListener {
            if (firstNameEdit.text.isBlank()) {
                Log.d(currentPage, "First name cannot be empty")
                Toast.makeText(this, "First name cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (familyNameEdit.text.isBlank()) {
                Log.d(currentPage, "Family name cannot be empty")
                Toast.makeText(this, "Family name cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            AlertDialog.Builder(this)
                .setTitle("Confirm Submission")
                .setMessage("Do you want to save this information?")
                .setPositiveButton("Yes") { _, _ ->
                    val userData = hashMapOf(
                        "firstName" to firstNameEdit.text.trim().toString(),
                        "familyName" to familyNameEdit.text.trim().toString(),
                        "age" to ageSpinner.selectedItem.toString(),
                        "city" to citySpinner.selectedItem.toString(),
                        "country" to countrySpinner.selectedItem.toString()
                    )

                    if (userId != null) {
                        db.collection("users").document(userId).set(userData)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Data saved successfully!", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener {
                                Toast.makeText(this, "Failed to save data.", Toast.LENGTH_SHORT).show()
                            }
                    }

                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        findViewById<ImageButton>(R.id.imageBtnPreviousPage).setOnClickListener {
            finish()
        }

        findViewById<Button>(R.id.buttonNext).setOnClickListener {
            val intent = Intent(this, QuestionAndMedia::class.java)
            startActivity(intent)
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun setupSpinner(spinner: Spinner, items: List<String>) {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, items)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
    }

    fun bindEditTextToViewModel(editText: EditText, onTextChanged: (String) -> Unit) {
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                onTextChanged(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setSpinnerSelection(spinner: Spinner, value: String?) {
        if (value == null) return
        val adapter = spinner.adapter
        for (i in 0 until adapter.count) {
            if (adapter.getItem(i) == value) {
                spinner.setSelection(i)
                break
            }
        }
    }
}