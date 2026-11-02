package com.flexlauncher.ui.setup

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.flexlauncher.FlexLauncherApp
import com.flexlauncher.databinding.ActivitySetupBinding
import com.flexlauncher.ui.main.MainActivity
import kotlinx.coroutines.launch

class SetupActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivitySetupBinding
    private lateinit var viewModel: SetupViewModel
    private var isMultiUserMode = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySetupBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        val factory = SetupViewModelFactory(
            (application as FlexLauncherApp).database.settingsDao(),
            (application as FlexLauncherApp).userRepository,
            (application as FlexLauncherApp).appRepository,
            (application as FlexLauncherApp).shizukuManager
        )
        viewModel = ViewModelProvider(this, factory)[SetupViewModel::class.java]
        
        setupUI()
    }
    
    private fun setupUI() {
        // Welcome screen
        binding.welcomeTitle.text = "Welcome to FlexLauncher"
        binding.welcomeDescription.text = "A powerful multi-user Android launcher"
        
        // Mode selection
        binding.singleUserButton.setOnClickListener {
            isMultiUserMode = false
            showPermissionsScreen()
        }
        
        binding.multiUserButton.setOnClickListener {
            isMultiUserMode = true
            showPermissionsScreen()
        }
    }
    
    private fun showPermissionsScreen() {
        binding.modeSelectionLayout.visibility = android.view.View.GONE
        binding.permissionsLayout.visibility = android.view.View.VISIBLE
        
        // Check Shizuku permission
        updateShizukuStatus()
        
        binding.requestShizukuButton.setOnClickListener {
            viewModel.requestShizukuPermission { granted ->
                if (granted) {
                    updateShizukuStatus()
                } else {
                    showShizukuInstallDialog()
                }
            }
        }
        
        binding.continueButton.setOnClickListener {
            if (isMultiUserMode) {
                showUserCreationScreen()
            } else {
                createDefaultUserAndFinish()
            }
        }
    }
    
    private fun updateShizukuStatus() {
        val hasShizuku = viewModel.hasShizukuPermission()
        binding.shizukuStatusText.text = if (hasShizuku) {
            "✓ Shizuku is active"
        } else {
            "⚠ Shizuku not detected"
        }
        binding.shizukuStatusText.setTextColor(
            if (hasShizuku) 
                getColor(android.R.color.holo_green_dark)
            else 
                getColor(android.R.color.holo_orange_dark)
        )
    }
    
    private fun showShizukuInstallDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Shizuku Required")
            .setMessage("FlexLauncher uses Shizuku for advanced features like app cloning. Please install Shizuku from Play Store or GitHub.")
            .setPositiveButton("Install") { _, _ ->
                // Open Play Store
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = android.net.Uri.parse("market://details?id=moe.shizuku.privileged.api")
                }
                startActivity(intent)
            }
            .setNegativeButton("Continue Without") { _, _ ->
                // Continue anyway
            }
            .show()
    }
    
    private fun showUserCreationScreen() {
        binding.permissionsLayout.visibility = android.view.View.GONE
        binding.userCreationLayout.visibility = android.view.View.VISIBLE
        
        binding.addUserButton.setOnClickListener {
            val name = binding.userNameInput.text.toString()
            val password = binding.userPasswordInput.text.toString()
            
            if (name.isNotEmpty() && password.isNotEmpty()) {
                lifecycleScope.launch {
                    viewModel.addUser(name, password, null, binding.userListLayout.childCount == 0)
                    binding.userNameInput.text?.clear()
                    binding.userPasswordInput.text?.clear()
                    
                    // Add to visual list
                    addUserToList(name)
                }
            }
        }
        
        binding.finishSetupButton.setOnClickListener {
            finishSetup()
        }
    }
    
    private fun addUserToList(name: String) {
        val textView = android.widget.TextView(this).apply {
            text = "✓ $name"
            textSize = 16f
            setPadding(16, 16, 16, 16)
        }
        binding.userListLayout.addView(textView)
    }
    
    private fun createDefaultUserAndFinish() {
        lifecycleScope.launch {
            viewModel.createDefaultSetup(isMultiUserMode)
            finishSetup()
        }
    }
    
    private fun finishSetup() {
        lifecycleScope.launch {
            viewModel.completeSetup(isMultiUserMode)
            
            // Go to main activity
            startActivity(Intent(this@SetupActivity, MainActivity::class.java))
            finish()
        }
    }
}
