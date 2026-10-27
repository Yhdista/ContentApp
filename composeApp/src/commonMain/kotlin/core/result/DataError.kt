package core.result

sealed interface DataError : RootError {
    enum class Local : DataError {
        DISK_FULL,
        NOT_FOUND,
        UNKNOWN
    }

    enum class Network : DataError {
        NO_INTERNET,
        REQUEST_TIMEOUT,
        SERVER_ERROR,
        SERIALIZATION,
        UNKNOWN
    }
}
