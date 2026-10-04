package com.example.vehiclemaintenance.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
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
import androidx.compose.material3.TextFieldLabelPosition
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.vehiclemaintenance.R
import com.example.vehiclemaintenance.ui.theme.LocalBrandColors
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.flow.drop

/**
 * A two column form row: [first] fills the remaining width and [second] keeps a fixed width, so
 * the second column lines up on every row. When [first] is a labelled field, [second] drops by the
 * room its outline label takes above the border, so the two line up.
 */
@Composable
fun FormRow(
    first: @Composable () -> Unit,
    second: @Composable () -> Unit,
    firstHasOutlineLabel: Boolean = true,
) {
    val secondTop = if (firstHasOutlineLabel) OUTLINE_LABEL_OFFSET else 0.dp
    Row(horizontalArrangement = Arrangement.spacedBy(FormRowGap)) {
        Box(Modifier.weight(1f)) { first() }
        Box(Modifier.width(FormSecondColumnWidth).padding(top = secondTop)) { second() }
    }
}

/** The gap between form rows, and between a row's two columns. */
val FormRowGap = 12.dp

/** Wide enough for a unit dropdown, which is the widest second column cell. */
val FormSecondColumnWidth = 132.dp

private val OUTLINE_LABEL_OFFSET = 8.dp

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

/**
 * A text field whose label always sits in the outline, so a placeholder can show beneath it
 * while the field is still empty. Only the state based text field offers that label position.
 *
 * The field owns its text: it starts from [initialValue] and reports every edit to
 * [onValueChange], but never reads a later value back. Pushing the parent's value back in would
 * race the keyboard, since that value trails the typing and would wipe out the newest characters.
 * Forms show their fields only after loading, so the initial value is already the saved one.
 */
@Composable
fun OutlineLabelTextField(
    initialValue: String,
    onValueChange: (String) -> Unit,
    label: String,
    error: String?,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
) {
    val state = rememberTextFieldState(initialValue)
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    LaunchedEffect(state) {
        snapshotFlow { state.text.toString() }.drop(1).collect { currentOnValueChange(it) }
    }
    OutlinedTextField(
        state = state,
        labelPosition = TextFieldLabelPosition.Attached(alwaysMinimize = true),
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        isError = error != null,
        lineLimits = TextFieldLineLimits.SingleLine,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        supportingText = error?.let { { Text(it) } },
        modifier = modifier
            .fillMaxWidth()
            .then(rememberKeepAboveKeyboard(error))
            .semantics { if (error != null) error(error) },
    )
}

/**
 * Keeps a whole text field, error text included, above the keyboard while it has focus. On its
 * own a text field scrolls only its cursor line into view, which leaves the outline and any
 * error under the keyboard. Asks again as the keyboard slides in and when an error appears.
 */
@Composable
fun rememberKeepAboveKeyboard(error: String?): Modifier {
    val requester = remember { BringIntoViewRequester() }
    var focused by remember { mutableStateOf(false) }
    // Read only while focused, so the other fields don't recompose as the keyboard animates.
    val keyboardHeight = if (focused) WindowInsets.ime.getBottom(LocalDensity.current) else 0
    LaunchedEffect(focused, keyboardHeight, error) {
        if (focused) requester.bringIntoView()
    }
    return remember(requester) {
        Modifier
            .bringIntoViewRequester(requester)
            .onFocusChanged { focused = it.isFocused }
    }
}

/** For an input with no visible label, carries its description to screen readers. */
fun Modifier.describedAs(description: String, error: String?): Modifier = semantics {
    contentDescription = description
    if (error != null) error(error)
}

@Composable
fun ChooseDateButton(
    onClick: () -> Unit,
    error: String?,
    modifier: Modifier = Modifier,
    text: String,
) {
    Column(modifier) {
        FormCell {
            OutlinedButton(
                onClick = onClick,
                modifier = if (error != null) Modifier.semantics { error(error) } else Modifier,
                colors = brandOutlinedButtonColors(),
                border = brandOutlinedButtonBorder(enabled = true),
            ) {
                Text(text)
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
