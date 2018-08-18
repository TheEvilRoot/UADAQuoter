package com.theevilroot.uadaquoter

import android.app.Activity
import android.support.annotation.IdRes
import android.support.v7.widget.RecyclerView
import android.view.View
import com.google.gson.JsonObject

fun <T: View> Activity.bind(@IdRes id: Int): Lazy<T> =
        lazy { findViewById<T>(id) }
fun <T: View> RecyclerView.ViewHolder.bindView(@IdRes id: Int): Lazy<T> =
        lazy { itemView.findViewById<T>(id) }

operator fun JsonObject.contains(key: String) = has(key)
operator fun JsonObject.contains(keys: Array<String>): Boolean {
    for (key in keys)
        if (key !in this)
            return false
    return true
}