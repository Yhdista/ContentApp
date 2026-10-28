package core.common

/**
 * Zformátuje desetinné číslo (např. naměřenou hodnotu) tak, že
 * celá čísla zobrazí bez desetinné části (`100.0` -> `"100"`) a
 * desetinná čísla zachová (`82.5` -> `"82.5"`).
 */
fun Double.formatCompactDecimal(): String =
    if (this % 1.0 == 0.0) this.toInt().toString() else this.toString()
