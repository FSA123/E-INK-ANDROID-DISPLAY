package com.xingtai.t1000.demo.ui.main.activity;

import android.Manifest;
import android.view.KeyEvent;
import android.view.View;

import com.permissionx.guolindev.PermissionX;
import com.permissionx.guolindev.callback.RequestCallback;
import com.xingtai.t1000.demo.R;
import com.xingtai.t1000.demo.app.MyApplication;
import com.xingtai.t1000.demo.databinding.MainActivityBinding;
import com.xingtai.t1000.demo.ui.base.BaseActivity;
import com.xingtai.t1000.demo.ui.mcu.activity.McuTestActivity;
import com.xingtai.t1000.demo.ui.t1000.activity.MultiT1000UsbTestActivity;
import com.xingtai.t1000.demo.ui.t1000.activity.T1000UsbTestActivity;

import java.util.List;

import androidx.annotation.NonNull;


/**
 * TODO
 *
 * @author Kelly
 * @version 1.0.0
 * @filename MainActivity
 * @time 2023/3/7 16:48
 * @copyright(C) 2023 song
 */
public class MainActivity extends BaseActivity<MainActivityBinding> {

    String[] perms = {Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.READ_EXTERNAL_STORAGE};

    @Override
    protected void initView() {
        getSupportActionBar().setTitle(getString(R.string.app_name)+"-V"+ MyApplication.Companion.getVersion());

    }


    @Override
    protected void initListener() {

    }

    @Override
    protected void initData() {
        PermissionX.init(this).permissions(perms).request(new RequestCallback() {
            @Override
            public void onResult(boolean allGranted, @NonNull List<String> grantedList, @NonNull List<String> deniedList) {

            }
        });

    }




    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            moveTaskToBack(false);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }


    public void btnT1000Test(View view) {
        openActivity(T1000UsbTestActivity.class);
    }

    public void btnMcuTest(View view) {
        openActivity(McuTestActivity.class);
    }

    public void btnMultiScreenTest(View view) {
        openActivity(MultiT1000UsbTestActivity.class);
    }
}
