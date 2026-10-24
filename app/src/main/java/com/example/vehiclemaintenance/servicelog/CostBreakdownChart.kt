package com.example.vehiclemaintenance.servicelog

import android.content.res.Configuration
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.vehiclemaintenance.R
import com.example.vehiclemaintenance.ui.theme.LocalChartColors
import com.example.vehiclemaintenance.ui.theme.VehicleMaintenanceTheme
import kotlin.math.cos
import kotlin.math.sin

/** Lets tests tell the legend's names and amounts apart from the same text in the history list. */
internal const val COST_BREAKDOWN_TAG = "cost_breakdown"

/**
 * The cost breakdown section: a heading, a pie of [slices], and a legend row per slice. The
 * legend carries every name, percent, and amount, so the pie itself is never the only way to read
 * the numbers.
 */
@Composable
fun CostBreakdownChart(slices: List<CostSlice>, modifier: Modifier = Modifier) {
    val chartColors = LocalChartColors.current
    val colors = sliceColors(slices.map { it.name }, chartColors.slices, chartColors.additionalRepairs)
    val chartDescription = stringResource(R.string.cost_breakdown_chart)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag(COST_BREAKDOWN_TAG)
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.cost_breakdown),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = HORIZONTAL_PADDING),
        )
        PieChart(
            slices = slices,
            colors = colors,
            separator = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(vertical = 8.dp)
                .size(PIE_SIZE)
                .semantics { contentDescription = chartDescription },
        )
        slices.forEachIndexed { index, slice -> LegendRow(slice, colors[index]) }
    }
}

/** Slices run clockwise from 12 o'clock, split by thin lines in the surface color. */
@Composable
private fun PieChart(
    slices: List<CostSlice>,
    colors: List<Color>,
    separator: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val sum = slices.sumOf { it.total }.toFloat()
        val sweeps = slices.map { 360f * it.total / sum }
        var start = START_ANGLE
        sweeps.forEachIndexed { index, sweep ->
            drawArc(colors[index], startAngle = start, sweepAngle = sweep, useCenter = true)
            start += sweep
        }
        // A lone slice is a full circle with no edge to mark.
        if (slices.size > 1) {
            val radius = size.minDimension / 2
            start = START_ANGLE
            sweeps.forEach { sweep ->
                val radians = Math.toRadians(start.toDouble())
                val edge = Offset(radius * cos(radians).toFloat(), radius * sin(radians).toFloat())
                drawLine(separator, center, center + edge, strokeWidth = SEPARATOR_WIDTH.toPx())
                start += sweep
            }
        }
    }
}

@Composable
private fun LegendRow(slice: CostSlice, color: Color, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {}
            .padding(horizontal = HORIZONTAL_PADDING, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(SWATCH_SIZE)
                .background(color, CircleShape),
        )
        Text(
            text = slice.name ?: stringResource(R.string.cost_breakdown_additional_repairs),
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = percentText(slice.percent),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = formatCost(slice.total),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun percentText(percent: Int): String =
    if (percent == 0) {
        stringResource(R.string.cost_breakdown_under_one_percent)
    } else {
        stringResource(R.string.cost_breakdown_percent, percent)
    }

/** Drawing angles put 0 at 3 o'clock, so -90 starts the first slice at 12. */
private const val START_ANGLE = -90f

private val PIE_SIZE = 160.dp
private val SEPARATOR_WIDTH = 2.dp
private val SWATCH_SIZE = 12.dp

private val previewSlices = listOf(
    CostSlice("Oil change", 41_996, 42),
    CostSlice("Brake pads", 21_000, 21),
    CostSlice("Tire rotation", 12_000, 12),
    CostSlice("Wiper blades", 400, 0),
    CostSlice(null, 24_604, 25),
)

@Preview(showBackground = true)
@Composable
private fun CostBreakdownChartPreview() {
    VehicleMaintenanceTheme(darkTheme = false) {
        Surface { CostBreakdownChart(previewSlices) }
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CostBreakdownChartDarkPreview() {
    VehicleMaintenanceTheme(darkTheme = true) {
        Surface { CostBreakdownChart(previewSlices) }
    }
}
