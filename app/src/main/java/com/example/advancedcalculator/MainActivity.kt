package com.example.advancedcalculator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.advancedcalculator.ui.theme.AdvancedCalculatorTheme
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class MainActivity : ComponentActivity() {
    private val viewModel: CalculatorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AdvancedCalculatorTheme {
                AdvancedCalculatorApp(viewModel)
            }
        }
    }
}

private enum class CalculatorMode(val title: String) {
    BASIC("Базовый"),
    SCIENTIFIC("Научный")
}

private sealed class Destination(val route: String, val label: String) {
    data object Calculator : Destination("calculator", "Калькулятор")
    data object Graphs : Destination("graphs", "Графики")
    data object History : Destination("history", "История")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdvancedCalculatorApp(viewModel: CalculatorViewModel) {
    val navController = rememberNavController()
    val calculatorState by viewModel.calculatorState.collectAsStateWithLifecycle()
    val graphState by viewModel.graphState.collectAsStateWithLifecycle()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: Destination.Calculator.route

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(text = "Advanced Calculator", fontWeight = FontWeight.Bold) })
        },
        bottomBar = {
            NavigationBar {
                val destinations = listOf(Destination.Calculator, Destination.Graphs, Destination.History)
                destinations.forEach { destination ->
                    NavigationBarItem(
                        selected = currentRoute == destination.route,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            when (destination) {
                                Destination.Calculator -> Icon(Icons.Default.Calculate, contentDescription = null)
                                Destination.Graphs -> Icon(Icons.AutoMirrored.Filled.ShowChart, contentDescription = null)
                                Destination.History -> Icon(Icons.Default.History, contentDescription = null)
                            }
                        },
                        label = { Text(destination.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Destination.Calculator.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Destination.Calculator.route) {
                CalculatorScreen(
                    state = calculatorState,
                    onSymbolAppend = viewModel::onSymbolAppend,
                    onBackspace = viewModel::onBackspace,
                    onEvaluate = viewModel::onEvaluate,
                    onClear = viewModel::onClearAll
                )
            }
            composable(Destination.Graphs.route) {
                GraphScreen(
                    state = graphState,
                    onExpressionChange = viewModel::updateGraphExpression,
                    onRangeChange = viewModel::updateGraphRange,
                    onRefreshGraph = viewModel::refreshGraph
                )
            }
            composable(Destination.History.route) {
                HistoryScreen(
                    history = calculatorState.history,
                    onEntrySelected = viewModel::onHistoryItemSelected,
                    onClearHistory = viewModel::clearHistory
                )
            }
        }
    }
}

@Composable
private fun CalculatorScreen(
    state: CalculatorUiState,
    onSymbolAppend: (String) -> Unit,
    onBackspace: () -> Unit,
    onEvaluate: () -> Unit,
    onClear: () -> Unit
) {
    var mode by rememberSaveable { mutableStateOf(CalculatorMode.BASIC) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (state.error != null) {
            ErrorHint(state.error)
        }
        ModeSelector(mode = mode, onModeChange = { mode = it })
        Display(expression = state.expression, result = state.result)
        Spacer(modifier = Modifier.height(8.dp))
        Keypad(
            mode = mode,
            onSymbolAppend = onSymbolAppend,
            onBackspace = onBackspace,
            onEvaluate = onEvaluate,
            onClear = onClear
        )
    }
}

@Composable
private fun ErrorHint(error: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Text(
            text = error,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(12.dp),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun ModeSelector(mode: CalculatorMode, onModeChange: (CalculatorMode) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        CalculatorMode.values().forEach { item ->
            val selected = mode == item
            OutlinedButton(
                onClick = { onModeChange(item) },
                modifier = Modifier.weight(1f),
                border = if (selected) CardDefaults.outlinedCardBorder() else null
            ) {
                Text(text = item.title, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
            }
        }
    }
}

@Composable
private fun Display(expression: String, result: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.medium)
            .padding(16.dp)
    ) {
        Text(
            text = expression.ifBlank { "Введите выражение" },
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.End,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = result,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.End,
            style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold)
        )
    }
}

@Composable
private fun Keypad(
    mode: CalculatorMode,
    onSymbolAppend: (String) -> Unit,
    onBackspace: () -> Unit,
    onEvaluate: () -> Unit,
    onClear: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Crossfade(targetState = mode) { currentMode ->
            when (currentMode) {
                CalculatorMode.BASIC -> BasicPad(onSymbolAppend, onBackspace, onEvaluate, onClear)
                CalculatorMode.SCIENTIFIC -> ScientificPad(onSymbolAppend, onBackspace, onEvaluate, onClear)
            }
        }
    }
}

