package com.stanleycx.fruitos.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay

/**
 * Petit dialogue simple et propre pour renommer un dossier
 * depuis le menu contextuel (style Fruit OS / Material épuré).
 */
@Composable
internal fun FolderRenameDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var textFieldValue by remember {
        mutableStateOf(
            TextFieldValue(
                text = currentName,
                selection = TextRange(0, currentName.length) // sélectionne tout le texte
            )
        )
    }

    val focusRequester = remember { FocusRequester() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Renommer le dossier")
        },
        text = {
            TextField(
                value = textFieldValue,
                onValueChange = { textFieldValue = it },
                singleLine = true,
                label = { Text("Nom du dossier") },
                modifier = Modifier
                    .focusRequester(focusRequester)
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(textFieldValue.text)
                },
                enabled = textFieldValue.text.isNotBlank()
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    )

    // Demande le focus et sélectionne le texte après l'ouverture du dialogue
    LaunchedEffect(Unit) {
        delay(100) // petit délai pour que le dialogue soit bien affiché
        focusRequester.requestFocus()
    }
}

/** Dialogue de renommage d'un widget + option « Masquer le nom ». */
@Composable
internal fun WidgetRenameDialog(
    currentName: String,
    hidden: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String, Boolean) -> Unit
) {
    var textFieldValue by remember {
        mutableStateOf(TextFieldValue(text = currentName, selection = TextRange(0, currentName.length)))
    }
    var hide by remember { mutableStateOf(hidden) }
    val focusRequester = remember { FocusRequester() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Renommer le widget") },
        text = {
            Column {
                TextField(
                    value = textFieldValue,
                    onValueChange = { textFieldValue = it },
                    singleLine = true,
                    enabled = !hide,
                    label = { Text("Nom du widget") },
                    modifier = Modifier.focusRequester(focusRequester)
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { hide = !hide }
                ) {
                    Text("Masquer le nom", modifier = Modifier.weight(1f))
                    Switch(checked = hide, onCheckedChange = { hide = it })
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(textFieldValue.text, hide) }) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        },
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
    )

    LaunchedEffect(Unit) {
        delay(100)
        if (!hidden) focusRequester.requestFocus()
    }
}
