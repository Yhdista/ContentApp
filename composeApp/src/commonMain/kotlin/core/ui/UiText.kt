package core.ui

sealed interface UiText {
    data class DynamicString(val value: String) : UiText

    fun asString(): String = when (this) {
        is DynamicString -> value
    }
}
