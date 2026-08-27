package com.kizen.tasks.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kizen.tasks.ui.components.SoftCard
import com.kizen.tasks.ui.theme.CreamBg
import com.kizen.tasks.ui.theme.CreamCard
import com.kizen.tasks.ui.theme.KizenTypography
import com.kizen.tasks.ui.theme.Lavender
import com.kizen.tasks.ui.theme.PinkDeep
import com.kizen.tasks.ui.theme.PinkSoft
import com.kizen.tasks.ui.theme.Sky
import com.kizen.tasks.ui.theme.TextMain
import com.kizen.tasks.ui.theme.TextMute

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

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
                Text("Nube y Alexa", style = KizenTypography.headlineSmall)
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            SoftCard(color = Lavender.copy(alpha = 0.45f), modifier = Modifier.fillMaxWidth()) {
                Text("Sin prisas, con copia", style = KizenTypography.titleMedium)
                Text(
                    "Activa la nube para que Alexa y otro teléfono vean lo mismo. Las claves de IA viven en el servidor, nunca aquí.",
                    style = KizenTypography.bodyMedium,
                    color = TextMute,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }

            SoftCard(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Sincronizar", style = KizenTypography.titleMedium)
                        Text("Cada 15 minutos si hay red", style = KizenTypography.bodyMedium, color = TextMute)
                    }
                    Switch(
                        checked = state.enabled,
                        onCheckedChange = viewModel::onEnabled,
                        colors = SwitchDefaults.colors(checkedTrackColor = PinkSoft, checkedThumbColor = PinkDeep),
                    )
                }
            }

            SoftCard(modifier = Modifier.fillMaxWidth()) {
                Text("URL del servidor", style = KizenTypography.titleMedium)
                Text(
                    "Echo Dot no puede usar localhost. Pon aquí la URL HTTPS de Render/Railway. El emulador: http://10.0.2.2:8787",
                    style = KizenTypography.bodyMedium,
                    color = TextMute,
                    modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
                )
                OutlinedTextField(
                    value = state.baseUrl,
                    onValueChange = viewModel::onUrl,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(22.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = CreamCard,
                        unfocusedContainerColor = CreamCard,
                        focusedBorderColor = PinkSoft,
                        unfocusedBorderColor = PinkSoft.copy(alpha = 0.35f),
                    ),
                )
                Spacer(Modifier.height(8.dp))
                Text("Id de casa", style = KizenTypography.titleMedium)
                Text(
                    "Tiene que ser el mismo que en el servidor (kizen-casa) para que el Echo y el teléfono vean las mismas tareas.",
                    style = KizenTypography.bodyMedium,
                    color = TextMute,
                    modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
                )
                OutlinedTextField(
                    value = state.userId,
                    onValueChange = viewModel::onUserId,
                    enabled = true,
                    readOnly = false,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("kizen-casa") },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        imeAction = ImeAction.Done,
                    ),
                    shape = RoundedCornerShape(22.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = CreamCard,
                        unfocusedContainerColor = CreamCard,
                        disabledContainerColor = CreamCard,
                        focusedBorderColor = PinkSoft,
                        unfocusedBorderColor = PinkSoft.copy(alpha = 0.35f),
                    ),
                )
            }

            Button(
                onClick = viewModel::syncNow,
                enabled = state.enabled && !state.busy,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Sky, contentColor = TextMain),
                shape = RoundedCornerShape(22.dp),
            ) {
                Text(if (state.busy) "Un segundo…" else "Sincronizar ahora")
            }

            if (state.message.isNotBlank()) {
                Text(state.message, style = KizenTypography.bodyMedium, color = TextMute)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
