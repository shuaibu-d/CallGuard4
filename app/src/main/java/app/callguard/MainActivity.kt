package app.callguard

import android.Manifest
import android.app.role.RoleManager
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import app.callguard.license.LicenseManager
import app.callguard.ui.ActivationScreen
import app.callguard.ui.CallGuardApp
import app.callguard.ui.CallGuardTheme
import app.callguard.ui.MainViewModel

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()
    private var roleHeld by mutableStateOf(false)
    private var activated by mutableStateOf(false)

    private val roleLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { refreshRole() }

    private val contactsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            vm.setNeverBlockContacts(granted)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        refreshRole()
        activated = LicenseManager.isActivated(this)
        val deviceId = LicenseManager.deviceId(this)
        setContent {
            CallGuardTheme {
                if (activated) {
                    CallGuardApp(
                        vm = vm,
                        roleHeld = roleHeld,
                        onRequestRole = ::requestRole,
                        onSetNeverBlockContacts = ::setNeverBlockContacts,
                    )
                } else {
                    ActivationScreen(
                        deviceId = deviceId,
                        onActivate = { code ->
                            val ok = LicenseManager.activate(this, code)
                            if (ok) activated = true
                            ok
                        },
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshRole()
    }

    private fun refreshRole() {
        val rm = getSystemService(RoleManager::class.java)
        roleHeld = rm.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING) &&
            rm.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
    }

    private fun requestRole() {
        val rm = getSystemService(RoleManager::class.java)
        if (rm.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)) {
            roleLauncher.launch(rm.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING))
        }
    }

    private fun setNeverBlockContacts(enabled: Boolean) {
        if (!enabled) {
            vm.setNeverBlockContacts(false)
            return
        }
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) vm.setNeverBlockContacts(true) else contactsLauncher.launch(Manifest.permission.READ_CONTACTS)
    }
}
