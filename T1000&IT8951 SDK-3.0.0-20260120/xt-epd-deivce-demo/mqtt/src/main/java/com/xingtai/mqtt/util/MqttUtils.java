package com.xingtai.mqtt.util;

import java.io.File;

/**
 * MQTT tool class
 *
 * @author Kelly
 * @version 1.0.0
 * @filename MqttUtils
 * @time 2024/3/21 9:49
 * @copyright(C) 2024 江西兴泰科技股份有限公司
 */
public class MqttUtils {


    public static void deleteDirectory(File directory) {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null && files.length > 0) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
            directory.delete();
        }
    }
}
