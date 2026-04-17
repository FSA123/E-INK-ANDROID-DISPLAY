package com.xingtai.t1000.demo.ui.t1000.activity;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.SystemClock;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.permissionx.guolindev.PermissionX;
import com.permissionx.guolindev.request.ChainTask;
import com.permissionx.guolindev.request.ExplainScope;
import com.permissionx.guolindev.request.ForwardScope;
import com.sjl.deviceconnector.ErrorCode;
import com.sjl.util.LogUtils;
import com.xingtai.device.t1000.T1000Constant;
import com.xingtai.device.t1000.T1000ErrorCode;
import com.xingtai.device.t1000.TConConstant;
import com.xingtai.device.t1000.entity.ScreenType;
import com.xingtai.device.t1000.entity.T1000DeviceInfo;
import com.xingtai.device.t1000.entity.T1000UsbDevice;
import com.xingtai.device.t1000.open.T1000Device;
import com.xingtai.device.t1000.open.T1000DeviceApi;
import com.xingtai.t1000.demo.R;
import com.xingtai.t1000.demo.app.MyApplication;
import com.xingtai.t1000.demo.databinding.T1000UsbTestActivityBinding;
import com.xingtai.t1000.demo.ui.base.BaseActivity;
import com.xingtai.t1000.demo.util.UriUtils;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import androidx.appcompat.app.AlertDialog;


/**
 * T1000测试界面
 *
 * @author Kelly
 * @version 1.0.0
 * @filename T1000UsbTestActivity
 * @time 2023/3/21 16:11
 * @copyright(C) 2023 江西兴泰科技股份有限公司
 */
public class T1000UsbTestActivity extends BaseActivity<T1000UsbTestActivityBinding> implements View.OnClickListener {


    private static List<T1000UsbDevice> mSupportedDevices = new ArrayList(Arrays.asList(new T1000UsbDevice(1165, 35159), new T1000UsbDevice(1165, 35153)));
    private static String[] REFRESH_MODE = new String[]{"Default","Portrait","Landscape","Portrait(From bottom to top)","Landscape(From left to right)"};

    private  ScreenType screenType = ScreenType.SCREEN_25_3;

    private T1000DeviceApi t1000Device;
    Map<String,UsbDevice> usbDeviceMap = new LinkedHashMap<>();
    private MyBroadcastReceiver mBroadcastReceiver;
    private int currentRefreshMode = TConConstant.RefreshMode.DEFAULT;
    private boolean runningFlag = false;
    private String currentUpdateFile;

    @Override
    protected void initView() {
        getSupportActionBar().setTitle(getString(R.string.t1000_test));
    }

    @Override
    protected void initListener() {


        viewBinding.btnConnect.setOnClickListener(this);
        viewBinding.btnDisconnect.setOnClickListener(this);
        viewBinding.btnClear.setOnClickListener(this);

        viewBinding.btnGetDeviceInfo.setOnClickListener(this);
        viewBinding.btnGetTemperature.setOnClickListener(this);
        viewBinding.btnSetTemperature.setOnClickListener(this);
        viewBinding.btnGetVcom.setOnClickListener(this);
        viewBinding.btnSetVcom.setOnClickListener(this);

        viewBinding.btnSelectImg.setOnClickListener(this);
        viewBinding.btnLoadImg.setOnClickListener(this);
        viewBinding.btnDisplayImg.setOnClickListener(this);
        viewBinding.btnDisplayClear.setOnClickListener(this);
        viewBinding.btnStartDuTest.setOnClickListener(this);
        viewBinding.btnStopTest.setOnClickListener(this);
        viewBinding.btnStartGc16Test.setOnClickListener(this);
        viewBinding.btnLocalImageTest.setOnClickListener(this);
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



        if (ScreenType.isColorsScreen(screenType)) {
            viewBinding.etDisplayMode.setText("0");
            viewBinding.cbDither.setChecked(true);
        } else if (ScreenType.isKaleidoscopeScreen(screenType)) {
            viewBinding.etDisplayMode.setText("2");
            viewBinding.cbDither.setChecked(true);
        } else {
            viewBinding.etDisplayMode.setText("2");
            viewBinding.cbDither.setChecked(false);
        }


        viewBinding.etW.setText(String.valueOf(screenType.width));
        viewBinding.etH.setText(String.valueOf(screenType.height));
        initDeviceList();
        initRefreshModeList();
    }



