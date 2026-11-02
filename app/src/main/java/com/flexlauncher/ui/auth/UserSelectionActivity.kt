package com.flexlauncher.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.flexlauncher.FlexLauncherApp
import com.flexlauncher.R
import com.flexlauncher.data.model.LauncherSettings
import com.flexlauncher.data.model.User
import com.flexlauncher.ui.main.MainActivity
import kotlinx.coroutines.launch

/**
 * Shown on the launcher when multi-user mode is on and no profile is selected yet.
 *
 * Password prompts belong here eventually; for now a tap selects the profile. The launcher
 * is the caller, so this is a transient screen rather than an entry point.
 */
class UserSelectionActivity : AppCompatActivity() {

    private lateinit var list: LinearLayout
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
        root.addView(list, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = padding })

        setContentView(root)

        app.userRepository.allUsers.observe(this) { users -> bind(users) }
    }

    private fun bind(users: List<User>) {
        list.removeAllViews()
        val density = resources.displayMetrics.density
        users.forEach { user ->
            list.addView(TextView(this).apply {
                text = user.name
                textSize = 18f
                gravity = Gravity.CENTER
                setPadding(0, (12 * density).toInt(), 0, (12 * density).toInt())
                setTextColor(getColor(R.color.text_primary))
                setOnClickListener { select(user) }
            })
        }
    }

    private fun select(user: User) {
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
