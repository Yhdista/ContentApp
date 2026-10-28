package core.sync

import core.common.ApplicationScope
import core.database.DEFAULT_MAX_SYNC_RETRIES
import core.database.MutationDao
import core.database.SyncMutationEntity
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

/**
 * Asynchronní Outbox engine zodpovědný za spolehlivé odesílání
 * lokálně zapsaných mutací na cloudový backend.
 *
 * Architektonické záruky:
 * - Single-Active-Job Guard: Opakované volání [startSyncLoop] nespustí duplicitní korutiny.
 * - Dead-Letter / Max-Retry Ochrana: Mutace se po překročení [maxRetries] přestanou nekonečně opakovat.
 * - Injektovatelný [networkUploader]: Umožňuje deterministické testování síťových výpadků.
 */
class SyncManager(
    private val mutationDao: MutationDao,
    private val applicationScope: ApplicationScope,
    private val maxRetries: Int = DEFAULT_MAX_SYNC_RETRIES,
    private val networkUploader: (suspend (SyncMutationEntity) -> Boolean)? = null
) {
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _lastSyncLog = MutableStateFlow("Čeká na první synchronizaci...")
    val lastSyncLog: StateFlow<String> = _lastSyncLog.asStateFlow()

    private var syncJob: Job? = null
    private val startGuard = Mutex()

    init { require(maxRetries > 0) }

    fun startSyncLoop() {
        if (networkUploader == null) {
            _lastSyncLog.value = "Synchronizace není nakonfigurovaná; data zůstávají lokálně."
            return
        }
        if (!startGuard.tryLock()) return
        try {
            if (syncJob?.isActive == true) return
            syncJob = applicationScope.launch(start = CoroutineStart.LAZY) {
                try {
                    mutationDao.getPendingMutations(maxRetries).collect { mutations ->
                        val actionable = mutations.filter { it.retryCount < maxRetries }
                        _isSyncing.value = actionable.isNotEmpty()
                        for (mutation in actionable) processMutation(mutation)
                        _isSyncing.value = false
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    _lastSyncLog.value = "Synchronizace byla přerušena chybou lokálního úložiště."
                    Napier.e("Sync loop stopped", error)
                } finally {
                    _isSyncing.value = false
                }
            }
            syncJob?.start()
        } finally {
            startGuard.unlock()
        }
    }

    private suspend fun processMutation(mutation: SyncMutationEntity) {
        try {
            _lastSyncLog.value = "Odesílám mutaci: ${mutation.entityName} (id: ${mutation.id.take(8)}...)"

            val success = withTimeout(3000) {
                requireNotNull(networkUploader).invoke(mutation)
            }

            if (success) {
                mutationDao.deleteMutation(mutation.id)
                _lastSyncLog.value = "Synchronizováno OK: ${mutation.recordId}"
                Napier.d("Sync úspěch pro mutaci ${mutation.id}")
            } else {
                handleFailure(mutation, "Server nepotvrdil přijetí mutace")
            }
        } catch (e: TimeoutCancellationException) {
            handleFailure(mutation, "Timeout požadavku (>3s)")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFailure(mutation, e::class.simpleName ?: "Neznámá chyba")
        }
    }

    private suspend fun handleFailure(mutation: SyncMutationEntity, reason: String) {
        val newRetry = mutation.retryCount + 1
        mutationDao.markError(mutation.id, reason, newRetry)
        if (newRetry >= maxRetries) {
            _lastSyncLog.value = "Odloženo po $newRetry pokusech ($reason): ${mutation.recordId}"
            Napier.e("Sync mutace ${mutation.id} dosáhla max. počtu pokusů ($maxRetries): $reason")
        } else {
            _lastSyncLog.value = "Chyba ($reason) – pokus č. $newRetry/$maxRetries"
            Napier.w("Sync selhal pro ${mutation.id}: $reason (pokus $newRetry/$maxRetries)")
            delay(1500)
        }
    }

}
