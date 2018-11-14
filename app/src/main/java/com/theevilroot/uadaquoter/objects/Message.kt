package com.theevilroot.uadaquoter.objects

import android.net.Uri
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes

data class Message(val title: String,
                   val message: String,
                   @ColorRes val color: Int,
                   @DrawableRes val icon: Int,
                   val actions: List<MessageAction> = emptyList())