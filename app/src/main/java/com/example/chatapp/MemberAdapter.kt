package com.example.chatapp

import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.chatapp.databinding.ItemGroupMemberBinding

class MemberAdapter(
    private val memberList: List<User>,
    private val adminId: String,
    private val currentUserId: String,
    private val onRemoveClick: (User) -> Unit
) : RecyclerView.Adapter<MemberAdapter.MemberViewHolder>() {

    class MemberViewHolder(val binding: ItemGroupMemberBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemberViewHolder {
        val binding = ItemGroupMemberBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MemberViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MemberViewHolder, position: Int) {
        val user = memberList[position]
        holder.binding.tvUserName.text = user.fullName

        // Admin Rozeti
        if (user.uid == adminId) {
            holder.binding.tvAdminBadge.visibility = View.VISIBLE
        } else {
            holder.binding.tvAdminBadge.visibility = View.GONE
        }

        // Üye Çıkarma Butonu (Sadece Admin görebilir ve kendisini çıkaramaz)
        if (currentUserId == adminId && user.uid != adminId) {
            holder.binding.btnRemoveMember.visibility = View.VISIBLE
            holder.binding.btnRemoveMember.setOnClickListener { onRemoveClick(user) }
        } else {
            holder.binding.btnRemoveMember.visibility = View.GONE
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
    }

    override fun getItemCount(): Int = memberList.size
}