package com.example.chatapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.chatapp.databinding.ItemDateHeaderBinding
import com.example.chatapp.databinding.ItemMessageReceivedBinding
import com.example.chatapp.databinding.ItemMessageSentBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MessageAdapter(private val messageList: List<Message>, private val isGroupChat: Boolean = false) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val ITEM_SENT = 1
    private val ITEM_RECEIVED = 2
    private val ITEM_DATE = 3
    
    private val database = FirebaseDatabase.getInstance("https://chatapp-df32e-default-rtdb.firebaseio.com").reference

    // Verileri tarih başlıklarıyla harmanlanmış şekilde tutmak için
    private var displayList: List<Any> = emptyList()

    init {
        updateDisplayList()
    }

    fun updateData() {
        updateDisplayList()
        notifyDataSetChanged()
    }

    private fun updateDisplayList() {
        val newList = mutableListOf<Any>()
        var lastDate = ""

        messageList.forEach { message ->
            val messageDate = getFormattedDate(message.timestamp)
            if (messageDate != lastDate) {
                newList.add(messageDate) // String olarak tarihi ekle
                lastDate = messageDate
            }
            newList.add(message)
        }
        displayList = newList
    }

    private fun getFormattedDate(timestamp: Long): String {
        val messageCalendar = Calendar.getInstance().apply { timeInMillis = timestamp }
        val now = Calendar.getInstance()

        return when {
            isSameDay(messageCalendar, now) -> "Bugün"
            isYesterday(messageCalendar, now) -> "Dün"
            else -> SimpleDateFormat("d MMMM yyyy", Locale("tr")).format(Date(timestamp))
        }
    }

    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    private fun isYesterday(cal1: Calendar, cal2: Calendar): Boolean {
        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        return isSameDay(cal1, yesterday)
    }

    override fun getItemViewType(position: Int): Int {
        val item = displayList[position]
        return when {
            item is String -> ITEM_DATE
            (item as Message).senderId == FirebaseAuth.getInstance().currentUser?.uid -> ITEM_SENT
            else -> ITEM_RECEIVED
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            ITEM_DATE -> DateViewHolder(ItemDateHeaderBinding.inflate(inflater, parent, false))
            ITEM_SENT -> SentViewHolder(ItemMessageSentBinding.inflate(inflater, parent, false))
            else -> ReceivedViewHolder(ItemMessageReceivedBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = displayList[position]

        when (holder) {
            is DateViewHolder -> {
                holder.binding.tvDateHeader.text = item as String
            }
            is SentViewHolder -> {
                val message = item as Message
                holder.binding.tvMessage.text = message.message
                holder.binding.tvTimestamp.text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp))
            }
            is ReceivedViewHolder -> {
                val message = item as Message
                holder.binding.tvMessage.text = message.message
                holder.binding.tvTimestamp.text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp))
                
                if (isGroupChat) {
                    holder.binding.tvSenderName.visibility = View.VISIBLE
                    database.child("Users").child(message.senderId).child("fullName")
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                holder.binding.tvSenderName.text = snapshot.value as? String ?: "Bilinmeyen"
                            }
                            override fun onCancelled(error: DatabaseError) {}
                        })
                } else {
                    holder.binding.tvSenderName.visibility = View.GONE
                }
            }
        }
    }

    override fun getItemCount(): Int = displayList.size

    class SentViewHolder(val binding: ItemMessageSentBinding) : RecyclerView.ViewHolder(binding.root)
    class ReceivedViewHolder(val binding: ItemMessageReceivedBinding) : RecyclerView.ViewHolder(binding.root)
    class DateViewHolder(val binding: ItemDateHeaderBinding) : RecyclerView.ViewHolder(binding.root)
}