    private void listUsbDevices() {
        UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
        for (UsbDevice usbDevice : deviceList.values()) {
            LogUtils.i("vendorId:" + usbDevice.getVendorId() + ",productId:" + usbDevice.getProductId());
            if (mSupportedDevices.contains(new T1000UsbDevice(usbDevice.getVendorId(),usbDevice.getProductId()))) {
                showMsg("Usb Device,vendorId:" + usbDevice.getVendorId() + ",productId:" + usbDevice.getProductId());
                T1000Device.requestPermission(usbDevice);
                usbDeviceMap.put(usbDevice.getDeviceName(),usbDevice);
            }
        }
        if (usbDeviceMap.size() == 0){
            showMsg(getString(R.string.tip_not_find_available_device));
        }
        sortUsbDevice(usbDeviceMap);

    }

    private void sortUsbDevice(Map<String, UsbDevice> usbDeviceMap) {
        List<Map.Entry<String, UsbDevice>> list =new ArrayList<Map.Entry<String, UsbDevice>>(usbDeviceMap.entrySet());
        Collections.sort(list, new Comparator<Map.Entry<String, UsbDevice>>() {
            public int compare(Map.Entry<String, UsbDevice> o1, Map.Entry<String, UsbDevice> o2) {
                String p1 =  o1.getKey();
                String p2 = o2.getKey();
                //升序
                return p1.compareTo(p2);
            }
        });
        usbDeviceMap.clear();
        for(Map.Entry<String,UsbDevice> entity : list){
            usbDeviceMap.put(entity.getKey(), entity.getValue());
        }
    }

