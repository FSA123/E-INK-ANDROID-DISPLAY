package com.xingtai.epd.device.demo.ui.main.activity

import android.Manifest
import android.text.TextUtils
import android.view.KeyEvent
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import com.permissionx.guolindev.PermissionX
import com.permissionx.guolindev.callback.RequestCallback
import com.sjl.util.LogUtils
import com.xingtai.device.mcu.Mcu
import com.xingtai.device.t1000.TConConstant
import com.xingtai.device.t1000.entity.ScreenType
import com.xingtai.epd.device.demo.R
import com.xingtai.epd.device.demo.app.MyApplication
import com.xingtai.epd.device.demo.databinding.MainActivityBinding
import com.xingtai.epd.device.demo.entity.BatteryType
import com.xingtai.epd.device.demo.mcu.entity.SerialConfig
import com.xingtai.epd.device.demo.mqtt.entity.MqttConfig
import com.xingtai.epd.device.demo.service.HttpPollingService
import com.xingtai.epd.device.demo.service.T1000Service
import com.xingtai.epd.device.demo.service.listener.ServiceListener
import com.xingtai.epd.device.demo.t1000.T1000HelperFactory
import com.xingtai.epd.device.demo.ui.base.BaseActivity
import com.xingtai.epd.device.demo.util.AppConfigUtils
import com.xingtai.epd.device.demo.util.AppConfigUtils.screenType
import com.xingtai.epd.device.demo.util.DeviceUtils
import com.xingtai.epd.device.demo.util.MyAppUtils
import com.xingtai.epd.device.demo.util.SpUtils
import com.xingtai.epd.device.demo.util.SpUtils.getMqttConfig
import com.xingtai.epd.device.demo.util.SpUtils.getSerialConfig
import com.xingtai.epd.device.demo.util.SpUtils.saveMqttConfig
import com.xingtai.epd.device.demo.util.ToastUtils
import com.xingtai.epd.device.demo.widget.MySpinnerAdapter
import com.xingtai.epd.device.demo.widget.SpinnerItemSelectedListener
import java.util.Arrays
import java.util.Collections

/**
 *
 *
 * @author Kelly
 * @version 1.0.0
 * @filename MainActivity
 * @time 2023/3/7 16:48
 * @copyright(C) 2023 song
 */
