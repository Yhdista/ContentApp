package core.storage

/**
 * Runs a complete command atomically, including its outbox records.
 * Implementations serialize commands and own their lifetime after acceptance.
 * Nested calls participate in the same transaction; blocks must not launch parallel work.
 */
interface CommandCoordinator {
    suspend fun <T> write(block: suspend () -> T): T
}
