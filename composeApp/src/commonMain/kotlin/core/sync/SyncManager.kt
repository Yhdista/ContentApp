package core.sync

import core.common.ApplicationScope
import core.database.MutationDao
import core.database.SyncMutationEntity
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlin.random.Random

/**
 * Asynchronní Outbox engine zodpovědný za spolehlivé odesílání
 * lokálně zapsaných mutací na cloudový backend.
 */
class SyncManager(
    private val mutationDao: MutationDao,
    private val applicationScope: ApplicationScope
) {
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _lastSyncLog = MutableStateFlow<String>("Čeká na první synchronizaci...")
    val lastSyncLog: StateFlow<String> = _lastSyncLog.asStateFlow()

    fun startSyncLoop() {
        applicationScope.launch {
            mutationDao.getPendingMutations().collect { mutations ->
                if (mutations.isNotEmpty()) {
                    _isSyncing.value = true
                    for (mutation in mutations) {
                        processMutation(mutation)
                    }
                    _isSyncing.value = false
                }
            }
        }
    }

    private suspend fun processMutation(mutation: SyncMutationEntity) {
        try {
            _lastSyncLog.value = "Odesílám mutaci: ${mutation.entityName} (id: ${mutation.id.take(8)}...)"

            // Obaleno do withTimeout pro simulaci reálného síťového limitu
            val success = withTimeout(3000) {
                simulateNetworkUpload(mutation)
            }

            if (success) {
                // Po úspěchu: smazat z outbox fronty (tím reaktivně klesne pending count)
                mutationDao.deleteMutation(mutation.id)
                _lastSyncLog.value = "Synchronizováno OK: ${mutation.recordId}"
                Napier.d("Sync úspěch pro mutaci ${mutation.id}")
            } else {
                handleFailure(mutation, "Simulovaná chyba sítě (30% drop)")
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
        _lastSyncLog.value = "Chyba ($reason) – pokus č. $newRetry"
        Napier.w("Sync selhal pro ${mutation.id}: $reason")
        // Krátká prodleva před dalším pokusem
        delay(1500)
    }

    /**
     * Simulátor odeslání sítě:
     * - Přidává umělou latenci 600 ms
     * - Náhodně způsobí 20% chybovost pro ověření retry logiky
     */
    private suspend fun simulateNetworkUpload(mutation: SyncMutationEntity): Boolean {
        delay(600)
        return Random.nextInt(100) >= 20 // 80% úspěšnost
    }
}
