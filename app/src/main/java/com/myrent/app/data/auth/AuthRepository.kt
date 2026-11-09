package com.myrent.app.data.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
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
     * Sumber data profil pengguna bersama (Shared Realtime Flow) dari Firestore ("users/{uid}")
     */
    fun getUserProfileFlow(): Flow<UserDoc> = callbackFlow {
        val user = currentUser
        if (user == null) {
            trySend(UserDoc())
            awaitClose { }
            return@callbackFlow
        }

        val docRef = firestore.collection("users").document(user.uid)
        val listener = docRef.addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null || !snapshot.exists()) {
                trySend(UserDoc(uid = user.uid, email = user.email ?: ""))
                return@addSnapshotListener
            }

            val email = snapshot.getString("email") ?: user.email ?: ""
            var namaToko = snapshot.getString("namaToko") ?: ""
            val migrasiFlag = snapshot.getBoolean("migrasiNamaToko") ?: false

            // Migrasi 1x Aman untuk kusumarefina@gmail.com jika namaToko kosong
            if (email.equals("kusumarefina@gmail.com", ignoreCase = true) && namaToko.isEmpty() && !migrasiFlag) {
                namaToko = "Kusuma Rent"
                docRef.update(
                    mapOf(
                        "namaToko" to "Kusuma Rent",
                        "migrasiNamaToko" to true
                    )
                )
            }

            val userDoc = UserDoc(
                uid = snapshot.id,
                nama = snapshot.getString("nama") ?: "",
                namaToko = namaToko,
                noWaToko = snapshot.getString("noWaToko") ?: "",
                alamatToko = snapshot.getString("alamatToko") ?: "",
                fotoProfilBase64 = snapshot.getString("fotoProfilBase64") ?: "",
                email = email,
                migrasiNamaToko = migrasiFlag,
                dibuatPada = snapshot.getLong("dibuatPada") ?: 0L
            )

            trySend(userDoc)
        }

        awaitClose { listener.remove() }
    }

    /**
     * Membaca dokumen profil pengguna sekali
     */
    suspend fun getUserProfile(): Result<UserDoc> = runCatching {
        val uid = currentUser?.uid ?: throw Exception("Belum login")
        val docRef = firestore.collection("users").document(uid)
        val snapshot = docRef.get().await()

        if (!snapshot.exists()) throw Exception("Dokumen user tidak ditemukan")

        val email = snapshot.getString("email") ?: currentUser?.email ?: ""
        var namaToko = snapshot.getString("namaToko") ?: ""
        val migrasiFlag = snapshot.getBoolean("migrasiNamaToko") ?: false

        if (email.equals("kusumarefina@gmail.com", ignoreCase = true) && namaToko.isEmpty() && !migrasiFlag) {
            namaToko = "Kusuma Rent"
            docRef.update(
                mapOf(
                    "namaToko" to "Kusuma Rent",
                    "migrasiNamaToko" to true
                )
            ).await()
        }

        UserDoc(
            uid = snapshot.id,
            nama = snapshot.getString("nama") ?: "",
            namaToko = namaToko,
            noWaToko = snapshot.getString("noWaToko") ?: "",
            alamatToko = snapshot.getString("alamatToko") ?: "",
            fotoProfilBase64 = snapshot.getString("fotoProfilBase64") ?: "",
            email = email,
            migrasiNamaToko = migrasiFlag,
            dibuatPada = snapshot.getLong("dibuatPada") ?: 0L
        )
    }

    suspend fun login(email: String, password: String): Result<FirebaseUser> = runCatching {
        val result = auth.signInWithEmailAndPassword(email, password).await()
        val user = result.user ?: throw Exception("User tidak ditemukan")

        val userDoc = firestore.collection("users").document(user.uid).get().await()
        if (!userDoc.exists()) {
            auth.signOut()
            throw Exception("Data profil akun tidak ditemukan di database.")
        }

        user
    }

    suspend fun register(
        nama: String,
        namaToko: String,
        email: String,
        password: String
    ): Result<FirebaseUser> = runCatching {
        val authResult = auth.createUserWithEmailAndPassword(email, password).await()
        val user = authResult.user ?: throw Exception("Gagal membuat akun")

        user.sendEmailVerification().await()

        val userDoc = UserDoc(
            uid = user.uid,
            nama = nama.trim(),
            namaToko = namaToko.trim(),
            email = email.trim(),
            dibuatPada = System.currentTimeMillis()
        )
        firestore.collection("users").document(user.uid).set(userDoc).await()

        user
    }

    suspend fun resendEmailVerification(): Result<Unit> = runCatching {
        val user = currentUser ?: throw Exception("Tidak ada pengguna aktif")
        user.sendEmailVerification().await()
    }

    suspend fun sendPasswordResetEmail(email: String): Result<Unit> = runCatching {
        auth.sendPasswordResetEmail(email).await()
    }

    suspend fun reloadUser(): Result<FirebaseUser> = runCatching {
        val user = currentUser ?: throw Exception("Tidak ada pengguna aktif")
        user.reload().await()
        auth.currentUser ?: user
    }

    fun logout() {
        auth.signOut()
    }
}
