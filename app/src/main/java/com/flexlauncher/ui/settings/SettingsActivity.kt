package com.flexlauncher.ui.settings

import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.flexlauncher.FlexLauncherApp
import com.flexlauncher.R
import com.flexlauncher.data.model.LauncherSettings
import kotlinx.coroutines.launch

/**
 * The settings that actually exist: three display switches backed by `LauncherSettings`,
 * read back by the home screen. Anything else the README promises is not here yet, and the
 * screen says so instead of pretending.
 */
class SettingsActivity : AppCompatActivity() {

    private lateinit var clockSwitch: Switch
    private lateinit var clock24Switch: Switch
    private lateinit var labelsSwitch: Switch
    private lateinit var settings: LauncherSettings

    private val app: FlexLauncherApp get() = application as FlexLauncherApp

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val density = resources.displayMetrics.density
        val padding = (24 * density).toInt()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, padding, padding, padding)
            setBackgroundColor(getColor(R.color.background))
        }

        root.addView(TextView(this).apply {
            text = getString(R.string.settings)
            textSize = 22f
            setTextColor(getColor(R.color.text_primary))
            setPadding(0, 0, 0, (16 * density).toInt())
        })

        clockSwitch = addSwitch(root, R.string.settings_show_clock)
        clock24Switch = addSwitch(root, R.string.settings_clock_24h)
        labelsSwitch = addSwitch(root, R.string.settings_show_labels)

        root.addView(TextView(this).apply {
            text = getString(R.string.settings_not_implemented)
            textSize = 13f
            setTextColor(getColor(R.color.text_secondary))
            setPadding(0, (24 * density).toInt(), 0, 0)
        })

        setContentView(root)

        lifecycleScope.launch {
            settings = app.database.settingsDao().getSettingsOnce() ?: LauncherSettings()
            clockSwitch.isChecked = settings.showClock
            clock24Switch.isChecked = settings.clockFormat24h
            labelsSwitch.isChecked = settings.showAppLabels
            clock24Switch.isEnabled = settings.showClock
            wire(clockSwitch) { clock ->
                clock24Switch.isEnabled = clock
                save(settings.copy(showClock = clock))
            }
            wire(clock24Switch) { on -> save(settings.copy(clockFormat24h = on)) }
            wire(labelsSwitch) { on -> save(settings.copy(showAppLabels = on)) }
        }
    }

    private fun addSwitch(parent: LinearLayout, labelRes: Int): Switch {
        val switch = Switch(this).apply {
            text = getString(labelRes)
            setTextColor(getColor(R.color.text_primary))
            setPadding(0, 0, 0, 0)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 8 }
        }
        parent.addView(switch)
        return switch
    }

    private fun wire(switch: Switch, onChange: (Boolean) -> Unit) {
        switch.setOnCheckedChangeListener { _: CompoundButton, checked: Boolean -> onChange(checked) }
    }

    private fun save(updated: LauncherSettings) {
        settings = updated
        lifecycleScope.launch {
            app.database.settingsDao().insertSettings(updated)
            Toast.makeText(this@SettingsActivity, R.string.settings_saved, Toast.LENGTH_SHORT).show()
        }
    }
}