@Composable
private fun BasicPad(
    onSymbolAppend: (String) -> Unit,
    onBackspace: () -> Unit,
    onEvaluate: () -> Unit,
    onClear: () -> Unit
) {
    val rows = listOf(
        listOf("7", "8", "9", "/"),
        listOf("4", "5", "6", "*"),
        listOf("1", "2", "3", "-"),
        listOf("0", ".", "+/-", "+"),
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            CalculatorButton(text = "C", modifier = Modifier.weight(1f), onClick = onClear, type = ButtonType.Function)
            CalculatorButton(text = "⌫", modifier = Modifier.weight(1f), onClick = onBackspace, type = ButtonType.Function)
            CalculatorButton(text = "(", modifier = Modifier.weight(1f), onClick = { onSymbolAppend("(") })
            CalculatorButton(text = ")", modifier = Modifier.weight(1f), onClick = { onSymbolAppend(")") })
        }
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { label ->
                    val handler = when (label) {
                        "+/-" -> { { onSymbolAppend("-") } }
                        else -> { { onSymbolAppend(label) } }
                    }
                    CalculatorButton(text = label, modifier = Modifier.weight(1f), onClick = handler)
                }
            }
        }
        CalculatorButton(
            text = "=",
            modifier = Modifier.fillMaxWidth().height(56.dp),
            onClick = onEvaluate,
            type = ButtonType.Primary
        )
    }
}

@Composable
private fun ScientificPad(
    onSymbolAppend: (String) -> Unit,
    onBackspace: () -> Unit,
    onEvaluate: () -> Unit,
    onClear: () -> Unit
) {
    val scientificRows = listOf(
        listOf("sin", "cos", "tan", "pi"),
        listOf("asin", "acos", "atan", "e"),
        listOf("ln", "log", "sqrt", "^"),
        listOf("abs", "exp", "!", "%"),
    )
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        BasicPad(onSymbolAppend, onBackspace, onEvaluate, onClear)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            scientificRows.forEach { row ->
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                    row.forEach { label ->
                        CalculatorButton(
                            text = label,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                when (label) {
                                    "%" -> onSymbolAppend("/100")
                                    "!" -> onSymbolAppend("!")
                                    "pi" -> onSymbolAppend("pi")
                                    "e" -> onSymbolAppend("e")
                                    "^" -> onSymbolAppend("^")
                                    else -> onSymbolAppend("$label(")
                                }
                            },
                            type = ButtonType.Function
                        )
                    }
                }
            }
        }
    }
}

private enum class ButtonType { Primary, Default, Function }

