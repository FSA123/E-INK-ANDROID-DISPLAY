package com.xingtai.sys.api;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Arrays;

/**
 * 系统常用api基类,公共的api
 *
 * @author Kelly
 * @version 1.0.0
 * @filename SysApi
 * @time 2023/4/26 11:43
 * @copyright(C) 2023 江西兴泰科技股份有限公司
 */
public abstract class SysApi {
    private static final String TAG = "SysApi";
    private Context mContext;

    public SysApi(Context context) {
        this.mContext = context;
    }

    public Context getContext() {
        return mContext;
    }

    protected void sendBroadcast(Intent intent) {
        mContext.sendBroadcast(intent);
    }

    /**
     * Silent installation
     *
     * @param path   The complete path for installing the application is required, it cannot be a private path for the application
     * @param isBoot true: installation completed, launch application, false: installation completed without launching the application
     */
    public abstract void installApk(String path, boolean isBoot);

    /**
     * System restart
     */
    public abstract void reboot();


    /**
     * System shutdown
     */
    public abstract void shutdown();


    /**
     * Status bar control
     *
     * @param show true: show the status bar, false: hide the status bar
     */
    public abstract void setStatusBar(boolean show);

    /**
     * Set USB mode
     *
     * @param value 0 = USB mode（Can use USB drive），1 = OTG mode（Can be debugged here (acting as a slave device)）
     */
    public abstract void setUsbMode(int value);

    /**
     * Execute shell, single cmd
     *
     * @param cmd
     * @return
     */
    public static boolean execShell(final String cmd) {
        return execShell(new String[]{cmd});
    }

    /**
     * Execute shell, multiple cmd
     *
     * @param cmds
     * @return
     */
    public static boolean execShell(String[] cmds) {
        boolean result = false;
        DataOutputStream outputStream;
        Process process = null;
        BufferedReader reader = null;
        try {

            // Apply for su permission
            process = Runtime.getRuntime().exec("su");
            outputStream = new DataOutputStream(process.getOutputStream());
            // Batch execute cmd commands

            for (String command : cmds) {
                if (command == null) continue;
                outputStream.write(command.getBytes(Charset.forName("utf-8")));
                outputStream.flush();
            }
            outputStream.close();
            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String msg = "";
            String line;
            // 读取命令的执行结果
            while ((line = reader.readLine()) != null) {
                msg += line;
            }
            Log.e(TAG, "msg: " + msg);

        } catch (Exception e) {
            Log.e(TAG, "exec：" + Arrays.toString(cmds) + "abnormal", e);
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
                Log.e(TAG, e.getMessage(), e);
            }
            try {
                if (process != null) {
                    int i = process.waitFor();
                    result = i == 0;
                    process.destroy();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return result;
    }
}
