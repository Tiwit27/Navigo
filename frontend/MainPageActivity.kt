package com.example.nawigacja_wewnatrz_budynku

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Rect
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.children
import androidx.drawerlayout.widget.DrawerLayout
import com.bumptech.glide.Glide
import com.example.nawigacja_wewnatrz_budynku.viewModels.MainViewModel
import com.example.nawigacja_wewnatrz_budynku.otherScripts.SecurePrefs
import com.example.nawigacja_wewnatrz_budynku.otherScripts.db
import com.example.nawigacja_wewnatrz_budynku.otherScripts.dpToPx
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.nawigacja_wewnatrz_budynku.otherScripts.PermissionsChecker
import com.example.nawigacja_wewnatrz_budynku.sealedClasses.BleScannerState
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.search.SearchBar
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class MainPageActivity : AppCompatActivity() {

    private var isRequestingPermissions = false

    private val enableBluetoothLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.entries.all { it.value }
        if (!granted) {
//            if (savedInstanceState != null) {
//                val intent = Intent(applicationContext, MainPageActivity::class.java)
//                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
//                startActivity(intent)
//                finish()
//                skipFirstResume = true
//                return
//            }
            Log.d("TEST", "TEST1")
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
            viewModel.changeBleScannerState(BleScannerState.Scanning)
        }
    }

    private var skipFirstResume = true
    private lateinit var roomsContainer: LinearLayout
    private val viewModel: MainViewModel by viewModels()
    private val bluetoothAdapter: BluetoothAdapter by lazy {
        val bluetoothManager = this.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }
    private lateinit var loadingText: TextView
    private lateinit var loading : LinearLayout
    private lateinit var timeout : LinearLayout
    private lateinit var noBluetooth : RelativeLayout
    private lateinit var noNetworkConnection : RelativeLayout
    private lateinit var searchInput : TextInputEditText
    private lateinit var drawerLayout : DrawerLayout

    private lateinit var mediaProjectionManager: MediaProjectionManager
    private val screenCaptureLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                val serviceIntent = Intent(this, ScreenCaptureService::class.java)
                serviceIntent.putExtra("intent_data", result.data)
                serviceIntent.putExtra("result_code", result.resultCode)
                ContextCompat.startForegroundService(this, serviceIntent)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if(!PermissionsChecker.getPermissionsStatus(this))
        {
            startActivity(Intent(this, LandingPageActivity::class.java))
        }
        enableEdgeToEdge()
        setContentView(R.layout.activity_main_page)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        loadingText = findViewById(R.id.loadingText)
        loading = findViewById(R.id.loading)
        timeout = findViewById(R.id.timeout)
        noBluetooth = findViewById(R.id.noBluetooth)
        noNetworkConnection = findViewById(R.id.noNetworkConnectionView)
        if(intent.hasExtra("autoLogin"))
        {
            Snackbar.make(findViewById(android.R.id.content), "Zalogowano Automatycznie", Snackbar.LENGTH_LONG)
                .show()
        }
        //val settingsButton = findViewById<TextView>(R.id.settingsButton)
        val logoutButton = findViewById<TextView>(R.id.logoutButton)
        val screenMirroringButton = findViewById<TextView>(R.id.screenMirroringButton)
        if(db.account?.institutionsAdmin?.isNotEmpty() == true)
        {
            Log.d("ADMIN", "1")
            val adminPanel = findViewById<TextView>(R.id.adminPanel)
            adminPanel.visibility = View.VISIBLE
            adminPanel.setOnClickListener{
                if(db.account!!.institutionsAdmin.size == 1)
                {
                    startActivity(Intent(this, AdminPageActivity::class.java).putExtra("institution_id", db.account!!.institutionsAdmin[0]))
                }
                else
                {
                    startActivity(Intent(this, AdminInstitutionChoosePage::class.java))
                }
            }
        }
        drawerLayout = findViewById(R.id.drawer_layout)
        val menuButton = findViewById<ImageButton>(R.id.burger)
        searchInput = findViewById(R.id.searchInput)
        searchInput.addTextChangedListener {
            roomsContainer.removeAllViews()
            createButtons(it.toString())
        }
        drawerLayout.closeDrawer(GravityCompat.END)
        menuButton.setOnClickListener {
            if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
                drawerLayout.closeDrawer(GravityCompat.END)
            } else {
                drawerLayout.openDrawer(GravityCompat.END)
            }
        }

        findViewById<Button>(R.id.testBtn).setOnClickListener {
            val captureIntent = mediaProjectionManager.createScreenCaptureIntent()
            screenCaptureLauncher.launch(captureIntent)
        }
