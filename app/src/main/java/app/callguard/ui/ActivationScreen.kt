package app.callguard.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import app.callguard.license.LicenseConfig

@Composable
fun ActivationScreen(deviceId: String, onActivate: (String) -> Boolean) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var code by rememberSaveable { mutableStateOf("") }
    var failed by remember { mutableStateOf(false) }
    val request = "Hello, I want to activate CallGuard. My Device ID is: $deviceId"

    Scaffold { padding ->
        Column(
            Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Activate CallGuard", style = MaterialTheme.typography.headlineSmall)
            Text(
                "CallGuard needs an activation code that is made for this phone only. " +
                    "Send your Device ID to get your code.",
            )
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Your Device ID", style = MaterialTheme.typography.labelMedium)
                    Text(deviceId, style = MaterialTheme.typography.titleLarge, fontFamily = FontFamily.Monospace)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { clipboard.setText(AnnotatedString(deviceId)) }) { Text("Copy") }
                        OutlinedButton(onClick = {
                            val send = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, request)
                            }
                            context.startActivity(Intent.createChooser(send, "Send Device ID"))
                        }) { Text("Share") }
                    }
                }
            }
            if (LicenseConfig.SELLER_WHATSAPP.isNotBlank()) {
                Button(
                    onClick = {
                        val url = "https://wa.me/${LicenseConfig.SELLER_WHATSAPP}?text=${Uri.encode(request)}"
                        try {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        } catch (e: ActivityNotFoundException) {
                            // no app can open the link
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Get my code on WhatsApp") }
            }
            OutlinedTextField(
                value = code,
                onValueChange = { code = it; failed = false },
                label = { Text("Activation code") },
                minLines = 3,
                isError = failed,
                modifier = Modifier.fillMaxWidth(),
            )
            if (failed) {
                Text("That code is not valid for this phone.", color = MaterialTheme.colorScheme.error)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    clipboard.getText()?.text?.let { code = it; failed = false }
                }) { Text("Paste") }
                Button(onClick = { failed = !onActivate(code) }, enabled = code.isNotBlank()) { Text("Activate") }
            }
        }
    }
}
