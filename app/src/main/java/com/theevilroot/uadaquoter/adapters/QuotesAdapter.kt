package com.theevilroot.uadaquoter.adapters

import android.annotation.SuppressLint
import android.content.Intent
import android.support.v7.app.AlertDialog
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import com.google.gson.GsonBuilder
import com.theevilroot.uadaquoter.*
import com.theevilroot.uadaquoter.activities.EditQuoteActivity
import java.text.SimpleDateFormat
import java.util.*

open class QuotesAdapter: RecyclerView.Adapter<QuotesAdapter.QuoteViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuoteViewHolder =
            QuoteViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.quote_layout, parent, false))

    override fun getItemCount(): Int =
            QuoterAPI.quotes.count()

    override fun onBindViewHolder(holder: QuoteViewHolder, position: Int) {
        holder.itemView.tag = position
        holder.bind(QuoterAPI.quotes[position])
    }
    open class QuoteViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {

        @SuppressLint("SimpleDateFormat")
        private val dFormat = SimpleDateFormat("dd.MM.yyyy")

        private val idView by bindView<TextView>(R.id.quote_id)
        private val infoView by bindView<TextView>(R.id.quote_info)
        private val contentView by bindView<TextView>(R.id.quote_content)
        private val editedView by bindView<TextView>(R.id.quote_edited)

        @SuppressLint("SetTextI18n")
        open fun bind(quote: Quote) {
            idView.text = "#${quote.id}"
            contentView.text = quote.text
            infoView.text = quote.author
            if(quote.editedBy != null && quote.editedAt != -1L) {
                editedView.text = "(ред. ${dFormat.format(Date(quote.editedAt))})"
            }else{
                editedView.text = ""
            }
            itemView.setOnClickListener {
                val view = LayoutInflater.from(itemView.context).inflate(R.layout.quote_dialog_layout, null, false)
                val builder = AlertDialog.Builder(itemView.context, R.style.CustomAlert_Dialog).setView(view)
                with(view) {
                    val dAdderView = findViewById<TextView>(R.id.quote_dialog_adder)
                    val dAuthorView = findViewById<TextView>(R.id.quote_dialog_author)
                    val dEditorView = findViewById<TextView>(R.id.quote_dialog_editor)
                    val dEditedAtView = findViewById<TextView>(R.id.quote_dialog_edited_at)
                    val dTextView = findViewById<TextView>(R.id.quote_dialog_text)
                    val dShareBtn = findViewById<ImageButton>(R.id.quote_dialog_share)
                    val dEditBtn = findViewById<ImageButton>(R.id.quote_dialog_edit)
                    val dEditedRoot = findViewById<View>(R.id.quote_dialog_edited_root)

                    dAdderView.text = quote.adder
                    dAuthorView.text = quote.author
                    if (quote.editedBy != null) {
                        dEditorView.text = quote.editedBy!!
                        dEditedAtView.text = dFormat.format(quote.editedAt)
                    } else dEditedRoot.visibility = View.GONE
                    dTextView.text = quote.text
                    dShareBtn.setOnClickListener { QuoterAPI.shareQuote(itemView.context, quote) }
                    dEditBtn.setOnClickListener {
                        val intent = Intent(itemView.context, EditQuoteActivity::class.java)
                        intent.putExtra("quote", GsonBuilder().create().toJson(quote.toJson()))
                        itemView.context.startActivity(intent)
                    }
                }
                builder.create().apply { window?.attributes?.windowAnimations = R.style.AppTheme_Dialog_PopUp }.show()
            }
        }
    }

}


