
## xingtai-epd-deivce-demo

This project demo is a comprehensive application of T1000 SDK and MCU SDK, aimed at guiding users on how to display images on the screen after connecting to the CMS service system through MQTT on the electronic paper Android device and issuing images.

### English | [中文](instructions for use.md)

This project has the following advantages:

1. Developers can refer to the project demo or conduct secondary development on it, establish a connection channel with CMS, and complete the development of the device terminal APP
2. Integrated example of how to display images via USB flash disk
3. Integrated example of how to display images via MQTT cloud server


The engineering demo flowchart is as follows:


[![pAuesqH.png](https://s21.ax1x.com/2024/09/14/pAuesqH.png)](https://imgse.com/i/pAuesqH)


### **It should be noted that**：This project demo is only for electronic paper machine equipment, including screen module, T1000 or ITE8951 Board, Android Board (A40I), and power board. If the users only purchases screen module, T1000 or ITE8951 Board, they need to ignore the function calls that appear on the Android Board (A40I) and power board in the demo, otherwise unpredictable exceptions may occur

## How to

**Step 1**. Compile the project using Android Studio and run the program to install it on the e-paper Android device. The app interface is as follows：

[![pAuElxe.jpg](https://s21.ax1x.com/2024/09/14/pAuElxe.jpg)](https://imgse.com/i/pAuElxe)

**Step 2**.Initialization settings

1. Configure the corresponding EPD device type
2. Battery type: Built in battery corresponds to BATTERY3S1P, plug-in battery corresponds to BATTERY4S1P, no battery corresponds to BATTERY1 Direct`

**Step 3**.MCU serial port setting

Configure the serial port path for communication between the Android board and the power board, using the default settings

**Step 4**.MQTT setting

MQTT server parameter settings, subject to actual service


**Step 5**.Display Test（EPD type:taking 25.3 EG as an example）

> Display pictures via MQTT Server


1. Assuming the device's message subscription address：`serverToDevice/13b0b4eb2e717b9e`

2. Sending JSON data：

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

3.Waiting for the device to display the image


> Display pictures via USB flash disk

1.The designated directory for the resource package on the USB flash drive (in FAT32 format) is as follows: My-Resourses：

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

3.Insert the USB drive into the corresponding USB port on the Android board and wait for the image to display


## Test images on different devices

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