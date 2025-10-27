package com.example.advancedcalculator

import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

object ExpressionEngine {
    private enum class Associativity { LEFT, RIGHT }

    private data class Operator(
        val symbol: String,
        val precedence: Int,
        val associativity: Associativity,
        val arity: Int,
        val isPostfix: Boolean = false
    )

    private val operators = mapOf(
        "+" to Operator("+", 2, Associativity.LEFT, 2),
        "-" to Operator("-", 2, Associativity.LEFT, 2),
        "*" to Operator("*", 3, Associativity.LEFT, 2),
        "/" to Operator("/", 3, Associativity.LEFT, 2),
        "^" to Operator("^", 4, Associativity.RIGHT, 2),
        "u-" to Operator("u-", 5, Associativity.RIGHT, 1),
        "!" to Operator("!", 6, Associativity.LEFT, 1, isPostfix = true)
    )

    private val functions = setOf(
        "sin", "cos", "tan", "asin", "acos", "atan",
        "sqrt", "abs", "ln", "log", "exp"
    )

    fun evaluate(expression: String, variableValue: Double? = null, variableName: String = "x"): Double {
        val tokens = tokenize(expression, variableName)
        val rpn = shuntingYard(tokens)
        return evaluateRpn(rpn, variableValue, variableName)
    }

    private fun tokenize(expression: String, variableName: String): List<String> {
        val normalized = expression.lowercase()
        val tokens = mutableListOf<String>()
        var index = 0
        var lastToken: String? = null
        while (index < normalized.length) {
            val char = normalized[index]
            when {
                char.isWhitespace() -> index++
                char.isDigit() || char == '.' -> {
                    val start = index
                    index++
                    while (index < normalized.length && (normalized[index].isDigit() || normalized[index] == '.')) {
                        index++
                    }
                    tokens += normalized.substring(start, index)
                    lastToken = tokens.last()
                }
                char.isLetter() -> {
                    val start = index
                    index++
                    while (index < normalized.length && (normalized[index].isLetter())) {
                        index++
                    }
                    val word = normalized.substring(start, index)
                    val mapped = when (word) {
                        "pi" -> kotlin.math.PI.toString()
                        "e" -> kotlin.math.E.toString()
                        variableName.lowercase() -> variableName
                        else -> word
                    }
                    tokens += mapped
                    lastToken = tokens.last()
                }
                char == '(' || char == ')' || char == ',' -> {
                    tokens += char.toString()
                    lastToken = tokens.last()
                    index++
                }
                char == '+' || char == '-' || char == '*' || char == '/' || char == '^' -> {
                    val symbol = when (char) {
                        '*' -> "*"
                        '/' -> "/"
                        '^' -> "^"
                        '+' -> "+"
                        '-' -> {
                            if (lastToken == null || lastToken in operators.keys || lastToken == "(" || lastToken == ",") {
                                "u-"
                            } else "-"
                        }
                        else -> char.toString()
                    }
                    tokens += symbol
                    lastToken = symbol
                    index++
                }
                char == '!' -> {
                    tokens += "!"
                    lastToken = "!"
                    index++
                }
                else -> throw IllegalArgumentException("Недопустимый символ: $char")
            }
        }
        return tokens
    }

