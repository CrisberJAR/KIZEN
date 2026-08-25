package com.kizen.tasks.sync

/**
 * Puerto de sincronización. Si el usuario no activa la nube, pull/push no hacen nada.
 * El backend vive en Node/AWS; Android nunca guarda claves de IA.
 */
interface SyncPort {
    val isEnabled: Boolean
    suspend fun pull(): Result<Unit>
    suspend fun push(): Result<Unit>
    suspend fun sync(): Result<Unit> = push().mapCatching { pull().getOrThrow() }
}
