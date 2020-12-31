#### 插件介绍
Cordova Zebra Rfid 读取插件

#### 插件安装
`cordova plugin add https://github.com/shuto-cn/cordova-plugin-zebra-rfid.git`

#### 插件使用
- 初始化RFID
	
	**调用一次就好**
	
	**返回结果需要去重**
	```
	// AppReady时初始化rfid
	zebraRfid.init("", function (rs) {
		alert(JSON.stringify(rs));
	}, function (err) {
		
	})
	```
- 查看RFID是否连接
	```
	$scope.check_connect = function () {
		zebraRfid.checkConnect("", function (rs) {
			alert(JSON.stringify(rs));
		}, function (err) {
			alert(err);
		})
	}
	```


- 连接RFID

	**能不用就不用，不知道会有什么问题**
	```
	$scope.connect = function () {
		zebraRfid.connect("", function (rs) {
			alert(JSON.stringify(rs));
		}, function (err) {
			alert(err);
		})
	}
	```

- 断开RFID

	**能不用就不用，不知道会有什么问题**
	```
	$scope.disconnect = function () {
		zebraRfid.disconnect("", function (rs) {
			alert(JSON.stringify(rs));
		}, function (err) {
			alert(err);
		})
	}
	```