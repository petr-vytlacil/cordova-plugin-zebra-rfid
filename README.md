#### Plug-in Introduction
Cordova Zebra Rfid Reader Plugin

#### Plugin Installation
`cordova plugin add https://github.com/Egalite-Dev-Ops/cordova-plugin-zebra-rfid.git`

#### plug-in use
- RFID Initialization
	
	**Just call it once**
	
	**The returned result needs to be deduplicated**
	```
	// AppReady time initialization rfid
	zebraRfid.init("", function (rs) {
		alert(JSON.stringify(rs));
	}, function (err) {
		
	})
	```
- Check if the RFID is connected
	```
	$scope.check_connect = function () {
		zebraRfid.checkConnect("", function (rs) {
			alert(JSON.stringify(rs));
		}, function (err) {
			alert(err);
		})
	}
	```


- Connect to RFID

	**I don't know what the problem will be**
	```
	$scope.connect = function () {
		zebraRfid.connect("", function (rs) {
			alert(JSON.stringify(rs));
		}, function (err) {
			alert(err);
		})
	}
	```

- Disconnect RFID

	**I don't know what the problem will be**
	```
	$scope.disconnect = function () {
		zebraRfid.disconnect("", function (rs) {
			alert(JSON.stringify(rs));
		}, function (err) {
			alert(err);
		})
	}
	```