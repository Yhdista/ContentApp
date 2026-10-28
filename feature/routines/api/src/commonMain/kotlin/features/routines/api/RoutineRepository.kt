package features.routines.api

import core.result.Result
import core.result.RootError
import kotlinx.coroutines.flow.Flow

/** NOT_FOUND also identifies missing catalogue references; STORAGE preserves no partial writes. */
enum class RoutineError : RootError { VALIDATION, NOT_FOUND, ARCHIVED, REVISION_CONFLICT, STORAGE }

interface RoutineRepository {
    fun observeAll(includeArchived: Boolean = false): Flow<List<Routine>>
    fun observeById(id: String): Flow<Routine?>
    suspend fun getById(id: String): Routine?
    /** Creates at revision 1 when expectedRevision is null; existing IDs require their current revision.
     * Names are trimmed. Archived aggregates cannot be saved or unarchived through this command.
     */
    suspend fun save(routine: Routine, expectedRevision: Long?): Result<Routine, RoutineError>
    /** Copies even an archived source into a new active routine, with fresh aggregate/entry/set IDs.
     * New copies cannot reference archived catalogue exercises.
     */
    suspend fun duplicate(id: String): Result<Routine, RoutineError>
    /** Soft archive increments revision once. An already archived ID returns ARCHIVED. */
    suspend fun archive(id: String, expectedRevision: Long): Result<Unit, RoutineError>
}