class MainActivity : BaseActivity<MainActivityBinding>(MainActivityBinding::inflate),
    View.OnClickListener {

    var perms = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE)
    var currentScreenType: ScreenType? = null
    var packType: String? = null
    var devicePath: String? = null

    override fun initView() {}
    override fun initListener() {
        mViewBinding.btnAppConfig.setOnClickListener(this)
        mViewBinding.btnSerialConfig.setOnClickListener(this)
        mViewBinding.btnMqttConfig.setOnClickListener(this)
        mViewBinding.btnStartService.setOnClickListener(this)
        mViewBinding.btnStopService.setOnClickListener(this)
    }

    override fun initData() {
        PermissionX.init(this).permissions(*perms)
            .request(RequestCallback { allGranted, grantedList, deniedList ->
                val mqttConfig = getMqttConfig()

                initAppConfig()
                initSerialPortPath()
                initMqttConfig(mqttConfig)
                if (isValidConfig()) {
                    HttpPollingService.startService(mContext)
                    T1000Service.startService(mContext)
                    MyApplication.getMainThreadExecutor().postDelayed({
                        val serviceRunning = MyAppUtils.isServiceRunning(mContext, T1000Service::class.java.name)
                        if (serviceRunning){
                            updateButtonStatus(true)
                        }else{
                            updateButtonStatus(false)
                        }
                    },2000)
                }else{
                    updateButtonStatus(false)
                }
            })
    }

    private fun initAppConfig() {
        val screenType = AppConfigUtils.screenType
        initDeviceList(screenType)
        initBatteryTypeList()
        mViewBinding.cbAutoOpenFrontLight.isChecked = AppConfigUtils.isAutoOpenLight()
        mViewBinding.cbAutoBootApp.isChecked = AppConfigUtils.isAutoBootApp()

    }

    private fun isValidConfig(): Boolean {
        val screenType = AppConfigUtils.screenType
        val serialConfig = getSerialConfig()
        return screenType != null && serialConfig != null
    }

    private fun initDeviceList(screenType: ScreenType?) {
        val sortScreenTypes = listOf(*ScreenType.listScreenTypes(TConConstant.GROUP_T1000))
        Collections.sort(sortScreenTypes, java.util.Comparator { o1, o2 -> o1.sort - o2.sort })
        val deviceList: MutableList<String> = ArrayList(sortScreenTypes.size)
        deviceList.add("Please select device")
        var index = 0
        for (i in sortScreenTypes.indices) {
            val value = sortScreenTypes[i]
            if (screenType != null && screenType.type == value.type) {
                index = i + 1
            }
            deviceList.add(value.name + "(" + value.width + "*" + value.height + ")")
        }
        mViewBinding.spinnerEpdType.adapter = MySpinnerAdapter(mContext, android.R.layout.simple_list_item_1, deviceList)
        mViewBinding.spinnerEpdType.setSpinnerItemSelectedListener(object :
            SpinnerItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                if (position != 0) {
                    currentScreenType = sortScreenTypes[position - 1]
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        })
        mViewBinding.spinnerEpdType.setSelection(index, false)
        currentScreenType = screenType
    }

    private fun initBatteryTypeList() {
        val values: Array<BatteryType> = BatteryType.values()
        val batteryType: BatteryType? = AppConfigUtils.getBatteryType()
        val deviceList: MutableList<String> = java.util.ArrayList(values.size)
        deviceList.add("Please select battery")
        var index = 0
        for (i in values.indices) {
            val value: BatteryType = values[i]
            if (batteryType != null && TextUtils.equals(batteryType.packType, value.packType)) {
                index = i + 1
            }
            deviceList.add(value.name)
        }
        mViewBinding.spinnerBatteryType.adapter = MySpinnerAdapter(mContext,
            android.R.layout.simple_list_item_1,
            deviceList)
        mViewBinding.spinnerBatteryType.setSpinnerItemSelectedListener(object :
            SpinnerItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                if (position != 0) {
                    val batteryType: BatteryType = values[position - 1]
                    packType = batteryType.packType
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        })
        mViewBinding.spinnerBatteryType.setSelection(index, false)
    }

    private fun initSerialPortPath() {
        val deviceList = Mcu.listSerialPortPath()
        mViewBinding.spinnerSerial.adapter =
            MySpinnerAdapter(mContext, android.R.layout.simple_list_item_1, deviceList)
        mViewBinding.spinnerSerial.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?,
                                        view: View,
                                        position: Int,
                                        id: Long) {
                devicePath = deviceList[position]
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        var defaultIndex = 0
        for (i in deviceList.indices) {
            val s = deviceList[i]
            if (SerialConfig.DEVICE_PATH_SMT == s && DeviceUtils.isSmtDevice()) {
                defaultIndex = i
                break
            } else if (SerialConfig.DEVICE_PATH_YF == s) {
                defaultIndex = i
                break
            }
        }
        mViewBinding.spinnerSerial.setSelection(defaultIndex, false)
    }

    private fun initMqttConfig(mqttConfig: MqttConfig?) {
        val deviceId = DeviceUtils.deviceId
        mViewBinding.etClientId.setText(deviceId)
        mqttConfig?.let {
            if (it != null) {
                val clientId = it.clientId
                if (TextUtils.isEmpty(clientId)) {
                    mViewBinding.etClientId.setText(deviceId)
                }
                mViewBinding.etIp.setText(it.ip)
                if (it.port != -1) {
                    mViewBinding.etPort.setText(it.port.toString())
                }
                val username = it.username
                if (TextUtils.isEmpty(username)) {
                    mViewBinding.etUsername.setText(deviceId)
                } else {
                    mViewBinding.etUsername.setText(username)
                }
                mViewBinding.etPwd.setText(it.password)
                mViewBinding.etSubTopic.setText(it.subTopic)
            }
            LogUtils.i(it.toString())
        }

    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            moveTaskToBack(false)
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.btn_app_config ->  {
                currentScreenType?.let {
                    T1000HelperFactory.reset(it)
                    val appConfig = AppConfigUtils.appConfig
                    appConfig.type = it.type
                    appConfig.batteryType = packType
                    appConfig.autoOpenLight = mViewBinding.cbAutoOpenFrontLight.isChecked
                    appConfig.autoBootApp = mViewBinding.cbAutoBootApp.isChecked
                    AppConfigUtils.writeConfig(appConfig)
                }
            }

            R.id.btn_serial_config -> if (!TextUtils.isEmpty(devicePath)) {
                val serialConfig = SerialConfig()
                serialConfig.devicePath = devicePath
                SpUtils.saveSerialConfig(serialConfig)

            }

            R.id.btn_mqtt_config -> {
                val clientIdStr = mViewBinding.etClientId.text.toString().trim { it <= ' ' }
                val ipStr = mViewBinding.etIp.text.toString().trim { it <= ' ' }
                val portStr = mViewBinding.etPort.text.toString().trim { it <= ' ' }
                val usernameStr = mViewBinding.etUsername.text.toString().trim { it <= ' ' }
                val pwdStr = mViewBinding.etPwd.text.toString().trim { it <= ' ' }
                val subTopicStr = mViewBinding.etSubTopic.text.toString().trim { it <= ' ' }
                val mqttConfig = MqttConfig()
                mqttConfig.clientId = clientIdStr
                mqttConfig.ip = ipStr
                if (ipStr.isNotEmpty()){
                    mqttConfig.port = portStr.toInt()
                }
                mqttConfig.username = usernameStr
                mqttConfig.password = pwdStr
                mqttConfig.subTopic = subTopicStr
                saveMqttConfig(mqttConfig)
            }

            R.id.btn_start_service -> {

                if (!isValidConfig()) {
                    ToastUtils.showShort(this, getString(R.string.tip_invalid_config))
                   return
                }
                HttpPollingService.startService(mContext)
                T1000Service.startService(mContext)
                updateButtonStatus(true)
            }

            R.id.btn_stop_service -> {
                if (!isValidConfig()) {
                    return
                }
                HttpPollingService.stopService(mContext)
                T1000Service.stopService(mContext,object :ServiceListener{
                    override fun onDestroy() {
                        updateButtonStatus(false)
                    }

                })

            }

            else -> {}
        }
    }

    private fun updateButtonStatus(start: Boolean) {
        if (start){
            mViewBinding.btnStartService.isEnabled = false
            mViewBinding.btnStopService.isEnabled = true
        }else{
            mViewBinding.btnStartService.isEnabled = true
            mViewBinding.btnStopService.isEnabled = false
        }

    }

}
