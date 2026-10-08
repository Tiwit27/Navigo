package com.example.nawigacja_wewnatrz_budynku

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.nawigacja_wewnatrz_budynku.viewModels.RegisterViewModel
import com.example.nawigacja_wewnatrz_budynku.sealedClasses.RegisterError
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class RegisterPageActivity : AppCompatActivity() {
    private val viewModel: RegisterViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register_page)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val submitButton = findViewById<Button>(R.id.submitButton)
        val email = findViewById<TextInputEditText>(R.id.emailInput)
        val password = findViewById<TextInputEditText>(R.id.passwordInput)
        val errorTextView = findViewById<TextView>(R.id.errorTextView)
        submitButton.setOnClickListener{
            lifecycleScope.launch {
                val registerResult = viewModel.register(email.text.toString(), password.text.toString())

                registerResult
                    .onSuccess {
                    errorTextView.visibility = View.INVISIBLE
                    Log.d("Account", it.data.email)
                    val intent = Intent(this@RegisterPageActivity, MainPageActivity::class.java)
                    startActivity(intent)
                }
                    .onFailure {
                        when(it){
                            is RegisterError.EmailAlreadyExist -> {
                                errorTextView.text = "Istnieje już konto z podanym emailem"
                            }
                            is RegisterError.InvalidEmailPattern -> {
                                errorTextView.text = "Email został podany w złym formacie"
                            }
                            is RegisterError.InvalidPasswordPattern -> {
                                errorTextView.text = "Hasło musi zawierać co najmniej 1 wielką literę i mieć więcej niż 7 znaków"
                            }
                            else -> {
                                Log.d("ERROR", it.message.toString())
                                errorTextView.text = "Wystąpił nieznany błąd"
                            }
                        }
                    errorTextView.visibility = View.VISIBLE
                }
            }
        }
    }
}