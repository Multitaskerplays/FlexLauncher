package com.flexlauncher.split

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.widget.EditText
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.flexlauncher.R
import com.flexlauncher.databinding.ActivitySplitPickerBinding

/**
 * Pick the two apps (and the divider) for a split, then launch it.
 *
 * Started from the home screen: long-press an app → "Split screen", which arrives here with
 * that app pre-filled as the left pane, or with no extras at all from the "Repeat last split"
 * action when nothing has been saved yet.
 */
class SplitPickerActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplitPickerBinding
    private lateinit var prefs: SplitPrefs

    private var leftPackage: String? = null
    private var rightPackage: String? = null

    private data class PickerApp(val packageName: String, val label: String, val icon: Drawable?)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplitPickerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = SplitPrefs(this)
        leftPackage = intent.getStringExtra(EXTRA_LEFT) ?: prefs.left
        rightPackage = intent.getStringExtra(EXTRA_RIGHT) ?: prefs.right

        binding.splitLeftSlot.setOnClickListener { chooseApp(forLeft = true) }
        binding.splitRightSlot.setOnClickListener { chooseApp(forLeft = false) }

        binding.splitSwapButton.setOnClickListener {
            val tmp = leftPackage
            leftPackage = rightPackage
            rightPackage = tmp
            render()
        }

        binding.splitStartButton.setOnClickListener { startSplit() }
        binding.splitGeometryHint.text = getString(R.string.split_reuse_hint)

        setupRatio()
        setupGeometryInputs()
        render()
    }

    private fun setupRatio() {
        val seek = binding.splitRatioSeekbar
        seek.max = RATIO_MAX - RATIO_MIN
        seek.progress = (prefs.ratioPct - RATIO_MIN).coerceIn(0, seek.max)
        updateRatioLabel(prefs.ratioPct)
        seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(bar: SeekBar?, progress: Int, fromUser: Boolean) {
                updateRatioLabel(progress + RATIO_MIN)
            }

            override fun onStartTrackingTouch(bar: SeekBar?) = Unit
            override fun onStopTrackingTouch(bar: SeekBar?) = Unit
        })
    }

    private fun updateRatioLabel(pct: Int) {
        val (appWidth, _) = SplitGeometryCalculator.appArea(this)
        val panel = SplitGeometryCalculator.panelWidth(this).coerceAtLeast(appWidth)
        val divider = (panel * pct / 100).coerceIn(1, appWidth - 1)
        binding.splitRatioLabel.text = getString(R.string.split_ratio, divider, pct)
    }

    private fun selectedRatio(): Int =
        binding.splitRatioSeekbar.progress + RATIO_MIN

    private fun setupGeometryInputs() {
        binding.splitInsetInput.setText(prefs.bottomInsetPx.toString())
        binding.splitOverscanInput.setText(prefs.overscanPx.toString())
    }

    private fun readIntOr(input: EditText, fallback: Int): Int =
        input.text?.toString()?.trim()?.toIntOrNull() ?: fallback

    private fun render() {
        bindSlot(
            icon = binding.splitLeftIcon,
            name = binding.splitLeftName,
            packageName = leftPackage
        )
        bindSlot(
            icon = binding.splitRightIcon,
            name = binding.splitRightName,
            packageName = rightPackage
        )
        binding.splitStartButton.isEnabled = leftPackage != null && rightPackage != null
    }

    private fun bindSlot(icon: android.widget.ImageView, name: android.widget.TextView, packageName: String?) {
        if (packageName.isNullOrEmpty()) {
            icon.setImageDrawable(null)
            name.text = getString(R.string.split_none)
            return
        }
        val pm = packageManager
        name.text = labelOf(pm, packageName)
        icon.setImageDrawable(iconOf(pm, packageName))
    }

    private fun chooseApp(forLeft: Boolean) {
        val apps = loadApps()
        if (apps.isEmpty()) return
        val labels = apps.map { it.label }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle(R.string.split_choose_app)
            .setItems(labels) { _, which ->
                val picked = apps[which].packageName
                if (forLeft) leftPackage = picked else rightPackage = picked
                render()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun startSplit() {
        val left = leftPackage
        val right = rightPackage
        if (left.isNullOrEmpty() || right.isNullOrEmpty()) {
            Toast.makeText(this, R.string.split_pick_both, Toast.LENGTH_SHORT).show()
            return
        }

        prefs.ratioPct = selectedRatio()
        prefs.bottomInsetPx = readIntOr(binding.splitInsetInput, prefs.bottomInsetPx)
        prefs.overscanPx = readIntOr(binding.splitOverscanInput, prefs.overscanPx)
        prefs.savePair(left, right)

        SplitLauncher.start(this, left, right) { launched, message ->
            if (launched) {
                Toast.makeText(this, R.string.split_started, Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, message ?: getString(R.string.split_failed), Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun loadApps(): List<PickerApp> {
        val pm = packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        @Suppress("DEPRECATION")
        val resolved = pm.queryIntentActivities(intent, 0)
        return resolved
            .asSequence()
            .filter { it.activityInfo.packageName != packageName }
            .map {
                PickerApp(
                    packageName = it.activityInfo.packageName,
                    label = it.loadLabel(pm).toString(),
                    icon = it.loadIcon(pm)
                )
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
            .toList()
    }

    private fun labelOf(pm: PackageManager, packageName: String): String = try {
        pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
    } catch (e: PackageManager.NameNotFoundException) {
        packageName
    }

    private fun iconOf(pm: PackageManager, packageName: String): Drawable? = try {
        pm.getApplicationIcon(packageName)
    } catch (e: PackageManager.NameNotFoundException) {
        null
    }

    companion object {
        const val EXTRA_LEFT = "left"
        const val EXTRA_RIGHT = "right"
        private const val RATIO_MIN = 40
        private const val RATIO_MAX = 90

        fun intent(context: android.content.Context, left: String?, right: String? = null): Intent =
            Intent(context, SplitPickerActivity::class.java).apply {
                left?.let { putExtra(EXTRA_LEFT, it) }
                right?.let { putExtra(EXTRA_RIGHT, it) }
            }
    }
}
