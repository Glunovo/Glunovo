package com.example.asus.blecurrent.ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.RequiresApi;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.UUID;

/**
 * Created by ASUS on 2017/6/7.
 */

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
public class BluetoothUtils {
    public static final int ENABLE_BLUETOOTH = 0;          // 发现蓝牙未开启发送的开启蓝牙消息

    public static final int DEVICE_SCAN_STARTED = 1;       // 扫描设备开始时发送的消息

    public static final int DEVICE_SCAN_STOPPED = 2;       // 扫描终止时发送的消息

    public static final int DEVICE_SCAN_COMPLETED = 3;     // 扫描设备完成时发送的消息

    public static final int DEVICE_CONNECTED = 4;          // 连接上设备时发送的消息

    public static final int DATA_SENDED = 5;               // 发送数据后

    public static final int DATA_READED_2AAC= 6;               // 读取到数据后发送使适配器更新的消息

    public static final int CHARACTERISTIC_ACCESSIBLE = 7; // 可操作特征值时发送的消息

    public static final int DEVICE_CONNECTING=8;

    public static final int DEVICE_DISCONNECTED=9;

    public static final int DATA_READED_2AA7= 10;

    private boolean mScanning;                             // 设备扫描状态的标志

    private byte[] readedData;                             // 读取到的字节数组数据



    private Context context;

    private Handler handler;

    private BluetoothAdapter mBleAdapter;

    private BluetoothGatt mBluetoothGatt;

    private BluetoothGattCharacteristic mCharacteristic;

    private BluetoothGattCharacteristic mCharacteristicNotice;



    //  private DeviceListAdapter mDeviceListAdapter;

    ArrayList<BlueBean> mLeDevices;

    private DataBuffer dataBuffer;

    private DataBuffer sendDataBuffer;

    private Timer sendTimer;



    public static String serviceUuid = "0000181f-0000-1000-8000-00805f9b34fb";

    public static String characterUuid = "00002aac-0000-1000-8000-00805f9b34fb ";

    public static String characterUuidNotice = "00002aa7-0000-1000-8000-00805f9b34fb";

   /* public static String serviceUuid = "e7810a71-73ae-499d-8c15-faa9aef0c3f2";

    public static String characterUuid = "bef8d6c9-9c21-4c9e-b632-bd58c1009f9f";

     public static String characterUuidNotice ="bef8d6c9-9c21-4c9e-b632-bd58c1009f9f";*/

    //该uuid必须可以通知，否则无法触发onCharacteristicChanged读值，可以和characterUuid相同但characterUuid必须可通知





    public static final String CHARDATA_2AA7 = "00002aa7-0000-1000-8000-00805f9b34fb";
    public static final String CHARDATA_2AAC="00002aac-0000-1000-8000-00805f9b34fb";
    public BluetoothGattCharacteristic CHAR_2AA7,CHAR_2AAC;



    public BluetoothUtils(Context context, Handler handler) {

        this.context = context;

        this.handler = handler;



        dataBuffer = new DataBuffer(4096);

        sendDataBuffer = new DataBuffer(4096);



    }


    public void initialize() {

        BluetoothManager manager = (BluetoothManager)

                context.getSystemService(Context.BLUETOOTH_SERVICE);

        mBleAdapter = manager.getAdapter();

        mLeDevices = new  ArrayList<BlueBean>();

    }



    /**

     * 检测蓝牙开启状态，若未开启则发送开启蓝牙消息

     */

    public void checkBluetoothEnabled() {

        if (mBleAdapter == null || !mBleAdapter.isEnabled()) {

            Message message = new Message();

            message.what = ENABLE_BLUETOOTH;

            handler.sendMessage(message);

        }

    }



    /**

     * 检测当前设备扫描的状态，若在扫描中则停止扫描

     */

    public void checkDeviceScanning() {

        if (mScanning) {

            scanBleDevice(false);

        }

    }



    /**

     * 检测蓝牙连接状态，若已连接则断开并关闭连接

     */

    public void checkGattConnected() {

        if (mBluetoothGatt != null) {

            if (mBluetoothGatt.connect()) {

                mBluetoothGatt.disconnect();

                mBluetoothGatt.close();

            }

        }

    }

    public void disconnect(){
        if (mBluetoothGatt!=null){
            mBluetoothGatt.disconnect();
        }
    }

    public BluetoothGatt getBleGatt(){

        return mBluetoothGatt;

    }



