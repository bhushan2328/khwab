package com.toblad.khwab.executor

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.toblad.khwab.integration.model.execution.ExecutionPlan

/**
 * Handles SEND_MESSAGE actions.
 *
 * [ExecutionPlan.target]              — phone number or contact name
 * [ExecutionPlan.parameters]["text"]  — message body (optional pre-fill)
 * [ExecutionPlan.parameters]["app"]   — "whatsapp", "telegram", or absent for SMS
 *
 * Routing:
 *   1. "whatsapp"  → opens WhatsApp share intent (requires WhatsApp installed)
 *   2. "telegram"  → opens Telegram share intent (requires Telegram installed)
 *   3. default     → opens system SMS app with recipient + body pre-filled
 *
 * The user must press Send themselves.
 */
class MessageExecutor(private val context: Context) : AndroidExecutor {

    override fun supports(action: String): Boolean =
        action.equals("SEND_MESSAGE", ignoreCase = true)

    override fun execute(plan: ExecutionPlan): Boolean {
        val recipient = plan.target?.trim() ?: ""
        val body = plan.parameters["text"]?.trim() ?: ""
        val app = plan.parameters["app"]?.trim()?.lowercase() ?: ""

        return when {
            app == "whatsapp" -> tryWhatsApp(recipient, body)
            app == "telegram" -> tryTelegram(recipient, body)
            else              -> trySms(recipient, body)
        }
    }

    private fun tryWhatsApp(recipient: String, body: String): Boolean {
        return try {
            // WhatsApp share intent: phone number must be in E.164 (digits only).
            // If recipient looks like a name (not a number) we open the chat list
            // so the user can pick the contact manually.
            val intent = if (recipient.matches(Regex("[+\\d][\\d\\s\\-().]+"))) {
                val phone = recipient.replace(Regex("[^\\d+]"), "")
                Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$phone")).apply {
                    if (body.isNotBlank()) putExtra(Intent.EXTRA_TEXT, body)
                }
            } else {
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    setPackage("com.whatsapp")
                    if (body.isNotBlank()) putExtra(Intent.EXTRA_TEXT, body)
                }
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (context.packageManager.resolveActivity(intent, 0) != null) {
                context.startActivity(intent)
                Log.d("MessageExecutor", "Opened WhatsApp for '$recipient'")
                true
            } else {
                Log.w("MessageExecutor", "WhatsApp not installed — falling back to SMS")
                trySms(recipient, body)
            }
        } catch (e: Exception) {
            Log.e("MessageExecutor", "WhatsApp failed: ${e.message}")
            trySms(recipient, body)
        }
    }

    private fun tryTelegram(recipient: String, body: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                setPackage("org.telegram.messenger")
                if (body.isNotBlank()) putExtra(Intent.EXTRA_TEXT, body)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (context.packageManager.resolveActivity(intent, 0) != null) {
                context.startActivity(intent)
                Log.d("MessageExecutor", "Opened Telegram for '$recipient'")
                true
            } else {
                Log.w("MessageExecutor", "Telegram not installed — falling back to SMS")
                trySms(recipient, body)
            }
        } catch (e: Exception) {
            Log.e("MessageExecutor", "Telegram failed: ${e.message}")
            trySms(recipient, body)
        }
    }

    private fun trySms(recipient: String, body: String): Boolean {
        return try {
            val uri = if (recipient.isNotBlank()) Uri.parse("smsto:$recipient")
                      else Uri.parse("smsto:")
            val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
                if (body.isNotBlank()) putExtra("sms_body", body)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Log.d("MessageExecutor", "Opened SMS composer for '$recipient'")
            true
        } catch (e: Exception) {
            Log.e("MessageExecutor", "Failed to open SMS composer: ${e.message}")
            false
        }
    }
}
