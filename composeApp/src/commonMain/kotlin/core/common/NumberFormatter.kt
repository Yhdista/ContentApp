package core.common

/**
 * Zformátuje desetinné číslo (např. váhu nebo objem v kg) tak, že
 * celá čísla zobrazí bez desetinné části (`100.0` -> `"100"`) a
 * desetinná čísla zachová (`82.5` -> `"82.5"`).
 */
fun Double.formatCleanWeight(): String =
    if (this % 1.0 == 0.0) this.toInt().toString() else this.toString()
