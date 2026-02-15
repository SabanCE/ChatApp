package com.example.chatapp

import android.content.Intent
import android.os.Bundle
import android.util.Base64
import android.util.Log
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
import com.example.chatapp.databinding.ActivityChatBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var adapter: MessageAdapter
    private val messageList = mutableListOf<Message>()

    private var receiverUid: String? = null
    private var senderUid: String? = null
    private var chatRoom: String? = null
    private var unmatchListener: ValueEventListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance("https://chatapp-df32e-default-rtdb.firebaseio.com").reference

        receiverUid = intent.getStringExtra("userId")
        senderUid = auth.currentUser?.uid
        val userName = intent.getStringExtra("userName")
        val userImage = intent.getStringExtra("userImage")

        if (senderUid.isNullOrEmpty() || receiverUid.isNullOrEmpty()) {
            Toast.makeText(this, "Hata: Kullanıcı bilgisi eksik", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        chatRoom = if (senderUid!! < receiverUid!!) {
            senderUid + receiverUid
        } else {
            receiverUid + senderUid
        }

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        binding.tvChatUserName.text = userName
        binding.toolbar.setNavigationOnClickListener { finish() }

        if (!userImage.isNullOrEmpty()) {
            loadBase64Image(userImage)
        } else {
            loadReceiverProfileImage()
        }

        setupRecyclerView()
        loadMessages()
        checkUnmatchStatus() // Eşleşme durumunu canlı izle

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

    private fun loadBase64Image(base64: String) {
        try {
            val imageBytes = Base64.decode(base64, Base64.DEFAULT)
            Glide.with(this).load(imageBytes).placeholder(R.drawable.ic_user_placeholder).into(binding.ivChatUser)
        } catch (e: Exception) {
            binding.ivChatUser.setImageResource(R.drawable.ic_user_placeholder)
        }
    }

    private fun loadReceiverProfileImage() {
        database.child("Users").child(receiverUid!!).child("profileImageUrl")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val base64 = snapshot.value as? String
                    if (!base64.isNullOrEmpty()) loadBase64Image(base64)
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun checkUnmatchStatus() {
        unmatchListener = database.child("Users").child(senderUid!!).child("friends").child(receiverUid!!)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) {
                        // Eğer arkadaşlık düğümü silinmişse (karşı taraf unmatch yaptıysa)
                        Toast.makeText(this@ChatActivity, "Sohbet sonlandırıldı", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun setupRecyclerView() {
        adapter = MessageAdapter(messageList)
        val layoutManager = LinearLayoutManager(this)
        layoutManager.stackFromEnd = true
        binding.rvMessages.layoutManager = layoutManager
        binding.rvMessages.adapter = adapter
    }

    private fun loadMessages() {
        database.child("Chats").child(chatRoom!!).child("messages")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    messageList.clear()
                    for (postSnapshot in snapshot.children) {
                        val message = postSnapshot.getValue(Message::class.java)
                        if (message != null) messageList.add(message)
                    }
                    adapter.notifyDataSetChanged()
                    if (messageList.isNotEmpty()) binding.rvMessages.scrollToPosition(messageList.size - 1)
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun sendMessage(text: String) {
        val messageObject = Message(text, senderUid!!, System.currentTimeMillis())
        database.child("Chats").child(chatRoom!!).child("messages").push()
            .setValue(messageObject)
            .addOnSuccessListener {
                binding.etMessage.text.clear()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Mesaj gönderilemedi", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.chat_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_unmatch) {
            showUnmatchDialog()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showUnmatchDialog() {
        AlertDialog.Builder(this)
            .setTitle("Eşleşmeyi Kaldır")
            .setMessage("Bu kişiyle olan sohbetiniz silinecektir.")
            .setPositiveButton("Evet") { _, _ -> unmatchUser() }
            .setNegativeButton("Hayır", null)
            .show()
    }

    private fun unmatchUser() {
        val updates = hashMapOf<String, Any?>(
            "/Users/$senderUid/friends/$receiverUid" to null,
            "/Users/$receiverUid/friends/$senderUid" to null,
            "/Chats/$chatRoom" to null
        )
        database.updateChildren(updates).addOnSuccessListener { finish() }
    }

    override fun onDestroy() {
        super.onDestroy()
        unmatchListener?.let {
            database.child("Users").child(senderUid!!).child("friends").child(receiverUid!!).removeEventListener(it)
        }
    }
}