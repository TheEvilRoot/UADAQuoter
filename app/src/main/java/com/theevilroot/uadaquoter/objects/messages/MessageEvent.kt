package com.theevilroot.uadaquoter.objects.messages

data class MessageEvent(
        val eventType: EventType,
        val message: Message? = null,
        val messageId: Int? = null,
        val newMessage: Message? = null
) {

    enum class EventType {
        MESSAGE_INSERT,
        MESSAGE_UPDATE,
        MESSAGE_DELETE
    }

}