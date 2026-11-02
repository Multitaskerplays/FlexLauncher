package com.flexlauncher.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.flexlauncher.FlexLauncherApp
import com.flexlauncher.R
import com.flexlauncher.databinding.ActivityMainBinding
import com.flexlauncher.data.model.AppInfo
import com.flexlauncher.split.SplitLauncher
import com.flexlauncher.split.SplitPickerActivity
import com.flexlauncher.split.SplitPrefs
import com.flexlauncher.ui.auth.UserSelectionActivity
import com.flexlauncher.ui.setup.SetupActivity
import kotlin.math.abs

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel
    private lateinit var gestureDetector: GestureDetector
    private lateinit var appAdapter: AppAdapter
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize ViewModel
        val factory = MainViewModelFactory(
            (application as FlexLauncherApp).database.settingsDao(),
            (application as FlexLauncherApp).userRepository,
            (application as FlexLauncherApp).appRepository
        )
        viewModel = ViewModelProvider(this, factory)[MainViewModel::class.java]
        
        // Check if first launch
        viewModel.settings.observe(this) { settings ->
            if (settings == null) {
                // First launch - show setup
                startActivity(Intent(this, SetupActivity::class.java))
                finish()
                return@observe
            }
            
            // Check if multi-user mode and no user selected
            if (settings.isMultiUserMode && settings.currentUserId == 0L) {
                startActivity(Intent(this, UserSelectionActivity::class.java))
                return@observe
            }
            
            // Setup UI
            setupUI()
        }
    }
    
    private fun setupUI() {
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Setup gesture detector
        gestureDetector = GestureDetector(this, GestureListener())
        
        // Setup RecyclerView for apps
        appAdapter = AppAdapter(
            icons = AppIcons(this),
            onAppClick = { app ->
                viewModel.launchApp(app.packageName)
            },
            onAppLongClick = { app ->
                showAppOptions(app)
                true
            }
        )
        
        binding.appsRecyclerView.apply {
            layoutManager = GridLayoutManager(this@MainActivity, 4)
            adapter = appAdapter
        }
        
        // Observe current user
        viewModel.currentUser.observe(this) { user ->
            user?.let {
                binding.userNameText.text = it.name
                loadUserApps(it.id)
            }
        }
        
        // Observe apps
        viewModel.apps.observe(this) { apps ->
            appAdapter.submitList(apps)
        }
        
        // Setup click listeners
        binding.appDrawerButton.setOnClickListener { showApps(searchMode = false) }
        binding.searchButton.setOnClickListener { showApps(searchMode = true) }
        binding.userProfileButton.setOnClickListener { showUserProfile() }

        // Dock shortcuts: resolve whatever the box ships for these intents, and say so when
        // there is nothing installed rather than doing nothing at all.
        binding.phoneButton.setOnClickListener { openIntent(Intent(Intent.ACTION_DIAL)) }
        binding.messagesButton.setOnClickListener {
            openIntent(Intent(Intent.ACTION_VIEW, android.net.Uri.parse("sms:")))
        }
        binding.browserButton.setOnClickListener {
            openIntent(Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://www.google.com")))
        }
        binding.cameraButton.setOnClickListener {
            openIntent(Intent(android.provider.MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA))
        }
    }
    
    private fun loadUserApps(userId: Long) {
        viewModel.loadAppsForUser(userId)
    }
    
    /** Everything the package manager can launch, icon included. */
    private fun launchables(): List<AppEntry> {
        val pm = packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        @Suppress("DEPRECATION")
        val resolved = pm.queryIntentActivities(intent, 0)
        return resolved
            .map { AppEntry(it.activityInfo.packageName, it.loadLabel(pm).toString(), it.loadIcon(pm)) }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }

    /**
     * One dialog serves both the dock's "all apps" icon and the search icon; [searchMode]
     * just adds the filter box and puts the cursor in it.
     */
    private fun showApps(searchMode: Boolean) {
        val entries = launchables()
        if (entries.isEmpty()) {
            toast(getString(R.string.no_apps_found))
            return
        }

        val density = resources.displayMetrics.density
        val container = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(0, (8 * density).toInt(), 0, 0)
        }
        val adapter = AppListAdapter(this, entries)
        val list = android.widget.ListView(this).apply { this.adapter = adapter }
        val filter = android.widget.EditText(this).apply {
            hint = getString(R.string.search_apps)
            setSingleLine()
            setPadding((16 * density).toInt(), 0, (16 * density).toInt(), (8 * density).toInt())
        }

        if (searchMode) container.addView(filter)
        container.addView(list)

        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(if (searchMode) R.string.search_apps else R.string.app_drawer)
            .setView(container)
            .setNegativeButton(android.R.string.cancel, null)
            .create()

        list.setOnItemClickListener { _, _, position, _ ->
            openPackage(adapter.item(position).packageName)
            dialog.dismiss()
        }
        if (searchMode) {
            filter.addTextChangedListener(object : android.text.TextWatcher {
                override fun afterTextChanged(editable: android.text.Editable?) {
                    adapter.filter(editable?.toString().orEmpty())
                }

                override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) = Unit
                override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) = Unit
            })
        }

        dialog.show()
        if (searchMode) filter.requestFocus()
    }

    private fun showUserProfile() {
        val app = application as FlexLauncherApp
        val current = viewModel.currentUser.value

        lifecycleScope.launch {
            val users = app.database.userDao().getAllUsersOnce()
            if (users.isEmpty()) {
                toast(getString(R.string.no_profiles))
                return@launch
            }

            val labels = users.map { user ->
                if (current != null && user.id == current.id) "\u2713 ${user.name}" else user.name
            }.toTypedArray()

            androidx.appcompat.app.AlertDialog.Builder(this@MainActivity)
                .setTitle(R.string.manage_profiles)
                .setItems(labels) { _, which ->
                    val picked = users[which]
                    if (current == null || picked.id != current.id) {
                        switchProfile(picked.id)
                    }
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }
    }

    private fun switchProfile(userId: Long) {
        val app = application as FlexLauncherApp
        lifecycleScope.launch {
            val dao = app.database.settingsDao()
            val settings = dao.getSettingsOnce()
                ?: com.flexlauncher.data.model.LauncherSettings()
            dao.insertSettings(settings.copy(currentUserId = userId))
            viewModel.loadAppsForUser(userId)
        }
    }

    private fun openPackage(packageName: String) {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        if (intent == null) {
            toast(getString(R.string.no_apps_found))
            return
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            startActivity(intent)
            viewModel.recordLaunch(packageName)
        } catch (t: Throwable) {
            toast(getString(R.string.no_apps_found))
        }
    }

    private fun openIntent(intent: Intent) {
        try {
            startActivity(intent)
        } catch (t: android.content.ActivityNotFoundException) {
            toast(getString(R.string.nothing_handles_that))
        } catch (t: Throwable) {
            toast(getString(R.string.nothing_handles_that))
        }
    }

    private fun toast(message: String) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()
    }
    
    private fun showAppOptions(app: AppInfo) {
        val prefs = SplitPrefs(this)
        val actions = mutableListOf<Pair<String, () -> Unit>>()

        actions += getString(R.string.open_app) to { viewModel.launchApp(app.packageName) }
        actions += getString(R.string.split_screen) to {
            startActivity(SplitPickerActivity.intent(this, app.packageName))
        }
        if (prefs.hasSavedPair()) {
            actions += getString(R.string.split_repeat) to { repeatSplit() }
        }
        actions += getString(R.string.app_info) to { openAppInfo(app.packageName) }

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(app.customLabel ?: app.appName)
            .setItems(actions.map { it.first }.toTypedArray()) { _, which ->
                actions[which].second.invoke()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun repeatSplit() {
        SplitLauncher.repeatLast(this) { launched, message ->
            if (!launched) {
                android.widget.Toast.makeText(
                    this,
                    message ?: getString(R.string.split_failed),
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun openAppInfo(packageName: String) {
        try {
            startActivity(
                android.content.Intent(
                    android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    android.net.Uri.fromParts("package", packageName, null)
                )
            )
        } catch (t: Throwable) {
            // Nothing sensible to do; ignore.
        }
    }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        return if (gestureDetector.onTouchEvent(event)) {
            true
        } else {
            super.onTouchEvent(event)
        }
    }
    
    override fun onBackPressed() {
        // Prevent going back from launcher
        // Do nothing or show home screen
    }
    
    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        
        private val SWIPE_THRESHOLD = 100
        private val SWIPE_VELOCITY_THRESHOLD = 100
        
        override fun onFling(
            e1: MotionEvent?,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            if (e1 == null) return false
            
            val diffY = e2.y - e1.y
            val diffX = e2.x - e1.x
            
            if (abs(diffY) > abs(diffX)) {
                if (abs(diffY) > SWIPE_THRESHOLD && abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffY > 0) {
                        onSwipeDown()
                    } else {
                        onSwipeUp()
                    }
                    return true
                }
            } else {
                if (abs(diffX) > SWIPE_THRESHOLD && abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffX > 0) {
                        onSwipeRight()
                    } else {
                        onSwipeLeft()
                    }
                    return true
                }
            }
            return false
        }
        
        override fun onDoubleTap(e: MotionEvent): Boolean {
            viewModel.handleDoubleTap()
            return true
        }
    }
    
    private fun onSwipeUp() {
        viewModel.handleSwipeUp()
    }
    
    private fun onSwipeDown() {
        viewModel.handleSwipeDown()
    }
    
    private fun onSwipeLeft() {
        viewModel.handleSwipeLeft()
    }
    
    private fun onSwipeRight() {
        viewModel.handleSwipeRight()
    }
}
