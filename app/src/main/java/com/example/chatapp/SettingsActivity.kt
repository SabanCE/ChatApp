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
import com.bumptech.glide.Glide
import com.example.chatapp.databinding.ActivitySettingsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.io.ByteArrayOutputStream
import java.io.InputStream

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private var base64Image: String? = null
    private var selectedChatColor: String = "#FFFFFF"

    private val selectImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val encodedImage = encodeImageToBase64(it)
            if (encodedImage != null) {
                base64Image = encodedImage
                // Seçilen resmi anında önizlemede göster
                Glide.with(this).load(it).into(binding.ivProfileImage)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance("https://chatapp-df32e-default-rtdb.firebaseio.com").reference

        setupUI()
        loadUserData()

        // Toolbar kayma sorununu kökten çözmek için padding'i ana layout'a veriyoruz
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun setupUI() {
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.fabEditImage.setOnClickListener { selectImage.launch("image/*") }
        binding.btnSave.setOnClickListener { saveProfile() }

        binding.colorWhite.setOnClickListener { updateColorSelection("#FFFFFF", binding.containerWhite) }
        binding.colorBlue.setOnClickListener { updateColorSelection("#BBDEFB", binding.containerBlue) }
        binding.colorGreen.setOnClickListener { updateColorSelection("#C8E6C9", binding.containerGreen) }
        binding.colorPink.setOnClickListener { updateColorSelection("#F8BBD0", binding.containerPink) }
        binding.colorYellow.setOnClickListener { updateColorSelection("#FFF9C4", binding.containerYellow) }
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

    private fun loadUserData() {
        val uid = auth.uid ?: return
        database.child("Users").child(uid).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val user = snapshot.getValue(User::class.java)
                user?.let {
                    binding.etFullName.setText(it.fullName)
                    binding.tvShortId.text = "Short ID: ${it.shortId}"
                    
                    // Mevcut rengi işaretle
                    when(it.chatColor) {
                        "#FFFFFF" -> updateColorSelection("#FFFFFF", binding.containerWhite)
                        "#BBDEFB" -> updateColorSelection("#BBDEFB", binding.containerBlue)
                        "#C8E6C9" -> updateColorSelection("#C8E6C9", binding.containerGreen)
                        "#F8BBD0" -> updateColorSelection("#F8BBD0", binding.containerPink)
                        "#FFF9C4" -> updateColorSelection("#FFF9C4", binding.containerYellow)
                    }

                    if (it.profileImageUrl.isNotEmpty()) {
                        try {
                            val imageBytes = Base64.decode(it.profileImageUrl, Base64.DEFAULT)
                            Glide.with(this@SettingsActivity)
                                .asBitmap()
                                .load(imageBytes)
                                .placeholder(R.drawable.ic_user_placeholder)
                                .into(binding.ivProfileImage)
                        } catch (e: Exception) {
                            binding.ivProfileImage.setImageResource(R.drawable.ic_user_placeholder)
                        }
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun encodeImageToBase64(uri: Uri): String? {
        return try {
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 250, 250, true)
            val outputStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            val bytes = outputStream.toByteArray()
            Base64.encodeToString(bytes, Base64.DEFAULT)
        } catch (e: Exception) {
            null
        }
    }

    private fun saveProfile() {
        val newName = binding.etFullName.text.toString().trim()
        if (newName.isEmpty()) {
            Toast.makeText(this, "İsim boş olamaz", Toast.LENGTH_SHORT).show()
            return
        }

        val uid = auth.uid ?: return
        val updates = mutableMapOf<String, Any>(
            "fullName" to newName,
            "chatColor" to selectedChatColor
        )
        
        base64Image?.let {
            updates["profileImageUrl"] = it
        }

        database.child("Users").child(uid).updateChildren(updates).addOnSuccessListener {
            Toast.makeText(this, "Profil güncellendi", Toast.LENGTH_SHORT).show()
            finish()
        }.addOnFailureListener {
            Toast.makeText(this, "Hata: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }
}