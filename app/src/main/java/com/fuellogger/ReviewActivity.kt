package com.fuellogger

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.fuellogger.databinding.ActivityReviewBinding
import kotlinx.coroutines.launch

class ReviewActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ENTRY = "entry"
        const val EXTRA_MODE = "mode"
        const val EXTRA_RAW_TEXT = "raw_text"
    }

    private lateinit var binding: ActivityReviewBinding
    private lateinit var entry: FuelEntry

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReviewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Review Entry"

        val entryJson = intent.getStringExtra(EXTRA_ENTRY) ?: run { finish(); return }
        entry = FuelEntry.fromJson(entryJson)

        populateFields()

        // If receipt mode + odometer not set, offer to scan odometer next
        val mode = intent.getStringExtra(EXTRA_MODE) ?: CameraActivity.MODE_RECEIPT
        if (mode == CameraActivity.MODE_RECEIPT && entry.odometer == 0) {
            binding.btnScanOdometer.visibility = View.VISIBLE
            binding.btnScanOdometer.setOnClickListener {
                collectFieldsIntoEntry()
                startActivity(Intent(this, CameraActivity::class.java).apply {
                    putExtra(CameraActivity.EXTRA_MODE, CameraActivity.MODE_ODOMETER)
                    putExtra(CameraActivity.EXTRA_ENTRY_JSON, entry.toJson())
                })
                finish()
            }
        } else {
            binding.btnScanOdometer.visibility = View.GONE
        }

        binding.btnSave.setOnClickListener { saveEntry() }
        binding.btnDiscard.setOnClickListener { finish() }
    }

    private fun populateFields() {
        binding.etDate.setText(entry.date)
        binding.etTime.setText(entry.time)
        binding.etStation.setText(entry.station)
        binding.etAddress.setText(entry.address)
        binding.etCity.setText(entry.city)
        binding.etState.setText(entry.state)
        binding.etPump.setText(entry.pumpNumber)
        binding.etGrade.setText(entry.fuelGrade)
        binding.etGallons.setText(if (entry.gallons > 0) "%.3f".format(entry.gallons) else "")
        binding.etPricePerGal.setText(if (entry.pricePerGallon > 0) "%.3f".format(entry.pricePerGallon) else "")
        binding.etTotal.setText(if (entry.totalAmount > 0) "%.2f".format(entry.totalAmount) else "")
        binding.etOdometer.setText(if (entry.odometer > 0) entry.odometer.toString() else "")
        binding.etTripMiles.setText(if (entry.tripMiles > 0) "%.1f".format(entry.tripMiles) else "")
        binding.etTemperature.setText(if (entry.temperature > 0) entry.temperature.toString() else "")
        binding.etPayment.setText(entry.paymentMethod)
        binding.etCardLast4.setText(entry.cardLast4)
        binding.etInvoice.setText(entry.invoiceNumber)
        binding.etAuth.setText(entry.authCode)
    }

    private fun collectFieldsIntoEntry() {
        entry.date = binding.etDate.text.toString()
        entry.time = binding.etTime.text.toString()
        entry.station = binding.etStation.text.toString()
        entry.address = binding.etAddress.text.toString()
        entry.city = binding.etCity.text.toString()
        entry.state = binding.etState.text.toString()
        entry.pumpNumber = binding.etPump.text.toString()
        entry.fuelGrade = binding.etGrade.text.toString()
        entry.gallons = binding.etGallons.text.toString().toDoubleOrNull() ?: 0.0
        entry.pricePerGallon = binding.etPricePerGal.text.toString().toDoubleOrNull() ?: 0.0
        entry.totalAmount = binding.etTotal.text.toString().toDoubleOrNull() ?: 0.0
        entry.odometer = binding.etOdometer.text.toString().toIntOrNull() ?: 0
        entry.tripMiles = binding.etTripMiles.text.toString().toDoubleOrNull() ?: 0.0
        entry.temperature = binding.etTemperature.text.toString().toIntOrNull() ?: 0
        entry.paymentMethod = binding.etPayment.text.toString()
        entry.cardLast4 = binding.etCardLast4.text.toString()
        entry.invoiceNumber = binding.etInvoice.text.toString()
        entry.authCode = binding.etAuth.text.toString()
    }

    private fun saveEntry() {
        collectFieldsIntoEntry()

        val webhookUrl = Prefs.getWebhookUrl(this)
        if (webhookUrl.isEmpty()) {
            Toast.makeText(this, "Please set your Google Sheet webhook URL in Settings first.", Toast.LENGTH_LONG).show()
            return
        }

        binding.btnSave.isEnabled = false
        binding.progressSave.visibility = View.VISIBLE

        lifecycleScope.launch {
            val result = SheetsRepository.postEntry(entry, webhookUrl)
            binding.progressSave.visibility = View.GONE
            binding.btnSave.isEnabled = true

            result.onSuccess {
                Toast.makeText(this@ReviewActivity, "✓ Saved to Google Sheet!", Toast.LENGTH_SHORT).show()
                finish()
            }.onFailure { e ->
                Toast.makeText(this@ReviewActivity, "Failed to save: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
