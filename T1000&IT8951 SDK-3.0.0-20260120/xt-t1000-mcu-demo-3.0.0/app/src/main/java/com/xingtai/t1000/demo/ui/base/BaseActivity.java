package com.xingtai.t1000.demo.ui.base;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;


import com.xingtai.t1000.demo.util.TUtils;

import java.lang.reflect.Method;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewbinding.ViewBinding;

/**
 * TODO
 *
 * @author Kelly
 * @version 1.0.0
 * @filename BaseActivity
 * @time 2021/9/11 14:40
 * @copyright(C) 2021 song
 */
public abstract class BaseActivity<VB extends ViewBinding> extends AppCompatActivity {
    protected VB viewBinding;
    protected Context mContext;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            Method inflate = TUtils.getClass(getClass()).getDeclaredMethod("inflate", LayoutInflater.class);
            viewBinding = (VB) inflate.invoke(null, getLayoutInflater());
            setContentView(viewBinding.getRoot());
        } catch (Exception e) {
            e.printStackTrace();
        }
        mContext = this;
        ready();
        initVM();
        initView();
        initListener();
        initData();

    }

    protected void ready() {
    }

    protected void initVM() {
    }


    protected abstract void initView();

    protected abstract void initListener();

    protected abstract void initData();



    public void openActivity(Class clz) {
        openActivity(clz, null);
    }

    public void openActivity(Class clz, Bundle bundle) {
        Intent intent = new Intent(mContext, clz);
        if (bundle != null) {
            intent.putExtras(bundle);
        }
        mContext.startActivity(intent);
    }



    /**
     * 判断Activity是否Destroy
     *
     * @param mActivity
     * @return
     */
    public static boolean isDestroy(Activity mActivity) {
        if (mActivity == null || mActivity.isFinishing() || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && mActivity.isDestroyed())) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        viewBinding = null;
    }

}