//        settingsButton.setOnClickListener {
//            startActivity(Intent(this, SettingsPageActivity::class.java))
//        }
        screenMirroringButton.setOnClickListener {
            startActivity(Intent(this, ScreenMirrorPageActivity::class.java))
        }
        logoutButton.setOnClickListener {
            //testowo zakomentowany dialog bo nie widać buttonów
            db.account = null
            SecurePrefs.sharedPreferences.edit { remove("auth_token") }
            startActivity(Intent(this, LoginPageActivity::class.java))
//            MaterialAlertDialogBuilder(this@MainPageActivity)
//                .setTitle("Wylogowanie z aplikacji")
//                .setMessage("Na pewno chcesz wylogować się z aplikacji?")
//                .setPositiveButton("Tak") { _, _ ->
//                    db.account = null
//                    SecurePrefs.sharedPreferences.edit { remove("auth_token") }
//                    startActivity(Intent(this, LoginPageActivity::class.java))
//                }
//                .setNegativeButton("Nie", null)
//                .show()
        }


//        searchBar.setOnQueryTextListener(object : SearchView.OnQueryTextListener{
//            override fun onQueryTextChange(newText: String?): Boolean {
//                roomsContainer.removeAllViews()
//                createButtons(newText)
//                return true
//            }
//
//            override fun onQueryTextSubmit(query: String?): Boolean {
//                return false
//            }
//        })


        //change backPress
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            //w przyszłości można zmienić na podwójne cofnięcie zamiast okna zapytania
            override fun handleOnBackPressed() {
                AlertDialog.Builder(this@MainPageActivity)
                    .setTitle("Wyjście z aplikacji")
                    .setMessage("Na pewno chcesz wyjść z aplikacji?")
                    .setPositiveButton("Tak") { _, _ ->
                        finishAffinity()
                    }
                    .setNegativeButton("Nie", null)
                    .show()
            }
        })

        roomsContainer = findViewById(R.id.roomsContainer)
        if (db.beacons == null) {
            viewModel.setBroadcastListener(applicationContext)
            viewModel.setNetworkListener(applicationContext)
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {

                launch {
                    viewModel.bluetoothStatus.collect { isAvailable ->
                        if(isAvailable)
                        {
                            noBluetooth.visibility = View.GONE
                        }
                        else
                        {
                            //noBluetooth.visibility = View.VISIBLE
                        }
                    }
                }

                launch {
                    viewModel.networkStatus.collect { isAvailable ->
                        if(isAvailable)
                        {
                            noNetworkConnection.visibility = View.GONE
                        }
                        else
                        {
                           // noNetworkConnection.visibility = View.VISIBLE
                        }
                    }
                }

                launch {
                    combine(
                        viewModel.networkStatus,
                        viewModel.bluetoothStatus
                    ) {network, bluetooth ->
                        bluetooth to network
                    }.collect{ (bt, net) ->
                        if(bt && net)
                        {
                            if(db.beacons == null)
                            {
                                PermissionsChecker.checkPermissionsAndRequire(this@MainPageActivity, permissionLauncher) {
                                    isRequestingPermissions = false
                                    viewModel.changeBleScannerState(BleScannerState.Scanning)
                                }
                            }
                            else
                            {
                                viewModel.changeBleScannerState(BleScannerState.End)
                            }
                        }
                        else
                        {
                            viewModel.changeBleScannerState(BleScannerState.NoBluetoothOrNetwork)
                        }
                    }
                }

                launch {
                    viewModel.bleScannerState.collect { state ->
                        when (state) {
                            is BleScannerState.Scanning -> {
                                loading.visibility = View.VISIBLE
                                timeout.visibility = View.GONE
                                loadingText.text = "Wyszukiwanie instytucji"
                                if (db.beacons == null) {
                                    if (viewModel.bleScanner == null) {
                                        viewModel.initBleScanner(applicationContext)
                                    } else {
                                        viewModel.startTimeout()
                                        viewModel.bleScanner?.startScan()
                                    }
                                }
                            }

                            is BleScannerState.NoBluetoothOrNetwork -> {
                                viewModel.stopTimeout()
                                viewModel.bleScanner?.stopScan()
                            }

                            is BleScannerState.Timeout -> {
                                timeout.visibility = View.VISIBLE
                                loading.visibility = View.GONE
                                findViewById<Button>(R.id.restartScanningButton).setOnClickListener {
                                    viewModel.changeBleScannerState(BleScannerState.Scanning)
                                }
                                viewModel.bleScanner?.stopScan()
                            }

                            is BleScannerState.LoadingData -> {
                                viewModel.stopTimeout()
                                loadingText.text = "Pobieranie danych o instytucji"
                            }

                            is BleScannerState.Success -> {
                                viewModel.stopTimeout()
                                createButtons()
                                viewModel.changeBleScannerState(BleScannerState.End)
                            }

                            else -> {
                                loading.visibility = View.GONE
                                timeout.visibility = View.GONE
                            }
                        }
                    }
                }
            }
        }
    }
    private fun setInstitutionName(){
        findViewById<TextView>(R.id.institutionName).text = db.institution?.shortName
        if(db.institution?.logo != null)
        {
            findViewById<ImageView>(R.id.institutionLogo).setImageBitmap(db.institution?.logo)
            findViewById<ImageView>(R.id.institutionLogo).visibility = View.VISIBLE
        }
        else
        {
            findViewById<ImageView>(R.id.institutionLogo).visibility = View.GONE
        }
    }

    private fun createButtons(filter : String? = null) {
        setInstitutionName()
        roomsContainer.removeAllViews()
        val rooms = if(filter != null) db.rooms?.filter { it.name.contains(filter, ignoreCase = true) } else db.rooms
        rooms?.forEach { room ->
            val button = Button(this).apply {
                text = room.name
                tag = room.beaconId
                backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this@MainPageActivity, R.color.opposite_input_color))
                setTextColor(ContextCompat.getColor(this@MainPageActivity,R.color.input_color))
            }
            val layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            layoutParams.setMargins(
                dpToPx(26),
                0,
                dpToPx(26),
                0
            )
            button.layoutParams = layoutParams
            roomsContainer.addView(button)
        }
        setListenerOnButtons()
        loading.visibility = View.INVISIBLE
    }

    private fun setListenerOnButtons() {
        roomsContainer.children.forEach { item ->
            if (item is Button) {
                item.setOnClickListener {
                    if (!bluetoothAdapter.isEnabled) {
                        Toast.makeText(
                            this,
                            "Włącz funkcję łącznenia Bluetooth",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        val intent = Intent(this, NavigationPageActivity::class.java)
                        intent.putExtra("Id", item.tag.toString().toInt())
                        intent.putExtra("RoomName", item.text)
                        startActivity(intent)
                    }
                }
            }
        }
    }

    //scripts which hide keyboard when blur searchBar
    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.action == MotionEvent.ACTION_DOWN) {
            val v = currentFocus
            if (v is EditText) {
                val outRect = Rect()
                v.getGlobalVisibleRect(outRect)
                if (!outRect.contains(ev.rawX.toInt(), ev.rawY.toInt())) {
                    v.clearFocus()
                    hideKeyboard(v)
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    private fun hideKeyboard(view: View) {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    override fun onResume() {
        super.onResume()
        viewModel.networkListener?.startListening()
//        Log.d("ADMIN", db.account?.institutionsAdmin?.size.toString())
//        if(db.account?.institutionsAdmin?.isNotEmpty() == true)
//        {
//            Log.d("ADMIN", "1")
//            val adminPanel = findViewById<TextView>(R.id.adminPanel)
//            adminPanel.visibility = View.VISIBLE
//            adminPanel.setOnClickListener{
//                if(db.account!!.institutionsAdmin.size == 1)
//                {
//                    startActivity(Intent(this, AdminPageActivity::class.java).putExtra("institution_id", db.account!!.institutionsAdmin[0]))
//                }
//                else
//                {
//                    startActivity(Intent(this, AdminInstitutionChoose_Page::class.java))
//                }
//            }
//        }
        if(skipFirstResume)
        {
            skipFirstResume = false
            return
        }
        if (!isRequestingPermissions) {
            PermissionsChecker.checkPermissionsAndRequire(this, permissionLauncher) {
                isRequestingPermissions = false
                findViewById<LinearLayout>(R.id.noPermissions).visibility = View.GONE
            }
            isRequestingPermissions = true
        }
        if(db.institution != null)
        {
            loading.visibility = View.VISIBLE
            lifecycleScope.launch {
                viewModel.fetchInstitutionData()
                    .onSuccess {
                        setInstitutionName()
                        delay(50)
                        loading.visibility = View.GONE
                    }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.bleScanner?.stopScan()
        viewModel.stopTimeout()
    }

    override fun onPause() {
        super.onPause()
        viewModel.bleScanner?.stopScan()
        viewModel.networkListener?.stopListening()
        viewModel.stopTimeout()
        drawerLayout.closeDrawer(GravityCompat.END)
    }

    override fun onStop() {
        super.onStop()
        isRequestingPermissions = false
    }
}