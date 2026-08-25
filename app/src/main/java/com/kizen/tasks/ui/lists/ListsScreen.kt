package com.kizen.tasks.ui.lists

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kizen.tasks.ui.components.SoftCard
import com.kizen.tasks.ui.theme.CreamBg
import com.kizen.tasks.ui.theme.CreamCard
import com.kizen.tasks.ui.theme.CreamYellow
import com.kizen.tasks.ui.theme.KizenTypography
import com.kizen.tasks.ui.theme.Lavender
import com.kizen.tasks.ui.theme.Mint
import com.kizen.tasks.ui.theme.PinkSoft
import com.kizen.tasks.ui.theme.Sky
import com.kizen.tasks.ui.theme.TextMain
import com.kizen.tasks.ui.theme.TextMute
import com.kizen.tasks.ui.theme.parseHexColor

private val pastelColors = listOf("#C7CEEA", "#A8D8EA", "#B5EAD7", "#F8C8DC", "#FFF2CC")
private val emojis = listOf("🌸", "☁️", "🍓", "💖", "✨", "🌙", "🍀", "🎀")

@Composable
fun ListsScreen(
    onBack: () -> Unit,
    viewModel: ListsViewModel = hiltViewModel(),
) {
    val lists by viewModel.lists.collectAsStateWithLifecycle()
    var name by remember { mutableStateOf("") }
    var emoji by remember { mutableStateOf("🌸") }
    var color by remember { mutableStateOf(pastelColors.first()) }

    Scaffold(
        containerColor = CreamBg,
        topBar = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 36.dp, start = 4.dp, end = 16.dp, bottom = 8.dp),
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Volver")
                }
                Text("Tus rincones", style = KizenTypography.headlineSmall)
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                SoftCard(modifier = Modifier.fillMaxWidth(), color = Lavender.copy(alpha = 0.45f)) {
                    Text("Nuevo rincón", style = KizenTypography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Nombre suave") },
                        singleLine = true,
                        shape = RoundedCornerShape(22.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = CreamCard,
                            unfocusedContainerColor = CreamCard,
                            focusedBorderColor = PinkSoft,
                            unfocusedBorderColor = PinkSoft.copy(alpha = 0.35f),
                        ),
                    )
                    Spacer(Modifier.height(10.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(emojis) { item ->
                            FilterChip(
                                selected = emoji == item,
                                onClick = { emoji = item },
                                label = { Text(item) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PinkSoft),
                                border = null,
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(pastelColors) { hex ->
                            FilterChip(
                                selected = color == hex,
                                onClick = { color = hex },
                                label = { Text("  ") },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = parseHexColor(hex),
                                    selectedContainerColor = parseHexColor(hex),
                                ),
                                border = null,
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = {
                            viewModel.add(name, emoji, color)
                            name = ""
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PinkSoft, contentColor = TextMain),
                        shape = RoundedCornerShape(20.dp),
                    ) {
                        Text("Crear rincón")
                    }
                }
            }
            items(lists, key = { it.id }) { list ->
                SoftCard(
                    modifier = Modifier.fillMaxWidth(),
                    color = parseHexColor(list.colorHex).copy(alpha = 0.55f),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("${list.emoji}  ${list.name}", style = KizenTypography.titleLarge)
                            Text(
                                "${list.doneCount}/${list.taskCount} hechas",
                                style = KizenTypography.bodyMedium,
                                color = TextMute,
                            )
                        }
                        if (lists.size > 1) {
                            IconButton(
                                onClick = { viewModel.delete(list.id) },
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(CreamCard),
                            ) {
                                Icon(Icons.Rounded.Delete, contentDescription = "Borrar", tint = TextMute)
                            }
                        }
                    }
                }
            }
        }
    }
}
