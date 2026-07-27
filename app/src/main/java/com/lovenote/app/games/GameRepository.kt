package com.lovenote.app.games

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.snapshots
import com.lovenote.app.common.fallbackTo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

data class GameSession(
    val id: String = "",
    val gameType: String = "",
    val status: String = "waiting",
    val createdBy: String = "",
    val p1Uid: String = "",
    val p1Name: String = "",
    val p2Uid: String = "",
    val p2Name: String = "",
    val currentTurn: String = "",
    val board: Map<String, Any> = emptyMap(),
    val moves: List<Map<String, Any>> = emptyList(),
    val winner: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
) {
    fun isMyTurn(myUid: String): Boolean = currentTurn == myUid && status == "playing"

    fun isFinished(): Boolean = status == "finished" || status == "won"

    fun isWaiting(): Boolean = status == "waiting"

    companion object {
        fun fromDoc(doc: DocumentSnapshot): GameSession {
            val data = doc.data ?: return GameSession(id = doc.id)
            return GameSession(
                id = doc.id,
                gameType = data["gameType"] as? String ?: "",
                status = data["status"] as? String ?: "waiting",
                createdBy = data["createdBy"] as? String ?: "",
                p1Uid = data["p1Uid"] as? String ?: "",
                p1Name = data["p1Name"] as? String ?: "",
                p2Uid = data["p2Uid"] as? String ?: "",
                p2Name = data["p2Name"] as? String ?: "",
                currentTurn = data["currentTurn"] as? String ?: "",
                board = data["board"] as? Map<String, Any> ?: emptyMap(),
                moves = (data["moves"] as? List<*>)?.filterIsInstance<Map<String, Any>>() ?: emptyList(),
                winner = data["winner"] as? String ?: "",
                createdAt = data["createdAt"] as? Timestamp,
                updatedAt = data["updatedAt"] as? Timestamp,
            )
        }
    }
}

class GameRepository(
    private val coupleId: String,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    val myUid: String
        get() = auth.currentUser?.uid ?: error("Not signed in")

    private val gamesRef
        get() = db.collection("couples").document(coupleId).collection("games")

    suspend fun createGame(
        gameType: String,
        myName: String,
        initialBoard: Map<String, Any>,
    ): String {
        val doc = gamesRef.document()
        doc.set(
            mapOf(
                "gameType" to gameType,
                "status" to "playing",
                "createdBy" to myUid,
                "p1Uid" to myUid,
                "p1Name" to myName,
                "p2Uid" to "",
                "p2Name" to "",
                "currentTurn" to myUid,
                "board" to initialBoard,
                "moves" to emptyList<Any>(),
                "winner" to "",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
            ),
        ).await()
        return doc.id
    }

    suspend fun joinGame(gameId: String, myName: String) {
        gamesRef.document(gameId).update(
            mapOf(
                "p2Uid" to myUid,
                "p2Name" to myName,
                "updatedAt" to FieldValue.serverTimestamp(),
            ),
        ).await()
    }

    fun observeGame(gameId: String): Flow<GameSession?> =
        gamesRef.document(gameId).snapshots().map { GameSession.fromDoc(it) }.fallbackTo(null)

    suspend fun makeMove(gameId: String, board: Map<String, Any>, nextTurn: String, move: Map<String, Any>, winner: String = "") {
        val updates = mutableMapOf<String, Any>(
            "board" to board,
            "currentTurn" to nextTurn,
            "moves" to FieldValue.arrayUnion(move),
            "updatedAt" to FieldValue.serverTimestamp(),
        )
        if (winner.isNotEmpty()) {
            updates["winner"] = winner
            updates["status"] = "finished"
        }
        gamesRef.document(gameId).update(updates).await()
    }

    fun myActiveGames(): Flow<List<GameSession>> =
        gamesRef
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .limit(20)
            .snapshots()
            .map { snap ->
                snap.documents.map { GameSession.fromDoc(it) }
                    .filter { it.status != "finished" && (it.p1Uid == myUid || it.p2Uid == myUid) }
            }
            .fallbackTo(emptyList())
}
