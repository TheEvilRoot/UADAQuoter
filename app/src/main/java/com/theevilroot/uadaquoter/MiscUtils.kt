package com.theevilroot.uadaquoter

import android.app.Activity
import android.content.Context
import android.support.annotation.IdRes
import android.support.v7.app.AlertDialog
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import com.google.gson.JsonObject
import me.philio.pinentry.PinEntryView

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

fun showAdderNameDialog(context: Context, defaultValue: String ,onSave: (EditText, TextView, AlertDialog) -> Unit, cancelable: Boolean = true) {
    val view = LayoutInflater.from(context).inflate(R.layout.personal_data_layout, null, false)
    val dialog = AlertDialog.Builder(context).setView(view).setCancelable(cancelable).create()
    with(view) {
        val label = findViewById<TextView>(R.id.personal_data_label)
        val adderNameField = findViewById<EditText>(R.id.personal_data_adder_name_field)
        val saveBtn = findViewById<ImageButton>(R.id.personal_data_save)
        val cancelBtn= findViewById<ImageButton>(R.id.personal_data_cancel)

        adderNameField.setText(defaultValue)

        if(!cancelable)
            cancelBtn.visibility = View.VISIBLE

        saveBtn.setOnClickListener { onSave(adderNameField, label, dialog) }
        cancelBtn.setOnClickListener { dialog.dismiss() }
    }
    dialog.show()
}

fun showPINDialog(context: Context, onPinChanged: (String, AlertDialog, PinEntryView) -> Unit) {
    val view = LayoutInflater.from(context).inflate(R.layout.security_code_dialog_layout, null, false)
    val dialog = AlertDialog.Builder(context).setView(view).create()
    with(view) {
        val pinView = findViewById<PinEntryView>(R.id.security_code_field)
        pinView.addTextChangedListener(TextWatcherWrapper(onChange = { str, _, _, _ -> onPinChanged(str, dialog, pinView) }))
    }
    dialog.show()
}