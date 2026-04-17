package com.xingtai.t1000.demo.entity;

import com.xingtai.device.t1000.open.T1000Device;

/**
 * 连接包装类
 *
 * @author Kelly
 * @version 1.0.0
 * @filename T1000DeviceWrap
 * @time 2023/10/9 17:20
 * @copyright(C) 2023 江西兴泰科技股份有限公司
 */
public class T1000DeviceWrap {
    public String key;
    public T1000Device t1000Device;
    public int startX, startY;
    public int width;
    public int height;
    public byte[] pixels;
}
