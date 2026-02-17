package com.example.chatapp

import android.util.Base64
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.chatapp.databinding.ItemSelectFriendBinding

class SelectFriendAdapter(
    private val friendList: List<User>,
    private val onSelectionChanged: (User, Boolean) -> Unit
) : RecyclerView.Adapter<SelectFriendAdapter.SelectViewHolder>() {

    private val selectedUsers = mutableSetOf<String>()

    class SelectViewHolder(val binding: ItemSelectFriendBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SelectViewHolder {
        val binding = ItemSelectFriendBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SelectViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SelectViewHolder, position: Int) {
        val user = friendList[position]
        holder.binding.tvUserName.text = user.fullName
        
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

        holder.binding.cbSelect.isChecked = selectedUsers.contains(user.uid)

        holder.itemView.setOnClickListener {
            val isChecked = !holder.binding.cbSelect.isChecked
            holder.binding.cbSelect.isChecked = isChecked
            if (isChecked) selectedUsers.add(user.uid) else selectedUsers.remove(user.uid)
            onSelectionChanged(user, isChecked)
        }
    }

    override fun getItemCount(): Int = friendList.size
}