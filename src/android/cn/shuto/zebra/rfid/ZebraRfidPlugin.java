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

import java.util.HashSet;
import java.util.Set;

/**
 * @Auther: lizj
 * @Date: 12/25/20 16:44，
 * @Description: Zebra rfid �?�件
 */
public class ZebraRfidPlugin extends CordovaPlugin {

  private RFIDHandler rfidHandler;
  private CallbackContext mCallbackContext;

  // 检测是�?�连接
  private static final String CHECK_CONNECT = "check_connect";
  // 连接
  private static final String CONNECT = "connect";
  // 断开连接
  private static final String DISCONNECT = "disconnect";

  private static final String CHANGEMODE = "change_mode";

  // 读�?�到的ID
  private Set<String> tagIdSet = new HashSet<>();
  // 是�?��?�客户端�?��?消�?�
  private boolean isSend = false;
  // 是�?�正在处�?�消�?�
  private boolean isHandle = false;

  @Override
  public void initialize(CordovaInterface cordova, CordovaWebView webView) {
    super.initialize(cordova, webView);
    //�?始化sdk
    rfidHandler = new RFIDHandler();
    rfidHandler.init(cordova.getContext().getApplicationContext());
    rfidHandler.setOnChangeListener(callBack);
  }

  @Override
  public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
    this.mCallbackContext = callbackContext;

    switch (action) {
      case CHECK_CONNECT:
        JSONObject obj = new JSONObject();
        try {
          boolean isConnect = rfidHandler.isReaderConnected();
          if (isConnect) {
            obj.put("code", 1);
            obj.put("msg", "已连接");
          } else {
            obj.put("code", 0);
            obj.put("msg", "未连接");
          }
          PluginResult result = new PluginResult(PluginResult.Status.OK, obj);
          result.setKeepCallback(true);
          mCallbackContext.sendPluginResult(result);
        } catch (Error e) {
          obj.put("msg", e.getMessage());
          mCallbackContext.error(obj);
        }
        break;
      case CONNECT:
        JSONObject obj1 = new JSONObject();
        try {
          String connect = rfidHandler.connect();
          if ("Connected".equals(connect)) {
            obj1.put("code", 1);
            obj1.put("msg", "连接�?功");
          } else {
            obj1.put("code", 0);
            obj1.put("msg", "连接失败");
          }
          PluginResult result = new PluginResult(PluginResult.Status.OK, obj1);
          result.setKeepCallback(true);
          mCallbackContext.sendPluginResult(result);
        } catch (Error e) {
          obj1.put("msg", e.getMessage());
          mCallbackContext.error(obj1);
        }
        break;
      case DISCONNECT:
        JSONObject obj2 = new JSONObject();
        try {
          rfidHandler.disconnect();
          obj2.put("code", 1);
          obj2.put("msg", "断开�?功");
          PluginResult result = new PluginResult(PluginResult.Status.OK, obj2);
          result.setKeepCallback(true);
          mCallbackContext.sendPluginResult(result);
        } catch (Error e) {
          obj2.put("msg", e.getMessage());
          mCallbackContext.error(obj2);
        }
        break;
      case CHANGEMODE:
        JSONObject obj3 = new JSONObject();
        int mode = args.optString(0);
        try {
          rfidHandler.setTriggerMode(mode);
          obj3.put("code", 1);
          obj3.put("msg", "Mode Changed");
          PluginResult result = new PluginResult(PluginResult.Status.OK, obj3);
          result.setKeepCallback(true);
          mCallbackContext.sendPluginResult(result);
        } catch (Error e) {
          obj3.put("msg", e.getMessage());
          mCallbackContext.error(obj3);
        }

    }
    return true;
  }

  RFIDCallBack callBack = new RFIDCallBack() {
    @Override
    public void handleTagData(TagData[] tagData) {
      // 处�?�一下，如果一直返回�?�一个rfid，则�?�返回一次
      // �?�有在新增id时，�?�?新返回
      if (isHandle) {
        return;
      }
      isHandle = true;
      for (TagData tagDatum : tagData) {
        if (tagIdSet.contains(tagDatum.getTagID())) {
          isSend = false;
          break;
        } else {
          tagIdSet.add(tagDatum.getTagID());
          isSend = true;
        }
      }
      if (isSend && !tagIdSet.isEmpty()) {
        //读�?��?功的返回值
        JSONObject obj = new JSONObject();
        try {
          obj.put("code", "1");
          obj.put("data", tagIdSet);
          PluginResult result = new PluginResult(PluginResult.Status.OK, obj);
          result.setKeepCallback(true);
          mCallbackContext.sendPluginResult(result);
        } catch (JSONException e) {
          mCallbackContext.error(e.getMessage());
        }
      }
      isHandle = false;
    }

    @Override
    public void handleTriggerPress(boolean pressed) {
      if (pressed) {
        tagIdSet.clear();
        rfidHandler.performInventory();
      } else {
        rfidHandler.stopInventory();
      }
    }
  };

  @Override
  public void onDestroy() {
    rfidHandler.dispose();
  }

  @Override
  public void onPause(boolean multitasking) {
    rfidHandler.disconnect();
  }

  public void onResume(boolean multitasking) {
    rfidHandler.connect();
  }

}
