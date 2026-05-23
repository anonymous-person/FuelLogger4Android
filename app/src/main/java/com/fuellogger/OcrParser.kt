package com.fuellogger

import java.text.SimpleDateFormat
import java.util.*

object OcrParser {

    fun parseReceiptText(text: String): FuelEntry {
        val entry = FuelEntry()
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val fullText = text.uppercase()

        // Date
        val dateRegex = Regex("""(\d{1,2}/\d{1,2}/\d{4})""")
        dateRegex.find(text)?.let { entry.date = it.value }

        // Time
        val timeRegex = Regex("""(\d{1,2}:\d{2}:\d{2}\s*[AP]M)""", RegexOption.IGNORE_CASE)
        timeRegex.find(text)?.let { entry.time = it.value }

        // Gallons
        val gallonsRegex = Regex("""(\d+\.\d{3})\s*G""", RegexOption.IGNORE_CASE)
        gallonsRegex.find(text)?.let {
            entry.gallons = it.groupValues[1].toDoubleOrNull() ?: 0.0
        }

        // Price per gallon
        val ppgRegex = Regex("""\$?(\d+\.\d{3})\s*(?:/GAL|PER GAL|PRICE)""", RegexOption.IGNORE_CASE)
        ppgRegex.find(text)?.let {
            entry.pricePerGallon = it.groupValues[1].toDoubleOrNull() ?: 0.0
        }
        // Fallback: look for price/gal pattern on line after PRICE/GAL label
        if (entry.pricePerGallon == 0.0) {
            lines.forEachIndexed { i, line ->
                if (line.contains("PRICE", true) && line.contains("GAL", true)) {
                    val nextLine = lines.getOrNull(i + 1) ?: ""
                    val priceMatch = Regex("""\$?(\d+\.\d{2,3})""").find(nextLine)
                    priceMatch?.let { entry.pricePerGallon = it.groupValues[1].toDoubleOrNull() ?: 0.0 }
                }
            }
        }

        // Total amount
        val totalRegex = Regex("""TOTAL[^$]*\$?\s*(\d+\.\d{2})""", RegexOption.IGNORE_CASE)
        totalRegex.find(text)?.let {
            entry.totalAmount = it.groupValues[1].toDoubleOrNull() ?: 0.0
        }

        // Fuel grade
        when {
            fullText.contains("PREMIUM") -> entry.fuelGrade = "Premium"
            fullText.contains("MIDGRADE") || fullText.contains("MID-GRADE") -> entry.fuelGrade = "Midgrade"
            fullText.contains("DIESEL") -> entry.fuelGrade = "Diesel"
            fullText.contains("REGULAR") -> entry.fuelGrade = "Regular"
        }

        // Pump number
        val pumpRegex = Regex("""PUMP[#\s]+(\d+)""", RegexOption.IGNORE_CASE)
        pumpRegex.find(text)?.let { entry.pumpNumber = it.groupValues[1] }

        // Payment method
        when {
            fullText.contains("VISA") -> entry.paymentMethod = "Visa"
            fullText.contains("MASTERCARD") -> entry.paymentMethod = "Mastercard"
            fullText.contains("AMEX") || fullText.contains("AMERICAN EXPRESS") -> entry.paymentMethod = "Amex"
            fullText.contains("CASH") -> entry.paymentMethod = "Cash"
            fullText.contains("DISCOVER") -> entry.paymentMethod = "Discover"
        }

        // Card last 4 digits
        val cardRegex = Regex("""X{4,}\s*(\d{4})""", RegexOption.IGNORE_CASE)
        cardRegex.find(text)?.let { entry.cardLast4 = it.groupValues[1] }

        // Invoice number
        val invoiceRegex = Regex("""INVOICE[#\s]+(\w+)""", RegexOption.IGNORE_CASE)
        invoiceRegex.find(text)?.let { entry.invoiceNumber = it.groupValues[1] }

        // Auth code
        val authRegex = Regex("""AUTH[#\s]+(\w+)""", RegexOption.IGNORE_CASE)
        authRegex.find(text)?.let { entry.authCode = it.groupValues[1] }

        // Station name - typically near the top, before address
        if (lines.isNotEmpty()) {
            val stationCandidates = lines.take(8).filter { line ->
                line.length > 3 &&
                !line.matches(Regex(""".*\d{5}.*""")) && // not zip
                !line.matches(Regex(""".*\d{1,2}/\d{1,2}/\d{4}.*""")) && // not date
                !line.matches(Regex(""".*\d{3,4}\s+\w+.*""")) // not address with number
            }
            entry.station = stationCandidates.firstOrNull { it.contains("ENERGY", true) ||
                it.contains("GAS", true) || it.contains("FUEL", true) ||
                it.contains("STATION", true) || it.contains("SHELL", true) ||
                it.contains("CHEVRON", true) || it.contains("ARCO", true) ||
                it.contains("76", true) } ?: ""
        }

        // Address line (has digits + street name)
        val addressRegex = Regex("""(\d+\s+[A-Za-z\s]+(?:ROAD|RD|STREET|ST|AVE|AVENUE|BLVD|DR|DRIVE|LN|LANE|WAY))""", RegexOption.IGNORE_CASE)
        addressRegex.find(text)?.let { entry.address = it.value.trim() }

        // City/State
        val cityStateRegex = Regex("""([A-Za-z\s]+),?\s+(CA|TX|NY|FL|WA|OR|AZ|NV|CO|IL|GA|NC|VA|OH|PA|MI|NJ|MA|MN|WI|MO|TN|IN|MD|SC|AL|KY|LA|OK|CT|UT|AR|MS|KS|NE|ID|MT|SD|ND|WV|NH|ME|RI|DE|VT|WY|AK|HI)\b""")
        cityStateRegex.find(text)?.let {
            entry.city = it.groupValues[1].trim()
            entry.state = it.groupValues[2]
        }

        return entry
    }

    fun parseOdometerText(text: String): Pair<Int, Double> {
        var odometer = 0
        var tripMiles = 0.0

        // Look for large mileage number (odometer) - typically 5-6 digits
        val odoRegex = Regex("""0*(\d{4,6})\s*(?:mi|miles)?""", RegexOption.IGNORE_CASE)
        val matches = odoRegex.findAll(text).toList()
        matches.forEach { match ->
            val value = match.groupValues[1].toIntOrNull() ?: 0
            if (value in 1000..999999 && value > odometer) {
                odometer = value
            }
        }

        // Trip miles - smaller number, typically 3-4 digits
        val tripRegex = Regex("""TRIP[AB\s]*[:\s]+(\d+\.?\d*)""", RegexOption.IGNORE_CASE)
        tripRegex.find(text)?.let {
            tripMiles = it.groupValues[1].toDoubleOrNull() ?: 0.0
        }
        if (tripMiles == 0.0) {
            // Look for 3-4 digit number that could be trip
            val smallNumRegex = Regex("""(\d{3,4}\.\d)""")
            smallNumRegex.find(text)?.let {
                tripMiles = it.value.toDoubleOrNull() ?: 0.0
            }
        }

        return Pair(odometer, tripMiles)
    }

    fun parseTemperature(text: String): Int {
        val tempRegex = Regex("""(\d{1,3})\s*°?\s*F""", RegexOption.IGNORE_CASE)
        return tempRegex.find(text)?.groupValues?.get(1)?.toIntOrNull() ?: 0
    }
}
