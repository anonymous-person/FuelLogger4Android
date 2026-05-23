package com.fuellogger

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.fuellogger.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Settings"

        binding.etWebhookUrl.setText(Prefs.getWebhookUrl(this))

        binding.btnSaveSettings.setOnClickListener {
            val url = binding.etWebhookUrl.text.toString().trim()
            if (url.isEmpty() || (!url.startsWith("https://script.google.com") && !url.startsWith("https://"))) {
                Toast.makeText(this, "Please enter a valid webhook URL", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            Prefs.setWebhookUrl(this, url)
            Toast.makeText(this, "Saved!", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
