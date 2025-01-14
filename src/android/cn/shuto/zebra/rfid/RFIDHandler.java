package cn.shuto.zebra.rfid;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.zebra.rfid.api3.ACCESS_OPERATION_CODE;
import com.zebra.rfid.api3.ACCESS_OPERATION_STATUS;
import com.zebra.rfid.api3.Antennas;
import com.zebra.rfid.api3.ENUM_TRANSPORT;
import com.zebra.rfid.api3.ENUM_TRIGGER_MODE;
import com.zebra.rfid.api3.HANDHELD_TRIGGER_EVENT_TYPE;
import com.zebra.rfid.api3.INVENTORY_STATE;
import com.zebra.rfid.api3.InvalidUsageException;
import com.zebra.rfid.api3.MEMORY_BANK;
import com.zebra.rfid.api3.OperationFailureException;
import com.zebra.rfid.api3.RFIDReader;
import com.zebra.rfid.api3.ReaderDevice;
import com.zebra.rfid.api3.Readers;
import com.zebra.rfid.api3.RfidEventsListener;
import com.zebra.rfid.api3.RfidReadEvents;
import com.zebra.rfid.api3.RfidStatusEvents;
import com.zebra.rfid.api3.SESSION;
import com.zebra.rfid.api3.SL_FLAG;
import com.zebra.rfid.api3.START_TRIGGER_TYPE;
import com.zebra.rfid.api3.STATUS_EVENT_TYPE;
import com.zebra.rfid.api3.STOP_TRIGGER_TYPE;
import com.zebra.rfid.api3.TagAccess;
import com.zebra.rfid.api3.TagData;
import com.zebra.rfid.api3.TriggerInfo;

import java.util.ArrayList;

import cn.shuto.zebra.rfid.RFIDCallBack;

class RFIDHandler implements Readers.RFIDReaderEventHandler {

  final static String TAG = "RFID_SAMPLE";
  // RFID Reader
  private static Readers readers;
  private static ArrayList<ReaderDevice> availableRFIDReaderList;
  private static ReaderDevice readerDevice;
  private static RFIDReader reader;
  private EventHandler eventHandler;
  // In case of RFD8500 change reader name with intended device below from list of paired RFD8500
  String readername = "RFD40P_23045520101388";//"RFD8500123";

  private Context context;
  RFIDCallBack rfidCallBackListener;

  private int MAX_POWER = 300;

  public void setOnChangeListener(RFIDCallBack rfidCallBackListener) {
    this.rfidCallBackListener = rfidCallBackListener;
  }

  public void init(Context context) {
    this.context = context;
    InitSDK();
  }

  private void InitSDK() {
    Log.d(TAG, "InitSDK");
    if (readers == null) {
      new CreateInstanceTask().execute();
    } else {
      if (!isReaderConnected()) {
        new ConnectionTask().execute();
      }
    }
  }

  // Enumerates SDK based on host device
  private class CreateInstanceTask extends AsyncTask<Void, Void, Void> {

    @Override
    protected Void doInBackground(Void... voids) {
      Log.d(TAG, "CreateInstanceTask");
      // Based on support available on host device choose the reader type
      InvalidUsageException invalidUsageException = null;
      readers = new Readers(context, ENUM_TRANSPORT.ALL);
      try {
        availableRFIDReaderList = readers.GetAvailableRFIDReaderList();
      } catch (InvalidUsageException e) {
        e.printStackTrace();
      }
      if (invalidUsageException != null) {
        readers.Dispose();
        readers = null;
        if (readers == null) {
          readers = new Readers(context, ENUM_TRANSPORT.BLUETOOTH);
        }
      }
      return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
      super.onPostExecute(aVoid);
      connectReader();
    }
  }

  private synchronized void connectReader() {
    if (!isReaderConnected()) {
      new ConnectionTask().execute();
    }
  }

  private class ConnectionTask extends AsyncTask<Void, Void, String> {

    @Override
    protected String doInBackground(Void... voids) {
      Log.d(TAG, "ConnectionTask");
      GetAvailableReader();
      if (reader != null) {
        return connect("RFID");
      }
      return "Failed to find or connect reader";
    }
  }

  public boolean isReaderConnected() {
    if (reader != null && reader.isConnected()) {
      return true;
    } else {
      Log.d(TAG, "reader is not connected");
      return false;
    }
  }

  private synchronized void GetAvailableReader() {
    Log.d(TAG, "GetAvailableReader");
    if (readers != null) {
      readers.attach(this);
      try {
        if (readers.GetAvailableRFIDReaderList() != null) {
          availableRFIDReaderList = readers.GetAvailableRFIDReaderList();
          if (availableRFIDReaderList.size() != 0) {
            // if single reader is available then connect it
            readerDevice = availableRFIDReaderList.get(0);
            reader = readerDevice.getRFIDReader();
          }
        }
      } catch (InvalidUsageException ie) {
        ie.printStackTrace();
      }
    }
  }

  // handler for receiving reader appearance events
  @Override
  public void RFIDReaderAppeared(ReaderDevice readerDevice) {
    Log.d(TAG, "RFIDReaderAppeared " + readerDevice.getName());
    connectReader();
  }

