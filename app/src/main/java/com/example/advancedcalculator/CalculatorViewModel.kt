package com.example.advancedcalculator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val timestampFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(ZoneId.systemDefault())

data class CalculatorHistoryEntry(
    val expression: String,
    val result: String,
    val timestamp: Instant = Instant.now()
) {
    val formattedTimestamp: String = timestampFormatter.format(timestamp)
}

data class CalculatorUiState(
    val expression: String = "",
    val result: String = "0",
    val history: List<CalculatorHistoryEntry> = emptyList(),
    val error: String? = null
)

data class GraphUiState(
    val expression: String = "sin(x)",
    val xMin: Double = -10.0,
    val xMax: Double = 10.0,
    val samples: Int = 360,
    val evaluatedPoints: List<Pair<Double, Double>> = emptyList(),
    val error: String? = null
)

class CalculatorViewModel : ViewModel() {
    private val _calculatorState = MutableStateFlow(CalculatorUiState())
    val calculatorState: StateFlow<CalculatorUiState> = _calculatorState

    private val _graphState = MutableStateFlow(GraphUiState())
    val graphState: StateFlow<GraphUiState> = _graphState

    fun onSymbolAppend(symbol: String) {
        _calculatorState.update { state ->
            val sanitizedExpression = (state.expression + symbol).normalizeSymbols()
            state.copy(expression = sanitizedExpression, error = null)
        }
    }

    fun onClearAll() {
        _calculatorState.update { it.copy(expression = "", result = "0", error = null) }
    }

    fun onBackspace() {
        _calculatorState.update { state ->
            if (state.expression.isEmpty()) state
            else state.copy(expression = state.expression.dropLast(1), error = null)
        }
    }

    fun onEvaluate() {
        val expression = _calculatorState.value.expression
        if (expression.isBlank()) {
            _calculatorState.update { it.copy(result = "0") }
            return
        }
        viewModelScope.launch {
            runCatching {
                val value = ExpressionEngine.evaluate(expression)
                value
            }.onSuccess { value ->
                val formatted = value.formatResult()
                val historyEntry = CalculatorHistoryEntry(expression, formatted)
                _calculatorState.update { state ->
                    state.copy(
                        result = formatted,
                        history = (listOf(historyEntry) + state.history).take(50),
                        error = null
                    )
                }
            }.onFailure { throwable ->
                _calculatorState.update { it.copy(error = throwable.message ?: "Не удалось вычислить выражение") }
            }
        }
    }

    fun onHistoryItemSelected(entry: CalculatorHistoryEntry) {
        _calculatorState.update { it.copy(expression = entry.expression, result = entry.result, error = null) }
    }

    fun clearHistory() {
        _calculatorState.update { it.copy(history = emptyList()) }
    }

    fun updateGraphExpression(expression: String) {
        _graphState.update { state ->
            state.copy(expression = expression.normalizeSymbols(), error = null)
        }
    }

    fun updateGraphRange(xMin: Double, xMax: Double, samples: Int = _graphState.value.samples) {
        _graphState.update { state ->
            state.copy(xMin = xMin, xMax = xMax, samples = samples)
        }
    }

    fun refreshGraph() {
        val stateSnapshot = _graphState.value
        if (stateSnapshot.expression.isBlank()) {
            _graphState.update { it.copy(error = "Введите выражение для построения графика") }
            return
        }
        viewModelScope.launch {
            runCatching {
                generateGraphPoints(stateSnapshot.expression, stateSnapshot.xMin, stateSnapshot.xMax, stateSnapshot.samples)
            }.onSuccess { points ->
                _graphState.update { it.copy(evaluatedPoints = points, error = null) }
            }.onFailure { throwable ->
                _graphState.update { it.copy(error = throwable.message ?: "Ошибка построения графика") }
            }
        }
    }

    private fun generateGraphPoints(expression: String, xMin: Double, xMax: Double, samples: Int): List<Pair<Double, Double>> {
        require(xMax > xMin) { "Верхняя граница должна быть больше нижней" }
        val step = (xMax - xMin) / samples.coerceAtLeast(64)
        val points = mutableListOf<Pair<Double, Double>>()
        var x = xMin
        while (x <= xMax) {
            val value = runCatching { ExpressionEngine.evaluate(expression, x) }.getOrNull()
            if (value != null && value.isFinite()) {
                points += x to value
            }
            x += step
        }
        if (points.isEmpty()) {
            throw IllegalArgumentException("Не удалось вычислить значения функции на выбранном диапазоне")
        }
        return points
    }
}

private fun Double.formatResult(): String {
    return if (this.isNaN() || this.isInfinite()) {
        "Неопределено"
    } else {
        val normalized = String.format("%.10f", this)
        normalized.trimEnd('0').trimEnd('.')
    }
}

private fun Double.isFinite(): Boolean = !isNaN() && !isInfinite()

private fun String.normalizeSymbols(): String {
    return replace("÷", "/")
        .replace("×", "*")
        .replace("√", "sqrt")
        .replace("π", "pi")
        .replace("Π", "pi")
        .replace("ｅ", "e")
}
