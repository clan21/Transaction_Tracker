package com.example.myapplication

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun TransactionRow(
    transaction: Transaction,
    onToggle: () -> Unit,
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {},
    isExpanded: Boolean = false,
    onExpand: () -> Unit = {},
    onCollapse: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandHorizontally() + fadeIn(),
            exit = shrinkHorizontally() + fadeOut()
        ) {

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {

                Button(
                    onClick = {
                        onEdit()
                    },
                    modifier = Modifier.height(40.dp),
                    contentPadding = PaddingValues(
                        horizontal = 10.dp
                    )
                ) {
                    Text("Edit")
                }

                Button(
                    onClick = {
                        onDelete()
                    },
                    modifier = Modifier.height(40.dp),
                    contentPadding = PaddingValues(
                        horizontal = 10.dp
                    ),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF44336)
                    )
                ) {
                    Text("Delete")
                }
            }
        }

        if (isExpanded) {
            Spacer(Modifier.width(6.dp))
        }

        Row(
            modifier = Modifier
                .weight(1f)
                .combinedClickable(

                    onClick = {
                        if (isExpanded) {
                            onCollapse()
                        }
                    },

                    onLongClick = {
                        onExpand()
                    }
                ),

            verticalAlignment = Alignment.CenterVertically
        ) {

            Checkbox(
                checked = transaction.isChecked,
                onCheckedChange = {
                    onToggle()
                }
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    transaction.details
                )

                Text(
                    "${transaction.date} • ${transaction.paymentMethod}",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Text(
                when (transaction.type) {

                    "income", "personalIncome" ->
                        "+₱${"%.2f".format(transaction.amount)}"

                    "expense", "personal" ->
                        "-₱${"%.2f".format(transaction.amount)}"

                    else ->
                        "₱${"%.2f".format(transaction.amount)}"
                },

                color = when (transaction.type) {

                    "income", "personalIncome" ->
                        Color(0xFF4CAF50)

                    "expense", "personal" ->
                        Color(0xFFF44336)

                    else ->
                        MaterialTheme.colorScheme.onSurface
                }
            )
        }
    }

    HorizontalDivider()
}