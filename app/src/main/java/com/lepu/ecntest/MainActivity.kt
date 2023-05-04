package com.lepu.ecntest

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothProfile
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.jeremyliao.liveeventbus.LiveEventBus
import com.lepu.pocketecn.InterfaceEvent
import com.lepu.pocketecn.SlaveManager
import com.lepu.pocketecn.cmd.EcnBleCmd
import com.lepu.pocketecn.data.*
import com.lepu.pocketecn.gatt.GaServerManager
import com.lepu.pocketecn.service.SlaveService
import com.lepu.pocketecn.utils.bytesToHex
import kotlinx.android.synthetic.main.activity_main.*
import org.apache.commons.io.IOUtils
import org.json.JSONArray
import org.json.JSONObject
import java.io.File


class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"
    private lateinit var device: BluetoothDevice
    private var deviceName = "O2Ring 4444"
    private var fileList = ""
    private var ecnFileList = mutableListOf<FileList.BleFile>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        myRequestPermission()
        initView()
        initEvent()
        initData()
        Log.d(TAG, "deviceName: $deviceName")
    }

    private fun initView() {
        device_type.setOnCheckedChangeListener { group, checkedId ->
            when (checkedId) {
                R.id.o2ring -> {
                    deviceName = "O2Ring 4444"
                }
                R.id.ecn -> {
                    deviceName = "ECN 4444"
                }
            }
            Log.d(TAG, "deviceName: $deviceName")
            SlaveManager.closeSlaveBle()
            SlaveManager.initSlaveBle(this, deviceName)
        }
        wifi.setOnClickListener {
            startActivity(Intent(this, WifiActivity::class.java))
        }
        finish_activity.setOnClickListener {
            finish()
        }
        init_slave_ble.setOnClickListener {
//            GaServerManager.enableBleServices(this)
            SlaveManager.initSlaveBle(this, deviceName)
//            startService(Intent(this, SlaveService::class.java))
        }
        stop_advertise.setOnClickListener {
            SlaveManager.stopBleAdvertise()
        }
        close_slave_ble.setOnClickListener {
//            GaServerManager.disableBleServices(this)
            SlaveManager.closeSlaveBle()
//            stopService(Intent(this, SlaveService::class.java))
        }
        disconnect.setOnClickListener {
            SlaveManager.disconnect()
        }
        collect_time.setOnChronometerTickListener {
            val data = RtData()
            val status = RtStatus()
            status.status = 0
            status.duration = getChronometerSecond()
            data.status = status
            data.wave = ByteArray(200)
            SlaveManager.sendRtData(data)
        }
    }
    private fun initEvent() {
        LiveEventBus.get<InterfaceEvent>(InterfaceEvent.Ble.EventBleInitResult)
            .observe(this) {
                val data = it.data as Int
                when (data) {
                    Constant.BleInitResult.INIT_SUCCESS -> Toast.makeText(this, "初始化成功", Toast.LENGTH_SHORT).show()
                    Constant.BleInitResult.INIT_FAILED -> Toast.makeText(this, "初始化失败", Toast.LENGTH_SHORT).show()
                    Constant.BleInitResult.BLUETOOTH_DISABLED -> Toast.makeText(this, "蓝牙未启用", Toast.LENGTH_SHORT).show()
                    Constant.BleInitResult.BROADCASTING_NOT_SUPPORTED -> Toast.makeText(this, "不支持广播", Toast.LENGTH_SHORT).show()
                }
            }
        LiveEventBus.get<InterfaceEvent>(InterfaceEvent.Ble.EventBleConnectState)
            .observe(this) {
                val data = it.data as Int
                Log.d(TAG, "$data")
                when (data) {
                    BluetoothProfile.STATE_DISCONNECTED -> {
                        collect_time.stop()
                        Toast.makeText(this, "蓝牙断开", Toast.LENGTH_SHORT).show()
                    }
                    BluetoothProfile.STATE_CONNECTING -> Toast.makeText(this, "蓝牙连接中", Toast.LENGTH_SHORT).show()
                    BluetoothProfile.STATE_CONNECTED -> Toast.makeText(this, "蓝牙已连接", Toast.LENGTH_SHORT).show()
                    BluetoothProfile.STATE_DISCONNECTING -> Toast.makeText(this, "蓝牙断开中", Toast.LENGTH_SHORT).show()
                }
            }
        LiveEventBus.get<InterfaceEvent>(InterfaceEvent.Ble.EventBleConnectedDevice)
            .observe(this) {
                device = it.data as BluetoothDevice
                Log.d(TAG, "${device.name} ${device.address}")
            }
        LiveEventBus.get<InterfaceEvent>(InterfaceEvent.Ble.EventBleSendDataResult)
            .observe(this) {
                val data = it.data as Int
                Log.d(TAG, "$data")
                response.text = "$data"
            }
        LiveEventBus.get<InterfaceEvent>(InterfaceEvent.Ble.EventBleOxyGetFileList)
            .observe(this) {
                val data = it.data as Boolean
                fileList = ""
                assets.list("o2")?.let { list ->
                    for (file in list) {
                        fileList += "$file,"
                    }
                }
                SlaveManager.sendOxyFileList(arrayOf("$fileList"))
            }
        LiveEventBus.get<InterfaceEvent>(InterfaceEvent.Ble.EventBleOxyReadFile)
            .observe(this) {
                val data = it.data as String
                val bytes = IOUtils.toByteArray(assets.open("o2/$data"))
                SlaveManager.sendOxyFile(2048, bytes)
            }
        LiveEventBus.get<InterfaceEvent>(InterfaceEvent.Ble.EventBleOxyReadFileProcess)
            .observe(this) {
                val data = it.data as Int
                response.text = "$data"
            }
        LiveEventBus.get<InterfaceEvent>(InterfaceEvent.Ble.EventBleEcnGetFileList)
            .observe(this) {
                val data = it.data as Boolean
                val list = FileList()
                list.leftSize = 0
                list.fileList = ecnFileList
                SlaveManager.sendFileList(list)
            }
        LiveEventBus.get<InterfaceEvent>(InterfaceEvent.Ble.EventBleEcnReadFile)
            .observe(this) {
                val data = it.data as String
                val bytes = IOUtils.toByteArray(assets.open("ecn/$data.pdf"))
                SlaveManager.sendFile(2048, bytes)
            }
        LiveEventBus.get<InterfaceEvent>(InterfaceEvent.Ble.EventBleEcnReadFileProcess)
            .observe(this) {
                val data = it.data as Int
                response.text = "$data"
            }
        LiveEventBus.get<InterfaceEvent>(InterfaceEvent.Ble.EventBleEcnStartRtData)
            .observe(this) {
                // 平板心电图机回复主机
                SlaveManager.startRtData()
                // 平板心电图机开始发实时数据
                val data = RtData()
                val status = RtStatus()
                status.status = 0
                status.duration = 0
                data.status = status
                data.wave = ByteArray(200)
                SlaveManager.sendRtData(data)
            }
        LiveEventBus.get<InterfaceEvent>(InterfaceEvent.Ble.EventBleEcnStopRtData)
            .observe(this) {
                // 平板心电图机停止发实时数据
                SlaveManager.stopRtData()
            }
        LiveEventBus.get<InterfaceEvent>(InterfaceEvent.Ble.EventBleEcnStartCollect)
            .observe(this) {
                // 上位机开始采集数据
                collect_time.base = SystemClock.elapsedRealtime()
                val hour = (SystemClock.elapsedRealtime() - collect_time.base) / 1000 / 60
                collect_time.format = "0${hour}:%s"
                collect_time.start()
                SlaveManager.startCollect()
                Toast.makeText(this, "开始采集", Toast.LENGTH_SHORT).show()
                val data = RtData()
                val status = RtStatus()
                status.status = 2
                data.status = status
                data.wave = ByteArray(200)
                SlaveManager.sendRtData(data)
            }
        LiveEventBus.get<InterfaceEvent>(InterfaceEvent.Ble.EventBleEcnStopCollect)
            .observe(this) {
                // 上位机停止采集数据
                Toast.makeText(this, "停止采集", Toast.LENGTH_SHORT).show()
                collect_time.stop()
                SlaveManager.stopCollect()
                val jsonArray = JSONArray()
                jsonArray.put("诊断结论1")
                jsonArray.put("诊断结论2")
                jsonArray.put("诊断结论3")
                jsonArray.put("诊断结论4")
                val jsonObject = JSONObject()
                jsonObject.put("DiagnosisResult", jsonArray)
                SlaveManager.sendDiagnosisResult(jsonObject.toString().toByteArray())
            }
        LiveEventBus.get<InterfaceEvent>(InterfaceEvent.Ble.EventBleEcnGetRtStatus)
            .observe(this) {
                // 上位机查询实时状态
                val status = RtStatus()
                status.status = 2
                status.duration = getChronometerSecond()
                SlaveManager.sendRtStatus(status)
            }
        LiveEventBus.get<InterfaceEvent>(InterfaceEvent.Ble.EventBleEcnGetDiagnosisResult)
            .observe(this) {
                val jsonArray = JSONArray()
                jsonArray.put("诊断结论1")
                jsonArray.put("诊断结论2")
                jsonArray.put("诊断结论3")
                jsonArray.put("诊断结论4")
                val jsonObject = JSONObject()
                jsonObject.put("DiagnosisResult", jsonArray)
                SlaveManager.sendDiagnosisResult(jsonObject.toString().toByteArray())
            }
    }

    private fun initData() {
        ecnFileList.clear()
        assets.list("ecn")?.let { list ->
            for (file in list) {
                val f = FileList.BleFile()
                f.fileName = file.replace(".pdf", "")
                ecnFileList.add(f)
            }
        }
    }

    private fun getChronometerSecond(): Int {
        var totalss = 0
        val split = collect_time.text.toString().split(":")
        val hour = split[0].toInt()
        totalss += hour*3600
        val min = split[1].toInt()
        totalss += min*60
        val ss = split[2].toInt()
        totalss += ss
        return totalss
    }

    private fun myRequestPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PERMISSION_GRANTED
                        || ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PERMISSION_GRANTED
                        || ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) != PERMISSION_GRANTED
                        || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                    arrayOf(
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.BLUETOOTH,
                        Manifest.permission.BLUETOOTH_ADMIN,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ),
                    1)
            } else {
                BluetoothAdapter.getDefaultAdapter().let {
                    if (!it.isEnabled) {
                        it.enable()
                        Log.e(TAG, "myRequestPermission enable")
                    }
                }
                Log.e(TAG, "myRequestPermission")
            }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Log.e(TAG, "onRequestPermissionsResult")
        if (requestCode != 1) {
            return
        }
        for (i in permissions.indices) {
            if (grantResults[i] == PERMISSION_GRANTED) { //选择了“始终允许”
                Log.e(TAG, "权限" + permissions[i] + "申请成功")
            } else {
                Log.e(TAG, "权限" + permissions[i] + "申请失败")
                if (!ActivityCompat.shouldShowRequestPermissionRationale(this, permissions[i])) { //用户选择了禁止不再询问
                    Log.e(TAG, "用户禁用权限并不再询问:" + permissions[i])
                } else { //选择禁止
                    Log.e(TAG, "用户选择了此次禁止权限")
                }
            }
        }
        BluetoothAdapter.getDefaultAdapter().let {
            if (!it.isEnabled) {
                it.enable()
                Log.e(TAG, "enable")
            }
        }
    }

    override fun onDestroy() {
        SlaveManager.closeSlaveBle()
        super.onDestroy()
    }

}