package com.kaonixx.guitarix

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.os.Handler
import android.os.Looper

class BleScanner(private val context: Context) {

    private val bluetoothManager: BluetoothManager? by lazy {
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    }

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        bluetoothManager?.adapter
    }

    private val bluetoothLeScanner: BluetoothLeScanner? by lazy {
        bluetoothAdapter?.bluetoothLeScanner
    }

    data class BleDevice(val name: String, val address: String, val rssi: Int)

    private var scanCallback: ScanCallback? = null
    private val handler = Handler(Looper.getMainLooper())

    fun isBleAvailable(): Boolean = bluetoothAdapter != null && bluetoothAdapter!!.isEnabled

    fun startScan(durationMs: Long = 5000L, onDeviceFound: (BleDevice) -> Unit, onFinish: () -> Unit) {
        if (!isBleAvailable() || bluetoothLeScanner == null) {
            onFinish()
            return
        }

        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val device = result.device
                val name = device.name ?: "Unknown"
                if (name.isNotBlank()) {
                    onDeviceFound(BleDevice(name, device.address, result.rssi))
                }
            }

            override fun onScanFailed(errorCode: Int) {
                onFinish()
            }
        }

        scanCallback = callback
        bluetoothLeScanner?.startScan(callback)

        handler.postDelayed({
            stopScan()
            onFinish()
        }, durationMs)
    }

    fun stopScan() {
        scanCallback?.let { bluetoothLeScanner?.stopScan(it) }
        scanCallback = null
    }
}
