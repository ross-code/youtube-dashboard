package com.duoplan.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.duoplan.app.data.model.Plan
import com.duoplan.app.data.model.PlanStatus
import com.duoplan.app.ui.asWhenLabel
import com.duoplan.app.ui.emoji
import com.duoplan.app.ui.label

@Composable
fun PlanCard(plan: Plan, me: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(plan.category.emoji(), style = MaterialTheme.typography.headlineMedium)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    plan.title.ifBlank { "(untitled)" },
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    plan.start.asWhenLabel(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    if (plan.isMine(me)) "From you" else "From your partner",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            StatusChip(plan.status)
        }
    }
}

@Composable
fun StatusChip(status: PlanStatus) {
    val (bg, fg) = when (status) {
        PlanStatus.PENDING -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        PlanStatus.APPROVED -> Color(0xFFDDF3E4) to Color(0xFF1B5E20)
        PlanStatus.DENIED -> Color(0xFFFBE0E0) to Color(0xFF8E1E1E)
        PlanStatus.REVISION_REQUESTED -> Color(0xFFFDF0D5) to Color(0xFF7A4F00)
    }
    Surface(color = bg, shape = RoundedCornerShape(50)) {
        Text(
            status.label(),
            color = fg,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}
