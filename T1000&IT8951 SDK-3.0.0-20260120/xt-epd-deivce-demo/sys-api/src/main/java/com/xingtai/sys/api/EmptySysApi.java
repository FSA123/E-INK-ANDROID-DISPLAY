package com.xingtai.sys.api;

import android.content.Context;

/**
 * Empty implementation
 *
 * @author Kelly
 * @version 1.0.0
 * @filename EmptySysApi
 * @time 2023/4/26 13:59
 * @copyright(C) 2023 江西兴泰科技股份有限公司
 */
public class EmptySysApi extends SysApi {
    public EmptySysApi(Context context) {
        super(context);
    }

    @Override
    public void installApk(String path, boolean isBoot) {

    }

    @Override
    public void reboot() {

    }

    @Override
    public void shutdown() {

    }

    @Override
    public void setStatusBar(boolean show) {

    }

    @Override
    public void setUsbMode(int value) {

    }


}
