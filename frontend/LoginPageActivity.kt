package com.example.nawigacja_wewnatrz_budynku

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bumptech.glide.Glide
import com.example.nawigacja_wewnatrz_budynku.viewModels.LoginViewModel
import com.example.nawigacja_wewnatrz_budynku.otherScripts.NetworkListener
import com.example.nawigacja_wewnatrz_budynku.otherScripts.PermissionsChecker
import com.example.nawigacja_wewnatrz_budynku.sealedClasses.AutoLoginError
import com.example.nawigacja_wewnatrz_budynku.sealedClasses.LoginError
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class LoginPageActivity : AppCompatActivity() {
    private val viewModel: LoginViewModel by viewModels()

    private var isRequestingPermissions = false

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.entries.all { it.value }
        if (!granted) {
            findViewById<LinearLayout>(R.id.noPermissions).visibility = View.VISIBLE
            findViewById<Button>(R.id.openSettingsButton).setOnClickListener {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = "package:$packageName".toUri()
                }
                startActivity(intent)
            }
        }
        else
        {
            findViewById<LinearLayout>(R.id.noPermissions).visibility = View.GONE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Glide.with(this)
            .asGif()
            .load(R.raw.loading)
            .preload()
        setContentView(R.layout.activity_login_page)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        viewModel.networkListener = NetworkListener(applicationContext)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED){
                viewModel.networkStatus.collect{state ->
                    if (state) {
                        findViewById<RelativeLayout>(R.id.noNetworkConnectionView).visibility = View.GONE
                        findViewById<LinearLayout>(R.id.loading).visibility = View.VISIBLE
                        autoLogin()
                    }
                    else
                    {
                        findViewById<RelativeLayout>(R.id.noNetworkConnectionView).visibility = View.VISIBLE
                        findViewById<LinearLayout>(R.id.loading).visibility = View.GONE
                    }
                }
            }
        }
//        viewModel.networkListener = NetworkListener(applicationContext,
//            onNetworkOn = {
//                runOnUiThread {
//                    Log.d("NETWORK", "ON")
//
//                }
//            },
//            onNetworkOff = {
//                runOnUiThread{
//                    Log.d("NETWORK", "OFF")
//
//                }
//            })
        //Glide.with(this@LoginPageActivity).asGif().load(R.raw.loading).into(findViewById(R.id.loadingGif))
        findViewById<TextView>(R.id.loadingText).text = "Sprawdzanie tokenu logowania"
        val submitButton = findViewById<Button>(R.id.submitButton)
        val registerButton = findViewById<TextView>(R.id.registerButton)
        val guestButton = findViewById<TextView>(R.id.guestButton)
        val email = findViewById<TextInputEditText>(R.id.emailInput)
        val password = findViewById<TextInputEditText>(R.id.passwordInput)
        val errorTextView = findViewById<TextView>(R.id.errorTextView)
        submitButton.setOnClickListener{
            lifecycleScope.launch {
                val loginResult = viewModel.login(email.text.toString(), password.text.toString())

                loginResult
                    .onSuccess {
                        errorTextView.visibility = View.INVISIBLE
                        Log.d("Account", email.text.toString())
                        val intent = Intent(this@LoginPageActivity, MainPageActivity::class.java)
                        startActivity(intent)
                    }
                    .onFailure {
                        when(it){
                            is LoginError.EmailDontExit -> {
                                errorTextView.text = "Konto z podanym emailem nie istnieje"
                            }
                            is LoginError.WrongPassword -> {
                                errorTextView.text = "Błędne hasło"
                            }
                            else -> {
                                errorTextView.text = "Wystąpił nieznany błąd"
                                Log.d("ERROR", it.message.toString())
                            }
                        }
                        errorTextView.visibility = View.VISIBLE
                    }
            }
        }
        registerButton.setOnClickListener{
            val intent = Intent(this, RegisterPageActivity::class.java)
            startActivity(intent)
        }
        guestButton.setOnClickListener{
            val intent = Intent(this, MainPageActivity::class.java)
            startActivity(intent)
        }
    }

    private fun autoLogin()
    {
        lifecycleScope.launch {
            val response = viewModel.autoLogin()
            response
                .onSuccess {
                    val intent = Intent(this@LoginPageActivity, MainPageActivity::class.java)
                    intent.putExtra("autoLogin", true)
                    startActivity(intent)
                }
                .onFailure {
                    when (it) {
                        is AutoLoginError.WrongTokenVersion -> {
                            Toast.makeText(
                                this@LoginPageActivity,
                                "Dane konta zostały zmienione. Wymaga ponownego zalogowania",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        is AutoLoginError.NoInternetConnection -> {
                            findViewById<RelativeLayout>(R.id.noNetworkConnectionView).visibility =
                                View.VISIBLE
                        }
                    }
                    Log.d("ERROR", it.message.toString())
                    findViewById<LinearLayout>(R.id.loading).visibility = View.GONE
                }
        }
    }

    override fun onResume() {
        super.onResume()
        if (!isRequestingPermissions) {
            PermissionsChecker.checkPermissionsAndRequire(this, permissionLauncher) {
                isRequestingPermissions = false
                findViewById<LinearLayout>(R.id.noPermissions).visibility = View.GONE
            }
            isRequestingPermissions = true
        }
        viewModel.setNetworkListener(applicationContext)
    }

    override fun onPause() {
        super.onPause()
        viewModel.networkListener?.stopListening()
    }

    override fun onStop() {
        super.onStop()
        isRequestingPermissions = false
    }
}