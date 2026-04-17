
## xingtai-epd-deivce-demo

本工程Demo是T1000 SDK、MCU SDK的综合应用，旨在指导用户在电子纸Android设备端通过MQTT连接CMS服务系统，并下发图片后，如何把图片显示屏幕上。

### 中文 | [English](instructions for use-en.md)

本工程有如下优点：

1. 开发者可对工程demo进行参考或者在其上面进行二次开发，打通与CMS连接的通道，完成设备终端程序开发
2. 集成了如何通过U盘刷图的示例
3. 集成了如何通过云服务（MQTT）刷图的示例


工程Demo流程图如下：


[![pAuesqH.png](https://s21.ax1x.com/2024/09/14/pAuesqH.png)](https://imgse.com/i/pAuesqH)


### **需要注意的是**：本工程Demo只针对电子纸整机设备，包括了屏模组、T1000或ITE8951 Board、Android Board（A40I）、电源板,如果用户只是购买了屏模组、T1000或ITE8951Board，需要忽略Demo中Android Board（A40I）、电源板出现的函数调用，否则会出现不可预知的异常

## 使用

**Step 1**. 使用Android Studio 编译工程并运行程序安装到电子纸Android设备端，界面如下：

[![pAuElxe.jpg](https://s21.ax1x.com/2024/09/14/pAuElxe.jpg)](https://imgse.com/i/pAuElxe)

**Step 2**.初始化设置

1. 配置对应的EPD设备类型
2. 电池类型：内置电池对应`BATTERY_3S1P`，插拔式电池对应`BATTERY_4S1P`，无电池对应`BATTERY_DIRECT`，

**Step 3**.MCU串口设置

配置Android板和电源板通讯的串口路径，使用默认即可

**Step 4**.MQTT设置

MQTT服务器参数设置，以实际服务为准


**Step 5**.刷图测试（以EPD type:25.3 EG为例）

> Display pictures via MQTT Server


1. 假设设备端消息订阅地址：`serverToDevice/13b0b4eb2e717b9e`

2. 发送Json数据：

	{
	  "action": "sendImg",
	  "devId": "13b0b4eb2e717b9e",
	  "url": "https://p6.itc.cn/images01/20201111/526473ac93954907a11fda0e21940b42.jpeg",
	  "startX":0,
	  "startY":0,
	  "width":3200,
	  "height":1800,
	  "intervalTime":30,
	  "displayMode":0
	}

3.等待设备端显示图片


> Display pictures via USB flash disk

1.约定目录U盘(格式为FAT32)资源包目录`My_Resources`如下：

[![pAuELi6.jpg](https://s21.ax1x.com/2024/09/14/pAuELi6.jpg)](https://imgse.com/i/pAuELi6)

2.slideShowImg.json
		
	[
	    {
	        "name": "1.jpg",
	        "startX": 0,
	        "startY": 0,
	        "width": 3200,
	        "height": 1800,
	        "intervalTime": 30,
	        "displayMode": 0
	    },
	    {
	        "name": "2.jpeg",
	        "startX": 0,
	        "startY": 0,
	        "width": 3200,
	        "height": 1800,
	        "intervalTime": 30,
	        "displayMode": 0
	    }
	]

3.把U盘插入到Android板对应的USB口，等待图片显示即可

## 不同设备测试图片

https://gitee.com/kellysong/sampleImgae/raw/master/S28EC/1_xt101.png
https://gitee.com/kellysong/sampleImgae/raw/master/S28EG/1_xt102.png
https://gitee.com/kellysong/sampleImgae/raw/master/S42EC/1_xt103.png
https://gitee.com/kellysong/sampleImgae/raw/master/S133EC/1_xt104.png
https://gitee.com/kellysong/sampleImgae/raw/master/S133EK/1_xt105.jpg
https://gitee.com/kellysong/sampleImgae/raw/master/S253E5/1_xt106.jpg
https://gitee.com/kellysong/sampleImgae/raw/master/S253E6/1_xt107.jpg
https://gitee.com/kellysong/sampleImgae/raw/master/S253EC/1_xt108.png
https://gitee.com/kellysong/sampleImgae/raw/master/S253EG/1_xt109.jpg
https://gitee.com/kellysong/sampleImgae/raw/master/S253EK/1_xt110.jpg
https://gitee.com/kellysong/sampleImgae/raw/master/S312EC/1_xt111.png