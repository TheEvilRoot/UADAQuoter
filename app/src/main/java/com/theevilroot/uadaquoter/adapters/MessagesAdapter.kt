package com.theevilroot.uadaquoter.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.theevilroot.uadaquoter.R
import com.theevilroot.uadaquoter.objects.Message
import com.theevilroot.uadaquoter.utils.bindView

class MessagesAdapter(private val updateMessagesBadgeFunction: () -> Unit): RecyclerView.Adapter<MessagesAdapter.MessageHolder>() {

    private val messages: ArrayList<Message> = ArrayList()

    fun addMessage(message: Message) {
        if (message.uniqueID != null) {
            for (index in messages.indices) {
                if (messages[index].uniqueID == message.uniqueID) {
                    messages[index] = message
                    notifyItemChanged(index)
                    return
                }
            }
        }
        messages.add(message)
        notifyItemInserted(messages.count() - 1)
        updateMessagesBadgeFunction()
    }

    fun dismissMessage(position: Int) {
        messages.removeAt(position)
        notifyItemRemoved(position)
        updateMessagesBadgeFunction()
    }

    fun messagesCount(): Int = messages.count()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageHolder =
            MessageHolder(LayoutInflater.from(parent.context).inflate(R.layout.message_layout, parent, false), this::dismissMessage)

    override fun getItemCount(): Int =
            messages.count()

    override fun onBindViewHolder(holder: MessageHolder, position: Int) =
            holder.bind(messages[position])

    class MessageHolder(itemView: View, val removeItemFunction: (Int) -> Unit): RecyclerView.ViewHolder(itemView) {

        private val titleView by bindView<TextView>(R.id.message_title)
        private val contentView by bindView<TextView>(R.id.message_content)
        private val headView by bindView<View>(R.id.message_head)
        private val actionsView by bindView<RecyclerView>(R.id.message_actions_view)
        private val headerIndicator by bindView<View>(R.id.message_header_indicator)

        private lateinit var actionsAdapter: MessageActionsAdapter

        fun bind(message: Message) {
            if (!::actionsAdapter.isInitialized)
                actionsAdapter = MessageActionsAdapter(message, this)
            headView.setBackgroundColor(itemView.context.resources.getColor(message.color))
            titleView.text = message.title
            contentView.text = message.message
            actionsView.layoutManager = LinearLayoutManager(itemView.context, LinearLayoutManager.HORIZONTAL, false)
            actionsView.adapter = actionsAdapter
            if (message.actions.isNotEmpty())
                headerIndicator.visibility = View.VISIBLE
            else headerIndicator.visibility = View.GONE
        }
    }

}

