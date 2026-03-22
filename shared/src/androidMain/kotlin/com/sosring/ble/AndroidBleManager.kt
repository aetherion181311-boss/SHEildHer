package com.sosring.ble

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.os.ParcelUuid
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import java.util.UUID

@SuppressLint("MissingPermission")
class AndroidBleManager(private val context: Context) : BleManager {

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? get() = bluetoothManager.adapter
    private val scanner: BluetoothLeScanner? get() = bluetoothAdapter?.bluetoothLeScanner

    private var gatt: BluetoothGatt? = null
    private var ackCharacteristic: BluetoothGattCharacteristic? = null

    private val _connectionState = MutableStateFlow<BleConnectionState>(BleConnectionState.Disconnected)
    override val connectionState: StateFlow<BleConnectionState> = _connectionState.asStateFlow()

    private val _touchPatterns = MutableSharedFlow<TouchPattern>(extraBufferCapacity = 8)
    override val touchPatterns: SharedFlow<TouchPattern> = _touchPatterns.asSharedFlow()

    private val serviceUuid  = UUID.fromString(BleConstants.SOS_SERVICE_UUID)
    private val touchCharUuid = UUID.fromString(BleConstants.TOUCH_PATTERN_CHAR_UUID)
    private val ackCharUuid   = UUID.fromString(BleConstants.ACK_CHAR_UUID)

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    _connectionState.value = BleConnectionState.Connected
                    gatt.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    _connectionState.value = BleConnectionState.Disconnected
                    this@AndroidBleManager.gatt = null
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) return
            val service = gatt.getService(serviceUuid) ?: return
            ackCharacteristic = service.getCharacteristic(ackCharUuid)

            // Enable notifications on touch-pattern characteristic
            val touchChar = service.getCharacteristic(touchCharUuid) ?: return
            gatt.setCharacteristicNotification(touchChar, true)
            val descriptor = touchChar.getDescriptor(
                UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
            )
            descriptor?.let {
                it.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                gatt.writeDescriptor(it)
            }
        }

        @Deprecated("Deprecated in API 33", ReplaceWith("onCharacteristicChanged(gatt, characteristic, value)"))
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            if (characteristic.uuid == touchCharUuid) {
                decodePattern(characteristic.value)
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            if (characteristic.uuid == touchCharUuid) {
                decodePattern(value)
            }
        }
    }

    private fun decodePattern(bytes: ByteArray) {
        val pattern = when (bytes.firstOrNull()) {
            BleConstants.TRIGGER_PATTERN -> TouchPattern.Trigger
            BleConstants.CANCEL_PATTERN  -> TouchPattern.Cancel
            else                          -> TouchPattern.Unknown(bytes)
        }
        _touchPatterns.tryEmit(pattern)
    }

    override suspend fun startScanAndConnect() {
        if (_connectionState.value == BleConnectionState.Connected ||
            _connectionState.value == BleConnectionState.Scanning) return

        _connectionState.value = BleConnectionState.Scanning

        val filter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(serviceUuid))
            .build()
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
            .build()

        val device = withTimeoutOrNull(15_000) {
            scanForDevice(filter, settings)
        }

        if (device == null) {
            _connectionState.value = BleConnectionState.Error("Ring not found – ensure it is nearby and charged.")
            return
        }

        _connectionState.value = BleConnectionState.Connecting
        gatt = device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
    }

    private suspend fun scanForDevice(
        filter: ScanFilter,
        settings: ScanSettings
    ): BluetoothDevice? = suspendCancellableCoroutine { cont ->
        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                scanner?.stopScan(this)
                cont.resumeWith(Result.success(result.device))
            }
        }
        scanner?.startScan(listOf(filter), settings, callback)
        cont.invokeOnCancellation { scanner?.stopScan(callback) }
    }

    @SuppressLint("MissingPermission")
    override suspend fun sendAcknowledgement(ack: Byte): Boolean {
        val char = ackCharacteristic ?: return false
        val g = gatt ?: return false
        return withContext(Dispatchers.IO) {
            char.value = byteArrayOf(ack)
            char.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
            g.writeCharacteristic(char)
        }
    }

    override fun disconnect() {
        gatt?.disconnect()
        gatt?.close()
        gatt = null
        _connectionState.value = BleConnectionState.Disconnected
    }
}
