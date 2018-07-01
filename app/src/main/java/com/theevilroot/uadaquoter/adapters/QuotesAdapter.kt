package com.theevilroot.uadaquoter.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.theevilroot.uadaquoter.Quote
import com.theevilroot.uadaquoter.R
import java.text.SimpleDateFormat
import java.util.*

class QuotesAdapter(context: Context, items: Array<Quote>): ArrayAdapter<Quote>(context, R.layout.quote_list_item, items) {


    @SuppressLint("SetTextI18n")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.quote_list_item, null)
        val item = getItem(position) ?: return view
        with(view) {
            val idView = findViewById<TextView>(R.id.quote_id)
            val textView = findViewById<TextView>(R.id.quote_content)
            val infoView = findViewById<TextView>(R.id.quote_info)
            val editedView = findViewById<TextView>(R.id.quote_edited)
            idView.text = "#${item.id}"
            textView.text = item.text
            infoView.text = item.author
            if(item.editedBy != null && item.editedAt != -1L) {
                editedView.text = "(ред. ${SimpleDateFormat("dd.HH.yyyy").format(Date(item.editedAt))})"
            }else{
                editedView.text = ""
            }
        }
        return view
    }
}