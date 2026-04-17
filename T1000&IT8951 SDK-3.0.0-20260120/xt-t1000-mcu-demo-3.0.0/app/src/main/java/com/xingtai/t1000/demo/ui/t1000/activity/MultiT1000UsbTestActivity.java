package com.xingtai.t1000.demo.ui.t1000.activity;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
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
import com.xingtai.device.t1000.TConConstant;
import com.xingtai.device.t1000.entity.ImagePiece;
import com.xingtai.device.t1000.entity.ScreenType;
import com.xingtai.device.t1000.entity.T1000DeviceInfo;
import com.xingtai.device.t1000.entity.T1000UsbDevice;
import com.xingtai.device.t1000.open.T1000Device;
import com.xingtai.device.t1000.util.ImageSplitter;
import com.xingtai.device.t1000.util.T1000Utils;
import com.xingtai.t1000.demo.R;
import com.xingtai.t1000.demo.app.MyApplication;
import com.xingtai.t1000.demo.databinding.MultiT1000UsbTestActivityBinding;
import com.xingtai.t1000.demo.entity.T1000DeviceWrap;
import com.xingtai.t1000.demo.ui.base.BaseActivity;
import com.xingtai.t1000.demo.util.UriUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 多屏测试界面
 *
 * @author Kelly
 * @version 1.0.0
 * @filename MultiT1000UsbTestActivity
 * @time 2023/10/9 11:28
 * @copyright(C) 2023 江西兴泰科技股份有限公司
 */
public class MultiT1000UsbTestActivity extends BaseActivity<MultiT1000UsbTestActivityBinding> implements View.OnClickListener {

    private static final List<T1000UsbDevice> mSupportedDevices = new ArrayList(Arrays.asList(new T1000UsbDevice(1165, 35159), new T1000UsbDevice(1165, 35153)));
    Map<String, UsbDevice> usbDeviceMap = new LinkedHashMap<>();
    Map<String, T1000Device> usbConnector = new LinkedHashMap<>();

    private ScreenType screenType = ScreenType.SCREEN_31_2_MONOCHROME_8951;



    int xFlip = 0;
    int yFlip = 1;

    private MyBroadcastReceiver mBroadcastReceiver;

    @Override
    protected void initView() {
        getSupportActionBar().setTitle(getString(R.string.multi_screen_test));
    }

    @Override
    protected void initListener() {
        viewBinding.ivImg.setOnClickListener(this);

        viewBinding.btnConnect.setOnClickListener(this);
        viewBinding.btnDisconnect.setOnClickListener(this);
        viewBinding.btnClear.setOnClickListener(this);

        viewBinding.btnSelectImg.setOnClickListener(this);
        viewBinding.btnDisplayImg.setOnClickListener(this);
        viewBinding.btnDisplayClear.setOnClickListener(this);


        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        mBroadcastReceiver = new MyBroadcastReceiver();
        registerReceiver(mBroadcastReceiver, filter);


    }

    @Override
    protected void initData() {

        initDeviceList();
    }

    private void initDeviceList() {
        listUsbDevices();
        ScreenType[] values = ScreenType.listScreenTypes(TConConstant.GROUP_T1000);

        List<String> screenTypeList = new ArrayList<>(values.length);
        screenTypeList.add(getString(R.string.screen_type));
        int index = 0;
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
                if (position == 0) {
                    return;
                }
                screenType = values[position - 1];
                xFlip = 0;
                yFlip = 1;
                if (ScreenType.isColorsScreen(screenType)) {
                    viewBinding.etDisplayMode.setText("0");
                    viewBinding.cbDither.setChecked(true);
                    viewBinding.cbSlice.setChecked(false);
                    viewBinding.etRow.setText("1");
                    viewBinding.etColumn.setText("1");
                } else if (ScreenType.isKaleidoscopeScreen(screenType)) {
                    viewBinding.etDisplayMode.setText("2");
                    viewBinding.cbDither.setChecked(true);
                    viewBinding.cbSlice.setChecked(false);
                    viewBinding.etRow.setText("1");
                    viewBinding.etColumn.setText("1");
                } else if (screenType == ScreenType.SCREEN_31_2_MONOCHROME_8951) {
                    viewBinding.etDisplayMode.setText("2");
                    viewBinding.cbDither.setChecked(false);
                    viewBinding.cbSlice.setChecked(true);
                    viewBinding.etRow.setText("4");
                    viewBinding.etColumn.setText("1");
                } else if (screenType == ScreenType.SCREEN_42_MONOCHROME_8951) {
                    viewBinding.etDisplayMode.setText("2");
                    viewBinding.cbDither.setChecked(false);
                    viewBinding.cbSlice.setChecked(true);
                    viewBinding.etRow.setText("2");
                    viewBinding.etColumn.setText("1");
                } else {
                    viewBinding.etDisplayMode.setText("2");
                    viewBinding.cbDither.setChecked(false);
                    viewBinding.cbSlice.setChecked(false);
                    viewBinding.etRow.setText("1");
                    viewBinding.etColumn.setText("1");
                }
                initT1000ApiImpl();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        viewBinding.spinnerScreenType.setSelection(index, false);

    }

