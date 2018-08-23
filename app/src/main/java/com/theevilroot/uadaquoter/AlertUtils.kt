package com.theevilroot.uadaquoter

import android.content.Context
import android.support.annotation.StyleRes
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.widget.Button
import android.widget.TextView

fun buildAlert(context: Context,
               tvTitle: (TextView) -> Unit,
               tvMessage: (TextView) -> Unit,
               yesBtn: (Button) -> Unit,
               noBtn: (Button) -> Unit,
               onClick: (Boolean) -> Boolean,
               autoShow: Boolean = true,
               @StyleRes theme: Int = R.style.AppTheme_Dialog): AlertDialog {
    val view = LayoutInflater.from(context).inflate(R.layout.alert_layout, null)
    val dialog = AlertDialog.Builder(context, theme).setView(view).create()
    val title = view.findViewById<TextView>(R.id.alert_title)
    val message = view.findViewById<TextView>(R.id.alert_msg)
    val yes = view.findViewById<Button>(R.id.alert_yes)
    val no = view.findViewById<Button>(R.id.alert_no)
    title.apply { tvTitle(this) }
    message.apply { tvMessage(this) }
    yes.apply { yesBtn(this) }
    no.apply { noBtn(this) }
    yes.setOnClickListener {
        if(onClick(true))
            dialog.dismiss()
    }
    no.setOnClickListener {
        if(onClick(false))
            dialog.dismiss()
    }
    if (autoShow)
        dialog.show()
    return dialog
}