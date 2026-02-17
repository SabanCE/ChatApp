package com.example.chatapp

import android.content.Intent
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.EditText
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.chatapp.databinding.ActivityDashboardBinding
import com.example.chatapp.databinding.DialogAddFriendBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var adapter: ChatAdapter
    private val dashboardItems = mutableListOf<Any>()
    private var isIdVisible = false
    private var myShortId: String = ""
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
        loadDashboardData()

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun setupRecyclerView() {
        adapter = ChatAdapter(dashboardItems)
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
        binding.fabAddFriend.setOnClickListener { view -> showFabMenu(view) }
    }

    private fun showFabMenu(view: View) {
        val popup = PopupMenu(this, view)
        popup.menu.add("Arkadaş Ekle")
        popup.menu.add("Grup Oluştur")
        popup.setOnMenuItemClickListener { item ->
            when (item.title) {
                "Arkadaş Ekle" -> showAddFriendDialogModern()
                "Grup Oluştur" -> startActivity(Intent(this, CreateGroupActivity::class.java))
            }
            true
        }
        popup.show()
    }

    private fun showAddFriendDialogModern() {
        val dialog = BottomSheetDialog(this)
        val dialogBinding = DialogAddFriendBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)

        dialogBinding.btnAddFriend.setOnClickListener {
            val shortId = dialogBinding.etFriendId.text.toString().trim().uppercase()
            if (shortId.length == 5) {
                addFriendByShortId(shortId)
                dialog.dismiss()
            } else {
                Toast.makeText(this, "ID 5 haneli olmalıdır", Toast.LENGTH_SHORT).show()
            }
        }
        dialog.show()
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
                            Glide.with(this@DashboardActivity).load(imageBytes).placeholder(R.drawable.ic_user_placeholder).into(binding.ivMyProfile)
                        } catch (e: Exception) {
                            binding.ivMyProfile.setImageResource(R.drawable.ic_user_placeholder)
                        }
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun loadDashboardData() {
        val currentUid = auth.uid ?: return
        database.child("Users").child(currentUid).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val friendIds = snapshot.child("friends").children.mapNotNull { it.key }
                val groupIds = snapshot.child("groups").children.mapNotNull { it.key }

                dashboardItems.clear()
                
                groupIds.forEach { id ->
                    database.child("Groups").child(id).addValueEventListener(object : ValueEventListener {
                        override fun onDataChange(groupSnapshot: DataSnapshot) {
                            val group = groupSnapshot.getValue(Group::class.java)
                            if (group != null) {
                                dashboardItems.removeAll { (it as? Group)?.groupId == group.groupId }
                                dashboardItems.add(0, group)
                                adapter.notifyDataSetChanged()
                                checkEmptyState()
                            }
                        }
                        override fun onCancelled(error: DatabaseError) {}
                    })
                }

                friendIds.forEach { id ->
                    database.child("Users").child(id).addValueEventListener(object : ValueEventListener {
                        override fun onDataChange(userSnapshot: DataSnapshot) {
                            val user = userSnapshot.getValue(User::class.java)
                            if (user != null) {
                                dashboardItems.removeAll { (it as? User)?.uid == user.uid }
                                dashboardItems.add(user)
                                adapter.notifyDataSetChanged()
                                checkEmptyState()
                            }
                        }
                        override fun onCancelled(error: DatabaseError) {}
                    })
                }
                checkEmptyState()
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun checkEmptyState() {
        binding.emptyState.visibility = if (dashboardItems.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun updateIdUI() {
        binding.tvMyId.text = if (isIdVisible) getString(R.string.my_id, myShortId) else getString(R.string.my_id, "*****")
        binding.btnToggleId.setImageResource(if (isIdVisible) R.drawable.ic_visibility else R.drawable.ic_visibility_off)
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
            database.updateChildren(updates).addOnSuccessListener {
                Toast.makeText(this, "Arkadaş eklendi", Toast.LENGTH_SHORT).show()
            }
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
}