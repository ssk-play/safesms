package ssk.kidssms.data.model

data class SmsThread(
    val threadId: Long,
    val address: String,
    val snippet: String,
    val date: Long,
    val messageCount: Int,
    val read: Boolean
)
