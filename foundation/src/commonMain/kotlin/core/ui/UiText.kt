package core.ui

import core.result.DataError

sealed interface UiText {
    data class DynamicString(val value: String) : UiText

    fun asString(): String = when (this) {
        is DynamicString -> value
    }
}

fun DataError.toUiText(): UiText = when (this) {
    DataError.Local.DISK_FULL -> UiText.DynamicString("Nedostatek místa v úložišti zařízení.")
    DataError.Local.NOT_FOUND -> UiText.DynamicString("Požadovaný záznam nebyl nalezen.")
    DataError.Local.CORRUPTED -> UiText.DynamicString("Lokální data jsou poškozena.")
    DataError.Local.UNKNOWN -> UiText.DynamicString("Došlo k neočekávané lokální chybě.")
    DataError.Network.NO_INTERNET -> UiText.DynamicString("Připojení k internetu není k dispozici.")
    DataError.Network.REQUEST_TIMEOUT -> UiText.DynamicString("Vypršel časový limit spojení se serverem.")
    DataError.Network.SERVER_ERROR -> UiText.DynamicString("Chyba na straně serveru. Zkuste to později.")
    DataError.Network.SERIALIZATION -> UiText.DynamicString("Chyba při čtení odpovědi ze serveru.")
    DataError.Network.UNKNOWN -> UiText.DynamicString("Došlo k neznámé síťové chybě.")
    DataError.Validation.EMPTY_NAME -> UiText.DynamicString("Název nesmí být prázdný.")
    DataError.Validation.INVALID_WEIGHT -> UiText.DynamicString("Zadaná váha není platná.")
    DataError.Validation.INVALID_REPS -> UiText.DynamicString("Zadaný počet opakování není platný.")
}