    /**

     * 扫描设备的方法，扫描按钮点击后调用，扫描持续3秒

     *

     * @param enable 扫描方法的使能标志

     */

    public void scanBleDevice(boolean enable) {

        if (enable) {

            handler.postDelayed(new Runnable() {

                @Override

                public void run() {

                    mScanning = false;

                    mBleAdapter.stopLeScan(mLeScanCallback);



                    Message message = new Message();

                    message.what = DEVICE_SCAN_COMPLETED;

                    handler.sendMessage(message);

                }

            }, 5000);

            mScanning = true;

            //mBleAdapter.startLeScan(new UUID[] {UUID.fromString("0000F445-0000-1000-8000-00805F9B34FB"),UUID.fromString("0000FEE0-0000-1000-8000-00805F9B34FB")},mLeScanCallback);

            mBleAdapter.startLeScan(mLeScanCallback);

            Message message = new Message();

            message.what = DEVICE_SCAN_STARTED;

            handler.sendMessage(message);

        } else {

            mScanning = false;

            mBleAdapter.stopLeScan(mLeScanCallback);



            Message message = new Message();

            message.what = DEVICE_SCAN_STOPPED;

            handler.sendMessage(message);

        }

    }



    /**

     * 往特征值里写入数据的方法

     *

     * @param data 字节数组类型的数据

     */

    public void writeData() {

        if (mBluetoothGatt != null) {

            if (mBluetoothGatt.connect() && mCharacteristic != null) {

                int len = sendDataBuffer.getSize();

                if (len==0)

                {

                    return;

                }



                if (len>20)

                {

                    len = 20;

                }

                byte[] buf = new byte[len];

                sendDataBuffer.dequeue(buf, len);

                mCharacteristic.setValue(buf);

                mBluetoothGatt.writeCharacteristic(mCharacteristic);

            }

        }

    }

    public void sendList(byte[] b){
        //将指令放置进来
        CHAR_2AAC.setValue(b);
        //设置回复形式
        CHAR_2AAC.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
        //开始写数据
        mBluetoothGatt.writeCharacteristic(CHAR_2AAC);

    }

    public void readValue(){
        mBluetoothGatt.readCharacteristic(mCharacteristic);
    }


    public void sendToBuf(byte[] data,int len){

        sendDataBuffer.enqueue(data, len);

        writeData();

    }



    public void sendToBle(byte[] data){

        mCharacteristic.setValue(data);

        mBluetoothGatt.writeCharacteristic(mCharacteristic);

    }



    /**

     * 创建一个新的设备列表对话框

     */

   /* public void creatDeviceListDialog() {

        if (mDeviceListAdapter.getCount() > 0) {

            new AlertDialog.Builder(context).setCancelable(true)

                .setAdapter(mDeviceListAdapter, new DialogInterface.OnClickListener() {

                    @Override

                    public void onClick(DialogInterface dialog, int which) {

                        BluetoothDevice device = mDeviceListAdapter.getDevice(which);

                        mBluetoothGatt = device.connectGatt(context, false, mGattCallback);

                    }

                }).show();

        }

    }*/

    public void connect(BluetoothDevice device ){

        mBluetoothGatt= device.connectGatt(context, false, mGattCallback);

    }

    public boolean isConnected(BluetoothDevice device){

        return mBluetoothGatt.connect();

    }

    /**

     * 开启特征值的notification，然后才能读取数据

     */

//    public void setCharacteristicNotification() {
//
//
//
//        mBluetoothGatt.setCharacteristicNotification(mCharacteristic, true);
//
//        for(BluetoothGattDescriptor dp: mCharacteristic.getDescriptors()){
//            if (dp != null) {
//                if ((mCharacteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) {
//                    dp.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
//                } else if ((mCharacteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0) {
//                    dp.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
//                }
//                mBluetoothGatt.writeDescriptor(dp);
//            }
//        }

//        BluetoothGattDescriptor descriptor = mCharacteristic.
//
//                getDescriptor(UUID.fromString(clientUuid));
//
//        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
//
//        mBluetoothGatt.writeDescriptor(descriptor);
//
//
//
//    }

