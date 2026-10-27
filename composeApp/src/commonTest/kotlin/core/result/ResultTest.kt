package core.result

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ResultTest {

    @Test
    fun testResultSuccessMappingAndOnSuccess() {
        val result: Result<Int, DataError.Local> = Result.Success(42)
        var captured = 0

        val mapped = result
            .onSuccess { captured = it }
            .map { it * 2 }

        assertEquals(42, captured)
        assertTrue(mapped is Result.Success)
        assertEquals(84, (mapped as Result.Success).data)
    }

    @Test
    fun testResultFailureFlow() {
        val result: Result<Int, DataError.Local> = Result.Error(DataError.Local.NOT_FOUND)
        var capturedError: DataError.Local? = null

        val mapped = result
            .onFailure { capturedError = it }
            .map { it * 2 }

        assertEquals(DataError.Local.NOT_FOUND, capturedError)
        assertTrue(mapped is Result.Error)
        assertEquals(DataError.Local.NOT_FOUND, (mapped as Result.Error).error)
    }

    @Test
    fun testAsEmptyResult() {
        val result: Result<String, DataError.Local> = Result.Success("Done")
        val empty = result.asEmptyResult()
        assertTrue(empty is Result.Success)
        assertEquals(Unit, (empty as Result.Success).data)
    }
}
