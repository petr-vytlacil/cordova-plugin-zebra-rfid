package cn.shuto.zebra.rfid;


import com.zebra.rfid.api3.TagData;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * @Auther: lizj
 * @Date: 12/25/20 16:44
 * @Description: Zebra rfid 插件
 */
public class ZebraRfidPlugin extends CordovaPlugin {

    private RFIDHandler rfidHandler;
    private CallbackContext mCallbackContext;

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        //初始化sdk
        rfidHandler = new RFIDHandler();
        rfidHandler.init(cordova.getContext().getApplicationContext());
        rfidHandler.setOnChangeListener(callBack);
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        this.mCallbackContext = callbackContext;
        return true;
    }

    RFIDCallBack callBack = new RFIDCallBack() {
        @Override
        public void handleTagData(TagData[] tagData) {
            List<String> tagIdList = new ArrayList<>();
            for (int index = 0; index < tagData.length; index++) {
                tagIdList.add(tagData[index].getTagID());
            }
            //读取成功的返回值
            JSONObject obj = new JSONObject();
            try {
                obj.put("message", "读取成功");
                obj.put("data", tagIdList);
                PluginResult result = new PluginResult(PluginResult.Status.OK, obj);
                result.setKeepCallback(true);
                mCallbackContext.sendPluginResult(result);
            } catch (JSONException e) {
                mCallbackContext.error(e.getMessage());
            }
        }

        @Override
        public void handleTriggerPress(boolean pressed) {
            if (pressed) {
                rfidHandler.performInventory();
            } else
                rfidHandler.stopInventory();
        }
    };

    void onPause() {
        rfidHandler.disconnect();
    }

    public void onDestroy() {
        rfidHandler.dispose();
    }

    void onResume() {
        rfidHandler.connect();
    }


}

