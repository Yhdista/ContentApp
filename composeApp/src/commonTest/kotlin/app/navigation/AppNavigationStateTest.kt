package app.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppNavigationStateTest {
    @Test
    fun restoredDetailPreservesBackDestination() {
        val state = AppNavigationState()
        state.navigate(AppDestination.ActiveWorkout)
        state.navigate(AppDestination.Detail("session:123"))
        val restored = AppNavigationState.restore(state.save())
        assertEquals(AppDestination.Detail("session:123"), restored.current)
        assertTrue(restored.back())
        assertEquals(AppDestination.ActiveWorkout, restored.current)
    }

    @Test
    fun rootCannotBePoppedAndDuplicateNavigationDoesNotAddEntry() {
        val state = AppNavigationState()
        assertFalse(state.back())
        state.navigate(AppDestination.ActiveWorkout)
        state.navigate(AppDestination.ActiveWorkout)
        assertTrue(state.back())
        assertEquals(AppDestination.History, state.current)
        assertFalse(state.back())
    }

    @Test
    fun corruptRestoredRouteFallsBackToHistory() {
        assertEquals(AppDestination.History, AppNavigationState.restore(listOf("unknown")).current)
    }
}