@Composable
private fun CalculatorButton(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    type: ButtonType = ButtonType.Default
) {
    val colors = when (type) {
        ButtonType.Primary -> ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
        ButtonType.Function -> ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.onSecondary
        )
        ButtonType.Default -> ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    }
    Button(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        colors = colors
    ) {
        Text(text = text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GraphScreen(
    state: GraphUiState,
    onExpressionChange: (String) -> Unit,
    onRangeChange: (Double, Double, Int) -> Unit,
    onRefreshGraph: () -> Unit
) {
    var xMin by rememberSaveable(state.xMin) { mutableDoubleStateOf(state.xMin) }
    var xMax by rememberSaveable(state.xMax) { mutableDoubleStateOf(state.xMax) }
    var samples by rememberSaveable(state.samples) { mutableIntStateOf(state.samples) }
    LaunchedEffect(state.expression) {
        if (state.evaluatedPoints.isEmpty()) {
            onRefreshGraph()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (state.error != null) {
            ErrorHint(state.error)
        }
        OutlinedTextField(
            value = state.expression,
            onValueChange = onExpressionChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("f(x) =") },
            textStyle = MaterialTheme.typography.titleMedium
        )
        RangeControls(
            xMin = xMin,
            xMax = xMax,
            samples = samples,
            onRangeChange = { min, max, s ->
                xMin = min
                xMax = max
                samples = s
                onRangeChange(min, max, s)
            }
        )
        Button(onClick = onRefreshGraph, modifier = Modifier.fillMaxWidth()) {
            Text("Построить график")
        }
        GraphCanvas(points = state.evaluatedPoints, xMin = xMin, xMax = xMax)
    }
}

@Composable
private fun RangeControls(
    xMin: Double,
    xMax: Double,
    samples: Int,
    onRangeChange: (Double, Double, Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        RangeSelector(
            label = "Диапазон X",
            start = xMin,
            end = xMax,
            range = -50.0..50.0,
            onValueChange = { min, max -> onRangeChange(min, max, samples) }
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Точность: $samples точек", style = MaterialTheme.typography.bodyMedium)
            Slider(
                value = samples.toFloat(),
                valueRange = 120f..720f,
                onValueChange = { value -> onRangeChange(xMin, xMax, value.toInt()) }
            )
        }
    }
}

@Composable
private fun RangeSelector(
    label: String,
    start: Double,
    end: Double,
    range: ClosedFloatingPointRange<Double>,
    onValueChange: (Double, Double) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            NumericField(
                value = start,
                onValueChange = { onValueChange(it, end) },
                modifier = Modifier.weight(1f)
            )
            NumericField(
                value = end,
                onValueChange = { onValueChange(start, it) },
                modifier = Modifier.weight(1f)
            )
        }
        RangeSlider(
            value = start.toFloat()..end.toFloat(),
            onValueChange = { updated ->
                val min = max(range.start, min(range.endInclusive, updated.start.toDouble()))
                val maxValue = max(range.start, min(range.endInclusive, updated.endInclusive.toDouble()))
                onValueChange(min, maxValue)
            },
            valueRange = range.start.toFloat()..range.endInclusive.toFloat()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NumericField(
    value: Double,
    onValueChange: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    var text by rememberSaveable { mutableStateOf(value.toString()) }
    LaunchedEffect(value) {
        if (text != value.toString()) {
            text = value.toString()
        }
    }
    OutlinedTextField(
        value = text,
        onValueChange = {
            text = it
            it.toDoubleOrNull()?.let(onValueChange)
        },
        singleLine = true,
        modifier = modifier
    )
}

@Composable
private fun GraphCanvas(points: List<Pair<Double, Double>>, xMin: Double, xMax: Double) {
    if (points.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.5f)
                .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.medium),
            contentAlignment = Alignment.Center
        ) {
            Text("Нет данных для отображения", style = MaterialTheme.typography.bodyMedium)
        }
        return
    }

    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val surfaceColor = MaterialTheme.colorScheme.surface
    val graphColor = MaterialTheme.colorScheme.primary

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(0.5f, 10f)
                        offset += pan
                    }
                }
        ) {
            val width = size.width
            val height = size.height
            val center = Offset(width / 2, height / 2)
            drawRect(color = surfaceColor, size = size)
            val safeXRange = (xMax - xMin).toFloat().takeIf { abs(it) > 1e-6 } ?: 1f
            translate(left = center.x + offset.x, top = center.y + offset.y) {
                val xScale = width / safeXRange * scale
                val yValues = points.map { it.second }
                val yMin = yValues.minOrNull() ?: -10.0
                val yMax = yValues.maxOrNull() ?: 10.0
                val safeYRange = (yMax - yMin).takeIf { abs(it) > 1e-6 } ?: 20.0
                val yScale = height / safeYRange.toFloat() * scale

                // axes
                drawLine(
                    color = Color.LightGray,
                    start = Offset(-center.x - offset.x, 0f),
                    end = Offset(center.x - offset.x, 0f),
                    strokeWidth = 2f
                )
                drawLine(
                    color = Color.LightGray,
                    start = Offset(0f, -center.y - offset.y),
                    end = Offset(0f, center.y - offset.y),
                    strokeWidth = 2f
                )

                val path = Path()
                var firstPoint = true
                points.forEach { (x, y) ->
                    val canvasX = (x - (xMin + xMax) / 2).toFloat() * xScale
                    val canvasY = -(y - (yMin + yMax) / 2).toFloat() * yScale
                    if (firstPoint) {
                        path.moveTo(canvasX, canvasY)
                        firstPoint = false
                    } else {
                        path.lineTo(canvasX, canvasY)
                    }
                }
                drawPath(
                    path = path,
                    color = graphColor,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryScreen(
    history: List<CalculatorHistoryEntry>,
    onEntrySelected: (CalculatorHistoryEntry) -> Unit,
    onClearHistory: () -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("История вычислений", style = MaterialTheme.typography.titleLarge)
            TextButton(onClick = { showDialog = true }) {
                Text("Очистить")
            }
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(history) { entry ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .background(MaterialTheme.colorScheme.surface),
                    onClick = { onEntrySelected(entry) }
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(entry.expression, style = MaterialTheme.typography.bodyLarge)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            entry.result,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(entry.formattedTimestamp, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    onClearHistory()
                    showDialog = false
                }) {
                    Text("Очистить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Отмена") }
            },
            title = { Text("Удалить историю?") },
            text = { Text("Это действие нельзя будет отменить.") }
        )
    }
}
