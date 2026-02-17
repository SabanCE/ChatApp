package com.example.chatapp

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.chatapp.databinding.ActivityCreateGroupBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.io.ByteArrayOutputStream
import java.io.InputStream

class CreateGroupActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreateGroupBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private val friendsList = mutableListOf<User>()
    private val selectedUserIds = mutableSetOf<String>()
    private lateinit var adapter: SelectFriendAdapter
    
    private var base64GroupImage: String = ""
    private var selectedChatColor: String = "#FFFFFF"

    private val selectImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val encoded = encodeImageToBase64(it)
            if (encoded != null) {
                base64GroupImage = encoded
                Glide.with(this).load(it).into(binding.ivGroupImage)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityCreateGroupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance("https://chatapp-df32e-default-rtdb.firebaseio.com").reference

        setupRecyclerView()
        loadFriends()
        setupUI()

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun setupUI() {
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.fabEditGroupImage.setOnClickListener { selectImage.launch("image/*") }
        binding.btnCreateGroup.setOnClickListener { createGroup() }
        binding.btnAddMemberById.setOnClickListener { addMemberById() }

        // Renk Seçimi
        binding.colorWhite.setOnClickListener { updateColorSelection("#FFFFFF", binding.containerWhite) }
        binding.colorBlue.setOnClickListener { updateColorSelection("#BBDEFB", binding.containerBlue) }
        binding.colorGreen.setOnClickListener { updateColorSelection("#C8E6C9", binding.containerGreen) }
        binding.colorPink.setOnClickListener { updateColorSelection("#F8BBD0", binding.containerPink) }
        binding.colorYellow.setOnClickListener { updateColorSelection("#FFF9C4", binding.containerYellow) }
        
        updateColorSelection("#FFFFFF", binding.containerWhite) // Varsayılan
    }

    private fun updateColorSelection(color: String, container: FrameLayout) {
        selectedChatColor = color
        binding.containerWhite.setBackgroundResource(0)
        binding.containerBlue.setBackgroundResource(0)
        binding.containerGreen.setBackgroundResource(0)
        binding.containerPink.setBackgroundResource(0)
        binding.containerYellow.setBackgroundResource(0)
        container.setBackgroundResource(R.drawable.bg_color_selector)
    }

    private fun encodeImageToBase64(uri: Uri): String? {
        return try {
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 200, 200, true)
            val outputStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
            Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)
        } catch (e: Exception) { null }
    }

    private fun setupRecyclerView() {
        adapter = SelectFriendAdapter(friendsList) { user, isSelected ->
            if (isSelected) selectedUserIds.add(user.uid) else selectedUserIds.remove(user.uid)
        }
        binding.rvFriendsToSelect.layoutManager = LinearLayoutManager(this)
        binding.rvFriendsToSelect.adapter = adapter
    }

    private fun loadFriends() {
        val currentUid = auth.uid ?: return
        database.child("Users").child(currentUid).child("friends").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                friendsList.clear()
                val ids = snapshot.children.mapNotNull { it.key }
                ids.forEach { id ->
                    database.child("Users").child(id).addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(s: DataSnapshot) {
                            val user = s.getValue(User::class.java)
                            if (user != null && !friendsList.any { it.uid == user.uid }) {
                                friendsList.add(user)
                                adapter.notifyDataSetChanged()
                            }
                        }
                        override fun onCancelled(error: DatabaseError) {}
                    })
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun addMemberById() {
        val shortId = binding.etMemberId.text.toString().trim().uppercase()
        if (shortId.length != 5) return
        database.child("ShortIds").child(shortId).get().addOnSuccessListener { snapshot ->
            val uid = snapshot.value as? String ?: return@addOnSuccessListener
            database.child("Users").child(uid).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(s: DataSnapshot) {
                    val user = s.getValue(User::class.java)
                    if (user != null && !friendsList.any { it.uid == user.uid }) {
                        friendsList.add(0, user)
                        selectedUserIds.add(user.uid)
                        adapter.notifyDataSetChanged()
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
        }
    }

    private fun createGroup() {
        val name = binding.etGroupName.text.toString().trim()
        if (name.isEmpty() || selectedUserIds.isEmpty()) return
        val currentUid = auth.uid ?: return
        val gid = database.child("Groups").push().key ?: return
        val members = mutableMapOf<String, Boolean>()
        members[currentUid] = true
        selectedUserIds.forEach { members[it] = true }

        val group = Group(gid, name, members, currentUid, base64GroupImage, selectedChatColor)
        val updates = hashMapOf<String, Any>()
        updates["/Groups/$gid"] = group
        members.keys.forEach { updates["/Users/$it/groups/$gid"] = true }

        database.updateChildren(updates).addOnSuccessListener { finish() }
    }
}