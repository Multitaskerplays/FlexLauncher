package com.flexlauncher.ui.common

import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * Temporary screen for features that are announced in the manifest but do not exist yet.
 *
 * These classes exist so the app links; replace each one with the real screen. Nothing here
 * is reachable from the home screen yet.
 */
open class PlaceholderActivity(
    private val heading: String,
    private val body: String
) : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val density = resources.displayMetrics.density
        val padding = (24 * density).toInt()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(padding, padding, padding, padding)
            setBackgroundColor(getColor(com.flexlauncher.R.color.background))
        }

        root.addView(TextView(this).apply {
            text = heading
            textSize = 24f
            gravity = Gravity.CENTER
            setTextColor(getColor(com.flexlauncher.R.color.text_primary))
        })

        root.addView(TextView(this).apply {
            text = body
            textSize = 14f
            gravity = Gravity.CENTER
            setPadding(0, padding / 2, 0, 0)
            setTextColor(getColor(com.flexlauncher.R.color.text_secondary))
        })

        setContentView(root)
    }
}
