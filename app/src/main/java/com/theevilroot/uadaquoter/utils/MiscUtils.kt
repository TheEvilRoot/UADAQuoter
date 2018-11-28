package com.theevilroot.uadaquoter.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.View
import androidx.annotation.IdRes
import androidx.recyclerview.widget.RecyclerView
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

fun <K,V> Map<K,V>.getOrDef(key: K, defValue: V): V =
        if (containsKey(key))
            get(key)!!
        else defValue

fun Context.openInBrowser(uri: Uri) {
    val intent = Intent(Intent.ACTION_VIEW)
    intent.data = uri
    startActivity(intent)
}

fun Context.log(msg: String) {
    Log.i(this::class.java.simpleName, msg)
}