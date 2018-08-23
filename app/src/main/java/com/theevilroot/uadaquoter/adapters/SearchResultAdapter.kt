package com.theevilroot.uadaquoter.adapters

import android.annotation.SuppressLint
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.theevilroot.uadaquoter.Quote
import com.theevilroot.uadaquoter.R
import com.theevilroot.uadaquoter.bindView
import java.text.SimpleDateFormat
import java.util.*

class SearchResultAdapter(private val onClick: (Quote) -> Unit): RecyclerView.Adapter<SearchResultAdapter.SearchResultHolder>() {

    private val items = ArrayList<Quote>()

    fun setQuotes(quotes: List<Quote>) {
        items.clear()
        items.addAll(quotes)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchResultHolder =
            SearchResultHolder(LayoutInflater.from(parent.context).inflate(R.layout.quote_layout, parent, false), onClick)

    override fun getItemCount(): Int =
            items.count()

    override fun onBindViewHolder(holder: SearchResultHolder, position: Int) {
        holder.itemView.tag = position
        holder.bind(items[position])
    }

    class SearchResultHolder(itemView: View,private val onClick: (Quote) -> Unit): RecyclerView.ViewHolder(itemView) {
        @SuppressLint("SimpleDateFormat")
        private val dFormat = SimpleDateFormat("dd.MM.yyyy")

        private val idView by bindView<TextView>(R.id.quote_id)
        private val infoView by bindView<TextView>(R.id.quote_info)
        private val contentView by bindView<TextView>(R.id.quote_content)
        private val editedView by bindView<TextView>(R.id.quote_edited)

        @SuppressLint("SetTextI18n")
        fun bind(quote: Quote) {
            idView.text = "#${quote.id}"
            contentView.text = quote.text
            infoView.text = quote.author
            if(quote.editedBy != null && quote.editedAt != -1L) {
                editedView.text = "(ред. ${dFormat.format(Date(quote.editedAt))})"
            }else{
                editedView.text = ""
            }
            itemView.setOnClickListener {
                try{
                    onClick(quote)
                }catch (e: Exception) {  }
            }
        }
    }

}