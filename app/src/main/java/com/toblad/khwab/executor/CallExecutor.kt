package com.toblad.khwab.executor

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.util.Log
import com.toblad.khwab.integration.model.execution.ExecutionPlan

/**
 * Handles CALL_CONTACT actions.
 *
 * [ExecutionPlan.target] is a contact name or phone number.
 *
 * Resolution order:
 *   1. If target already looks like a number (digits/+/-) → use directly.
 *   2. Query ContactsContract by display name → use first matched number.
 *   3. Open dialler with raw target as fallback (user can edit before calling).
 *
 * Opens ACTION_DIAL (not ACTION_CALL) so the user confirms before the call is placed.
 * Requires READ_CONTACTS permission for name lookup (gracefully falls back without it).
 */
class CallExecutor(private val context: Context) : AndroidExecutor {

    override fun supports(action: String): Boolean =
        action.equals("CALL_CONTACT", ignoreCase = true)

    override fun execute(plan: ExecutionPlan): Boolean {
        val raw = plan.target?.trim() ?: return false

        val number = when {
            isPhoneNumber(raw) -> raw
            else -> resolveContactNumber(raw) ?: raw
        }

        return try {
            val uri = if (number.startsWith("tel:")) Uri.parse(number)
                      else Uri.parse("tel:$number")
            val intent = Intent(Intent.ACTION_DIAL, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Log.d("CallExecutor", "Opened dialler for '$raw' → $number")
            true
        } catch (e: Exception) {
            Log.e("CallExecutor", "Failed to open dialler: ${e.message}")
            false
        }
    }

    private fun isPhoneNumber(s: String): Boolean =
        s.matches(Regex("[+\\d][\\d\\s\\-().]{3,}"))

    /**
     * Queries ContactsContract.CommonDataKinds.Phone for a display name
     * that contains [name] (case-insensitive). Returns the first number found.
     */
    private fun resolveContactNumber(name: String): String? {
        return try {
            val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
            )
            val selection =
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
            val args = arrayOf("%$name%")

            context.contentResolver.query(uri, projection, selection, args, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndexOrThrow(
                            ContactsContract.CommonDataKinds.Phone.NUMBER
                        )
                        cursor.getString(idx)
                    } else null
                }
        } catch (e: SecurityException) {
            // READ_CONTACTS not granted — fall back to raw target
            Log.w("CallExecutor", "READ_CONTACTS not granted, using raw target")
            null
        } catch (e: Exception) {
            Log.e("CallExecutor", "Contact lookup failed: ${e.message}")
            null
        }
    }
}
