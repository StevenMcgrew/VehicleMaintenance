package com.example.vehiclemaintenance.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.dp
import com.example.vehiclemaintenance.R
import com.example.vehiclemaintenance.ui.theme.LocalBrandColors
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

private object SpansExtraColumn

/** Lets a [FormTable] input take over its row's third column as well. */
fun Modifier.spansExtraColumn(): Modifier = layoutId(SpansExtraColumn)

/**
 * A borderless label, input, extra grid. Each slot emits exactly one child per row, in row
 * order, so the columns line up without a table widget.
 */
@Composable
fun FormTable(
    labels: @Composable () -> Unit,
    inputs: @Composable () -> Unit,
    extras: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Layout(
        contents = listOf(labels, inputs, extras),
        modifier = modifier,
    ) { (labelCells, inputCells, extraCells), constraints ->
        val width = constraints.maxWidth
        val columnGap = TABLE_COLUMN_GAP.roundToPx()
        val rowGap = TABLE_ROW_GAP.roundToPx()

        val labelPlaceables = labelCells.map {
            it.measure(Constraints(maxWidth = (width * MAX_LABEL_WIDTH_FRACTION).toInt()))
        }
        val extraPlaceables = extraCells.map {
            it.measure(Constraints(maxWidth = (width * MAX_EXTRA_WIDTH_FRACTION).toInt()))
        }
        val labelWidth = labelPlaceables.maxOf { it.width }
        val extraWidth = extraPlaceables.maxOf { it.width }
        val inputWidth = (width - labelWidth - extraWidth - 2 * columnGap).coerceAtLeast(0)
        val inputPlaceables = inputCells.map {
            val spans = it.layoutId == SpansExtraColumn
            it.measure(
                Constraints(maxWidth = if (spans) inputWidth + columnGap + extraWidth else inputWidth),
            )
        }

        val rowHeights = labelPlaceables.indices.map { row ->
            maxOf(
                labelPlaceables[row].height,
                inputPlaceables[row].height,
                extraPlaceables[row].height,
            )
        }
        val height = rowHeights.sum() + rowGap * (rowHeights.size - 1).coerceAtLeast(0)

        layout(width, constraints.constrainHeight(height)) {
            var y = 0
            rowHeights.forEachIndexed { row, rowHeight ->
                labelPlaceables[row].place(0, y)
                inputPlaceables[row].place(labelWidth + columnGap, y)
                extraPlaceables[row].place(width - extraWidth, y)
                y += rowHeight + rowGap
            }
        }
    }
}

private val TABLE_COLUMN_GAP = 12.dp
private val TABLE_ROW_GAP = 12.dp
private const val MAX_LABEL_WIDTH_FRACTION = 0.3f
private const val MAX_EXTRA_WIDTH_FRACTION = 0.4f

/** Cells are top aligned, so single line content is centered against a text field's height. */
@Composable
fun FormCell(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier.heightIn(min = OutlinedTextFieldDefaults.MinHeight),
        contentAlignment = Alignment.CenterStart,
    ) {
        content()
    }
}

@Composable
fun FormCellText(text: String, modifier: Modifier = Modifier) {
    FormCell(modifier) {
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun FormTextField(
    value: String,
    onValueChange: (String) -> Unit,
    description: String,
    error: String?,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = placeholder?.let { { Text(it) } },
        isError = error != null,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        supportingText = error?.let { { Text(it) } },
        modifier = modifier
            .fillMaxWidth()
            .describedAs(description, error),
    )
}

/** The row label sits in its own cell, so the input carries it for screen readers. */
fun Modifier.describedAs(description: String, error: String?): Modifier = semantics {
    contentDescription = description
    if (error != null) error(error)
}

@Composable
fun ChooseDateButton(
    onClick: () -> Unit,
    error: String?,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        FormCell {
            OutlinedButton(
                onClick = onClick,
                modifier = if (error != null) Modifier.semantics { error(error) } else Modifier,
                colors = brandOutlinedButtonColors(),
                border = brandOutlinedButtonBorder(enabled = true),
            ) {
                Text(stringResource(R.string.choose_date))
            }
        }
        if (error != null) {
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp),
            )
        }
    }
}

/** Offers a Clear button only when [onClear] is given, for dates the user may leave unset. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormDatePickerDialog(
    date: LocalDate?,
    onDateChange: (LocalDate?) -> Unit,
    onDismiss: () -> Unit,
    onClear: (() -> Unit)? = null,
) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = date?.toEpochDay()?.times(MILLIS_PER_DAY),
    )
    // The dialog and the calendar inside it each draw a background, so both take the popup color.
    val colors = DatePickerDefaults.colors(containerColor = LocalBrandColors.current.popupContainer)
    DatePickerDialog(
        onDismissRequest = onDismiss,
        colors = colors,
        confirmButton = {
            TextButton(
                onClick = {
                    onDateChange(state.selectedDateMillis?.toLocalDate())
                    onDismiss()
                },
                colors = brandTextButtonColors(),
            ) {
                Text(stringResource(R.string.ok))
            }
        },
        dismissButton = {
            Row {
                if (onClear != null) {
                    TextButton(
                        onClick = {
                            onClear()
                            onDismiss()
                        },
                        colors = brandTextButtonColors(),
                    ) {
                        Text(stringResource(R.string.clear_date))
                    }
                }
                TextButton(onClick = onDismiss, colors = brandTextButtonColors()) {
                    Text(stringResource(R.string.cancel))
                }
            }
        },
    ) {
        DatePicker(state = state, colors = colors)
    }
}

private const val MILLIS_PER_DAY = 86_400_000L

/** The picker reports UTC midnight, so read it back in UTC or the day can shift. */
private fun Long.toLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()
