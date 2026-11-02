package com.flexlauncher.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.flexlauncher.FlexLauncherApp
import com.flexlauncher.R
import com.flexlauncher.databinding.ActivityMainBinding
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
        binding.appDrawerButton.setOnClickListener {
            showAppDrawer()
        }
        
        binding.searchButton.setOnClickListener {
            showSearch()
        }
        
        binding.userProfileButton.setOnClickListener {
            showUserProfile()
        }
    }
    
    private fun loadUserApps(userId: Long) {
        viewModel.loadAppsForUser(userId)
    }
    
    private fun showAppDrawer() {
        // TODO: Implement app drawer
    }
    
    private fun showSearch() {
        // TODO: Implement search
    }
    
    private fun showUserProfile() {
        // TODO: Implement user profile
    }
    
    private fun showAppOptions(app: com.flexlauncher.data.model.AppInfo) {
        // TODO: Show bottom sheet with app options
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
