package com.fuellogger

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object SheetsRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun postEntry(entry: FuelEntry, webhookUrl: String): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val payload = JSONObject().apply {
                    put("date", entry.date)
                    put("time", entry.time)
                    put("station", entry.station)
                    put("address", entry.address)
                    put("city", entry.city)
                    put("state", entry.state)
                    put("pumpNumber", entry.pumpNumber)
                    put("fuelGrade", entry.fuelGrade)
                    put("gallons", entry.gallons)
                    put("pricePerGallon", entry.pricePerGallon)
                    put("totalAmount", entry.totalAmount)
                    put("odometer", entry.odometer)
                    put("tripMiles", entry.tripMiles)
                    put("temperature", entry.temperature)
                    put("paymentMethod", entry.paymentMethod)
                    put("cardLast4", entry.cardLast4)
                    put("invoiceNumber", entry.invoiceNumber)
                    put("authCode", entry.authCode)
                }

                val body = payload.toString().toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url(webhookUrl)
                    .post(body)
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string() ?: ""

                if (response.isSuccessful) {
                    Result.success(responseBody)
                } else {
                    Result.failure(Exception("HTTP ${response.code}: $responseBody"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun fetchHistory(webhookUrl: String): Result<List<FuelEntry>> =
        withContext(Dispatchers.IO) {
            try {
                val url = "$webhookUrl?action=get"
                val request = Request.Builder().url(url).get().build()
                val response = client.newCall(request).execute()
                val body = response.body?.string() ?: "[]"

                if (response.isSuccessful) {
                    val arr = JSONArray(body)
                    val entries = mutableListOf<FuelEntry>()
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        entries.add(FuelEntry(
                            date = obj.optString("date"),
                            time = obj.optString("time"),
                            station = obj.optString("station"),
                            address = obj.optString("address"),
                            city = obj.optString("city"),
                            state = obj.optString("state"),
                            pumpNumber = obj.optString("pumpNumber"),
                            fuelGrade = obj.optString("fuelGrade"),
                            gallons = obj.optDouble("gallons", 0.0),
                            pricePerGallon = obj.optDouble("pricePerGallon", 0.0),
                            totalAmount = obj.optDouble("totalAmount", 0.0),
                            odometer = obj.optInt("odometer", 0),
                            tripMiles = obj.optDouble("tripMiles", 0.0),
                            temperature = obj.optInt("temperature", 0),
                            paymentMethod = obj.optString("paymentMethod"),
                            cardLast4 = obj.optString("cardLast4"),
                            invoiceNumber = obj.optString("invoiceNumber"),
                            authCode = obj.optString("authCode")
                        ))
                    }
                    Result.success(entries)
                } else {
                    Result.failure(Exception("HTTP ${response.code}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
