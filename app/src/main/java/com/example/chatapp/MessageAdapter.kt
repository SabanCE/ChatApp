package com.example.chatapp

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.chatapp.databinding.ItemMessageReceivedBinding
import com.example.chatapp.databinding.ItemMessageSentBinding
import com.google.firebase.auth.FirebaseAuth

class MessageAdapter(private val messageList: List<Message>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val ITEM_SENT = 1
    private val ITEM_RECEIVED = 2

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == ITEM_SENT) {
            val binding = ItemMessageSentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            SentViewHolder(binding)
        } else {
            val binding = ItemMessageReceivedBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            ReceivedViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messageList[position]
        if (holder is SentViewHolder) {
            holder.binding.tvMessage.text = message.message
        } else if (holder is ReceivedViewHolder) {
            holder.binding.tvMessage.text = message.message
        }
    }

    override fun getItemCount(): Int = messageList.size

    override fun getItemViewType(position: Int): Int {
        return if (messageList[position].senderId == FirebaseAuth.getInstance().currentUser?.uid) {
            ITEM_SENT
        } else {
            ITEM_RECEIVED
        }
    }

    class SentViewHolder(val binding: ItemMessageSentBinding) : RecyclerView.ViewHolder(binding.root)
    class ReceivedViewHolder(val binding: ItemMessageReceivedBinding) : RecyclerView.ViewHolder(binding.root)
}