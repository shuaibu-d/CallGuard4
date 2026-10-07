@file:OptIn(ExperimentalMaterial3Api::class)

package app.callguard.ui

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.callguard.data.RuleEntity
import app.callguard.domain.Decision
import app.callguard.domain.MatchType
import app.callguard.domain.PhoneNumberNormalizer
import app.callguard.domain.RuleAction

private data class Preset(val name: String, val type: MatchType, val pattern: String)

private val presets = listOf(
    Preset("Lagos 020 numbers", MatchType.PREFIX, "020"),
    Preset("Outside Nigeria", MatchType.INTERNATIONAL, ""),
    Preset("Private numbers", MatchType.PRIVATE, ""),
)

private fun actionLabel(a: RuleAction) = if (a == RuleAction.BLOCK) "Block" else "Silence"

private fun describe(r: RuleEntity): String {
    val what = when (r.matchType) {
        MatchType.PREFIX -> "Starts with ${r.pattern}"
        MatchType.EXACT -> "Exactly ${r.pattern}"
        MatchType.CONTAINS -> "Contains ${r.pattern}"
        MatchType.INTERNATIONAL -> "Numbers from outside Nigeria"
        MatchType.PRIVATE -> "Private or hidden numbers"
    }
    return "$what · ${actionLabel(r.action)}"
}

@Composable
fun CallGuardApp(
    vm: MainViewModel,
    roleHeld: Boolean,
    onRequestRole: () -> Unit,
    onSetNeverBlockContacts: (Boolean) -> Unit,
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == 0, onClick = { tab = 0 },
                    icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
                    label = { Text("Rules") },
                )
                NavigationBarItem(
                    selected = tab == 1, onClick = { tab = 1 },
                    icon = { Icon(Icons.Default.Call, contentDescription = null) },
                    label = { Text("Blocked") },
                )
                NavigationBarItem(
                    selected = tab == 2, onClick = { tab = 2 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text("Settings") },
                )
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (tab) {
                0 -> RulesScreen(vm, roleHeld, onRequestRole)
                1 -> BlockedScreen(vm)
                else -> SettingsScreen(vm, onSetNeverBlockContacts)
            }
        }
    }
}

@Composable
private fun RulesScreen(vm: MainViewModel, roleHeld: Boolean, onRequestRole: () -> Unit) {
    val rules by vm.rules.collectAsStateWithLifecycle()
    val protection by vm.protection.collectAsStateWithLifecycle()
    val week by vm.weekCount.collectAsStateWithLifecycle()
    val defaultAction by vm.defaultAction.collectAsStateWithLifecycle()
    var dialogOpen by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<RuleEntity?>(null) }
    var testNumber by rememberSaveable { mutableStateOf("") }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { StatusCard(roleHeld, protection, week, onRequestRole) }

            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Test a number", style = MaterialTheme.typography.titleMedium)
                        OutlinedTextField(
                            value = testNumber,
                            onValueChange = { testNumber = it },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("+234 20 1234 5678") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        )
                        if (testNumber.isNotBlank()) {
                            val decision = vm.test(testNumber, rules)
                            val blocked = decision is Decision.Apply
                            Text(
                                text = when (decision) {
                                    is Decision.Apply ->
                                        "${actionLabel(decision.rule.action)} by “${decision.rule.name}”"
                                    is Decision.Allow ->
                                        if (decision.reason == "allowed") "Allowed: on your allowed list"
                                        else "Allowed: no rule matches"
                                },
                                color = if (blocked) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Text(
                                "Read as ${PhoneNumberNormalizer.normalize(testNumber)}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Quick add", style = MaterialTheme.typography.titleSmall)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(presets) { p ->
                            AssistChip(
                                onClick = { vm.addPreset(p.name, p.type, p.pattern) },
                                label = { Text(p.name) },
                            )
                        }
                    }
                }
            }

            if (rules.isEmpty()) {
                item {
                    Text(
                        "No rules yet. Add one above or tap Add rule.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            } else {
                items(rules, key = { it.id }) { rule ->
                    Card(
                        onClick = { editing = rule; dialogOpen = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(rule.name, style = MaterialTheme.typography.titleMedium)
                                Text(describe(rule), style = MaterialTheme.typography.bodySmall)
                            }
                            Switch(
                                checked = rule.enabled,
                                onCheckedChange = { vm.setRuleEnabled(rule.id, it) },
                            )
                            IconButton(onClick = { vm.deleteRule(rule.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete rule")
                            }
                        }
                    }
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = { editing = null; dialogOpen = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            icon = { Icon(Icons.Default.Add, contentDescription = null) },
            text = { Text("Add rule") },
        )
    }

    if (dialogOpen) {
        RuleDialog(
            initial = editing,
            defaultAction = defaultAction,
            onDismiss = { dialogOpen = false },
            onSave = { name, pattern, type, action ->
                vm.saveRule(editing, name, pattern, type, action)
                dialogOpen = false
            },
        )
    }
}

@Composable
private fun StatusCard(roleHeld: Boolean, protection: Boolean, week: Int, onRequestRole: () -> Unit) {
    val ok = roleHeld && protection
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (ok) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.errorContainer,
        ),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            when {
                !roleHeld -> {
                    Text("Call screening is off", style = MaterialTheme.typography.titleMedium)
                    Text("Let CallGuard screen incoming calls. Your rules stay on this phone.")
                    Button(onClick = onRequestRole) { Text("Turn on") }
                }
                !protection -> {
                    Text("Protection paused", style = MaterialTheme.typography.titleMedium)
                    Text("Turn it back on in Settings.")
                }
                else -> {
                    Text("Protection on", style = MaterialTheme.typography.titleMedium)
                    Text("$week calls handled this week")
                }
            }
        }
    }
}

