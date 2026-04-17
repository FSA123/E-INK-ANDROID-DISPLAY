package com.xingtai.t1000.demo.ui.mcu.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Environment;
import android.os.SystemClock;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import com.sjl.deviceconnector.ErrorCode;
import com.sjl.deviceconnector.util.LogUtils;
import com.xingtai.device.mcu.Mcu;
import com.xingtai.device.mcu.McuApiImpl;
import com.xingtai.device.mcu.entity.McuInfo;
import com.xingtai.device.mcu.listener.BatteryChangeListener;
import com.xingtai.device.mcu.listener.McuKeyListener;
import com.xingtai.device.mcu.listener.TimerRemindListener;
import com.xingtai.t1000.demo.R;
import com.xingtai.t1000.demo.app.MyApplication;
import com.xingtai.t1000.demo.databinding.McuTestActivityBinding;
import com.xingtai.t1000.demo.ui.base.BaseActivity;
import com.xingtai.t1000.demo.ui.t1000.activity.T1000UsbTestActivity;

import java.io.File;
import java.io.FileFilter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import androidx.appcompat.app.AlertDialog;

/**
 * MCU测试界面
 *
 * @author Kelly
 * @version 1.0.0
 * @filename McuTestActivity
 * @time 2023/5/18 10:26
 * @copyright(C) 2023 江西兴泰科技股份有限公司
 */
public class McuTestActivity extends BaseActivity<McuTestActivityBinding> implements View.OnClickListener {

    String otaDirName = "ota";
    File otaDir = new File(Environment.getExternalStorageDirectory().toString() + File.separator + Environment.DIRECTORY_DOWNLOADS, otaDirName);

    private  String devicePath;
    private McuApiImpl mcuApi;

    private static final AtomicInteger atomicNum = new AtomicInteger();
    private String currentUpdateFile;
    private MyBroadcastReceiver mBroadcastReceiver;

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void initView() {
        getSupportActionBar().setTitle(R.string.mcu_test);
    }

    @Override
    protected void initListener() {
        viewBinding.btnConnect.setOnClickListener(this);
        viewBinding.btnDisconnect.setOnClickListener(this);
        viewBinding.btnConnect.setOnClickListener(this);
        viewBinding.btnLowPower.setOnClickListener(this);
        viewBinding.btnHeartbeat.setOnClickListener(this);
        viewBinding.btnTimer.setOnClickListener(this);
        viewBinding.btnPowerControl.setOnClickListener(this);
        viewBinding.btnFrontLightBrightnessDown.setOnClickListener(this);
        viewBinding.btnFrontLightBrightnessUp.setOnClickListener(this);
        viewBinding.btnLedExecute.setOnClickListener(this);

        viewBinding.btnBoot.setOnClickListener(this);
        viewBinding.btnMcuInfo.setOnClickListener(this);
        viewBinding.btnClear.setOnClickListener(this);
        viewBinding.llUpdateFile.setOnClickListener(this);
        viewBinding.btnUpdate.setOnClickListener(this);

        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        mBroadcastReceiver =  new MyBroadcastReceiver();
        registerReceiver(mBroadcastReceiver, filter);
    }

    @Override
    protected void initData() {
        if (!otaDir.exists()){
            otaDir.mkdirs();
        }
        mcuApi = new McuApiImpl();
        initDeviceList();

    }

