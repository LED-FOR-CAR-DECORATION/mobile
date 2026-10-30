package com.carled.app.presentation.screens.firmware

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.carled.app.presentation.components.*

@Composable
fun FirmwareUpdateScreen(onClose: () -> Unit, vm: FirmwareViewModel=viewModel()) {
    LaunchedEffect(Unit) { vm.checkUpdate() }
    val state by vm.state.collectAsState()
    var ssid by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf(false) }
    Scaffold(topBar={ MirageBar("Cập nhật firmware",actions={ TextButton(onClick=onClose,enabled=!state.updating) { Text("Đóng") } }) },containerColor=MirageBackground) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            MirageCard {
                Text("Phần mềm thiết bị LED",style=MaterialTheme.typography.titleLarge)
                state.info?.let { Text("Phiên bản: ${it.version} · ${it.hardware}"); Text("Mã thiết bị: ${it.deviceId}"); Text(if(it.wifi) "LED đã kết nối Wi-Fi" else "LED chưa kết nối Wi-Fi") }
                Text(state.message)
                state.error?.let { Text(it,color=MaterialTheme.colorScheme.error) }
                if(state.busy) LinearProgressIndicator(modifier=Modifier.fillMaxWidth())
                MirageButton("Kiểm tra bản cập nhật",vm::checkUpdate,Modifier.fillMaxWidth(),enabled=!state.busy,secondary=true)
            }
            MirageCard {
                Text("Wi-Fi cho LED",style=MaterialTheme.typography.titleMedium)
                Text("Bật điểm phát sóng Internet 2.4 GHz trên điện thoại hoặc dùng Wi-Fi có Internet. Điện thoại vẫn kết nối Bluetooth với LED.")
                OutlinedTextField(ssid,{ssid=it},label={Text("Tên Wi-Fi / hotspot")},singleLine=true,enabled=!state.busy,modifier=Modifier.fillMaxWidth())
                OutlinedTextField(password,{password=it},label={Text("Mật khẩu Wi-Fi")},visualTransformation=PasswordVisualTransformation(),singleLine=true,enabled=!state.busy,modifier=Modifier.fillMaxWidth())
                MirageButton("Kết nối Wi-Fi cho LED",{vm.connectWifi(ssid,password); password=""},Modifier.fillMaxWidth(),enabled=!state.busy && ssid.isNotBlank())
            }
            state.releaseVersion?.let { version ->
                MirageCard {
                    Text("Bản phát hành $version",style=MaterialTheme.typography.titleMedium)
                    Text("Đỗ xe và giữ nguồn LED ổn định. LED sẽ tải bản cập nhật qua Wi-Fi rồi tự khởi động lại. Giữ ứng dụng mở để xác nhận kết quả.")
                    MirageButton("Cập nhật lên $version",{confirm=true},Modifier.fillMaxWidth(),enabled=!state.busy && state.info?.wifi==true)
                }
            }
        }
    }
    if(confirm) AlertDialog(onDismissRequest={confirm=false},title={Text("Cập nhật firmware LED?")},text={Text("Xác nhận xe đã đỗ và nguồn LED ổn định. Không rút nguồn trong khi cập nhật.")},confirmButton={ TextButton(onClick={confirm=false;vm.install()}){Text("Bắt đầu cập nhật")} },dismissButton={ TextButton(onClick={confirm=false}){Text("Hủy")} })
}