    private fun shuntingYard(tokens: List<String>): List<String> {
        val output = mutableListOf<String>()
        val stack = ArrayDeque<String>()
        for (token in tokens) {
            when {
                token.isNumber() || token == "x" -> output += token
                token in functions -> stack.addFirst(token)
                operators.containsKey(token) -> {
                    val operator = operators.getValue(token)
                    while (stack.isNotEmpty()) {
                        val top = stack.first()
                        val topOperator = operators[top]
                        if (topOperator != null && (!operator.isPostfix)) {
                            val shouldPop = (operator.associativity == Associativity.LEFT && operator.precedence <= topOperator.precedence) ||
                                (operator.associativity == Associativity.RIGHT && operator.precedence < topOperator.precedence)
                            if (shouldPop) {
                                output += stack.removeFirst()
                                continue
                            }
                        } else if (topOperator != null && operator.isPostfix) {
                            output += stack.removeFirst()
                            continue
                        }
                        break
                    }
                    stack.addFirst(token)
                }
                token == "(" -> stack.addFirst(token)
                token == ")" -> {
                    while (stack.isNotEmpty() && stack.first() != "(") {
                        output += stack.removeFirst()
                    }
                    if (stack.isEmpty() || stack.first() != "(") {
                        throw IllegalArgumentException("Несогласованные скобки")
                    }
                    stack.removeFirst()
                    if (stack.isNotEmpty() && stack.first() in functions) {
                        output += stack.removeFirst()
                    }
                }
                token == "," -> {
                    while (stack.isNotEmpty() && stack.first() != "(") {
                        output += stack.removeFirst()
                    }
                }
                else -> throw IllegalArgumentException("Не удалось разобрать токен: $token")
            }
        }
        while (stack.isNotEmpty()) {
            val token = stack.removeFirst()
            if (token == "(" || token == ")") {
                throw IllegalArgumentException("Несогласованные скобки")
            }
            output += token
        }
        return output
    }

    private fun evaluateRpn(tokens: List<String>, variableValue: Double?, variableName: String): Double {
        val stack = ArrayDeque<Double>()
        for (token in tokens) {
            when {
                token.isNumber() -> stack.addLast(token.toDouble())
                token == variableName -> {
                    val value = variableValue ?: throw IllegalArgumentException("Не задано значение для переменной $variableName")
                    stack.addLast(value)
                }
                token in operators -> {
                    val operator = operators.getValue(token)
                    if (operator.arity == 2) {
                        if (stack.size < 2) throw IllegalArgumentException("Недостаточно аргументов для оператора $token")
                        val right = stack.removeLast()
                        val left = stack.removeLast()
                        val result = when (token) {
                            "+" -> left + right
                            "-" -> left - right
                            "*" -> left * right
                            "/" -> {
                                if (right == 0.0) throw IllegalArgumentException("Деление на ноль")
                                left / right
                            }
                            "^" -> left.pow(right)
                            else -> error("Неизвестный оператор $token")
                        }
                        stack.addLast(result)
                    } else {
                        if (stack.isEmpty()) throw IllegalArgumentException("Недостаточно аргументов для оператора $token")
                        val value = stack.removeLast()
                        val result = when (token) {
                            "u-" -> -value
                            "!" -> factorial(value)
                            else -> error("Неизвестный оператор $token")
                        }
                        stack.addLast(result)
                    }
                }
                token in functions -> {
                    if (stack.isEmpty()) throw IllegalArgumentException("Недостаточно аргументов для функции $token")
                    val value = stack.removeLast()
                    val result = when (token) {
                        "sin" -> sin(value)
                        "cos" -> cos(value)
                        "tan" -> tan(value)
                        "asin" -> asin(value)
                        "acos" -> acos(value)
                        "atan" -> atan(value)
                        "sqrt" -> sqrt(value)
                        "abs" -> abs(value)
                        "ln" -> ln(value)
                        "log" -> log10(value)
                        "exp" -> exp(value)
                        else -> error("Неизвестная функция $token")
                    }
                    stack.addLast(result)
                }
                else -> throw IllegalArgumentException("Неизвестный токен $token")
            }
        }
        if (stack.size != 1) {
            throw IllegalArgumentException("Не удалось вычислить выражение")
        }
        return stack.last()
    }

    private fun factorial(value: Double): Double {
        if (value < 0) throw IllegalArgumentException("Факториал определён только для неотрицательных чисел")
        val rounded = value.roundToIntOrNull() ?: throw IllegalArgumentException("Факториал принимает только целые значения")
        var result = 1.0
        for (i in 2..rounded) {
            result *= i
        }
        return result
    }

    private fun Double.roundToIntOrNull(): Int? {
        val rounded = kotlin.math.round(this)
        return if (abs(this - rounded) < 1e-9) rounded.toInt() else null
    }

    private fun String.isNumber(): Boolean = toDoubleOrNull() != null
}