    public boolean onDeviceNotificationOpen(String uuid) {
        switch (uuid) {
            case CHARDATA_2AA7:
                setNotificationForChara(mBluetoothGatt, CHAR_2AA7);
//                handler.postDelayed(new Runnable() {
//                    @Override
//                    public void run() {
//                        setNotificationForChara(mBluetoothGatt, CHAR_2AA7);
//                    }
//                }, 300);
                break;
            case CHARDATA_2AAC:
                setNotificationForChara(mBluetoothGatt, CHAR_2AAC);
                break;
        }
        return false;
    }

    public void setNotificationForChara(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        final String uuid = characteristic.getUuid().toString();
        gatt.setCharacteristicNotification(characteristic, true);

        for(BluetoothGattDescriptor dp: characteristic.getDescriptors()){
            if (dp != null) {
                if ((characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) {
                    dp.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                } else if ((characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0) {
                    dp.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
                }
                mBluetoothGatt.writeDescriptor(dp);
            }
        }

    }



    /**

     * 字节数组转化为标准的16进制字符串

     *

     * @param bytes 字节数组数据

     * @return 字符串

     */

    public String  bytesToString(byte[] bytes) {

        final char[] hexArray = "0123456789ABCDEF".toCharArray();

        char[] hexChars = new char[bytes.length * 2];

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < bytes.length; i++) {

            int v = bytes[i] & 0xFF;

            hexChars[i * 2] = hexArray[v >>> 4];

            hexChars[i * 2 + 1] = hexArray[v & 0x0F];



            sb.append(hexChars[i * 2]);

            sb.append(hexChars[i * 2 + 1]);

            sb.append(' ');

        }

        return sb.toString();



    }

    public String byte2hex(byte b[]) {
         if (b == null) {
             throw new IllegalArgumentException(
                     "Argument b ( byte array ) is null! ");
             }
         String hs = "";
         String stmp = "";
         for (int n = 0; n < b.length; n++) {
             stmp = Integer.toHexString(b[n] & 0xff);
             if (stmp.length() == 1) {
                 hs = hs + "0" + stmp;
                 } else {
                 hs = hs + stmp;
                 }
             }
         return hs.toUpperCase();
        }

    public void toHex(byte[] bytes){
        for (int i=0;i<(bytes.length+1)/4;i++){
            for (int j=0;j<bytes.length;j++){

            }
        }
    }

    /**

     * 将字符串转为16进制值的字节数组

     *

     * @param s 字符串数据

     * @return buf 字节数组

     */

    public byte[] stringToBytes(String s) {

        byte[] buf = new byte[s.length() / 2];

        for (int i = 0; i < buf.length; i++) {

            try {

                buf[i] = (byte) Integer.parseInt(s.substring(i * 2, i * 2 + 2), 16);
//                Log.i("HEYY", java.lang.String.valueOf(buf[i]));

            } catch (NumberFormatException e) {

                e.printStackTrace();

            }

        }

        return buf;

    }


    public static byte[] hexStringToByte(String hex) {

        hex=hex.toUpperCase();

        int len = (hex.length() / 2);

        byte[] result = new byte[len];

        char[] achar = hex.toCharArray();

        for (int i = 0; i < len; i++) {

            int pos = i * 2;

            result[i] = (byte) (toByte(achar[pos]) << 4 | toByte(achar[pos + 1]));

        }

        return result;

    }



    private static byte toByte(char c) {

        byte b = (byte) "0123456789ABCDEF".indexOf(c);

        return b;

    }

    /**

     * Ascii编码的字节数组转化为对应的字符串

     *

     * @param bytes 字节数组

     * @return 字符串

     */

    public String asciiToString(byte[] bytes) {

        char[] buf = new char[bytes.length];

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < buf.length; i++) {

            buf[i] = (char) bytes[i];

            sb.append(buf[i]);

        }

        return sb.toString();

    }





    /**

     * 获取已连接设备的设备名

     *

     * @return 字符串形式的设备名

     */

    public String getDeviceName() {

        return mBluetoothGatt.getDevice().getName();

    }



    /**

     * 获取已读取的数据

     *

     * @return 字节数组数据

     */

    public byte[] getReadedData() {

        return readedData;

    }



    /**

     * 获取已读取的数据长度

     *

     * @return

     */

    public int getDataLen() {

        return dataBuffer.getSize();

    }



    /**

     * 获取已读取的数据

     *

     * @return

     */

    public int getData(byte[] data_out,int len) {

        return dataBuffer.dequeue(data_out, len);

    }





    /**

     * 连接Gatt之后的回调

     */

    String String="";

    String aString="";

    String bString="";