@Composable
private fun RuleDialog(
    initial: RuleEntity?,
    defaultAction: RuleAction,
    onDismiss: () -> Unit,
    onSave: (String, String, MatchType, RuleAction) -> Unit,
) {
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var pattern by remember { mutableStateOf(initial?.pattern.orEmpty()) }
    var type by remember { mutableStateOf(initial?.matchType ?: MatchType.PREFIX) }
    var action by remember { mutableStateOf(initial?.action ?: defaultAction) }
    val needsPattern = type == MatchType.PREFIX || type == MatchType.EXACT || type == MatchType.CONTAINS
    val valid = !needsPattern || pattern.any { it.isDigit() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Add rule" else "Edit rule") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Rule name (optional)") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (needsPattern) {
                    Text("Match", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp))
                    RadioRow(type == MatchType.PREFIX, "Starts with") { type = MatchType.PREFIX }
                    RadioRow(type == MatchType.EXACT, "Exact number") { type = MatchType.EXACT }
                    RadioRow(type == MatchType.CONTAINS, "Contains") { type = MatchType.CONTAINS }
                    OutlinedTextField(
                        value = pattern, onValueChange = { pattern = it },
                        label = { Text("Number or prefix") }, singleLine = true,
                        supportingText = { Text("For example 020 or +23420. A trailing * is optional.") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Text("Action", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp))
                RadioRow(action == RuleAction.SILENCE, "Silence (still shows in your call log)") {
                    action = RuleAction.SILENCE
                }
                RadioRow(action == RuleAction.BLOCK, "Block (rejects the call)") { action = RuleAction.BLOCK }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(name.trim(), pattern.trim(), type, action) }, enabled = valid) {
                Text("Save")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun RadioRow(selected: Boolean, label: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(Modifier.width(8.dp))
        Text(label)
    }
}

@Composable
private fun BlockedScreen(vm: MainViewModel) {
    val history by vm.history.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item { Text("Blocked and silenced calls", style = MaterialTheme.typography.titleLarge) }
        if (history.isEmpty()) {
            item { Text("Nothing yet. Calls your rules catch will show up here.") }
        }
        items(history, key = { it.id }) { h ->
            Card(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(PhoneNumberNormalizer.mask(h.number), style = MaterialTheme.typography.titleMedium)
                        val whenText = DateUtils.formatDateTime(
                            context, h.timestamp,
                            DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_ABBREV_ALL,
                        )
                        Text(
                            "${actionLabel(h.action)}d by ${h.ruleName} · $whenText",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    if (h.number.isNotEmpty()) {
                        TextButton(onClick = { vm.allowFromHistory(h) }) { Text("Not spam") }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(vm: MainViewModel, onSetNeverBlockContacts: (Boolean) -> Unit) {
    val protection by vm.protection.collectAsStateWithLifecycle()
    val contacts by vm.neverBlockContacts.collectAsStateWithLifecycle()
    val defaultAction by vm.defaultAction.collectAsStateWithLifecycle()
    val allowed by vm.allowed.collectAsStateWithLifecycle()

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item { Text("Settings", style = MaterialTheme.typography.titleLarge) }
        item { SwitchRow("Protection", "Screen incoming calls with your rules", protection) { vm.setProtection(it) } }
        item {
            SwitchRow("Never block my contacts", "Needs access to your contacts", contacts) {
                onSetNeverBlockContacts(it)
            }
        }
        item {
            Column {
                Text("Default action for new rules", style = MaterialTheme.typography.titleSmall)
                RadioRow(defaultAction == RuleAction.SILENCE, "Silence") { vm.setDefaultAction(RuleAction.SILENCE) }
                RadioRow(defaultAction == RuleAction.BLOCK, "Block") { vm.setDefaultAction(RuleAction.BLOCK) }
            }
        }
        item { Text("Allowed numbers", style = MaterialTheme.typography.titleSmall) }
        if (allowed.isEmpty()) {
            item { Text("None. Tap “Not spam” on a blocked call to add one.", style = MaterialTheme.typography.bodySmall) }
        }
        items(allowed, key = { it.number }) { a ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(a.number, Modifier.weight(1f))
                IconButton(onClick = { vm.removeAllowed(a.number) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove from allowed list")
                }
            }
        }
        item { OutlinedButton(onClick = { vm.clearHistory() }) { Text("Clear blocked history") } }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Privacy", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Your calls stay on your phone. CallGuard has no internet permission, no account, " +
                            "and uploads nothing. History is kept for 30 days.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
