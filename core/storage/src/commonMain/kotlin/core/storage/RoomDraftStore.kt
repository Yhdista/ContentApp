package core.storage

import org.koin.core.module.Module
import org.koin.dsl.module

internal class RoomDraftStore(private val dao: EditorDraftDao, private val writes: CommandCoordinator) : DraftStore {
    override suspend fun get(kind: String, id: String): String? = dao.get(kind, id)
    override suspend fun put(kind: String, id: String, payload: String) = writes.write { dao.put(EditorDraftEntity(kind, id, payload)) }
    override suspend fun delete(kind: String, id: String) = writes.write { dao.delete(kind, id) }
}

fun draftStorageModule(): Module = module {
    single<DraftStore> { RoomDraftStore(get(), get()) }
}
