package com.myrent.app.data.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Repository untuk mengelola otentikasi Firebase dan data user Firestore
 */
class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    val currentUser: FirebaseUser?
        get() = auth.currentUser

    /**
     * Membaca dokumen profil pengguna dari Firestore (users/{uid})
     */
    suspend fun getUserProfile(): Result<UserDoc> = runCatching {
        val uid = currentUser?.uid ?: throw Exception("Belum login")
        val doc = firestore.collection("users").document(uid).get().await()
        if (!doc.exists()) throw Exception("Dokumen user tidak ditemukan")

        UserDoc(
            uid = doc.id,
            nama = doc.getString("nama") ?: "",
            email = doc.getString("email") ?: "",
            cabangId = doc.getString("cabangId") ?: "",
            dibuatPada = doc.getLong("dibuatPada") ?: 0L
        )
    }

    /**
     * Login dengan email dan kata sandi
     * Dilengkapi verifikasi keberadaan dokumen user di Firestore users/{uid}
     */
    suspend fun login(email: String, password: String): Result<FirebaseUser> = runCatching {
        val result = auth.signInWithEmailAndPassword(email, password).await()
        val user = result.user ?: throw Exception("User tidak ditemukan")

        // Periksa apakah dokumen profil pengguna di Firestore users/{uid} masih ada
        val userDoc = firestore.collection("users").document(user.uid).get().await()
        if (!userDoc.exists()) {
            auth.signOut()
            throw Exception("Data profil akun tidak ditemukan di database.")
        }

        user
    }

    /**
     * Pendaftaran akun baru:
     * 1. Buat akun auth
     * 2. Kirim email verifikasi
     * 3. Simpan data user ke Firestore users/{uid}
     */
    suspend fun register(
        nama: String,
        email: String,
        password: String,
        cabangId: String
    ): Result<FirebaseUser> = runCatching {
        val authResult = auth.createUserWithEmailAndPassword(email, password).await()
        val user = authResult.user ?: throw Exception("Gagal membuat akun")

        // Kirim email verifikasi
        user.sendEmailVerification().await()

        // Simpan ke Firestore users/{uid}
        val userDoc = UserDoc(
            uid = user.uid,
            nama = nama,
            email = email,
            cabangId = cabangId,
            dibuatPada = System.currentTimeMillis()
        )
        firestore.collection("users").document(user.uid).set(userDoc).await()

        user
    }

    /**
     * Kirim ulang email verifikasi
     */
    suspend fun resendEmailVerification(): Result<Unit> = runCatching {
        val user = currentUser ?: throw Exception("Tidak ada pengguna aktif")
        user.sendEmailVerification().await()
    }

    /**
     * Kirim tautan reset kata sandi ke email
     */
    suspend fun sendPasswordResetEmail(email: String): Result<Unit> = runCatching {
        auth.sendPasswordResetEmail(email).await()
    }

    /**
     * Reload data user dari server untuk memperbarui status emailVerified
     */
    suspend fun reloadUser(): Result<FirebaseUser> = runCatching {
        val user = currentUser ?: throw Exception("Tidak ada pengguna aktif")
        user.reload().await()
        auth.currentUser ?: user
    }

    /**
     * Keluar sesi (Logout)
     */
    fun logout() {
        auth.signOut()
    }
}
