package com.theevilroot.uadaquoter

import android.content.Context
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.widget.Button
import android.widget.TextView

fun buildAlert(context: Context, alertTitle: String = "Alert!", alertMessage: String = "Ahtung!",yesText: String = "Yes", noText: String = "No",onClick: (Boolean) -> Boolean) {
    val view = LayoutInflater.from(context).inflate(R.layout.alert_layout, null)
    val dialog = AlertDialog.Builder(context, R.style.AppTheme_Dialog).setView(view).create()
    val title = view.findViewById<TextView>(R.id.alert_title)
    val message = view.findViewById<TextView>(R.id.alert_msg)
    val yes = view.findViewById<Button>(R.id.alert_yes)
    val no = view.findViewById<Button>(R.id.alert_no)
    title.text = alertTitle
    message.text = alertMessage
    yes.text = yesText
    no.text = noText
    yes.setOnClickListener {
        if(onClick(true))
            dialog.dismiss()
    }
    no.setOnClickListener {
        if(onClick(false))
            dialog.dismiss()
    }
    dialog.show()
}