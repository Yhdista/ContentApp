package app.database

import androidx.room.immediateTransaction
import androidx.room.useWriterConnection
import core.common.ApplicationScope
import app.database.AppDatabase
import features.workout.data.WorkoutWriteCoordinator
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.currentCoroutineContext
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

/** The application owns both the database and accepted writes, independently of any screen. */
class RoomWorkoutWriteCoordinator(
    private val database: AppDatabase,
    private val applicationScope: ApplicationScope
) : WorkoutWriteCoordinator {
    private class TransactionContext(val owner: RoomWorkoutWriteCoordinator) :
        AbstractCoroutineContextElement(Key) {
        companion object Key : CoroutineContext.Key<TransactionContext>
    }

    private val commands = Channel<suspend () -> Unit>(Channel.UNLIMITED)
    private val worker = applicationScope.launch {
        for (command in commands) command()
    }.also { job -> job.invokeOnCompletion { commands.close(it) } }

    override suspend fun <T> write(block: suspend () -> T): T {
        if (currentCoroutineContext()[TransactionContext]?.owner === this) return block()
        val result = CompletableDeferred<T>()
        val completion = worker.invokeOnCompletion { cause ->
            result.completeExceptionally(cause ?: CancellationException("Write coordinator stopped"))
        }
        try {
            commands.send {
                try {
                    val value = withContext(TransactionContext(this@RoomWorkoutWriteCoordinator)) {
                        database.useWriterConnection<T> { connection ->
                            connection.immediateTransaction<T> { block() }
                        }
                    }
                    result.complete(value)
                } catch (error: Exception) {
                    result.completeExceptionally(error)
                    currentCoroutineContext().ensureActive()
                    Napier.e("Workout command failed; transaction rolled back", error)
                }
            }
            return result.await()
        } finally {
            completion.dispose()
        }
    }
}
