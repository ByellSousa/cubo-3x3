package com.gabs.cubo3x3.ui

import android.app.TimePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gabs.cubo3x3.domain.reminder.ReminderDay
import com.gabs.cubo3x3.domain.reminder.ReminderSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderScreen(
    settings: ReminderSettings,
    permissionGranted: Boolean,
    statusMessage: String?,
    onSave: (ReminderSettings) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var enabled by remember(settings) { mutableStateOf(settings.enabled) }
    var hour by remember(settings) { mutableIntStateOf(settings.hour) }
    var minute by remember(settings) { mutableIntStateOf(settings.minute) }
    var days by remember(settings) { mutableStateOf(settings.days) }
    var validationMessage by remember(settings) { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Lembretes locais") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Voltar") } },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Text(
                    "Escolha quando o aparelho deve lembrar você de praticar. O agendamento é local e funciona sem internet.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Lembrar de praticar", style = MaterialTheme.typography.titleMedium)
                            Text(
                                if (enabled) "Ativado" else "Desativado",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(checked = enabled, onCheckedChange = { enabled = it })
                    }
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Horário", style = MaterialTheme.typography.titleMedium)
                    OutlinedButton(
                        onClick = {
                            TimePickerDialog(
                                context,
                                { _, selectedHour, selectedMinute ->
                                    hour = selectedHour
                                    minute = selectedMinute
                                },
                                hour,
                                minute,
                                true,
                            ).show()
                        },
                    ) {
                        Text("${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}")
                    }
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Dias da semana", style = MaterialTheme.typography.titleMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(ReminderDay.entries) { day ->
                            FilterChip(
                                selected = day in days,
                                onClick = {
                                    days = if (day in days) days - day else days + day
                                    validationMessage = null
                                },
                                label = { Text(day.shortLabel) },
                            )
                        }
                    }
                }
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text("Permissão de notificação", fontWeight = FontWeight.Medium)
                        Text(
                            if (permissionGranted) {
                                "Autorizada neste aparelho."
                            } else {
                                "Será solicitada somente ao salvar um lembrete ativado."
                            },
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            (validationMessage ?: statusMessage)?.let { message ->
                item {
                    Text(message, color = MaterialTheme.colorScheme.primary)
                }
            }
            item {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        if (enabled && days.isEmpty()) {
                            validationMessage = "Selecione ao menos um dia da semana."
                        } else {
                            validationMessage = null
                            onSave(ReminderSettings(enabled, hour, minute, days))
                        }
                    },
                ) {
                    Text("Salvar lembrete")
                }
            }
            item {
                Text(
                    "O Android pode ajustar alguns minutos do horário para economizar bateria.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
