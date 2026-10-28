package core.storage

/** Local editor payloads, namespaced by editor kind and stable draft ID. No domain validation or sync. */
interface DraftStore {
    suspend fun get(kind: String, id: String): String?
    suspend fun put(kind: String, id: String, payload: String)
    suspend fun delete(kind: String, id: String)
}
