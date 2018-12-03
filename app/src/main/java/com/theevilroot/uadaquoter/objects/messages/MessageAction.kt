package com.theevilroot.uadaquoter.objects.messages

import android.app.Activity
import android.content.Context
import android.net.Uri

enum class MessageActionType {
    TYPE_DISMISS,
    TYPE_URI,
    TYPE_ACTIVITY,
    TYPE_ACTION
}

data class MessageAction(val actionTitle: String = "Убрать",
                         val actionType: MessageActionType = MessageActionType.TYPE_DISMISS,
                         val activity: Class<out Activity>? = null,
                         val uri: Uri? = null,
                         val action: ((Context) -> Boolean)? = null)