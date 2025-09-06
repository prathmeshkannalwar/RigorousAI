package com.example.Rigorous_X

import android.os.Bundle
import android.widget.RadioGroup
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate

class SettingsActivity : AppCompatActivity() {

    private lateinit var appPreferences: AppPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        appPreferences = AppPreferences(this)

        setupTypewriterSpeed()
        setupThemeSelector()
    }

    private fun setupTypewriterSpeed() {
        val seekBar = findViewById<SeekBar>(R.id.seekBar_typewriter_speed)

        // Speed range is 1ms (fastest) to 20ms (slowest).
        // SeekBar range is 0 to 19.
        // We map progress=0 to speed=20ms, progress=19 to speed=1ms.

        // Load saved value and set initial progress
        val savedSpeed = appPreferences.typewriterSpeed
        seekBar.progress = 20 - savedSpeed.toInt()

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    // Convert progress (0-19) to speed (20ms-1ms) and save it
                    val newSpeed = 20 - progress
                    appPreferences.typewriterSpeed = newSpeed.toLong()
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun setupThemeSelector() {
        val radioGroup = findViewById<RadioGroup>(R.id.radioGroup_theme)

        // Set the initial radio button based on saved preference
        when (appPreferences.theme) {
            AppCompatDelegate.MODE_NIGHT_NO -> radioGroup.check(R.id.radio_light)
            AppCompatDelegate.MODE_NIGHT_YES -> radioGroup.check(R.id.radio_dark)
            else -> radioGroup.check(R.id.radio_system)
        }

        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            val newTheme = when (checkedId) {
                R.id.radio_light -> AppCompatDelegate.MODE_NIGHT_NO
                R.id.radio_dark -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
            // Check if the theme has actually changed before applying
            if (appPreferences.theme != newTheme) {
                appPreferences.theme = newTheme
            }
        }
    }
}