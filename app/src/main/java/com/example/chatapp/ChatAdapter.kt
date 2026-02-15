package com.example.chatapp

import android.content.Intent
import android.graphics.Color
import android.util.Base64
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.chatapp.databinding.ItemChatBinding

class ChatAdapter(private var userList: List<User>) : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    class ChatViewHolder(val binding: ItemChatBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val binding = ItemChatBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ChatViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val user = userList[position]
        holder.binding.tvUserName.text = user.fullName
        holder.binding.tvLastMessage.text = "Sohbeti başlatmak için tıkla"

        // Kullanıcının seçtiği arka plan rengini uygula
        try {
            holder.binding.root.setCardBackgroundColor(Color.parseColor(user.chatColor))
        } catch (e: Exception) {
            holder.binding.root.setCardBackgroundColor(Color.WHITE)
        }

        if (user.profileImageUrl.isNotEmpty()) {
            try {
                val imageBytes = Base64.decode(user.profileImageUrl, Base64.DEFAULT)
                Glide.with(holder.itemView.context)
                    .load(imageBytes)
                    .placeholder(R.drawable.ic_user_placeholder)
                    .into(holder.binding.ivUser)
            } catch (e: Exception) {
                holder.binding.ivUser.setImageResource(R.drawable.ic_user_placeholder)
            }
        } else {
            holder.binding.ivUser.setImageResource(R.drawable.ic_user_placeholder)
        }

        holder.itemView.setOnClickListener {
            val intent = Intent(holder.itemView.context, ChatActivity::class.java)
            intent.putExtra("userId", user.uid)
            intent.putExtra("userName", user.fullName)
            intent.putExtra("userImage", user.profileImageUrl)
            holder.itemView.context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = userList.size

    fun updateList(newList: List<User>) {
        userList = newList
        notifyDataSetChanged()
    }
}