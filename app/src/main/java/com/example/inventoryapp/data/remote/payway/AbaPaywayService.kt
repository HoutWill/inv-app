package com.example.inventoryapp.data.remote.payway

import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

data class PayWayQrResult(
    val success: Boolean,
    val qrString: String? = null,
    val qrImageBase64: String? = null,
    val deeplink: String? = null,
    val tranId: String,
    val isLiveApi: Boolean = false,
    val errorMessage: String? = null
)

data class PayWayCheckResult(
    val isApproved: Boolean,
    val statusCode: Int = -1,
    val statusMessage: String = "",
    val rawResponse: String? = null
)

object AbaPaywayService {

    private const val TAG = "AbaPaywayService"

    const val SANDBOX_BASE_URL = "https://checkout-sandbox.payway.com.kh"
    const val PRODUCTION_BASE_URL = "https://checkout.payway.com.kh"

    const val DEFAULT_SANDBOX_MERCHANT_ID = "ec000002"

    /**
     * Computes the HMAC-SHA512 hash and encodes it into Base64 as required by ABA PayWay.
     */
    fun computeHmacSha512(data: String, key: String): String {
        return try {
            val secretKeySpec = SecretKeySpec(key.toByteArray(Charsets.UTF_8), "HmacSHA512")
            val mac = Mac.getInstance("HmacSHA512")
            mac.init(secretKeySpec)
            val hmacBytes = mac.doFinal(data.toByteArray(Charsets.UTF_8))
            Base64.encodeToString(hmacBytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to compute HMAC-SHA512", e)
            ""
        }
    }

    /**
     * Formats current UTC timestamp in PayWay format: yyyyMMddHHmmss
     */
    fun getReqTimeUtc(): String {
        val sdf = SimpleDateFormat("yyyyMMddHHmmss", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date())
    }

    /**
     * Generates a unique transaction reference (max 20 chars).
     */
    fun generateTranId(): String {
        val timestamp = System.currentTimeMillis()
        val randomSuffix = (100..999).random()
        return "NW$timestamp$randomSuffix".take(20)
    }

    /**
     * Calls ABA PayWay Generate QR API (/api/payment-gateway/v1/payments/generate-qr)
     * Supports both USD and KHR currencies.
     */
    suspend fun generateKhqr(
        merchantId: String,
        apiKey: String,
        isSandbox: Boolean,
        amount: Double,
        currency: String = "USD",
        itemsSummary: String = "Nawano Order",
        firstName: String = "Nawano",
        lastName: String = "Customer",
        phone: String = "012345678",
        email: String = "customer@nawano.com",
        tranId: String = generateTranId()
    ): PayWayQrResult = withContext(Dispatchers.IO) {
        val effectiveMerchantId = merchantId.ifBlank { DEFAULT_SANDBOX_MERCHANT_ID }
        val baseUrl = if (isSandbox) SANDBOX_BASE_URL else PRODUCTION_BASE_URL
        val endpoint = "$baseUrl/api/payment-gateway/v1/payments/generate-qr"

        val reqTime = getReqTimeUtc()
        val amountStr = if (currency.equals("KHR", ignoreCase = true)) {
            amount.toLong().toString()
        } else {
            String.format(Locale.US, "%.2f", amount)
        }

        // Build items JSON base64
        val itemsArray = JSONArray().apply {
            put(JSONObject().apply {
                put("name", itemsSummary.take(50))
                put("quantity", 1)
                put("price", if (currency.equals("KHR", ignoreCase = true)) amount.toLong() else amount)
            })
        }
        val itemsBase64 = Base64.encodeToString(itemsArray.toString().toByteArray(Charsets.UTF_8), Base64.NO_WRAP)

        val purchaseType = "purchase"
        val paymentOption = "abapay_khqr"
        val callbackUrl = ""
        val returnDeeplink = ""
        val customFields = ""
        val returnParams = ""
        val payout = ""
        val lifetime = "3" // 3 minutes
        val qrImageTemplate = "template3_color"

        // Concatenate parameters strictly in ABA PayWay sequence
        val b4hash = reqTime +
                effectiveMerchantId +
                tranId +
                amountStr +
                itemsBase64 +
                firstName +
                lastName +
                email +
                phone +
                purchaseType +
                paymentOption +
                callbackUrl +
                returnDeeplink +
                currency.uppercase(Locale.US) +
                customFields +
                returnParams +
                payout +
                lifetime +
                qrImageTemplate

        // Only attempt remote HTTP call if API key is provided and valid
        if (apiKey.isNotBlank()) {
            val hash = computeHmacSha512(b4hash, apiKey)

            try {
                val jsonPayload = JSONObject().apply {
                    put("req_time", reqTime)
                    put("merchant_id", effectiveMerchantId)
                    put("tran_id", tranId)
                    put("amount", if (currency.equals("KHR", ignoreCase = true)) amount.toLong() else amount)
                    put("items", itemsBase64)
                    put("first_name", firstName)
                    put("last_name", lastName)
                    put("email", email)
                    put("phone", phone)
                    put("purchase_type", purchaseType)
                    put("payment_option", paymentOption)
                    put("callback_url", callbackUrl)
                    put("return_deeplink", returnDeeplink)
                    put("currency", currency.uppercase(Locale.US))
                    put("custom_fields", customFields)
                    put("return_params", returnParams)
                    put("payout", payout)
                    put("lifetime", lifetime.toInt())
                    put("qr_image_template", qrImageTemplate)
                    put("hash", hash)
                }

                val url = URL(endpoint)
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 8000
                    readTimeout = 8000
                    doInput = true
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Accept", "application/json")
                }

                OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
                    writer.write(jsonPayload.toString())
                    writer.flush()
                }

                val responseCode = conn.responseCode
                val stream = if (responseCode in 200..299) conn.inputStream else conn.errorStream
                val responseText = stream?.let {
                    BufferedReader(InputStreamReader(it, Charsets.UTF_8)).use { reader -> reader.readText() }
                } ?: ""

                if (responseCode in 200..299 && responseText.isNotBlank()) {
                    val respJson = JSONObject(responseText)
                    val qrString = respJson.optString("qrString", "")
                    val qrImage = respJson.optString("qrImage", "")
                    val deeplink = respJson.optString("abapay_deeplink", "")

                    if (qrString.isNotBlank() || qrImage.isNotBlank()) {
                        return@withContext PayWayQrResult(
                            success = true,
                            qrString = qrString,
                            qrImageBase64 = qrImage,
                            deeplink = deeplink.ifBlank { "abamobilebank://ababank.com?type=payway&qrcode=$qrString" },
                            tranId = tranId,
                            isLiveApi = true
                        )
                    }
                }
                Log.w(TAG, "PayWay API response ($responseCode): $responseText. Falling back to local KHQR.")
            } catch (e: Exception) {
                Log.e(TAG, "HTTP error communicating with PayWay sandbox API", e)
            }
        }

