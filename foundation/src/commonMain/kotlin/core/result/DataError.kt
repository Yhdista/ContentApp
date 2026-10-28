package core.result

sealed interface DataError : RootError {
    enum class Local : DataError {
        DISK_FULL,
        NOT_FOUND,
        CORRUPTED,
        UNKNOWN
    }

    enum class Network : DataError {
        NO_INTERNET,
        REQUEST_TIMEOUT,
        SERVER_ERROR,
        SERIALIZATION,
        UNKNOWN
    }

    enum class Validation : DataError {
        EMPTY_NAME,
        INVALID_WEIGHT,
        INVALID_REPS
    }
}
