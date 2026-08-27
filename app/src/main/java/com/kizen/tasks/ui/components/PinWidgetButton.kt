package com.kizen.tasks.ui.components

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.kizen.tasks.ui.theme.KizenTypography
import com.kizen.tasks.ui.theme.PinkDeep
import com.kizen.tasks.ui.theme.PinkSoft
import com.kizen.tasks.ui.theme.Sky
import com.kizen.tasks.ui.theme.TextMain
import com.kizen.tasks.widget.WidgetPin

@Composable
fun PinWidgetButton(
    label: String,
    onPin: () -> String,
    filled: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val colors = if (filled) {
        ButtonDefaults.buttonColors(containerColor = Sky, contentColor = TextMain)
    } else {
        ButtonDefaults.buttonColors(containerColor = PinkSoft, contentColor = TextMain)
    }
    Button(
        onClick = {
            Toast.makeText(context, onPin(), Toast.LENGTH_LONG).show()
        },
        modifier = modifier.fillMaxWidth(),
        colors = colors,
        shape = RoundedCornerShape(22.dp),
    ) {
        Text(label)
    }
}

@Composable
fun WidgetPinRow(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    fun toast(message: String) = Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        TextButton(onClick = { toast(WidgetPin.pinHabitsList(context)) }, modifier = Modifier.weight(1f)) {
            Text("Hábitos", color = PinkDeep, style = KizenTypography.labelLarge)
        }
        TextButton(onClick = { toast(WidgetPin.pinTasksList(context)) }, modifier = Modifier.weight(1f)) {
            Text("Tareas", color = PinkDeep, style = KizenTypography.labelLarge)
        }
        TextButton(onClick = { toast(WidgetPin.pinNudgesList(context)) }, modifier = Modifier.weight(1f)) {
            Text("Avisos", color = PinkDeep, style = KizenTypography.labelLarge)
        }
        TextButton(onClick = { toast(WidgetPin.pinToday(context)) }, modifier = Modifier.weight(1f)) {
            Text("Hoy", color = PinkDeep, style = KizenTypography.labelLarge)
        }
    }
}
