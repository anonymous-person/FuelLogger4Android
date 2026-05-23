package com.fuellogger

import com.google.gson.Gson

data class FuelEntry(
    var date: String = "",
    var time: String = "",
    var station: String = "",
    var address: String = "",
    var city: String = "",
    var state: String = "",
    var pumpNumber: String = "",
    var fuelGrade: String = "",
    var gallons: Double = 0.0,
    var pricePerGallon: Double = 0.0,
    var totalAmount: Double = 0.0,
    var odometer: Int = 0,
    var tripMiles: Double = 0.0,
    var temperature: Int = 0,
    var paymentMethod: String = "",
    var cardLast4: String = "",
    var invoiceNumber: String = "",
    var authCode: String = ""
) {
    fun toJson(): String = Gson().toJson(this)

    companion object {
        fun fromJson(json: String): FuelEntry = Gson().fromJson(json, FuelEntry::class.java)
    }
}
