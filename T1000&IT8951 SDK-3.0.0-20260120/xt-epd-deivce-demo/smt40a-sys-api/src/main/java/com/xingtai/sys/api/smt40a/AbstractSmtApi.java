package com.xingtai.sys.api.smt40a;

import android.content.Context;

import com.xingtai.sys.api.SysApi;

/**
 * Smt Api
 *
 * @author Kelly
 * @version 1.0.0
 * @filename AbstractSmtApi
 * @time 2023/9/7 11:55
 * @copyright(C) 2023 江西兴泰科技股份有限公司
 */
public abstract class AbstractSmtApi extends SysApi {

    public AbstractSmtApi(Context context) {
        super(context);
    }


    /**
     * Set USB port power supply
     *
     * @param num   USB number：1~3
     * @param value turn on：1， turn off：0
     * @return success: 0,  fail:-1
     */
    public abstract int setUsbPower(int num, int value);


    /**
     * led switch
     *
     * @param open  true: turn on，false: turn off
     *
     */
    public abstract void ledControl(boolean open);
}
