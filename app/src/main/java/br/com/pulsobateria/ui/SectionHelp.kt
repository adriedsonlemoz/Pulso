package br.com.pulsobateria.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*

/** Explicações extensas ficam acessíveis, sem ocupar o fluxo principal. */
@Composable
fun SectionHelpButton(title: String, explanation: String) {
 var open by remember { mutableStateOf(false) }
 IconButton(onClick={open=true}) { Icon(Icons.Default.Info,contentDescription="Ajuda: $title") }
 if(open) AlertDialog(
  onDismissRequest={open=false},
  title={Text(title)},
  text={Text(explanation)},
  confirmButton={TextButton(onClick={open=false}) { Text("Fechar") }}
 )
}