  @Override
  public void RFIDReaderDisappeared(ReaderDevice readerDevice) {
    Log.d(TAG, "RFIDReaderDisappeared " + readerDevice.getName());
    if (readerDevice.getName().equals(reader.getHostName())) {
      disconnect();
    }
  }

  public synchronized String connect(String mode) {
    if (reader != null) {
      Log.d(TAG, "connect " + reader.getHostName());
      try {
        if (!reader.isConnected()) {
          // Establish connection to the RFID Reader
          reader.connect();
          ConfigureReader(mode);
          if (reader.isConnected()) {
            return "Connected: " + reader.getHostName();
          }
        }
      } catch (InvalidUsageException e) {
        e.printStackTrace();
      } catch (OperationFailureException e) {
        e.printStackTrace();
        Log.d(TAG, "OperationFailureException " + e.getVendorMessage());
        String des = e.getResults().toString();
        return "Connection failed" + e.getVendorMessage() + " " + des;
      }
    }
    return "";
  }

  private void ConfigureReader(String mode) {
    Log.d(TAG, "ConfigureReader " + reader.getHostName());
    if (reader.isConnected()) {
      TriggerInfo triggerInfo = new TriggerInfo();
      triggerInfo.StartTrigger.setTriggerType(START_TRIGGER_TYPE.START_TRIGGER_TYPE_IMMEDIATE);
      triggerInfo.StopTrigger.setTriggerType(STOP_TRIGGER_TYPE.STOP_TRIGGER_TYPE_IMMEDIATE);
      try {
        // receive events from reader
        if (eventHandler == null) {
          eventHandler = new EventHandler();
        }
        reader.Events.addEventsListener(eventHandler);
        // HH event
        reader.Events.setHandheldEvent(true);
        // tag event with tag data
        reader.Events.setTagReadEvent(true);
        reader.Events.setAttachTagDataWithReadEvent(false);
        // set trigger mode as rfid so scanner beam will not come
        //reader.Config.setTriggerMode(ENUM_TRIGGER_MODE.RFID_MODE, true);
        if (mode == "BARCODE") {
          reader.Config.setTriggerMode(ENUM_TRIGGER_MODE.BARCODE_MODE, true);
        } else {
          reader.Config.setTriggerMode(ENUM_TRIGGER_MODE.RFID_MODE, true);
        }

        // set start and stop triggers
        reader.Config.setStartTrigger(triggerInfo.StartTrigger);
        reader.Config.setStopTrigger(triggerInfo.StopTrigger);
        // power levels are index based so maximum power supported get the last one
        MAX_POWER = reader.ReaderCapabilities.getTransmitPowerLevelValues().length - 1;
        // set antenna configurations
        Antennas.AntennaRfConfig config = reader.Config.Antennas.getAntennaRfConfig(1);
        config.setTransmitPowerIndex(MAX_POWER);
        config.setrfModeTableIndex(0);
        config.setTari(0);
        reader.Config.Antennas.setAntennaRfConfig(1, config);
        // Set the singulation control
        Antennas.SingulationControl s1_singulationControl = reader.Config.Antennas.getSingulationControl(1);
        s1_singulationControl.setSession(SESSION.SESSION_S0);
        s1_singulationControl.Action.setInventoryState(INVENTORY_STATE.INVENTORY_STATE_A);
        s1_singulationControl.Action.setSLFlag(SL_FLAG.SL_ALL);
        reader.Config.Antennas.setSingulationControl(1, s1_singulationControl);
        // delete any prefilters
        reader.Actions.PreFilters.deleteAll();
        //
      } catch (InvalidUsageException | OperationFailureException e) {
        e.printStackTrace();
      }
    }
  }

