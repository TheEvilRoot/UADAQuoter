package com.theevilroot.uadaquoter.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.theevilroot.uadaquoter.App
import com.theevilroot.uadaquoter.R
import com.theevilroot.uadaquoter.objects.messages.Message
import com.theevilroot.uadaquoter.utils.bindView

class MessagesAdapter: RecyclerView.Adapter<MessagesAdapter.MessageHolder>() {

    fun messagesCount(): Int = App.instance.messages.count()

    fun indexByMessage(message: Message): Int? {
        val index = App.instance.messages.indexOf(message)
        if (index < 0)
            return null
        return index
    }

    fun indexById(id: Int): Int? {
        val index = App.instance.messages.indexOfFirst { it.uniqueID == id }
        if (index < 0)
            return null
        return index
    }

    fun indexBy(any: Any): Int? {
        return when (any) {
            is Int -> indexById(any)
            is Message -> indexByMessage(any)
            else -> null
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageHolder =
            MessageHolder(LayoutInflater.from(parent.context).inflate(R.layout.message_layout, parent, false))

    override fun getItemCount(): Int =
            App.instance.messages.count()

    override fun onBindViewHolder(holder: MessageHolder, position: Int) =
            holder.bind(App.instance.messages[position])

    class MessageHolder(itemView: View): RecyclerView.ViewHolder(itemView) {

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

