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

    private var binding: ActivityMainBinding? = null
    private lateinit var viewModel: MainViewModel

    private lateinit var appAdapter: AppAdapter

    private var uiReady = false
    private var setupLaunched = false
    private var userSelectionLaunched = false
    private var lockOverlay: android.view.View? = null
    private lateinit var gestures: GestureDetector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // A launcher must not close on Back; keep the callback empty instead of overriding
        // the deprecated onBackPressed().
        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = Unit
        })

        // Initialize ViewModel
        val factory = MainViewModelFactory(
            (application as FlexLauncherApp).database.settingsDao(),
            (application as FlexLauncherApp).userRepository,
            (application as FlexLauncherApp).appRepository
        )
        viewModel = ViewModelProvider(this, factory)[MainViewModel::class.java]

        // Watch the settings only to decide *navigation* and to inflate the UI once. It used
        // to call setupUI() on every emission, which re-inflated the layout and stacked a
        // fresh currentUser observer each time a profile was switched.
        viewModel.settings.observe(this) { settings ->
            if (settings == null) {
                launchSetupOnce()
                return@observe
            }
            ensureUi()
            applyDisplaySettings(settings)
            applyProfileLock(settings)
            if (!settings.isMultiUserMode || settings.currentUserId != 0L) {
                // Unlock / profile switch: the ViewModel resolved its user before the new
                // settings row existed, so ask it to re-read.
                viewModel.refreshCurrentUser()
            }
        }

        viewModel.currentUser.observe(this) { user ->
            val ui = binding ?: return@observe
            user?.let {
                ui.userNameText.text = it.name
                viewModel.loadAppsForUser(it.id)
            }
        }

        viewModel.apps.observe(this) { apps ->
            if (uiReady) appAdapter.submitList(apps)
        }
    }

    /**
     * In multi-user mode with no profile chosen the home screen must not show anyone's apps,
     * so cover it until a profile is picked. Re-opening the picker is a tap on the overlay.
     */
    private fun applyProfileLock(settings: com.flexlauncher.data.model.LauncherSettings) {
        val locked = settings.isMultiUserMode && settings.currentUserId == 0L
        if (!locked) {
            lockOverlay?.let { overlay ->
                (overlay.parent as? android.view.ViewGroup)?.removeView(overlay)
            }
            lockOverlay = null
            userSelectionLaunched = false
            return
        }

        if (lockOverlay == null) {
            val density = resources.displayMetrics.density
            val overlay = android.widget.LinearLayout(this).apply {
                orientation = android.widget.LinearLayout.VERTICAL
                gravity = android.view.Gravity.CENTER
                setBackgroundColor(getColor(R.color.background))
                isClickable = true
                elevation = 8 * density

                addView(android.widget.TextView(this@MainActivity).apply {
                    text = getString(R.string.choose_profile)
                    textSize = 20f
                    gravity = android.view.Gravity.CENTER
                    setTextColor(getColor(R.color.text_primary))
                    setPadding(0, 0, 0, (16 * density).toInt())
                })
                addView(android.widget.Button(this@MainActivity).apply {
                    text = getString(R.string.open_profile_picker)
                    setOnClickListener { openProfilePicker() }
                })
            }
            (binding?.root as? android.view.ViewGroup)?.addView(
                overlay,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT
            )
            lockOverlay = overlay
            openProfilePicker()
        }
    }

    private fun openProfilePicker() {
        if (userSelectionLaunched) return
        userSelectionLaunched = true
        startActivity(Intent(this, UserSelectionActivity::class.java))
    }

    override fun onResume() {
        super.onResume()
        // Coming back from the picker (chosen or not) must leave the button tappable again;
        // otherwise a dismissed picker left the overlay with a dead button.
        userSelectionLaunched = false
        viewModel.settings.value?.let { applyProfileLock(it) }
    }

    private fun launchSetupOnce() {
        if (setupLaunched) return
        setupLaunched = true
        startActivity(Intent(this, SetupActivity::class.java))
        finish()
    }

    /** Inflates the home screen exactly once, no matter how often settings change. */
    private fun ensureUi() {
        if (uiReady) return
        uiReady = true

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding!!.root)

        // A fling up while the grid is already at the top opens All Apps. The RecyclerView
        // eats vertical swipes, so the gesture is observed in dispatchTouchEvent and only
        // claimed when there is nothing left to scroll.
        gestures = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            private var startedAtTop = false

            override fun onDown(e: MotionEvent): Boolean {
                // Recorded on touch-down: by the time the fling fires the list has already
                // scrolled, so asking then would always return false.
                startedAtTop = gridAtTop()
                return super.onDown(e)
            }

            override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                val upward = e1 != null && e1.y - e2.y > SWIPE_THRESHOLD && abs(velocityY) > SWIPE_VELOCITY_THRESHOLD
                if (upward && startedAtTop) {
                    showApps(searchMode = false)
                    return true
                }
                return false
            }
        })

        appAdapter = AppAdapter(
            icons = AppIcons(this),
            onAppClick = { app -> viewModel.launchApp(app.packageName) },
            onAppLongClick = { app ->
                showAppOptions(app)
                true
            }
        )

        binding!!.appsRecyclerView.apply {
            layoutManager = GridLayoutManager(this@MainActivity, gridColumns())
            adapter = appAdapter
        }

        binding!!.appDrawerButton.setOnClickListener { showApps(searchMode = false) }
        binding!!.searchButton.setOnClickListener { showApps(searchMode = true) }
        binding!!.userProfileButton.setOnClickListener { showUserProfile() }

        // Dock shortcuts: resolve whatever the box ships for these intents, and say so when
        // there is nothing installed rather than doing nothing at all.
        binding!!.phoneButton.setOnClickListener { openIntent(Intent(Intent.ACTION_DIAL)) }
        binding!!.messagesButton.setOnClickListener {
            openIntent(Intent(Intent.ACTION_VIEW, android.net.Uri.parse("sms:")))
        }
        binding!!.browserButton.setOnClickListener {
            openIntent(Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://www.google.com")))
        }
        binding!!.cameraButton.setOnClickListener {
            openIntent(Intent(android.provider.MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA))
        }
        binding!!.settingsButton.setOnClickListener {
            startActivity(Intent(this, com.flexlauncher.ui.settings.SettingsActivity::class.java))
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (this::gestures.isInitialized && gestures.onTouchEvent(ev)) return true
        return super.dispatchTouchEvent(ev)
    }

    private fun gridAtTop(): Boolean {
        val manager = binding?.appsRecyclerView?.layoutManager as? GridLayoutManager ?: return true
        return manager.findFirstCompletelyVisibleItemPosition() <= 0
    }

    /** Applies the three display preferences the settings screen actually exposes. */
    private fun applyDisplaySettings(settings: com.flexlauncher.data.model.LauncherSettings) {
        val ui = binding ?: return

        ui.clockWidget.visibility = if (settings.showClock) android.view.View.VISIBLE else android.view.View.GONE
        // TextClock ignores the 12/24 preference on its own; force the pair it renders.
        ui.timeText.format24Hour = "HH:mm"
        ui.timeText.format12Hour = if (settings.clockFormat24h) "HH:mm" else "h:mm"
        ui.dateText.text = java.text.SimpleDateFormat(
            if (settings.clockFormat24h) "EEEE d MMMM" else "EEEE, d MMMM",
            java.util.Locale.getDefault()
        ).format(java.util.Date())

        appAdapter.showLabels = settings.showAppLabels
    }

    /** Roughly 150 dp per tile, clamped to 4..8 columns. */
    private fun gridColumns(): Int {
        val dm = resources.displayMetrics
        val widthDp = if (dm.density > 0f) dm.widthPixels / dm.density else 360f
        return (widthDp / 150f).toInt().coerceIn(4, 8)
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
        actions += getString(R.string.hide_app) to {
            viewModel.hideApp(app.packageName)
            toast(getString(R.string.hide_app) + ": " + (app.customLabel ?: app.appName))
        }
        actions += getString(R.string.uninstall) to { viewModel.uninstallApp(app.packageName) }
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

    private companion object {
        const val SWIPE_THRESHOLD = 100
        const val SWIPE_VELOCITY_THRESHOLD = 100
    }
}
