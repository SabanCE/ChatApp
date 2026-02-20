package com.example.chatapp

import android.os.Bundle
import android.util.Base64
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.chatapp.databinding.ActivityGroupChatBinding
import com.example.chatapp.databinding.DialogConfirmationBinding
import com.example.chatapp.databinding.DialogGroupMembersBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class GroupChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGroupChatBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var adapter: MessageAdapter
    private val messageList = mutableListOf<Message>()
    private var groupId: String? = null
    private var groupAdminId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityGroupChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance("https://chatapp-df32e-default-rtdb.firebaseio.com").reference

        groupId = intent.getStringExtra("groupId")
        val groupName = intent.getStringExtra("groupName")

        if (groupId.isNullOrEmpty()) {
            Toast.makeText(this, "Grup bilgisi alınamadı", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.tvGroupName.text = groupName

        binding.tvGroupName.setOnClickListener { showGroupMembersModern() }
        binding.ivGroupIcon.setOnClickListener { showGroupMembersModern() }

        loadGroupData()
        setupRecyclerView()
        loadMessages()

        binding.btnSend.setOnClickListener {
            val messageText = binding.etMessage.text.toString().trim()
            if (messageText.isNotEmpty()) {
                sendMessage(messageText)
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun loadGroupData() {
        database.child("Groups").child(groupId!!).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    Toast.makeText(this@GroupChatActivity, "Grup silindi", Toast.LENGTH_SHORT).show()
                    finish()
                    return
                }
                val group = snapshot.getValue(Group::class.java) ?: return
                groupAdminId = group.adminId
                binding.tvGroupName.text = group.groupName
                if (group.groupImageUrl.isNotEmpty()) {
                    try {
                        val bytes = Base64.decode(group.groupImageUrl, Base64.DEFAULT)
                        Glide.with(this@GroupChatActivity).load(bytes).into(binding.ivGroupIcon)
                    } catch (e: Exception) {}
                }
                invalidateOptionsMenu()
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun showModernConfirmationDialog(title: String, message: String, onConfirm: () -> Unit) {
        val dialogBinding = DialogConfirmationBinding.inflate(layoutInflater)
        val builder = AlertDialog.Builder(this)
        builder.setView(dialogBinding.root)
        val dialog = builder.create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialogBinding.tvDialogTitle.text = title
        dialogBinding.tvDialogMessage.text = message
        dialogBinding.btnCancel.setOnClickListener { dialog.dismiss() }
        dialogBinding.btnConfirm.setOnClickListener {
            onConfirm()
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun showGroupMembersModern() {
        val dialog = BottomSheetDialog(this)
        val dialogBinding = DialogGroupMembersBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)

        val members = mutableListOf<User>()
        val currentUid = auth.uid ?: ""
        
        val memberAdapter = MemberAdapter(members, groupAdminId ?: "", currentUid) { userToRemove ->
            showModernConfirmationDialog("Üyeyi Çıkar", "${userToRemove.fullName} kişisini gruptan çıkarmak istediğinize emin misiniz?") {
                val updates = hashMapOf<String, Any?>(
                    "/Groups/$groupId/members/${userToRemove.uid}" to null,
                    "/Users/${userToRemove.uid}/groups/$groupId" to null
                )
                database.updateChildren(updates).addOnSuccessListener {
                    Toast.makeText(this, "Üye çıkarıldı", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
            }
        }
        
        dialogBinding.rvMembersList.layoutManager = LinearLayoutManager(this)
        dialogBinding.rvMembersList.adapter = memberAdapter

        database.child("Groups").child(groupId!!).child("members").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val memberIds = snapshot.children.mapNotNull { it.key }
                memberIds.forEach { uid ->
                    database.child("Users").child(uid).addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(s: DataSnapshot) {
                            val user = s.getValue(User::class.java)
                            if (user != null) {
                                members.add(user)
                                memberAdapter.notifyDataSetChanged()
                            }
                        }
                        override fun onCancelled(error: DatabaseError) {}
                    })
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        dialogBinding.btnCloseDialog.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun setupRecyclerView() {
        adapter = MessageAdapter(messageList, isGroupChat = true)
        val layoutManager = LinearLayoutManager(this)
        layoutManager.stackFromEnd = true
        binding.rvMessages.layoutManager = layoutManager
        binding.rvMessages.adapter = adapter
    }

    private fun loadMessages() {
        database.child("GroupChats").child(groupId!!).child("messages")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    messageList.clear()
                    for (postSnapshot in snapshot.children) {
                        val message = postSnapshot.getValue(Message::class.java)
                        if (message != null) messageList.add(message)
                    }
                    adapter.updateData() // updateData() fonksiyonunu çağırdık
                    if (adapter.itemCount > 0) {
                        binding.rvMessages.scrollToPosition(adapter.itemCount - 1)
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun sendMessage(text: String) {
        val senderId = auth.uid ?: return
        val messageObject = Message(text, senderId, System.currentTimeMillis())
        database.child("GroupChats").child(groupId!!).child("messages").push().setValue(messageObject)
            .addOnSuccessListener { 
                binding.etMessage.text.clear() 
            }
            .addOnFailureListener {
                Toast.makeText(this, "Mesaj gönderilemedi: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.chat_menu, menu)
        val leaveItem = menu?.findItem(R.id.action_leave_group)
        val deleteItem = menu?.findItem(R.id.action_delete_group)
        val unmatchItem = menu?.findItem(R.id.action_unmatch)
        unmatchItem?.isVisible = false
        leaveItem?.isVisible = true
        deleteItem?.isVisible = (auth.uid == groupAdminId)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_leave_group -> {
                showModernConfirmationDialog("Gruptan Ayrıl", "Bu gruptan çıkmak istediğinize emin misiniz?") {
                    leaveGroup()
                }
                return true
            }
            R.id.action_delete_group -> {
                showModernConfirmationDialog("Grubu Sil", "Bu grubu tamamen silmek istediğinize emin misiniz? Bu işlem geri alınamaz.") {
                    deleteGroup()
                }
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun deleteGroup() {
        val gid = groupId ?: return
        database.child("Groups").child(gid).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val group = snapshot.getValue(Group::class.java) ?: return
                val updates = hashMapOf<String, Any?>()
                group.members.keys.forEach { memberId -> updates["/Users/$memberId/groups/$gid"] = null }
                updates["/Groups/$gid"] = null
                updates["/GroupChats/$gid"] = null
                database.updateChildren(updates).addOnSuccessListener {
                    Toast.makeText(this@GroupChatActivity, "Grup silindi", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun leaveGroup() {
        val uid = auth.uid ?: return
        val gid = groupId ?: return
        database.child("Groups").child(gid).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val group = snapshot.getValue(Group::class.java) ?: return
                val members = group.members.toMutableMap()
                members.remove(uid)
                val updates = hashMapOf<String, Any?>()
                updates["/Users/$uid/groups/$gid"] = null
                if (members.isEmpty()) {
                    updates["/Groups/$gid"] = null
                    updates["/GroupChats/$gid"] = null
                } else if (uid == groupAdminId) {
                    val newAdminId = members.keys.random()
                    updates["/Groups/$gid/adminId"] = newAdminId
                    updates["/Groups/$gid/members/$uid"] = null
                } else {
                    updates["/Groups/$gid/members/$uid"] = null
                }
                database.updateChildren(updates).addOnSuccessListener {
                    Toast.makeText(this@GroupChatActivity, "Gruptan çıkıldı", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }
}