    private void listUsbDevices() {
        UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
        for (UsbDevice usbDevice : deviceList.values()) {
            LogUtils.i("vendorId:" + usbDevice.getVendorId() + ",productId:" + usbDevice.getProductId());
            if (mSupportedDevices.contains(new T1000UsbDevice(usbDevice.getVendorId(), usbDevice.getProductId()))) {
                showMsg("Usb Device,vendorId:" + usbDevice.getVendorId() + ",productId:" + usbDevice.getProductId());
                T1000Device.requestPermission(usbDevice);
                usbDeviceMap.put(usbDevice.getDeviceName(), usbDevice);
            }
        }
        if (usbDeviceMap.size() == 0) {
            showMsg(getString(R.string.tip_not_find_available_device));
        }
        sortUsbDevice(usbDeviceMap);

    }

    private void sortUsbDevice(Map<String, UsbDevice> usbDeviceMap) {
        List<Map.Entry<String, UsbDevice>> list = new ArrayList<Map.Entry<String, UsbDevice>>(usbDeviceMap.entrySet());
        Collections.sort(list, new Comparator<Map.Entry<String, UsbDevice>>() {
            public int compare(Map.Entry<String, UsbDevice> o1, Map.Entry<String, UsbDevice> o2) {
                String p1 = o1.getKey();
                String p2 = o2.getKey();
                //升序
                return p1.compareTo(p2);
            }
        });
        usbDeviceMap.clear();
        for (Map.Entry<String, UsbDevice> entity : list) {
            usbDeviceMap.put(entity.getKey(), entity.getValue());
        }
    }


