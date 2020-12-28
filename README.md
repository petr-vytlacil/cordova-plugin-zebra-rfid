#### 插件介绍
Cordova Zebra Rfid 读取插件

#### 插件安装
`cordova plugin add https://github.com/shuto-cn/cordova-plugin-zebra-rfid.git`

#### 插件使用
- 初始化RFID
	```
	// AppReady时初始化rfid
	zebraRfid.init("", function (rs) {
		// 读取到的结果，需要去重
		alert(JSON.stringify(rs));
	}, function (err) {
		
	})
	```