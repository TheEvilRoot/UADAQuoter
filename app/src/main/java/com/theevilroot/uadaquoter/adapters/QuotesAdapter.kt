package com.theevilroot.uadaquoter.adapters

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.RecyclerView
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import com.chauthai.swipereveallayout.SwipeRevealLayout
import com.chauthai.swipereveallayout.ViewBinderHelper
import com.google.gson.GsonBuilder
import com.theevilroot.uadaquoter.Quote
import com.theevilroot.uadaquoter.QuoterAPI
import com.theevilroot.uadaquoter.R
import com.theevilroot.uadaquoter.activities.EditQuoteActivity
import com.theevilroot.uadaquoter.bindView
import java.text.SimpleDateFormat
import java.util.*

class QuotesAdapter: RecyclerView.Adapter<QuotesAdapter.QuoteViewHolder>() {

    private val helper = ViewBinderHelper()

    init {
        helper.setOpenOnlyOne(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuoteViewHolder =
            QuoteViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.quote_item_layout, parent, false))

    override fun getItemCount(): Int =
            QuoterAPI.quotes.count()

    override fun onBindViewHolder(holder: QuoteViewHolder, position: Int) {
        holder.itemView.tag = position
        helper.bind(holder.swipeLayout, QuoterAPI.quotes[position].id.toString())
        holder.bind(QuoterAPI.quotes[position])
    }

    fun saveStates(state: Bundle) =
            helper.saveStates(state)

    fun restoreStates(state: Bundle) =
            helper.restoreStates(state)

    class QuoteViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {

        @SuppressLint("SimpleDateFormat")
        private val dFormat = SimpleDateFormat("dd.MM.yyyy")

        val swipeLayout by bindView<SwipeRevealLayout>(R.id.quote_swipe_layout)

        private val idView by bindView<TextView>(R.id.quote_id)
        private val infoView by bindView<TextView>(R.id.quote_info)
        private val contentView by bindView<TextView>(R.id.quote_content)
        private val editedView by bindView<TextView>(R.id.quote_edited)

        private val moreAuthorView by bindView<TextView>(R.id.quote_more_author)
        private val moreAdderView by bindView<TextView>(R.id.quote_more_adder)
        private val moreEditorView by bindView<TextView>(R.id.quote_more_editor)

        private val moreEdit by bindView<View>(R.id.quote_more_edit)
        private val moreShare by bindView<View>(R.id.quote_more_share)

        @SuppressLint("SetTextI18n")
        fun bind(quote: Quote) {
            idView.text = "#${quote.id}"
            contentView.text = Html.fromHtml(quote.text)
            infoView.text = quote.author
            moreAuthorView.text = quote.author
            moreAdderView.text = quote.adder
            if(quote.editedBy != null && quote.editedAt != -1L) {
                editedView.text = "(ред. ${dFormat.format(Date(quote.editedAt))})"
                moreEditorView.text = quote.editedBy!!
            }else{
                editedView.text = ""
                moreEditorView.text = "Не редактировано"
            }
            moreEdit.setOnClickListener {
                val intent = Intent(itemView.context, EditQuoteActivity::class.java)
                intent.putExtra("quote", GsonBuilder().create().toJson(quote.toJson()))
                itemView.context.startActivity(intent)
                swipeLayout.close(true)
            }
            moreShare.setOnClickListener {
                val intent = Intent(Intent.ACTION_SEND)
                intent.putExtra(Intent.EXTRA_TEXT, "${quote.text}\n\n(c) ${quote.author}")
                intent.type = "text/plain"
                itemView.context.startActivity(intent)
            }

        }
    }

}


