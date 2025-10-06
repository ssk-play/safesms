package ssk.safesms.data.model

data class SmsMessage(
    val id: Long,
    val threadId: Long,
    val address: String,
    val body: String,
    val date: Long,
    val type: Int, // 1 = received, 2 = sent
    val read: Boolean
) {
    companion object {
        const val TYPE_RECEIVED = 1
        const val TYPE_SENT = 2
    }
}
