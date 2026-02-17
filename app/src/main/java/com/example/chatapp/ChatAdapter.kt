package com.example.chatapp

import android.content.Intent
import android.graphics.Color
import android.util.Base64
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.chatapp.databinding.ItemChatBinding
import com.example.chatapp.databinding.ItemGroupBinding

class ChatAdapter(private var itemList: MutableList<Any>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val TYPE_USER = 1
    private val TYPE_GROUP = 2

    class ChatViewHolder(val binding: ItemChatBinding) : RecyclerView.ViewHolder(binding.root)
    class GroupViewHolder(val binding: ItemGroupBinding) : RecyclerView.ViewHolder(binding.root)

    override fun getItemViewType(position: Int): Int {
        return if (itemList[position] is User) TYPE_USER else TYPE_GROUP
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_USER) {
            val binding = ItemChatBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            ChatViewHolder(binding)
        } else {
            val binding = ItemGroupBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            GroupViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = itemList[position]

        if (holder is ChatViewHolder && item is User) {
            holder.binding.tvUserName.text = item.fullName
            holder.binding.tvLastMessage.text = "Sohbeti başlatmak için tıkla"

            try {
                holder.binding.root.setCardBackgroundColor(Color.parseColor(item.chatColor))
            } catch (e: Exception) {
                holder.binding.root.setCardBackgroundColor(Color.WHITE)
            }

            if (item.profileImageUrl.isNotEmpty()) {
                try {
                    val imageBytes = Base64.decode(item.profileImageUrl, Base64.DEFAULT)
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
                intent.putExtra("userId", item.uid)
                intent.putExtra("userName", item.fullName)
                intent.putExtra("userImage", item.profileImageUrl)
                holder.itemView.context.startActivity(intent)
            }
        } else if (holder is GroupViewHolder && item is Group) {
            holder.binding.tvGroupName.text = item.groupName
            holder.binding.tvLastMessage.text = "Grup sohbeti için tıkla"

            // Grup için seçilen rengi uygula
            try {
                holder.binding.root.setCardBackgroundColor(Color.parseColor(item.chatColor))
            } catch (e: Exception) {
                holder.binding.root.setCardBackgroundColor(Color.WHITE)
            }

            if (item.groupImageUrl.isNotEmpty()) {
                try {
                    val imageBytes = Base64.decode(item.groupImageUrl, Base64.DEFAULT)
                    Glide.with(holder.itemView.context)
                        .load(imageBytes)
                        .placeholder(R.drawable.ic_group)
                        .into(holder.binding.ivGroup)
                } catch (e: Exception) {
                    holder.binding.ivGroup.setImageResource(R.drawable.ic_group)
                }
            } else {
                holder.binding.ivGroup.setImageResource(R.drawable.ic_group)
            }

            holder.itemView.setOnClickListener {
                val intent = Intent(holder.itemView.context, GroupChatActivity::class.java)
                intent.putExtra("groupId", item.groupId)
                intent.putExtra("groupName", item.groupName)
                holder.itemView.context.startActivity(intent)
            }
        }
    }

    override fun getItemCount(): Int = itemList.size

    fun updateList(newList: List<Any>) {
        itemList.clear()
        itemList.addAll(newList)
        notifyDataSetChanged()
    }
}