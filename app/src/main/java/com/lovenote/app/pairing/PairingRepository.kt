package com.lovenote.app.pairing

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

/** Where the signed-in user stands in the pairing flow. */
data class CoupleStatus(
    val coupleId: String?,
    val partnerJoined: Boolean,
    val inviteCode: String?,
)

class PairingRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    private val uid: String
        get() = auth.currentUser?.uid ?: error("Not signed in")

    /** Creates a new couple with a fresh invite code; returns the code. */
    suspend fun createCouple(): String {
        repeat(5) {
            val code = InviteCode.generate()
            val taken = db.collection("couples")
                .whereEqualTo("inviteCode", code)
                .limit(1)
                .get()
                .await()
            if (taken.isEmpty) {
                val doc = db.collection("couples").document()
                doc.set(
                    mapOf(
                        "members" to listOf(uid),
                        "inviteCode" to code,
                        "createdAt" to FieldValue.serverTimestamp(),
                    ),
                ).await()
                db.collection("users").document(uid)
                    .set(mapOf("coupleId" to doc.id), SetOptions.merge())
                    .await()
                return code
            }
        }
        error("Couldn't generate a unique code — please try again")
    }

    /** Joins the couple that owns [rawCode]. Throws with a friendly message on failure. */
    suspend fun joinWithCode(rawCode: String) {
        val code = InviteCode.normalize(rawCode)
        require(InviteCode.isValid(code)) {
            "That code doesn't look right — it should be 6 letters or numbers."
        }
        val snap = db.collection("couples")
            .whereEqualTo("inviteCode", code)
            .limit(1)
            .get()
            .await()
        val doc = snap.documents.firstOrNull()
            ?: throw IllegalStateException("No couple found with that code. Double-check it with your partner.")
        val members = doc.get("members") as? List<*> ?: emptyList<Any>()
        if (uid in members) {
            throw IllegalStateException("That's your own code — share it with your partner instead.")
        }
        if (members.size >= 2) {
            throw IllegalStateException("That code has already been used.")
        }
        try {
            db.runTransaction { tx ->
                val fresh = tx.get(doc.reference)
                val current = fresh.get("members") as? List<*> ?: emptyList<Any>()
                check(current.size == 1) { "That code has already been used." }
                tx.update(doc.reference, "members", current + uid)
            }.await()
        } catch (e: FirebaseFirestoreException) {
            throw IllegalStateException("Couldn't join right now — check your connection and try again.", e)
        }
        db.collection("users").document(uid)
            .set(mapOf("coupleId" to doc.id), SetOptions.merge())
            .await()
    }

    /** Live pairing state: no couple yet → waiting for partner → paired. */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeStatus(): Flow<CoupleStatus> =
        db.collection("users").document(uid).snapshots()
            .map { it.getString("coupleId") }
            .distinctUntilChanged()
            .flatMapLatest { coupleId ->
                if (coupleId == null) {
                    flowOf(CoupleStatus(null, partnerJoined = false, inviteCode = null))
                } else {
                    db.collection("couples").document(coupleId).snapshots().map { couple ->
                        val members = couple.get("members") as? List<*> ?: emptyList<Any>()
                        CoupleStatus(
                            coupleId = coupleId,
                            partnerJoined = members.size >= 2,
                            inviteCode = couple.getString("inviteCode"),
                        )
                    }
                }
            }
}