    private BluetoothGattCallback mGattCallback =

            new BluetoothGattCallback() {



                @Override

                public void onConnectionStateChange(BluetoothGatt gatt, int status,

                                                    int newState) {

                    Log.i("aaa", status+" "+newState+" "+gatt.getDevice());



                    if (newState == BluetoothProfile.STATE_CONNECTING){

                        Message message = new Message();

                        message.what = DEVICE_CONNECTING;

                        handler.sendMessage(message);

                        Log.i("aaa", "DEVICE_CONNECTING");

                        mLeDevices.clear();

                        gatt.discoverServices();

                    }

                    else  if (status==0&&newState == BluetoothProfile.STATE_CONNECTED) {

                        Message message = new Message();

                        message.what = DEVICE_CONNECTED;

                        Log.i("aaa", "STATE_CONNECTED");

                        handler.sendMessage(message);

                        mLeDevices.clear();

                        gatt.discoverServices();

                    }

                    else if (status!=0||newState == BluetoothProfile.STATE_DISCONNECTING||newState == BluetoothProfile.STATE_DISCONNECTED) {

                        Message message = new Message();

                        message.what = DEVICE_DISCONNECTED;

                        handler.sendMessage(message);

                        Log.i("aaa", "STATE_DISCONNECTED");

                        mLeDevices.clear();

                        gatt.discoverServices();

                    }

                }

                @Override

                public void onCharacteristicRead(BluetoothGatt gatt,

                                                 BluetoothGattCharacteristic characteristic, int status) {

                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        Log.e("HEYY", "读取成功" +characteristic.getValue());
                    }


//                    byte [] readedData0=characteristic.getValue();
//
//                    Log.e("hhh", bytesToString(readedData0));



                };

                @Override

                public void onCharacteristicChanged(BluetoothGatt gatt,

                                                    BluetoothGattCharacteristic characteristic) {

                    Log.i("recevied", bytesToString(characteristic.getValue()));

                    if (characteristic != null) {

                     /*   dataBuffer.enqueue(characteristic.getValue(), characteristic.getValue().length);

                        readedData=characteristic.getValue();*/


                        readedData=characteristic .getValue();

                        String s=byte2hex(readedData);

                        Log.i("recevied",s);


//                        if(readedData.length<5) return;
//
//                        if(readedData.length==14)    { aString=bytesToString(readedData).replace(" ", "");return;}
//
//                        if(readedData.length==6)  bString=bytesToString(readedData).replace(" ", "");
//
//                        if(readedData.length==20) {aString=bytesToString(readedData).replace(" ", ""); bString="";}
//
//                        byte []aa=hexStringToByte(aString+bString);
//
//
//
//                        readedData=aa;
//
//
//
//                        dataBuffer.enqueue(aa, aa.length);


                        Message message = new Message();

                        if (characteristic.getUuid().toString().equals(CHARDATA_2AAC))
                        {
                            message.what = DATA_READED_2AAC;
                        }else if (characteristic.getUuid().toString().equals(CHARDATA_2AA7)){
                            message.what=DATA_READED_2AA7;
                        }

                        message.obj=s;

                        handler.sendMessage(message);

                    }

                }



                @Override

                public void onCharacteristicWrite(BluetoothGatt gatt,

                                                  BluetoothGattCharacteristic characteristic, int status) {

                    if (status == BluetoothGatt.GATT_SUCCESS) {

                        Message message = new Message();

                        message.what = DATA_SENDED;

                        handler.sendMessage(message);

                    }

                    writeData();

                }





                public void setUUid(String serviceUuid0,String characterUuid0,String characterUuidNotice0){

                    serviceUuid=serviceUuid0;

                    characterUuid=characterUuid0;

                    characterUuidNotice=characterUuidNotice0;

                }

                @Override

                public void onServicesDiscovered(BluetoothGatt gatt, int status) {

                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        Log.i("HEYY","discoverServices已成功");

                        // 得到目标特征值

                      /*  String serviceUuid = "e7810a71-73ae-499d-8c15-faa9aef0c3f2";

                        String characterUuid = "bef8d6c9-9c21-4c9e-b632-bd58c1009f9f";*/

                        BluetoothGattService service = gatt.getService(UUID.fromString(serviceUuid)) ;
                        if (service==null){
                            Log.i("HEYY","getServices未成功");
                        }else {
                            Log.i("HEYY","getServices成功");
                        }

                        mCharacteristic = service.getCharacteristic(UUID.fromString(characterUuid));
                        if (mCharacteristic==null){
                            Log.i("HEYY","getCharacteristic未成功");
                        }else {
                            Log.i("HEYY","getCharacteristic成功");
                        }


                        mCharacteristicNotice = service.getCharacteristic(UUID

                                .fromString(characterUuidNotice));



                        //开启通知

//                        setCharacteristicNotification();



                        Message message = new Message();

                        message.what = CHARACTERISTIC_ACCESSIBLE;

                        handler.sendMessage(message);

                    }

