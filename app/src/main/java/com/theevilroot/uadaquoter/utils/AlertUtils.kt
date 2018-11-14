package com.theevilroot.uadaquoter.utils

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.theevilroot.uadaquoter.R
import com.theevilroot.uadaquoter.objects.TextWatcherWrapper

fun showAdderNameDialog(context: Context, defaultValue: String, onSave: (EditText, TextView, AlertDialog) -> Unit, cancelable: Boolean = true) {
    val view = LayoutInflater.from(context).inflate(R.layout.personal_data, null, false)
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

fun showPINDialog(context: Context, onPinChanged: (String, AlertDialog, EditText) -> Unit) {
    val view = LayoutInflater.from(context).inflate(R.layout.security_code_dialog_layout, null, false)
    val dialog = AlertDialog.Builder(context).setView(view).create()
    with(view) {
        val pinView = findViewById<EditText>(R.id.security_code_field)
        pinView.addTextChangedListener(TextWatcherWrapper(onChange = { string, _, _, _ ->
            onPinChanged(string, dialog, pinView)
        }))
    }
    dialog.show()
}