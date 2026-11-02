package com.flexlauncher.ui.auth

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.flexlauncher.FlexLauncherApp
import com.flexlauncher.R
import com.flexlauncher.data.model.LauncherSettings
import com.flexlauncher.data.model.User
import com.flexlauncher.ui.main.MainActivity
import com.flexlauncher.ui.setup.SetupActivity
import kotlinx.coroutines.launch

/**
 * Profile picker shown when multi-user mode is on and no profile has been selected yet.
 *
 * A profile created with a password is not opened until that password verifies; profiles
 * without one (the default single-user profile) open directly.
 */
class UserSelectionActivity : AppCompatActivity() {

    private lateinit var list: LinearLayout
    private lateinit var emptyState: LinearLayout
    private val app: FlexLauncherApp get() = application as FlexLauncherApp

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val density = resources.displayMetrics.density
        val padding = (24 * density).toInt()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(padding, padding, padding, padding)
            setBackgroundColor(getColor(R.color.background))
        }
        root.addView(TextView(this).apply {
            text = "Who is using this device?"
            textSize = 22f
            gravity = Gravity.CENTER
            setTextColor(getColor(R.color.text_primary))
        })

        list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
        }
        root.addView(list)

        emptyState = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            visibility = android.view.View.GONE
        }
        emptyState.addView(TextView(this).apply {
            text = getString(R.string.no_profiles)
            textSize = 16f
            gravity = Gravity.CENTER
            setTextColor(getColor(R.color.text_secondary))
        })
        emptyState.addView(Button(this).apply {
            text = "Set up a profile"
            setOnClickListener {
                startActivity(Intent(this@UserSelectionActivity, SetupActivity::class.java))
                finish()
            }
        })
        root.addView(emptyState, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = padding })

        setContentView(root)

        app.userRepository.allUsers.observe(this) { users -> bind(users) }
    }

    private fun bind(users: List<User>) {
        list.removeAllViews()
        emptyState.visibility = if (users.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE

        val density = resources.displayMetrics.density
        users.forEach { user ->
            list.addView(TextView(this).apply {
                text = if (user.passwordHash.isEmpty()) user.name else "\uD83D\uDD12 ${user.name}"
                textSize = 18f
                gravity = Gravity.CENTER
                setPadding(0, (12 * density).toInt(), 0, (12 * density).toInt())
                setTextColor(getColor(R.color.text_primary))
                setOnClickListener { choose(user) }
            })
        }
    }

    /** Ask for the password when the profile has one, otherwise go straight in. */
    private fun choose(user: User) {
        if (user.passwordHash.isEmpty()) {
            commitSelection(user)
            return
        }

        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            hint = getString(R.string.user_password)
        }
        val dialog = AlertDialog.Builder(this)
            .setTitle(user.name)
            .setView(input)
            .setPositiveButton(R.string.unlock) { _, _ -> verify(user, input.text.toString()) }
            .setNegativeButton(android.R.string.cancel, null)
            .create()
        dialog.show()
    }

    private fun verify(user: User, password: String) {
        lifecycleScope.launch {
            if (app.userRepository.verifyPassword(user.id, password)) {
                commitSelection(user)
            } else {
                Toast.makeText(
                    this@UserSelectionActivity,
                    R.string.password_incorrect,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun commitSelection(user: User) {
        lifecycleScope.launch {
            val dao = app.database.settingsDao()
            val settings = dao.getSettingsOnce() ?: LauncherSettings()
            dao.insertSettings(settings.copy(currentUserId = user.id))
            app.userRepository.updateLastAccessed(user.id)
            startActivity(Intent(this@UserSelectionActivity, MainActivity::class.java))
            finish()
        }
    }
}
