package com.theevilroot.uadaquoter.adapters

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.recyclerview.widget.RecyclerView
import com.theevilroot.uadaquoter.App
import com.theevilroot.uadaquoter.objects.Message
import com.theevilroot.uadaquoter.R
import com.theevilroot.uadaquoter.objects.MessageAction
import com.theevilroot.uadaquoter.utils.bindView
import com.theevilroot.uadaquoter.objects.MessageActionType.*
import com.theevilroot.uadaquoter.objects.MessageEvent
import com.theevilroot.uadaquoter.utils.openInBrowser

class MessageActionsAdapter(private val message: Message, private val holder: MessagesAdapter.MessageHolder): RecyclerView.Adapter<MessageActionsAdapter.MessageActionHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageActionHolder =
            MessageActionHolder(LayoutInflater.from(parent.context).inflate(R.layout.message_action_layout, parent, false), holder)

    override fun getItemCount(): Int =
            message.actions.count()

    override fun onBindViewHolder(holder: MessageActionHolder, position: Int) =
            holder.bind(message.actions[position])

    class MessageActionHolder(itemView: View, private val holder: MessagesAdapter.MessageHolder): RecyclerView.ViewHolder(itemView) {

        private val actionView by bindView<Button>(R.id.message_action_view)

        fun bind(messageAction: MessageAction) {
            actionView.text = messageAction.actionTitle
            actionView.setOnClickListener {
                when (messageAction.actionType) {
                    TYPE_DISMISS -> {
                        val message = App.instance.messages.getOrNull(holder.adapterPosition)
                        if (message != null)
                            App.instance.api.messagesService().onNext(MessageEvent(MessageEvent.EventType.MESSAGE_DELETE, message = message))
                    }
                    TYPE_URI -> if (messageAction.uri != null) {
                        itemView.context.openInBrowser(messageAction.uri)
                    }
                    TYPE_ACTIVITY -> if (messageAction.activity != null) {
                        itemView.context.startActivity(Intent(itemView.context, messageAction.activity))
                    }
                    TYPE_ACTION -> if (messageAction.action != null) {
                        if (messageAction.action.invoke(holder.itemView.context)) {
                            val message = App.instance.messages.getOrNull(holder.adapterPosition)
                            if (message != null)
                                App.instance.api.messagesService().onNext(MessageEvent(MessageEvent.EventType.MESSAGE_DELETE, message = message))
                        }
                    }
                }
            }
        }

    }
}