package com.theevilroot.uadaquoter.adapters

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.GsonBuilder
import com.theevilroot.uadaquoter.*
import com.theevilroot.uadaquoter.activities.EditQuoteActivity
import com.theevilroot.uadaquoter.objects.Quote
import com.theevilroot.uadaquoter.utils.bindView
import java.text.SimpleDateFormat
import java.util.*

open class QuotesAdapter: RecyclerView.Adapter<QuotesAdapter.QuoteViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuoteViewHolder =
            QuoteViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.quote_layout, parent, false))

    override fun getItemCount(): Int =
            QuoterApi.quotes.count()

    override fun onBindViewHolder(holder: QuoteViewHolder, position: Int) {
        holder.itemView.tag = position
        holder.bind(QuoterApi.quotes[position])
    }
    open class QuoteViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {

        @SuppressLint("SimpleDateFormat")
        private val dFormat = SimpleDateFormat("dd.MM.yyyy")

        private val idView by bindView<TextView>(R.id.quote_id)
        private val infoView by bindView<TextView>(R.id.quote_info)
        private val contentView by bindView<TextView>(R.id.quote_content)
        private val adderRootView by bindView<View>(R.id.quote_adder_root)
        private val adderView by bindView<TextView>(R.id.quote_adder)
        private val editInfoRootView by bindView<View>(R.id.quote_edit_info_root)
        private val editInfoView by bindView<TextView>(R.id.quote_edit_info)
        private val actionRootView by bindView<View>(R.id.quote_action_root)
        private val editAction by bindView<Button>(R.id.quote_action_edit)
        private val shareAction by bindView<Button>(R.id.quote_action_share)

        @SuppressLint("SetTextI18n")
        open fun bind(quote: Quote) {
            idView.text = "#${quote.id}"
            contentView.text = quote.text
            infoView.text = quote.author
            adderView.text = quote.adder
            if(quote.editedBy != null && quote.editedAt != -1L) {
                editInfoView.text = "${dFormat.format(Date(quote.editedAt))} ${quote.editedBy}"
                editInfoRootView.visibility = View.VISIBLE
            }else{
                editInfoRootView.visibility = View.GONE
            }
            editAction.setOnClickListener {
                val intent = Intent(itemView.context, EditQuoteActivity::class.java)
                intent.putExtra("quote", GsonBuilder().create().toJson(quote.toJson()))
                itemView.context.startActivity(intent)
            }
            shareAction.setOnClickListener {
                QuoterApi.shareQuote(itemView.context, quote)
            }
        }
    }

}