    private void initDeviceList() {
        listUsbDevices();
        List<UsbDevice> usbDevices = new ArrayList<>(usbDeviceMap.values());
        List<String> deviceList = new ArrayList<>();
        int index = 0;
        for (int i = 0; i < usbDevices.size(); i++) {
            UsbDevice usbDevice = usbDevices.get(i);
            deviceList.add(usbDevice.getDeviceName());
        }
        viewBinding.spinner.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, deviceList));
        viewBinding.spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, final int position, long id) {
                /**
                 * Multiple devices, create multiple instances, can be controlled at the application layer
                 */
                if (t1000Device != null){
                    t1000Device.close();
                }

                UsbDevice usbDevice = usbDeviceMap.get(deviceList.get(position));
                int interfaceCount = usbDevice.getInterfaceCount();
                T1000Device.requestPermission(usbDevice);
                t1000Device = new T1000Device(usbDevice);
                t1000Device.setType(screenType.type);
                LogUtils.i("current usbDevice:" + usbDevice + ",t1000Device:" + t1000Device+",interfaceCount："+interfaceCount);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        viewBinding.spinner.setSelection(index, false);


//        ScreenType[] values = ScreenType.values();
        ScreenType[] values = ScreenType.listScreenTypes(TConConstant.GROUP_T1000);

        List<String> screenTypeList = new ArrayList<>(values.length);
        screenTypeList.add(getString(R.string.screen_type));
        index = 0;
        for (int i = 0; i < values.length; i++) {
            ScreenType value = values[i];
            if (screenType != null && screenType.type == value.type) {
                index = i + 1;
            }
            screenTypeList.add(value.name() + "(" + value.width + "*" + value.height + ")");
        }
        viewBinding.spinnerScreenType.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, screenTypeList));

        viewBinding.spinnerScreenType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, final int position, long id) {
                if (position == 0){
                    return;
                }
                screenType = values[position - 1];
                if (ScreenType.isColorsScreen(screenType)) {
                    viewBinding.etDisplayMode.setText("0");
                    viewBinding.cbDither.setChecked(true);
                } else if (ScreenType.isKaleidoscopeScreen(screenType)) {
                    viewBinding.etDisplayMode.setText("2");
                    viewBinding.cbDither.setChecked(true);
                } else {
                    viewBinding.etDisplayMode.setText("2");
                    viewBinding.cbDither.setChecked(false);
                }
                if (t1000Device != null){
                    t1000Device.setType(screenType.type);
                }
                viewBinding.etX.setText(String.valueOf(0));
                viewBinding.etY.setText(String.valueOf(0));
                viewBinding.etW.setText(String.valueOf(screenType.width));
                viewBinding.etH.setText(String.valueOf(screenType.height));
                if (ScreenType.isSupportRefreshMode(screenType)){
                    viewBinding.llRefreshMode.setVisibility(View.VISIBLE);
                }else {
                    currentRefreshMode = TConConstant.RefreshMode.DEFAULT;
                    viewBinding.llRefreshMode.setVisibility(View.GONE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        viewBinding.spinnerScreenType.setSelection(index, false);

    }

    private void initRefreshModeList() {


        viewBinding.spinnerRefreshMode.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, REFRESH_MODE));

        viewBinding.spinnerRefreshMode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, final int position, long id) {

                currentRefreshMode = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_connect) {
            connect();
        } else if (id == R.id.btn_disconnect) {
            disconnect(v);
        } else if (id == R.id.btn_clear) {
            viewBinding.tvMsg.setText("");
        } else if (id == R.id.btn_get_device_info) {
            MyApplication.Companion.getExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    T1000DeviceInfo t1000DeviceInfo = t1000Device.getT1000DeviceInfo();
                    if (t1000DeviceInfo == null){
                        showMsg(getString(R.string.tip_get_device_info_failure));
                        return;
                    }
                    showMsg(t1000DeviceInfo.toString());
                }
            });
        } else if (id == R.id.btn_get_temperature) {
            MyApplication.Companion.getExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    int temperature = t1000Device.getTemperature();
                    showMsg("get temperature:" + temperature);
                    if (temperature != -1){
                        runOnUiThread(() -> viewBinding.etTemperature.setText(String.valueOf(temperature)));
                    }
                }
            });
        } else if (id == R.id.btn_set_temperature) {
            String tempStr = viewBinding.etTemperature.getText().toString().trim();
            if (TextUtils.isEmpty(tempStr)){
                return;
            }
            int temp = Integer.parseInt(tempStr);
            MyApplication.Companion.getExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    int ret = t1000Device.setTemperature(temp);
                    if (ret == T1000ErrorCode.ERROR_OK){
                        showMsg(getString(R.string.tip_set_temperature_success));
                    }else {
                        showMsg(getString(R.string.tip_set_temperature_failure)+ret);
                    }

                }
            });
        } else if (id == R.id.btn_get_vcom) {
            MyApplication.Companion.getExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    int vCom = t1000Device.getVCom();
                    showMsg("get vCom:" + vCom);
                }
            });
        } else if (id == R.id.btn_set_vcom) {
            String vComStr = viewBinding.etVcom.getText().toString().trim();
            if (TextUtils.isEmpty(vComStr)){
                return;
            }
            int vCom = Integer.parseInt(vComStr);
            MyApplication.Companion.getExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    int ret = t1000Device.setVCom(vCom);
                    if (ret == T1000ErrorCode.ERROR_OK){
                        showMsg(getString(R.string.tip_set_vcom_success));
                    }else {
                        showMsg(getString(R.string.tip_set_vcom_failure)+ret);
                    }
                }
            });
        } else if (id == R.id.btn_select_img) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                PermissionX.init(T1000UsbTestActivity.this)
                        .permissions(Manifest.permission.MANAGE_EXTERNAL_STORAGE)
                        .requestManageExternalStoragePermissionNow(new ChainTask() {
                            @Override
                            public ExplainScope getExplainScope() {
                                return null;
                            }

                            @Override
                            public ForwardScope getForwardScope() {
                                return null;
                            }

                            @Override
                            public void request() {
                                LogUtils.e("request");
                            }

                            @Override
                            public void requestAgain(List<String> permissions) {

                            }

                            @Override
                            public void finish() {
                                LogUtils.e("Approval of authorization");
                                openFileManager();

                            }
                        });
            } else {
                openFileManager();
            }
        } else if (id == R.id.btn_load_img) {
            if (TextUtils.isEmpty(sourcePath)) {
                showMsg(getString(R.string.tip_not_select_image));
                return;
            }
            String path = sourcePath;
            String etX = viewBinding.etX.getText().toString().trim();
            String etY = viewBinding.etY.getText().toString().trim();
            String etW = viewBinding.etW.getText().toString().trim();
            String etH = viewBinding.etH.getText().toString().trim();
            String etDisplayMode = viewBinding.etDisplayMode.getText().toString().trim();
            if (TextUtils.isEmpty(etX) || TextUtils.isEmpty(etY) || TextUtils.isEmpty(etW) || TextUtils.isEmpty(etH)) {
                showMsg(getString(R.string.tip_please_specify_display_parameters));
                return;
            }
            if (TextUtils.isEmpty(etDisplayMode)) {
                showMsg(getString(R.string.tip_please_specify_display_mode));
                return;
            }
            int startX = Integer.parseInt(etX);
            int startY = Integer.parseInt(etY);
            int width = Integer.parseInt(etW);
            int height = Integer.parseInt(etH);

            Rect rect = new Rect(0, 0, screenType.width, screenType.height);
            if (!rect.contains(startX,startY,startX + width, startY + height)) {
                showMsg(getString(R.string.tip_coordinate_crossing));
                return;
            }


            int displayMode = Integer.parseInt(etDisplayMode);

            boolean dither = viewBinding.cbDither.isChecked();
            showMsg(getString(R.string.tip_load_waiting));
            MyApplication.Companion.getExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        int ret;
//                            Bitmap bitmap = BitmapFactory.decodeFile(path);
                        ret = t1000Device.loadImage(path, dither, startX, startY, width, height, displayMode);
                        if (ret != ErrorCode.ERROR_OK){
                            showMsg(getString(R.string.tip_image_load_failure) + ret);
                        }else{
                            showMsg(getString(R.string.tip_image_load_success));
                        }
                    } catch (Exception e) {
                        LogUtils.e("加载图像异常", e);
                        showMsg(getString(R.string.tip_image_load_failure) + e.getMessage());
                    }

                }
            });
        } else if (id == R.id.btn_display_img) {
            if (TextUtils.isEmpty(sourcePath)) {
                showMsg(getString(R.string.tip_not_select_image));
                return;
            }
            String etX = viewBinding.etX.getText().toString().trim();
            String etY = viewBinding.etY.getText().toString().trim();
            String etW = viewBinding.etW.getText().toString().trim();
            String etH = viewBinding.etH.getText().toString().trim();
            String etDisplayMode = viewBinding.etDisplayMode.getText().toString().trim();
            if (TextUtils.isEmpty(etX) || TextUtils.isEmpty(etY) || TextUtils.isEmpty(etW) || TextUtils.isEmpty(etH)) {
                showMsg(getString(R.string.tip_please_specify_display_parameters));
                return;
            }
            if (TextUtils.isEmpty(etDisplayMode)) {
                showMsg(getString(R.string.tip_please_specify_display_mode));
                return;
            }
            int startX = Integer.parseInt(etX);
            int startY = Integer.parseInt(etY);
            int width = Integer.parseInt(etW);
            int height = Integer.parseInt(etH);

            Rect rect = new Rect(0, 0, screenType.width, screenType.height);
            if (!rect.contains(startX,startY,startX + width, startY + height)) {
                showMsg(getString(R.string.tip_coordinate_crossing));
                return;
            }


            int displayMode = Integer.parseInt(etDisplayMode);

            showMsg(getString(R.string.tip_display_waiting));
            MyApplication.Companion.getExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        int ret;
//                            Bitmap bitmap = BitmapFactory.decodeFile(path);
                        //The following method includes two steps: load image and display image
//                            ret = t1000Device.display(screenType.type, path, dither, startX, startY, width, height, displayMode);
//                            ret = t1000Device.display(startX, startY, width, height, displayMode);
                        ret = performDisplay(currentRefreshMode,startX, startY, width, height, displayMode);
                        if (ret == ErrorCode.ERROR_OK) {
                            showMsg(getString(R.string.tip_display_image_success));
                        } else {
                            showMsg(getString(R.string.tip_image_display_failure) + ret);
                        }

                    } catch (Exception e) {
                        LogUtils.e("显示图像异常", e);
                        showMsg(getString(R.string.tip_image_display_failure) + e.getMessage());
                    }

                }
            });
        } else if (id == R.id.btn_display_clear) {
            String etX = viewBinding.etX.getText().toString().trim();
            String etY = viewBinding.etY.getText().toString().trim();
            String etW = viewBinding.etW.getText().toString().trim();
            String etH = viewBinding.etH.getText().toString().trim();
            String etDisplayMode = viewBinding.etDisplayMode.getText().toString().trim();
            if (TextUtils.isEmpty(etX) || TextUtils.isEmpty(etY) || TextUtils.isEmpty(etW) || TextUtils.isEmpty(etH)) {
                showMsg(getString(R.string.tip_please_specify_display_parameters));
                return;
            }
            if (TextUtils.isEmpty(etDisplayMode)) {
                showMsg(getString(R.string.tip_please_specify_display_mode));
                return;
            }
            int startX = Integer.parseInt(etX);
            int startY = Integer.parseInt(etY);
            int width = Integer.parseInt(etW);
            int height = Integer.parseInt(etH);


            if (TextUtils.isEmpty(etDisplayMode)) {
                showMsg(getString(R.string.tip_please_specify_display_mode));
                return;
            }
            int displayMode = Integer.parseInt(etDisplayMode);

            showMsg(getString(R.string.tip_clear_display_waiting));
            MyApplication.Companion.getExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    try {

                        int ret;
//                            ret = t1000Device.clearDisplay(screenType.type, startX, startY, width, height, displayMode);
                        ret = t1000Device.clearDisplay(startX, startY, width, height, displayMode);
                        if (ret == ErrorCode.ERROR_OK) {
                            showMsg(getString(R.string.tip_clear_display_success));
                        } else {
                            showMsg(getString(R.string.tip_clear_display_failure) + ret);
                        }
                    } catch (Exception e) {
                        LogUtils.e("清屏异常", e);
                        showMsg(getString(R.string.tip_clear_display_failure) + e.getMessage());

                    }

                }
            });
        } else if (id == R.id.btn_start_du_test) {
            if (ScreenType.isColorsScreen(screenType)){
                showMsg("unsupported");
                return;
            }
            MyApplication.Companion.getExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    int startX =0,startY = 0;
                    int waveformMode = 1;
                    runningFlag = true;

                    for (int i = 0; i <= 60; i++) {
                        if (!runningFlag){
                            break;
                        }
                        String timeText = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
                        Bitmap bitmap = generateNumberBitmap(timeText, 400, 100);
                        int ret = t1000Device.loadImage(bitmap, false, 0, 0, bitmap.getWidth(), bitmap.getHeight(), waveformMode);
                        if (ret == ErrorCode.ERROR_OK) {
                            ret = t1000Device.display(startX, startY, bitmap.getWidth(), bitmap.getHeight(), waveformMode);
                            if (ret == ErrorCode.ERROR_OK) {
                                showMsg(i+":du display success");
                            }else {
                                showMsg(i+":du display fail");
                                break;
                            }
                        }else {
                            showMsg(i+":du load fail");
                            break;
                        }

                    }
                }
            });
        } else if (id == R.id.btn_stop_test) {
            runningFlag = false;
        } else if (id == R.id.btn_start_gc16_test) {
            if (ScreenType.isColorsScreen(screenType)){
                showMsg("unsupported");
                return;
            }
            MyApplication.Companion.getExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    int startX =0,startY = 0;
                    int waveformMode = 2;
                    runningFlag = true;
                    String pattern = "yyyy-MM-dd HH:mm:ss:SSS";
                    int width = 400, height = 100;
                    for (int i = 0; i <= 60; i++) {
                        if (!runningFlag){
                            break;
                        }
                        String timeText = new SimpleDateFormat(pattern).format(new Date());
                        Bitmap bitmap = generateNumberBitmap(timeText, width, height);

                        int ret = t1000Device.loadImage(bitmap, false, 0, 0, bitmap.getWidth(), bitmap.getHeight(), waveformMode);
                        if (ret == ErrorCode.ERROR_OK) {
                            ret = t1000Device.display(startX, startY, bitmap.getWidth(), bitmap.getHeight(), waveformMode);
                            if (ret == ErrorCode.ERROR_OK) {
                                showMsg(i+":gc16 display success");
                            }else {
                                showMsg(i+":gc16 display fail");
                                break;
                            }
                        }else {
                            showMsg(i+":gc16 load fail");
                            break;
                        }
                    }
                }
            });
        } else if (id == R.id.btn_local_image_test) {
            if (ScreenType.isColorsScreen(screenType)) {
                showMsg("unsupported");
                return;
            }
            MyApplication.Companion.getExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    int startX = 0, startY = 0;
                    int waveformMode = 2;
                    runningFlag = true;
                    //You need to put the image of the specified size in the specified directory in advance
                    File dir = new File(Environment.getExternalStorageDirectory(), Environment.DIRECTORY_DOWNLOADS + File.separator + "13.3");
                    File[] files = dir.listFiles(new FilenameFilter() {
                        public boolean accept(File dir, String name) {
                            return name.toLowerCase().endsWith(".jpg") || name.toLowerCase().endsWith(".jpeg") ||
                                    name.toLowerCase().endsWith(".png") ||
                                    name.toLowerCase().endsWith(".bmp");
                        }
                    });
                    if (files == null || files.length == 0) {
                        showMsg("No images available.");
                        return;
                    }
                    int index = 0;
                    try {
                        for (int i = 0; i <= 60; i++) {
                            if (!runningFlag) {
                                break;
                            }
                            for (int j = 0; j < files.length; j++) {
                                if (!runningFlag) {
                                    break;
                                }
                                File file = files[j];
                                Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());

                                int ret = t1000Device.loadImage(bitmap, false, 0, 0, bitmap.getWidth(), bitmap.getHeight(), waveformMode);
                                if (ret == ErrorCode.ERROR_OK) {
                                    ret = t1000Device.display(startX, startY, bitmap.getWidth(), bitmap.getHeight(), waveformMode);
                                    if (ret == ErrorCode.ERROR_OK) {
                                        showMsg(index + ":gc16 display success");
                                    } else {
                                        showMsg(index + ":gc16 display fail");
                                        runningFlag = false;
                                        break;
                                    }
                                } else {
                                    showMsg(index + ":gc16 load fail");
                                    runningFlag = false;
                                    break;
                                }
                                index++;
                            }

                        }
                    } catch (Exception e) {
                        showMsg("display exception:" + e.getMessage());
                    }
                }
            });
        }else if (id == R.id.ll_update_file) {
            boolean checkedFirmware = viewBinding.rbFirmware.isChecked();
            boolean checkedWaveform = viewBinding.rbWaveform.isChecked();
            if (!checkedFirmware && !checkedWaveform) {
                return;
            }
            String[] strings;
            File[] files;
            if (checkedWaveform){
                files = new File(T1000Constant.T1000_DIR,"wbf").listFiles(new FileFilter() {
                    @Override
                    public boolean accept(File pathname) {
                        String name = pathname.getName();
                        return name.toLowerCase().endsWith(".wbf");
                    }
                });
            }else{
                files = new File(T1000Constant.T1000_DIR,"bin").listFiles(new FileFilter() {
                    @Override
                    public boolean accept(File pathname) {
                        String name = pathname.getName();
                        return name.toLowerCase().endsWith(".bin");
                    }
                });
            }
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

            boolean checkedFirmware = viewBinding.rbFirmware.isChecked();
            boolean checkedWaveform = viewBinding.rbWaveform.isChecked();
            if (currentUpdateFile == null){
                showMsg(getString(R.string.tip_not_select_file));
                return;
            }
            File dir;
            if (checkedWaveform){
                dir = new File(T1000Constant.T1000_DIR,"wbf");
            }else if (checkedFirmware){
                dir = new File(T1000Constant.T1000_DIR,"bin");
            }else{
                dir = new File(T1000Constant.T1000_DIR,"fullBin");
            }
            MyApplication.Companion.getExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        File file = new File(dir,currentUpdateFile);
                        if (checkedWaveform){
                            if (file.exists() && file.isFile() && file.getName().toLowerCase().endsWith(".wbf")){
                                showMsg(getString(R.string.tip_wbf_upgrade_waiting));
                                int ret = t1000Device.upgradeWbf(file);
                                if (ret == ErrorCode.ERROR_OK){
                                    showMsg(getString(R.string.tip_update_successfully));
                                }else {
                                    showMsg(getString(R.string.tip_update_failure) + "ret：" + ret);
                                }

                            }else {
                                showMsg(getString(R.string.tip_wbf_file_not_exist));
                            }
                        }else {
                            if (file.exists() && file.isFile() && file.getName().toLowerCase().endsWith(".bin")){
                                showMsg(getString(R.string.tip_upgrade_waiting));

                                int ret = t1000Device.upgradeFirmware(file);
                                if (ret == ErrorCode.ERROR_OK){
                                    showMsg(getString(R.string.tip_update_successfully));
                                }else {
                                    showMsg(getString(R.string.tip_update_failure) + "ret：" + ret);
                                }

                            }else {
                                showMsg(getString(R.string.tip_firmware_file_not_exist));
                            }
                        }

                    } catch (Exception e) {
                        LogUtils.e("Update failure", e);
                        showMsg(getString(R.string.tip_update_failure) + ":" + e.getMessage());
                    }

                }
            });

        }

    }

    public static Bitmap generateNumberBitmap(String text, int width, int height) {
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.eraseColor(Color.WHITE);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.BLACK);
        paint.setTextSize(40);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        float v = height / 2 - ((paint.descent() + paint.ascent()) / 2);
        canvas.drawText(text,0, v, paint);
        return bitmap;
    }

    private synchronized int performDisplay(int refreshMode, int startX, int startY, int width, int height, int waveformMode) {
        int ret = 0;
        if (ScreenType.isSupportRefreshMode(screenType)){
            int refreshDelayTime = 20;
            if (refreshMode == TConConstant.RefreshMode.REFRESH_PORTRAIT) {
                //垂直滚动刷图(From top to bottom)
                int xDiv = 1;
                int yDiv = 16;
                int xGap = width / xDiv;
                int yGap = height / yDiv;
                int force = 0;
                int endVCom = 0;
                for (int y = 0; y < yDiv; y++) {
                    for (int x = 0; x < xDiv; x++) {
                        if (y == yDiv - 1) {
                            if (width % xDiv != 0) {
                                t1000Device.display(startX + x * xGap, startY + y * yGap, width - xGap * x, yGap, waveformMode, force, endVCom);
                            } else if (height % yDiv != 0) {
                                t1000Device.display(startX + x * xGap, startY + y * yGap, xGap, height - yGap * y, waveformMode, force, endVCom);
                            } else {
                                t1000Device.display(startX + x * xGap, startY + y * yGap, xGap, yGap, waveformMode, force, endVCom);
                            }
                        } else {
                            t1000Device.display(startX + x * xGap, startY + y * yGap, xGap, yGap, waveformMode, force, endVCom);
                        }
                        if (refreshDelayTime > 0){
                            SystemClock.sleep(refreshDelayTime);
                        }

                    }
                }
            } else  if (refreshMode == TConConstant.RefreshMode.REFRESH_LANDSCAPE){
                //水平滚动刷图(From right to left)
                int xDiv = 16;
                int yDiv = 1;
                int xGap = width / xDiv;
                int yGap = height / yDiv;
                int force = 0;
                int endVCom = 0;
                for (int y = 0; y < yDiv; y++) {
                    for (int x = 0; x < xDiv; x++) {
                        if (x == xDiv - 1) {
                            if (width % xDiv != 0) {
                                t1000Device.display(startX + x * xGap, startY + y * yGap, width - xGap * x, yGap, waveformMode, force, endVCom);
                            } else if (height % yDiv != 0) {
                                t1000Device.display(startX + x * xGap, startY + y * yGap, xGap, height - yGap * y, waveformMode, force, endVCom);
                            } else {
                                t1000Device.display(startX + x * xGap, startY + y * yGap, xGap, yGap, waveformMode, force, endVCom);
                            }
                        } else {
                            t1000Device.display(startX + x * xGap, startY + y * yGap, xGap, yGap, waveformMode, force, endVCom);
                        }
                        if (refreshDelayTime > 0){
                            SystemClock.sleep(refreshDelayTime);
                        }
                    }
                }
            }else if (refreshMode == TConConstant.RefreshMode.REFRESH_PORTRAIT_BOTTOM_TO_TOP) {
                //垂直滚动刷图(From bottom to top)
                int xDiv = 1;
                int yDiv = 16;
                int xGap = width / xDiv;
                int yGap = height / yDiv;
                int force = 0;
                int endVCom = 0;
                for (int y = 0; y < yDiv; y++) {
                    for (int x = 0; x < xDiv; x++) {
                        if (y == yDiv - 1) {
                            if (width % xDiv != 0) {
                                t1000Device.display(startX + x * xGap, height -(startY + y * yGap)-yGap, width - xGap * x, yGap, waveformMode, force, endVCom);
                            } else if (height % yDiv != 0) {
                                t1000Device.display(startX + x * xGap, height -(startY + y * yGap)-(height - yGap * y), xGap, height - yGap * y, waveformMode, force, endVCom);
                            } else {
                                t1000Device.display(startX + x * xGap, height -(startY + y * yGap)-yGap, xGap, yGap, waveformMode, force, endVCom);
                            }
                        } else {
                            t1000Device.display(startX + x * xGap, height -(startY + y * yGap)-yGap, xGap, yGap, waveformMode, force, endVCom);
                        }
                        if (refreshDelayTime > 0){
                            SystemClock.sleep(refreshDelayTime);
                        }
                    }
                }
            } else if (refreshMode == TConConstant.RefreshMode.REFRESH_LANDSCAPE_LEFT_TO_RIGHT) {
                //水平滚动刷图(From left to right)
                int xDiv = 16;
                int yDiv = 1;
                int xGap = width / xDiv;
                int yGap = height / yDiv;
                int force = 0;
                int endVCom = 0;
                for (int y = 0; y < yDiv; y++) {
                    for (int x = 0; x < xDiv; x++) {
                        if (x == xDiv - 1) {
                            if (width % xDiv != 0) {
                                t1000Device.display(width-(startX + x * xGap)-(width - xGap * x), startY + y * yGap, width - xGap * x, yGap, waveformMode, force, endVCom);
                            } else if (height % yDiv != 0) {
                                t1000Device.display(width-(startX + x * xGap)-xGap, startY + y * yGap, xGap, height - yGap * y, waveformMode, force, endVCom);
                            } else {
                                t1000Device.display(width-(startX + x * xGap)-xGap, startY + y * yGap, xGap, yGap, waveformMode, force, endVCom);
                            }
                        } else {
                            t1000Device.display(width-(startX + x * xGap)-xGap, startY + y * yGap, xGap, yGap, waveformMode, force, endVCom);
                        }
                        if (refreshDelayTime > 0){
                            SystemClock.sleep(refreshDelayTime);
                        }
                    }
                }
            }else {
                ret = t1000Device.display(startX, startY, width, height, waveformMode);
            }
            if (refreshMode != TConConstant.RefreshMode.DEFAULT){
                boolean b = t1000Device.waitDisplay();
                if (!b){
                    ret = T1000ErrorCode.ERROR_DISPLAY_TIMEOUT;
                }
            }
        }else {
            ret = t1000Device.display(startX, startY, width, height, waveformMode);
        }

        return ret;
    }
    private void connect() {
        if (t1000Device == null) {
            Toast.makeText(mContext, getString(R.string.tip_not_find_device), Toast.LENGTH_SHORT).show();
            return;
        }

        MyApplication.Companion.getExecutor().execute(new Runnable() {
            @Override
            public void run() {
                int ret = t1000Device.open();

                if (ret == ErrorCode.ERROR_OK) {
                    showMsg(getString(R.string.tip_device_connect_success));
                    T1000DeviceInfo t1000DeviceInfo = t1000Device.getT1000DeviceInfo();
                    if (t1000DeviceInfo == null){
                        showMsg(getString(R.string.tip_get_device_info_failure));
                        return;
                    }
                    showMsg(t1000DeviceInfo.toString());
                } else {
                    showMsg(getString(R.string.tip_device_connect_failure) + ret);
                }
            }
        });

    }

    private void disconnect(View v) {
        if (t1000Device != null) {
            t1000Device.close();
            if (v != null) {
                showMsg(getString(R.string.tip_device_disconnect_failure));
            }

        }
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
        usbDeviceMap.clear();
        disconnect(null);
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

    public static final int REQUEST_CODE_FILE_ACTIVITY = 1;
    private String sourcePath;

    private void openFileManager() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");//无类型限制
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, REQUEST_CODE_FILE_ACTIVITY);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode == REQUEST_CODE_FILE_ACTIVITY && resultCode == Activity.RESULT_OK) {
            Uri uri = intent.getData();
            if (uri != null) {
                showMsg(getString(R.string.tip_image_loading_waiting));

                MyApplication.Companion.getExecutor().execute(new Runnable() {
                    @Override
                    public void run() {
                        sourcePath = UriUtils.fileUriToPath(mContext, uri);
                        Bitmap bitmap = BitmapFactory.decodeFile(sourcePath);
                        int width = bitmap.getWidth();
                        int height = bitmap.getHeight();
                        showMsg(sourcePath + ": width：" + width + ",height:" + height);

                        MyApplication.Companion.getMainThreadExecutor().post(new Runnable() {
                            @Override
                            public void run() {
                                viewBinding.ivImg.setImageBitmap(bitmap);
                                viewBinding.etX.setText(String.valueOf(0));
                                viewBinding.etY.setText(String.valueOf(0));
                                viewBinding.etW.setText(String.valueOf(width));
                                viewBinding.etH.setText(String.valueOf(height));

                            }
                        });


                    }
                });
            }
        }
    }

}
