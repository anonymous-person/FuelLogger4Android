package com.fuellogger

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.fuellogger.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: HistoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        adapter = HistoryAdapter()
        binding.recyclerHistory.layoutManager = LinearLayoutManager(this)
        binding.recyclerHistory.adapter = adapter

        binding.btnScanReceipt.setOnClickListener {
            startActivity(Intent(this, CameraActivity::class.java).apply {
                putExtra(CameraActivity.EXTRA_MODE, CameraActivity.MODE_RECEIPT)
            })
        }

        binding.btnScanOdometer.setOnClickListener {
            startActivity(Intent(this, CameraActivity::class.java).apply {
                putExtra(CameraActivity.EXTRA_MODE, CameraActivity.MODE_ODOMETER)
            })
        }

        binding.btnScanBoth.setOnClickListener {
            startActivity(Intent(this, CameraActivity::class.java).apply {
                putExtra(CameraActivity.EXTRA_MODE, CameraActivity.MODE_BOTH)
            })
        }
    }

    override fun onResume() {
        super.onResume()
        loadHistory()
    }

    private fun loadHistory() {
        val webhookUrl = Prefs.getWebhookUrl(this)
        if (webhookUrl.isEmpty()) {
            binding.tvEmptyState.visibility = View.VISIBLE
            binding.tvEmptyState.text = "Set your Google Sheet webhook URL in Settings to get started."
            binding.recyclerHistory.visibility = View.GONE
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            val result = SheetsRepository.fetchHistory(webhookUrl)
            binding.progressBar.visibility = View.GONE
            result.onSuccess { entries ->
                if (entries.isEmpty()) {
                    binding.tvEmptyState.visibility = View.VISIBLE
                    binding.tvEmptyState.text = "No fill-ups logged yet. Tap Scan Receipt to get started!"
                    binding.recyclerHistory.visibility = View.GONE
                } else {
                    binding.tvEmptyState.visibility = View.GONE
                    binding.recyclerHistory.visibility = View.VISIBLE
                    adapter.submitList(entries.reversed())
                }
            }.onFailure {
                binding.tvEmptyState.visibility = View.VISIBLE
                binding.tvEmptyState.text = "Could not load history. Check your webhook URL in Settings."
                binding.recyclerHistory.visibility = View.GONE
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