    private void initT1000ApiImpl() {
        disconnect(null);
        for (Map.Entry<String, UsbDevice> entry : usbDeviceMap.entrySet()) {
            T1000Device t1000Api = new T1000Device(entry.getValue());
            t1000Api.setType(screenType.type);
            usbConnector.put(entry.getKey(), t1000Api);
        }

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
        } else if (id == R.id.btn_select_img) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                PermissionX.init(MultiT1000UsbTestActivity.this)
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
        } else if (id == R.id.btn_display_img) {
            if (TextUtils.isEmpty(sourcePath)) {
                showMsg(getString(R.string.tip_not_select_image));
                return;
            }
            String etDisplayMode = viewBinding.etDisplayMode.getText().toString().trim();

            if (TextUtils.isEmpty(etDisplayMode)) {
                showMsg(getString(R.string.tip_please_specify_display_mode));
                return;
            }
            int displayMode = Integer.parseInt(etDisplayMode);

            String path = sourcePath;
            boolean checked = viewBinding.cbSlice.isChecked();
            int row = 1, column = 1;
            if (checked) {
                String etRow = viewBinding.etRow.getText().toString().trim();
                String etColumn = viewBinding.etColumn.getText().toString().trim();
                if (TextUtils.isEmpty(etRow) || TextUtils.isEmpty(etColumn)) {
                    showMsg(getString(R.string.tip_row_col_not_null));
                    return;
                }
                row = Integer.parseInt(etRow);
                column = Integer.parseInt(etColumn);
            }

            boolean dither = viewBinding.cbDither.isChecked();

            int finalRow = row;
            int finalColumn = column;
            MyApplication.Companion.getExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    String filePath = path;
                    try {
                        List<T1000DeviceWrap> t1000DeviceWraps = initT1000ApiImplWrap(screenType, usbConnector);

                        if (checked) {
                            showMsg(getString(R.string.tip_cutting_image_waiting));
                            BitmapFactory.Options options = new BitmapFactory.Options();
                            options.inJustDecodeBounds = true;
                            BitmapFactory.decodeFile(filePath, options);
                            int width = options.outWidth;
                            int height = options.outHeight;
                            //startX and startY must be 0
                            int startX = 0;
                            int startY = 0;
                            if (ScreenType.isMulti8951Screen(screenType) && !(screenType.width == width / finalColumn && screenType.height == height / finalRow)) {
                                //31.2 is a large screen composed of four small screens
                                showMsg(getString(R.string.tip_cut_image_resolution_not_match));
                                return;
                            } else {
                                if (!(screenType.width * finalColumn == width && screenType.height * finalRow == height)) {
                                    showMsg(getString(R.string.tip_cut_image_resolution_not_match));
                                    return;
                                }
                            }
                            List<ImagePiece> split = ImageSplitter.splitBig(path, finalRow, finalColumn);
                            //Output cutting images
                            boolean outSplitImg = false;
                            if (outSplitImg){
                                for (int i = 0; i < split.size(); i++) {
                                    ImagePiece imagePiece = split.get(i);
                                    saveImage(imagePiece.bitmap, "sub_" + i);
                                }
                            }

                            if (split.size() != t1000DeviceWraps.size()){
                                showMsg("Unable to allocate connection");
                                return;
                            }

                            int size = t1000DeviceWraps.size();

                            for (int i = 0; i < t1000DeviceWraps.size(); i++) {
                                T1000DeviceWrap t1000DeviceWrap = t1000DeviceWraps.get(i);
                                ImagePiece imagePiece = split.get(i);
                                imagePiece.startX = startX;
                                imagePiece.startY = startY;
                                if (screenType == ScreenType.SCREEN_42_MONOCHROME_8951) {
                                    if (i == size - 1) {
                                        xFlip = 1;
                                    } else {
                                        xFlip = 0;
                                    }
                                    yFlip = 0;
                                }
                                byte[] pixels;
                                if (dither) {
                                    pixels = T1000Utils.colorMapAndDither(screenType, imagePiece.bitmap,imagePiece.width, imagePiece.height, xFlip, yFlip);
                                } else {
                                    pixels = T1000Utils.bitmapFlip(screenType, imagePiece.bitmap, imagePiece.width, imagePiece.height, xFlip, yFlip,displayMode);
                                }
                                t1000DeviceWrap.pixels = pixels;
                                t1000DeviceWrap.width = imagePiece.width;
                                t1000DeviceWrap.height = imagePiece.height;
                                t1000DeviceWrap.startX = imagePiece.startX;
                                t1000DeviceWrap.startY = imagePiece.startY;
                            }

                        }else {
                            showMsg(getString(R.string.tip_processing_image_waiting));
                            Bitmap bitmap = BitmapFactory.decodeFile(filePath);
                            int width = bitmap.getWidth();
                            int height = bitmap.getHeight();
                            byte[] pixels;
                            if (dither) {
                                pixels = T1000Utils.colorMapAndDither(screenType, bitmap, width, height, xFlip, yFlip);
                            } else {
                                pixels = T1000Utils.bitmapFlip(screenType, bitmap, width, height, xFlip, yFlip,displayMode);
                            }
                            for (int i = 0; i < t1000DeviceWraps.size(); i++) {
                                T1000DeviceWrap t1000DeviceWrap = t1000DeviceWraps.get(i);
                                t1000DeviceWrap.pixels = pixels;
                                t1000DeviceWrap.width = width;
                                t1000DeviceWrap.height = height;
                            }
                        }
                        //Set average temperature
                        setAverageTemp(t1000DeviceWraps);
                        showMsg(getString(R.string.tip_display_waiting));


                        ExecutorService executorService = Executors.newFixedThreadPool(t1000DeviceWraps.size());
                        //Concurrent display of images
                        for (T1000DeviceWrap entry : t1000DeviceWraps) {
                            executorService.execute(() -> display(entry, displayMode));

                        }
                        executorService.shutdown();
                        try {
                            executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }


                    } catch (Exception e) {
                        LogUtils.e("显示图像异常", e);
                        showMsg(getString(R.string.tip_image_display_failure) + e.getMessage());
                    }

                }
            });
        } else if (id == R.id.btn_display_clear) {
            String etDisplayMode = viewBinding.etDisplayMode.getText().toString().trim();

            if (TextUtils.isEmpty(etDisplayMode)) {
                showMsg(getString(R.string.tip_please_specify_display_mode));
                return;
            }
            int displayMode = Integer.parseInt(etDisplayMode);

            MyApplication.Companion.getExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        List<T1000DeviceWrap> t1000DeviceWraps = initT1000ApiImplWrap(screenType, usbConnector);

                        int width = screenType.width;
                        int height = screenType.height;
                        showMsg(getString(R.string.tip_clear_display_waiting));


                        ExecutorService executorService = Executors.newFixedThreadPool(t1000DeviceWraps.size());
                        //并发刷图
                        for (T1000DeviceWrap entry : t1000DeviceWraps) {
                            entry.width = width;
                            entry.height = height;
                            executorService.execute(() -> clearDisplay(entry, displayMode));

                        }
                        executorService.shutdown();
                        try {
                            executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }


                    } catch (Exception e) {
                        LogUtils.e("清屏异常", e);
                        showMsg(getString(R.string.tip_clear_display_failure) + e.getMessage());
                    }
                }
            });
        }
    }

    private void setAverageTemp(List<T1000DeviceWrap> t1000DeviceWraps) {
        int tempTotal = 0;
        boolean flag = true;
        for (T1000DeviceWrap entry : t1000DeviceWraps) {
            String key = entry.key;
            T1000Device t1000Device = entry.t1000Device;
            int currentTemperature = t1000Device.getTemperature();
            if (currentTemperature != ErrorCode.ERROR_FAIL) {
                showMsg(key + getString(R.string.tip_current_temperature) + currentTemperature);
                tempTotal += currentTemperature;
            } else {
                showMsg(key + getString(R.string.tip_unable_read_temperature) + currentTemperature);
                flag = false;
                break;
            }
        }
        if (flag) {
            //average temperature
            int averageTemp = tempTotal / t1000DeviceWraps.size();
            for (T1000DeviceWrap entry : t1000DeviceWraps) {
                String key = entry.key;
                T1000Device t1000Device = entry.t1000Device;
                int ret = t1000Device.setTemperature(averageTemp);
                if (ret != ErrorCode.ERROR_OK) {
                    showMsg(key + getString(R.string.tip_unable_write_temperature) + ret);
                }
            }
        }
    }

    private static String saveImage(Bitmap bmp, String name) {
        if (bmp == null) {
            return null;
        }
        File appDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        String fileName = name + ".png";
        File file = new File(appDir, fileName);
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            bmp.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
            return file.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    private List<T1000DeviceWrap> initT1000ApiImplWrap(ScreenType screenType, Map<String, T1000Device> usbConnector) {
        List<T1000DeviceWrap> t1000DeviceWraps = new ArrayList<>();
        T1000DeviceWrap t1000DeviceWrap;
        if (ScreenType.isMulti8951Screen(screenType)) {
            //31.2 8951,Reverse display
            List<Map.Entry<String, T1000Device>> entries = new ArrayList<>(usbConnector.entrySet());
            if (screenType == ScreenType.SCREEN_31_2_MONOCHROME_8951){
                Collections.reverse(entries);
            }
            for (Map.Entry<String, T1000Device> entry : entries) {
                t1000DeviceWrap = new T1000DeviceWrap();
                t1000DeviceWrap.key = entry.getKey();
                t1000DeviceWrap.t1000Device = entry.getValue();
                t1000DeviceWraps.add(t1000DeviceWrap);
            }

        } else {
            for (Map.Entry<String, T1000Device> entry : usbConnector.entrySet()) {
                t1000DeviceWrap = new T1000DeviceWrap();
                t1000DeviceWrap.key = entry.getKey();
                t1000DeviceWrap.t1000Device = entry.getValue();
                t1000DeviceWraps.add(t1000DeviceWrap);
            }
        }
        return t1000DeviceWraps;
    }


    private void display(T1000DeviceWrap entry, int displayMode) {
        String key = entry.key;
        try {

            T1000Device t1000Api = entry.t1000Device;
            showMsg(key + ":display");
            byte[] pixels = entry.pixels;
            int width = entry.width;
            int height = entry.height;

            int startX = entry.startX, startY = entry.startY;
            int ret = t1000Api.display(screenType.type, pixels, startX, startY, width, height, displayMode);
            if (ret == ErrorCode.ERROR_OK) {
                showMsg(key + getString(R.string.tip_split_screen_display_success));
            } else {
                showMsg(key + getString(R.string.tip_split_screen_display_failure) + ":" + ret);
            }
        } catch (Exception e) {
            LogUtils.e(key + getString(R.string.tip_split_screen_display_failure), e);
        }
    }

    private void clearDisplay(T1000DeviceWrap entry, int displayMode) {
        String key = entry.key;
        try {

            T1000Device t1000Device = entry.t1000Device;
            showMsg(key + ":clearDisplay");
            int startX = 0, startY = 0;
            int width = entry.width;
            int height = entry.height;
            int ret = t1000Device.clearDisplay(screenType.type, startX, startY, width, height, displayMode);
            if (ret == ErrorCode.ERROR_OK) {
                showMsg(key + getString(R.string.tip_split_screen_clear_display_success));
            } else {
                showMsg(key + getString(R.string.tip_split_screen_clear_display_failure) + ":" + ret);
            }
        } catch (Exception e) {
            LogUtils.e(key + getString(R.string.tip_split_screen_clear_display_failure), e);
        }
    }


    private void connect() {
        if (usbConnector.size() == 0) {
            Toast.makeText(mContext, getString(R.string.tip_not_find_available_connection), Toast.LENGTH_SHORT).show();
            return;
        }

        MyApplication.Companion.getExecutor().execute(new Runnable() {
            @Override
            public void run() {
                for (Map.Entry<String, T1000Device> entry : usbConnector.entrySet()) {
                    String key = entry.getKey();
                    T1000Device t1000Device = entry.getValue();
                    t1000Device.setType(screenType.type);
                    int ret = t1000Device.open();
                    if (ret == ErrorCode.ERROR_OK) {
                        showMsg(key + ":" + getString(R.string.tip_device_connect_success));
                        T1000DeviceInfo t1000DeviceInfo = t1000Device.getT1000DeviceInfo();
                        if (t1000DeviceInfo != null) {
                            showMsg(getString(R.string.tip_device_init_success)+":"+t1000DeviceInfo);
                        } else {
                            showMsg(getString(R.string.tip_device_init_failed));
                        }
                    } else {
                        showMsg(key + ":" + getString(R.string.tip_device_connect_failure) + "ret:" + ret);
                    }
                }

            }
        });

    }

    private void disconnect(View v) {
        for (Map.Entry<String, T1000Device> entry : usbConnector.entrySet()) {
            String key = entry.getKey();
            T1000Device t1000Api = entry.getValue();
            t1000Api.close();
            showMsg(key + ":" + getString(R.string.tip_device_disconnect_failure));
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
                        try {
                            sourcePath = UriUtils.fileUriToPath(MultiT1000UsbTestActivity.this, uri);
                            Bitmap bitmap = BitmapFactory.decodeFile(sourcePath);
                            int width = bitmap.getWidth();
                            int height = bitmap.getHeight();
                            showMsg(sourcePath + ": width：" + width + ",height:" + height);

                            MyApplication.Companion.getMainThreadExecutor().post(new Runnable() {
                                @Override
                                public void run() {
                                    viewBinding.ivImg.setImageBitmap(bitmap);
                                }
                            });
                        } catch (Exception e) {
                            showMsg("Abnormal loading of images:"+e.getMessage());
                        }


                    }
                });


            }
        }
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
        usbConnector.clear();
    }

    private final class MyBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                UsbDevice usbDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                showMsg("USB ATTACHED:" + usbDevice.getDeviceName() + ",vendorId:" + usbDevice.getVendorId() + ",productId:" + usbDevice.getProductId());
            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                UsbDevice usbDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                showMsg("USB DETACHED:" + usbDevice.getDeviceName() + ",vendorId:" + usbDevice.getVendorId() + ",productId:" + usbDevice.getProductId());
            }
        }
    }

}
