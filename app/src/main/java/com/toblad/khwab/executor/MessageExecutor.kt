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
 *
 * Opens the default SMS app with the recipient and optional body pre-filled.
 * The user must press Send themselves.
 */
class MessageExecutor(private val context: Context) : AndroidExecutor {

    override fun supports(action: String): Boolean =
        action.equals("SEND_MESSAGE", ignoreCase = true)

    override fun execute(plan: ExecutionPlan): Boolean {
        val recipient = plan.target?.trim() ?: ""
        val body = plan.parameters["text"]?.trim() ?: ""
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
