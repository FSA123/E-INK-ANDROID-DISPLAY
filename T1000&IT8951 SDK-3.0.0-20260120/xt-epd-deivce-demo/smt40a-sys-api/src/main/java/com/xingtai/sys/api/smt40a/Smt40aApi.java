package com.xingtai.sys.api.smt40a;

import android.app.smdt.SmdtManager;
import android.content.Context;
import android.util.Log;

/**
 * Smt40aApi(7.1.1)
 *
 * @author Kelly
 * @version 1.0.0
 * @filename Smt40aApi
 * @time 2023/6/19 14:53
 * @copyright(C) 2023 江西兴泰科技股份有限公司
 */
public class Smt40aApi extends AbstractSmtApi {
    private static final String TAG = "Smt40aApi";

    private SmdtManager smdt;

    public Smt40aApi(Context context) {
        super(context);
        smdt = SmdtManager.create(context);
    }

    @Override
    public int setUsbPower(int num, int value) {
        /**
         * USB type
         * OTG/HOST: 1
         * HUB: 2
         */
        return smdt.smdtSetUsbPower(1, num, value);
    }

    @Override
    public void ledControl(boolean open) {
        if (open){
            smdt.smdtSetControl(5, 1);
        }else {
            smdt.smdtSetControl(5, 0);
        }
    }

    @Override
    public void installApk(String path, boolean isBoot) {
        smdt.smdtSilentInstall(path,getContext());
    }


    @Override
    public void reboot() {
        smdt.smdtReboot();
    }

    @Override
    public void shutdown() {
        smdt.shutDown();
    }



    @Override
    public void setStatusBar(boolean show) {
        smdt.smdtSetStatusBar(getContext(), show);
    }

    @Override
    public void setUsbMode(int value) {
        boolean ret = false;
        if (value == 1) {
            //0->DEVICES mode
            ret = execShell("setprop persist.sys.smdt_usb_mode 0");
        } else if (value == 0) {
            //1->HOST mode
            ret = execShell("setprop persist.sys.smdt_usb_mode 1");
        }
        Log.i(TAG, "ret:" + ret);
    }


}