  public synchronized void disconnect() {
    Log.d(TAG, "disconnect " + reader);
    try {
      if (reader != null) {
        reader.Events.removeEventsListener(eventHandler);
        reader.disconnect();
      }
    } catch (InvalidUsageException e) {
      e.printStackTrace();
    } catch (OperationFailureException e) {
      e.printStackTrace();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public synchronized void dispose() {
    try {
      if (readers != null) {
        reader = null;
        readers.Dispose();
        readers = null;
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  synchronized void performInventory() {
    // check reader connection
    if (!isReaderConnected()) {
      return;
    }
    try {
      reader.Actions.Inventory.perform();
    } catch (InvalidUsageException e) {
      e.printStackTrace();
    } catch (OperationFailureException e) {
      e.printStackTrace();
    }
  }

  synchronized void stopInventory() {
    // check reader connection
    if (!isReaderConnected()) {
      return;
    }
    try {
      reader.Actions.Inventory.stop();
    } catch (InvalidUsageException e) {
      e.printStackTrace();
    } catch (OperationFailureException e) {
      e.printStackTrace();
    }
  }

  public synchronized void setTriggerMode(String mode) {
    try {
      if (mode.equals("RFID")) {
        reader.Config.setTriggerMode(ENUM_TRIGGER_MODE.RFID_MODE, true);
      } else if (mode.equals("BARCODE")) {
        reader.Config.setTriggerMode(ENUM_TRIGGER_MODE.BARCODE_MODE, true);
      }
    } catch (InvalidUsageException e) {
      e.printStackTrace();
    } catch (OperationFailureException e) {
      e.printStackTrace();
    }
  }

  public synchronized void setAntennaPower(int new_power) {
    try {
      // get the configuration
      Antennas.AntennaRfConfig config = reader.Config.Antennas.getAntennaRfConfig(1);
      this.MAX_POWER = new_power;
      config.setTransmitPowerIndex(this.MAX_POWER);
      config.setrfModeTableIndex(0);
      config.setTari(0);
      reader.Config.Antennas.setAntennaRfConfig(1, config);
    } catch (InvalidUsageException e) {
      e.printStackTrace();
    } catch (OperationFailureException e) {
      e.printStackTrace();
    }
  }

  public synchronized void setMaxPower(int new_power) {
    this.MAX_POWER = new_power;
  }

  public void writeTag(String sourceEPC, String Password, String targetData, int offset) {
    Log.d(TAG, "WriteTag " + targetData);
    try {
      MEMORY_BANK memory_bank = MEMORY_BANK.MEMORY_BANK_USER;
      TagData tagData = null;
      String tagId = sourceEPC;
      TagAccess tagAccess = new TagAccess();
      TagAccess.WriteAccessParams writeAccessParams = tagAccess.new WriteAccessParams();
      String writeData = targetData; //write data in string
      writeAccessParams.setAccessPassword(Long.parseLong(Password, 16));
      writeAccessParams.setMemoryBank(memory_bank);
      writeAccessParams.setOffset(offset); // start writing from word offset 0
      writeAccessParams.setWriteData(writeData);
      // set retries in case of partial write happens
      writeAccessParams.setWriteRetries(3);
      // data length in words
      writeAccessParams.setWriteDataLength(writeData.length() / 4);
      // 5th parameter bPrefilter flag is true which means API will apply pre filter internally
      // 6th parameter should be true in case of changing EPC ID it self i.e. source and target both is EPC
      boolean useTIDfilter = memory_bank == MEMORY_BANK.MEMORY_BANK_EPC;
      reader.Actions.TagAccess.writeWait(tagId, writeAccessParams, null, tagData, true, useTIDfilter);
    } catch (InvalidUsageException e) {
      e.printStackTrace();
    } catch (OperationFailureException e) {
      e.printStackTrace();
    }
  }

  // Read/Status Notify handler
  // Implement the RfidEventsLister class to receive event notifications
  public class EventHandler implements RfidEventsListener {
    // Read Event Notification

    public void eventReadNotify(RfidReadEvents e) {
      // Recommended to use new method getReadTagsEx for better performance in case of large tag population
      TagData[] myTags = reader.Actions.getReadTags(100);
      if (myTags != null) {
        for (int index = 0; index < myTags.length; index++) {
          Log.d(TAG, "Tag ID " + myTags[index].getTagID());
          if (myTags[index].getOpCode() == ACCESS_OPERATION_CODE.ACCESS_OPERATION_READ
                  && myTags[index].getOpStatus() == ACCESS_OPERATION_STATUS.ACCESS_SUCCESS) {
            if (myTags[index].getMemoryBankData().length() > 0) {
              Log.d(TAG, " Mem Bank Data " + myTags[index].getMemoryBankData());
            }
          }
          if (myTags[index].isContainsLocationInfo()) {
            short dist = myTags[index].LocationInfo.getRelativeDistance();
            Log.d(TAG, "Tag relative distance " + dist);
          }
        }
        // possibly if operation was invoked from async task and still busy
        // handle tag data responses on parallel thread thus THREAD_POOL_EXECUTOR
        new AsyncDataUpdate().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, myTags);
      }
    }

    // Status Event Notification
    public void eventStatusNotify(RfidStatusEvents rfidStatusEvents) {
      Log.d(TAG, "Status Notification: " + rfidStatusEvents.StatusEventData.getStatusEventType());
      if (rfidStatusEvents.StatusEventData.getStatusEventType() == STATUS_EVENT_TYPE.HANDHELD_TRIGGER_EVENT) {
        if (rfidStatusEvents.StatusEventData.HandheldTriggerEventData.getHandheldEvent() == HANDHELD_TRIGGER_EVENT_TYPE.HANDHELD_TRIGGER_PRESSED) {
          new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
              rfidCallBackListener.handleTriggerPress(true);
              return null;
            }
          }.execute();
        }
        if (rfidStatusEvents.StatusEventData.HandheldTriggerEventData.getHandheldEvent() == HANDHELD_TRIGGER_EVENT_TYPE.HANDHELD_TRIGGER_RELEASED) {
          new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
              rfidCallBackListener.handleTriggerPress(false);
              return null;
            }
          }.execute();
        }
      }
    }
  }

  private class AsyncDataUpdate extends AsyncTask<TagData[], Void, Void> {

    @Override
    protected Void doInBackground(TagData[]... params) {
      rfidCallBackListener.handleTagData(params[0]);
      return null;
    }
  }

}
