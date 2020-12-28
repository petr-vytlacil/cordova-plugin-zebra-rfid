package cn.shuto.zebra.rfid;


import com.zebra.rfid.api3.TagData;

/**
 * @Auther: lizj
 * @Date: 12/25/20 17:03
 * @Description: rfid 扫码回调
 */
interface RFIDCallBack {
    void handleTagData(TagData[] tagData);

    void handleTriggerPress(boolean pressed);
}
