package com.example.myapplication

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun todayString(): String =
    SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()).format(Date())

fun peso(amount: Double): String = "₱${"%,.2f".format(amount)}"

/**
 * Single-line text that shrinks its font until it fits the available width,
 * so cards never need to grow/stretch on small screens.
 */
@Composable
fun AutoFitText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    maxFontSize: TextUnit = 16.sp,
    minFontSize: TextUnit = 9.sp,
    textAlign: TextAlign? = null,
    color: Color = Color.Unspecified
) {
    var fontSize by remember(text, maxFontSize) { mutableStateOf(maxFontSize) }

    Text(
        text = text,
        modifier = modifier,
        color = color,
        textAlign = textAlign,
        style = style.copy(fontSize = fontSize),
        maxLines = 1,
        softWrap = false,
        onTextLayout = { result ->
            if (result.didOverflowWidth && fontSize.value > minFontSize.value) {
                fontSize = (fontSize.value * 0.9f)
                    .coerceAtLeast(minFontSize.value).sp
            }
        }
    )
}

/**
 * Fixed-height card whose text scales down instead of stretching the card.
 * With an [icon] the layout is: round icon on the left, title + amount on the right.
 * If the card is too narrow the icon is hidden so the text keeps its room.
 */
@Composable
fun MoneyCard(
    title: String,
    amount: Double,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    iconTint: Color = Color(0xFF1C9999)
) {
    Card(
        modifier = modifier.height(84.dp),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {

            // Below this card width the text matters more than the icon
            val showIcon = icon != null && maxWidth >= 140.dp

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = if (showIcon) 6.dp else 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {

                if (showIcon && icon != null) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(iconTint.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(Modifier.width(6.dp))
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment =
                        if (showIcon) Alignment.Start else Alignment.CenterHorizontally
                ) {
                    AutoFitText(
                        text = title,
                        modifier = Modifier.fillMaxWidth(),
                        maxFontSize = 14.sp,
                        minFontSize = 9.sp,
                        textAlign = if (showIcon) TextAlign.Start else TextAlign.Center
                    )

                    Spacer(Modifier.height(4.dp))

                    AutoFitText(
                        text = peso(amount),
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.titleMedium,
                        maxFontSize = 18.sp,
                        minFontSize = 10.sp,
                        textAlign = if (showIcon) TextAlign.Start else TextAlign.Center
                    )
                }
            }
        }
    }
}

/** Home screen action button: one line, text shrinks instead of wrapping. */
@Composable
fun HomeActionButton(
    text: String,
    modifier: Modifier = Modifier,
    containerColor: Color? = null,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        contentPadding = PaddingValues(horizontal = 6.dp),
        colors = if (containerColor != null)
            ButtonDefaults.buttonColors(containerColor = containerColor)
        else
            ButtonDefaults.buttonColors()
    ) {
        AutoFitText(
            text = text,
            modifier = Modifier.fillMaxWidth(),
            maxFontSize = 14.sp,
            minFontSize = 10.sp,
            textAlign = TextAlign.Center
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateField(
    date: String,
    onDateChange: (String) -> Unit
) {
    var showPicker by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = date,
        onValueChange = {},
        label = { Text("Date") },
        readOnly = true,
        trailingIcon = {
            IconButton(onClick = { showPicker = true }) {
                Text("📅")
            }
        }
    )

    if (showPicker) {
        val state = rememberDatePickerState()

        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        onDateChange(
                            SimpleDateFormat(
                                "MM/dd/yyyy",
                                Locale.getDefault()
                            ).format(Date(millis))
                        )
                    }
                    showPicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = state)
        }
    }
}