        // Clean offline / sandbox simulation KHQR generator fallback
        val mockKhqr = "00020101021230510016abaakhppxxx@abaa0115$effectiveMerchantId" +
                "520482495303" + (if (currency.equals("KHR", ignoreCase = true)) "116" else "840") +
                "5404$amountStr" +
                "5802KH5910NAWANO POS6011PHNOM PENH62210117$tranId" +
                "6304ABCD"

        PayWayQrResult(
            success = true,
            qrString = mockKhqr,
            qrImageBase64 = null,
            deeplink = "abamobilebank://ababank.com?type=payway&qrcode=$mockKhqr",
            tranId = tranId,
            isLiveApi = false
        )
    }

    /**
     * Checks the transaction status via PayWay check-transaction-2 API.
     */
    suspend fun checkTransaction(
        merchantId: String,
        apiKey: String,
        isSandbox: Boolean,
        tranId: String
    ): PayWayCheckResult = withContext(Dispatchers.IO) {
        val effectiveMerchantId = merchantId.ifBlank { DEFAULT_SANDBOX_MERCHANT_ID }
        if (apiKey.isBlank()) {
            return@withContext PayWayCheckResult(
                isApproved = false,
                statusMessage = "No API Key configured. Please configure in Settings or use Confirm button."
            )
        }

        val baseUrl = if (isSandbox) SANDBOX_BASE_URL else PRODUCTION_BASE_URL
        val endpoint = "$baseUrl/api/payment-gateway/v1/payments/check-transaction-2"

        val reqTime = getReqTimeUtc()
        val b4hash = reqTime + effectiveMerchantId + tranId
        val hash = computeHmacSha512(b4hash, apiKey)

        try {
            val payload = JSONObject().apply {
                put("req_time", reqTime)
                put("merchant_id", effectiveMerchantId)
                put("tran_id", tranId)
                put("hash", hash)
            }

            val url = URL(endpoint)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 6000
                readTimeout = 6000
                doInput = true
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
            }

            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(payload.toString())
                writer.flush()
            }

            val responseCode = conn.responseCode
            val stream = if (responseCode in 200..299) conn.inputStream else conn.errorStream
            val responseText = stream?.let {
                BufferedReader(InputStreamReader(it, Charsets.UTF_8)).use { reader -> reader.readText() }
            } ?: ""

            if (responseCode in 200..299 && responseText.isNotBlank()) {
                val respJson = JSONObject(responseText)
                val statusObj = respJson.optJSONObject("status")
                val dataObj = respJson.optJSONObject("data")

                val paymentStatus = dataObj?.optString("payment_status", "") ?: ""
                val paymentStatusCode = dataObj?.optInt("payment_status_code", -1) ?: -1

                val isApproved = paymentStatusCode == 0 || paymentStatus.equals("APPROVED", ignoreCase = true)

                return@withContext PayWayCheckResult(
                    isApproved = isApproved,
                    statusCode = paymentStatusCode,
                    statusMessage = paymentStatus.ifBlank { statusObj?.optString("message", "") ?: "Checked" },
                    rawResponse = responseText
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking PayWay transaction status", e)
        }

        PayWayCheckResult(
            isApproved = false,
            statusMessage = "Pending payment"
        )
    }
}
