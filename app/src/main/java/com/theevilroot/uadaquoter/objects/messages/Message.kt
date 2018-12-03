package com.theevilroot.uadaquoter.objects.messages

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes

data class Message(val title: String,
                   val message: String,
                   @ColorRes val color: Int,
                   @DrawableRes val icon: Int,
                   val actions: List<MessageAction> = emptyList(),
                   val uniqueID: Int? = null)