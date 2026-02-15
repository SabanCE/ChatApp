package com.example.chatapp

import android.content.Intent
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.chatapp.databinding.ActivityDashboardBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var adapter: ChatAdapter
    private val matchedUsers = mutableListOf<User>()
    private var isIdVisible = false
    private var myShortId: String = ""
    
    // Arkadaşların verilerini dinleyen listener'ları takip etmek için
    private val friendListeners = mutableMapOf<String, ValueEventListener>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance("https://chatapp-df32e-default-rtdb.firebaseio.com").reference

        auth.uid?.let { uid ->
            myShortId = uid.take(5).uppercase()
        }

        setupRecyclerView()
        setupListeners()
        updateIdUI()
        loadMyProfileImage()
        loadMatchedUsers()

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun setupRecyclerView() {
        adapter = ChatAdapter(matchedUsers)
        binding.rvChats.layoutManager = LinearLayoutManager(this)
        binding.rvChats.adapter = adapter
    }

    private fun setupListeners() {
        binding.btnLogout.setOnClickListener { showLogoutDialog() }
        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        binding.btnToggleId.setOnClickListener {
            isIdVisible = !isIdVisible
            updateIdUI()
        }
        binding.fabAddFriend.setOnClickListener { showAddFriendDialog() }
    }

    private fun loadMyProfileImage() {
        val uid = auth.uid ?: return
        database.child("Users").child(uid).child("profileImageUrl")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val base64 = snapshot.value as? String
                    if (!base64.isNullOrEmpty()) {
                        try {
                            val imageBytes = Base64.decode(base64, Base64.DEFAULT)
                            Glide.with(this@DashboardActivity)
                                .load(imageBytes)
                                .placeholder(R.drawable.ic_user_placeholder)
                                .into(binding.ivMyProfile)
                        } catch (e: Exception) {
                            binding.ivMyProfile.setImageResource(R.drawable.ic_user_placeholder)
                        }
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun loadMatchedUsers() {
        val currentUid = auth.uid ?: return
        database.child("Users").child(currentUid).child("friends").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val friendIds = snapshot.children.mapNotNull { it.key }
                
                // Artık listede olmayan arkadaşların listener'larını temizle ve listeden çıkar
                val listenerIterator = friendListeners.entries.iterator()
                while (listenerIterator.hasNext()) {
                    val entry = listenerIterator.next()
                    if (!friendIds.contains(entry.key)) {
                        database.child("Users").child(entry.key).removeEventListener(entry.value)
                        listenerIterator.remove()
                    }
                }
                
                // Eşleşen kullanıcılar listesini temizleyip yeni friendIds'e göre güncelleme yapmak yerine, 
                // sadece listeden çıkanları çıkaralım.
                matchedUsers.removeAll { !friendIds.contains(it.uid) }

                if (friendIds.isEmpty()) {
                    binding.emptyState.visibility = View.VISIBLE
                    adapter.notifyDataSetChanged()
                    return
                }
                binding.emptyState.visibility = View.GONE

                // Her arkadaş için canlı dinleyici ekle
                friendIds.forEach { id ->
                    if (!friendListeners.containsKey(id)) {
                        val listener = object : ValueEventListener {
                            override fun onDataChange(userSnapshot: DataSnapshot) {
                                val user = userSnapshot.getValue(User::class.java)
                                if (user != null) {
                                    val index = matchedUsers.indexOfFirst { it.uid == user.uid }
                                    if (index != -1) {
                                        matchedUsers[index] = user
                                    } else {
                                        matchedUsers.add(user)
                                    }
                                    adapter.notifyDataSetChanged()
                                }
                            }
                            override fun onCancelled(error: DatabaseError) {}
                        }
                        database.child("Users").child(id).addValueEventListener(listener)
                        friendListeners[id] = listener
                    }
                }
                adapter.notifyDataSetChanged()
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun updateIdUI() {
        binding.tvMyId.text = if (isIdVisible) getString(R.string.my_id, myShortId) else getString(R.string.my_id, "*****")
        binding.btnToggleId.setImageResource(if (isIdVisible) R.drawable.ic_visibility else R.drawable.ic_visibility_off)
    }

    private fun showAddFriendDialog() {
        val editText = EditText(this)
        editText.hint = "5 haneli ID'yi girin"
        AlertDialog.Builder(this)
            .setTitle("Arkadaş Ekle")
            .setView(editText)
            .setPositiveButton("Ekle") { _, _ ->
                val shortId = editText.text.toString().trim().uppercase()
                if (shortId.length == 5) addFriendByShortId(shortId)
                else Toast.makeText(this, "ID 5 haneli olmalıdır", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    private fun addFriendByShortId(shortId: String) {
        val currentUid = auth.uid ?: return
        database.child("ShortIds").child(shortId).get().addOnSuccessListener { snapshot ->
            val friendUid = snapshot.value as? String
            if (friendUid == null) {
                Toast.makeText(this, "Kullanıcı bulunamadı", Toast.LENGTH_SHORT).show()
                return@addOnSuccessListener
            }
            if (friendUid == currentUid) {
                Toast.makeText(this, "Kendinizi ekleyemezsiniz", Toast.LENGTH_SHORT).show()
                return@addOnSuccessListener
            }

            val updates = hashMapOf<String, Any>(
                "/Users/$currentUid/friends/$friendUid" to true,
                "/Users/$friendUid/friends/$currentUid" to true
            )
            database.updateChildren(updates)
        }
    }

    private fun showLogoutDialog() {
        AlertDialog.Builder(this)
            .setTitle("Çıkış Yap")
            .setMessage("Emin misiniz?")
            .setPositiveButton("Evet") { _, _ ->
                auth.signOut()
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
            .setNegativeButton("Hayır", null)
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Bellek sızıntısını önlemek için tüm dinleyicileri kaldır
        friendListeners.forEach { (id, listener) ->
            database.child("Users").child(id).removeEventListener(listener)
        }
    }
}