    private void initDeviceList() {
        List<String> deviceList = Mcu.listSerialPortPath();
        viewBinding.spinner.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, deviceList));
        viewBinding.spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, final int position, long id) {
                devicePath = deviceList.get(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        int defaultIndex = 0;
        for (int i = 0; i < deviceList.size(); i++) {
            String s = deviceList.get(i);
            if ("/dev/ttyS3".equals(s) && isYfDevice()) {
                defaultIndex = i;
                break;
            }else if ("/dev/ttyS2".equals(s)) {
                defaultIndex = i;
                break;
            }
        }
        viewBinding.spinner.setSelection(defaultIndex, false);
    }

    public static boolean isYfDevice() {
        String model = Build.MODEL;
        if (!TextUtils.isEmpty(model) && model.toUpperCase().contains("YF")){
            return true;
        }else {
            return false;
        }
    }


    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_connect) {
            connect();
        } else if (id == R.id.btn_disconnect) {
            disconnect();
        } else if (id == R.id.btn_clear) {
            viewBinding.tvMsg.setText("");
        } else if (id == R.id.btn_low_power) {
            boolean enable = viewBinding.cbLowPower.isChecked();

            MyApplication.Companion.getExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    int ret = mcuApi.startLowPower(enable);
                    if (ret == ErrorCode.ERROR_OK) {
                        showMsg(getString(R.string.tip_low_power_success)+"，enable:"+enable);
                    } else {
                        showMsg(getString(R.string.tip_low_power_failure)+"，ret:" + ret);
                    }
                }
            });
        } else if (id == R.id.btn_heartbeat) {
            MyApplication.Companion.getExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    int ret = mcuApi.sendHeartbeat();
                    if (ret == ErrorCode.ERROR_OK) {
                        showMsg(getString(R.string.tip_heartbeat_success));
                    } else {
                        showMsg(getString(R.string.tip_heartbeat_failure)+"，ret:" + ret);
                    }
                }
            });
        } else if (id == R.id.btn_mcu_info) {
            MyApplication.Companion.getExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    McuInfo mcuInfo = new McuInfo();
                    int ret = mcuApi.getMcuInfo(mcuInfo);
                    if (ret == ErrorCode.ERROR_OK) {
                        showMsg(getString(R.string.tip_get_mcu_success) +":"+ mcuInfo);
                    } else {
                        showMsg(getString(R.string.tip_get_mcu_failure) +"，ret:" + ret);
                    }
                }
            });
        } else if (id == R.id.btn_timer) {
            int no = atomicNum.incrementAndGet();
            viewBinding.etNo.setText(String.valueOf(no));
            String timeStr = viewBinding.etDelayTime.getText().toString().trim();
            if (TextUtils.isEmpty(timeStr)){
                showMsg(getString(R.string.tip_time_is_null));
                return;
            }
            int time = Integer.parseInt(timeStr);
            if (time <= 0){
                showMsg(getString(R.string.tip_time_error));
                return;
            }
            MyApplication.Companion.getExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    int ret = mcuApi.startTimer(no, time, new TimerRemindListener() {
                        @Override
                        public void onTimeEnd(int no) {
                            showMsg(getString(R.string.tip_timing_remind_end) + ":" + no);
                        }
                    });
                    if (ret == ErrorCode.ERROR_OK) {
                        showMsg(getString(R.string.tip_timing_remind_success));
                    } else {
                        showMsg(getString(R.string.tip_timing_remind_failure)+"，ret:" + ret);
                    }
                }
            });
        } else if (id == R.id.btn_power_control) {
            String timeStr = viewBinding.etMainDelayTime.getText().toString().trim();
            if (TextUtils.isEmpty(timeStr)){
                showMsg(getString(R.string.tip_time_is_null));
                return;
            }
            int time = Integer.parseInt(timeStr);
            if (time <= 0){
                showMsg(getString(R.string.tip_time_error));
                return;
            }
            boolean mainPower = viewBinding.cbMainPower.isChecked();
            boolean t1000Power = viewBinding.cbT1000Power.isChecked();
            boolean fivePower = viewBinding.cb5vPower.isChecked();

            MyApplication.Companion.getExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    int ret = mcuApi.powerControl(mainPower,time,t1000Power,fivePower);
                    if (ret == ErrorCode.ERROR_OK) {
                        showMsg(getString(R.string.tip_power_control_success));
                    } else {
                        showMsg(getString(R.string.tip_power_control_failure)+"，ret:" + ret);
                    }
                }
            });
        } else if (id == R.id.btn_front_light_brightness_down) {
            String brightnessLevelStr = viewBinding.etBrightnessLevel.getText().toString().trim();
            if (TextUtils.isEmpty(brightnessLevelStr)) {
                showMsg(getString(R.string.brightness_level_is_null));
                return;
            }
            int brightnessLevel = Integer.parseInt(brightnessLevelStr);
            if (brightnessLevel <= 1) {
                brightnessLevel = 1;
            } else {
                brightnessLevel -= 1;
            }
            int finalBrightnessLevel = brightnessLevel;
            MyApplication.Companion.getExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    int ret = mcuApi.adjustBrightness(finalBrightnessLevel);
                    if (ret == ErrorCode.ERROR_OK) {
                        showMsg(getString(R.string.tip_brightness_adjust_success));
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                viewBinding.etBrightnessLevel.setText(String.valueOf(finalBrightnessLevel));
                            }
                        });
                    } else {
                        showMsg(getString(R.string.tip_brightness_adjust_failure) + "，ret:" + ret);
                    }
                }
            });
        } else if (id == R.id.btn_front_light_brightness_up) {
            String brightnessLevelStr = viewBinding.etBrightnessLevel.getText().toString().trim();
            if (TextUtils.isEmpty(brightnessLevelStr)) {
                showMsg(getString(R.string.brightness_level_is_null));
                return;
            }
            int brightnessLevel = Integer.parseInt(brightnessLevelStr);
            if (brightnessLevel >= 10) {
                brightnessLevel = 10;
            } else {
                brightnessLevel += 1;
            }

            int finalBrightnessLevel = brightnessLevel;
            MyApplication.Companion.getExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    int ret = mcuApi.adjustBrightness(finalBrightnessLevel);
                    if (ret == ErrorCode.ERROR_OK) {
                        showMsg(getString(R.string.tip_brightness_adjust_success));
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                viewBinding.etBrightnessLevel.setText(String.valueOf(finalBrightnessLevel));
                            }
                        });
                    } else {
                        showMsg(getString(R.string.tip_brightness_adjust_failure) + "，ret:" + ret);
                    }
                }
            });
        } else if (id == R.id.btn_boot) {
            //If the host device needs deep sleep(low-power mode), you can set a specified time delay for device startup
            int no = atomicNum.incrementAndGet();
            viewBinding.etBootNo.setText(String.valueOf(no));
            String timeStr = viewBinding.etBootDelayTime.getText().toString().trim();
            if (TextUtils.isEmpty(timeStr)){
                showMsg(getString(R.string.tip_time_is_null));
                return;
            }
            int time = Integer.parseInt(timeStr);
            if (time <= 0){
                showMsg(getString(R.string.tip_time_error));
                return;
            }
            MyApplication.Companion.getExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    //After shutdown, perform delayed boot
                    int ret = mcuApi.boot(no, time);
                    if (ret == ErrorCode.ERROR_OK) {
                        showMsg(getString(R.string.tip_delay_startup_success));
                        SystemClock.sleep(1000);
                        ret = mcuApi.powerControl(false, 2, false, false);
                        if (ret == ErrorCode.ERROR_OK) {
                            showMsg(getString(R.string.tip_power_control_success));
                        } else {
                            showMsg(getString(R.string.tip_power_control_failure)+"，ret:" + ret);
                        }
                    } else {
                        showMsg(getString(R.string.tip_delay_startup_failure)+"，ret:" + ret);
                    }
                }
            });
        } else if (id == R.id.btn_led_execute) {
            boolean ledSwitch = viewBinding.cbLed.isChecked();
            int ledType,intervalTime = 0,times = 0;
            if (ledSwitch){
                String intervalTimeStr = viewBinding.etLedInterval.getText().toString().trim();
                if (TextUtils.isEmpty(intervalTimeStr)){
                    showMsg(getString(R.string.tip_time_interval_is_null));
                    return;
                }
                intervalTime = Integer.parseInt(intervalTimeStr);
                if (intervalTime <= 0){
                    showMsg(getString(R.string.tip_time_interval_error));
                    return;
                }
                String timesStr = viewBinding.etLedTimes.getText().toString().trim();
                if (TextUtils.isEmpty(timesStr)){
                    showMsg(getString(R.string.tip_flashing_frequency_is_null));
                    return;
                }
                times = Integer.parseInt(timesStr);
                if (times <= 0){
                    showMsg(getString(R.string.tip_flashing_frequency_error));
                    return;
                }
            }

            boolean greenLed = viewBinding.cbGreenLed.isChecked();
            boolean redLed = viewBinding.cbRedLed.isChecked();
            if (greenLed){
                ledType = Mcu.LED_TYPE_GREEN;
            }else if(redLed){
                ledType = Mcu.LED_TYPE_RED;
            }else {
                ledType = Mcu.LED_TYPE_BLUE;
            }
            int finalIntervalTime = intervalTime;
            int finalTimes = times;
            MyApplication.Companion.getExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    int ret = mcuApi.ledControl(ledSwitch, ledType, finalIntervalTime, finalTimes);
                    if (ret == ErrorCode.ERROR_OK) {
                        showMsg("LED Switch:" + ledSwitch + " -> " + getString(R.string.tip_led_control_success));
                    } else {
                        showMsg("LED Switch:" + ledSwitch + " -> " + getString(R.string.tip_led_control_failure) + "，ret:" + ret);
                    }
                }
            });
        } else if (id == R.id.ll_update_file) {
            String[] strings;

            File[] files = otaDir.listFiles(new FileFilter() {
                @Override
                public boolean accept(File pathname) {
                    String name = pathname.getName();
                    return name.toLowerCase().endsWith(".bin");
                }
            });
            if (files == null || files.length == 0){
                showMsg(getString(R.string.tip_not_find_firmware));
                return;
            }
            strings = new String[files.length];
            for (int i = 0; i < files.length; i++) {
                File f = files[i];
                String name = f.getName();
                strings[i]= name;
            }
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setSingleChoiceItems(strings, 0, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialogInterface, int i) {
                    currentUpdateFile = strings[i];
                    viewBinding.tvUpdateFile.setText(strings[i]);
                    dialogInterface.dismiss();
                }
            });
            builder.setPositiveButton(R.string.dlg_cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();
                }
            });
            AlertDialog alertDialog= builder
                    .create();
            alertDialog.show();

        } else if (id == R.id.btn_update) {
            if (currentUpdateFile == null){
                showMsg(getString(R.string.tip_not_select_file));
                return;
            }
            MyApplication.Companion.getExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        File file = new File(otaDir,currentUpdateFile);
                        if (file.exists() && file.isFile() && file.getName().toLowerCase().endsWith(".bin")){
                            showMsg(getString(R.string.tip_upgrade_waiting));
                            int ret = mcuApi.upgradeFirmware(file);
                            if (ret == ErrorCode.ERROR_OK){
                                showMsg(getString(R.string.tip_update_successfully));
                            }else {
                                showMsg(getString(R.string.tip_update_failure) + "ret：" + ret);
                            }
                        }else {
                            showMsg(getString(R.string.tip_firmware_file_not_exist));
                        }

                    } catch (Exception e) {
                        LogUtils.e("更新异常", e);
                        showMsg(getString(R.string.tip_update_failure) + ":" + e.getMessage());
                    }
                }
            });
        }
    }

    private void connect() {
        if (devicePath == null) {
            showMsg(getString(R.string.tip_select_device));
            return;
        }
        if (mcuApi != null){
            mcuApi.close();
        }
        mcuApi = new McuApiImpl(devicePath);
        mcuApi.setMcuKeyListener(new McuKeyListener() {
            @Override
            public void onKeyDown(int keyCode, int keyDelayFlag, int det) {
                Integer keyDelayTime = mcuApi.getKeyDelay().get(keyDelayFlag);
                showMsg(getString(R.string.key_touch) + keyCode + ",keyDelay:" + ((keyDelayTime == 0 ? getString(R.string.key_short_press) : getString(R.string.key_long_press) + keyDelayTime + "s")));
            }
        });
        mcuApi.setBatteryChangeListener(new BatteryChangeListener() {
            @Override
            public void onChange(int charge, int mV, int soc) {
                if (charge == Mcu.BATTERY_UNCHARGED) {
                    showMsg(getString(R.string.battery_not_charging) + mV + ",soc:" + soc);
                } else if (charge == Mcu.BATTERY_CHARGING) {
                    showMsg(getString(R.string.battery_charging) + mV + ",soc:" + soc);
                } else {
                    showMsg(getString(R.string.battery_fully_charged) + mV + ",soc:" + soc);
                }
            }

        });
        int ret = mcuApi.open();
        if (ret == 0) {
            showMsg(getString(R.string.tip_device_connect_success));
        } else {
            showMsg(getString(R.string.tip_device_connect_failure)+"：" + ret);
        }
    }

    private void disconnect() {
        mcuApi.close();
        showMsg(getString(R.string.tip_device_disconnect_failure));
    }

    private void showMsg(String s) {
        showMsg(s, null);
    }

    private void showMsg(String s, Boolean flag) {
        if (isDestroy(this)) {
            return;
        }

        MyApplication.Companion.getMainThreadExecutor().post(() -> {
            LogUtils.i(s);
            Date curDate = new Date(System.currentTimeMillis());
            String strDate = new SimpleDateFormat("HH:mm:ss.SSS").format(curDate);
            String log = strDate + ":  " + s;
            viewBinding.tvMsg.append(log + "\n");

            viewBinding.nestedScrollView.post(() -> {
                viewBinding.nestedScrollView.fullScroll(View.FOCUS_DOWN);

            });
        });

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mBroadcastReceiver != null) {
            unregisterReceiver(mBroadcastReceiver);
            mBroadcastReceiver = null;
        }
        mcuApi.close();

    }

    private final class MyBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                UsbDevice usbDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                showMsg("USB ATTACHED:" + usbDevice.getDeviceName()+",vendorId:" + usbDevice.getVendorId() + ",productId:" + usbDevice.getProductId());
            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                UsbDevice usbDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                showMsg("USB DETACHED:" + usbDevice.getDeviceName()+",vendorId:" + usbDevice.getVendorId() + ",productId:" + usbDevice.getProductId());
            }
        }
    };
}