                    List<BluetoothGattService> supportedGattServices=new ArrayList<>();
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        supportedGattServices =gatt.getServices();
                        for(int i=0;i<supportedGattServices.size();i++){
                            Log.e("AAAAA","1:BluetoothGattService UUID=:"+supportedGattServices.get(i).getUuid());
                            List<BluetoothGattCharacteristic> listGattCharacteristic=supportedGattServices.get(i).getCharacteristics();
                            for(int j=0;j<listGattCharacteristic.size();j++){
                                Log.e("a","2:   BluetoothGattCharacteristic UUID=:"+listGattCharacteristic.get(j).getUuid());
                            }
                        }
                    } else {
                        Log.e("AAAAA", "onservicesdiscovered收到: " + status);
                    }

                    for (BluetoothGattService gattService :supportedGattServices) {
                        List<BluetoothGattCharacteristic> gattCharacteristics =gattService.getCharacteristics();
                        for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                            int charaProp = gattCharacteristic.getProperties();
                            if ((charaProp & BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
                                 Log.e("nihao","gattCharacteristic的UUID为:"+gattCharacteristic.getUuid());
                                 Log.e("nihao","gattCharacteristic的属性为:  可读");
                            }
                            if ((charaProp & BluetoothGattCharacteristic.PROPERTY_WRITE) > 0) {
                                 Log.e("nihao","gattCharacteristic的UUID为:"+gattCharacteristic.getUuid());
                                 Log.e("nihao","gattCharacteristic的属性为:  可写");
                            }
                            if ((charaProp & BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                                 Log.e("nihao","gattCharacteristic的UUID为:"+gattCharacteristic.getUuid()+gattCharacteristic);
                                 Log.e("nihao","gattCharacteristic的属性为:  具备通知属性");
                            }

                            if (gattCharacteristic.getUuid().toString().equalsIgnoreCase(CHARDATA_2AA7)){
                                CHAR_2AA7=gattCharacteristic;
                            }

                            if (gattCharacteristic.getUuid().toString().equalsIgnoreCase(CHARDATA_2AAC)){
                                CHAR_2AAC=gattCharacteristic;
                            }

                        }
                    }

                    if (CHAR_2AAC!=null){
                        onDeviceNotificationOpen(CHARDATA_2AAC);
                    }
                }


            };





    /**

     * 蓝牙扫描时的回调

     */

    private BluetoothAdapter.LeScanCallback mLeScanCallback =

            new BluetoothAdapter.LeScanCallback() {

                @Override

                    public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {

                    String buf = bytesToString(scanRecord);

                    System.out.println("BluetoothUtils.enclosing_method()："+device.getName()+"\nscanRecord"+buf+"rssi:"+rssi);



                    //if ("E0 FE".equals(buf.substring(0, buf.length())))

                    {
                        BlueBean bean=new BlueBean();
                        String adresses=device.getAddress();
                        Log.i("HEYY",adresses);
                        boolean flag=true;
                        if (mLeDevices.size()==0){
                            bean.setAdresses(adresses);

                            bean.setDevice(device);

                            bean.setRssi(rssi);

                            bean.setScanRecord(scanRecord);

                            mLeDevices.add(bean);
                        }else {
                            int i=0;
                            while (i<mLeDevices.size()){
//                                Log.i("HEYY",mLeDevices.get(i).getAdresses());
                                if (mLeDevices.get(i).getAdresses().equals(adresses)){
                                    flag=false;
                                }
                                i++;
                            }

                            if (flag){
                                bean.setAdresses(adresses);

                                bean.setDevice(device);

                                bean.setRssi(rssi);

                                bean.setScanRecord(scanRecord);

                                mLeDevices.add(bean);
                            }
                        }


                        //mDeviceListAdapter.notifyDataSetChanged();

                    }

                }

            };
}
