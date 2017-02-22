package com.praescient.components.bluetoothreader;

import android.app.Activity;
import android.app.FragmentManager;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Base64;
import android.widget.Toast;

import com.fgtit.fpcore.FPMatch;
import com.praescient.components.bluetoothservice.ConnectionService;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Timer;
import java.util.TimerTask;

public class BluetoothReader extends Activity implements com.praescient.components.bluetoothservice.BluetoothReader.ConnectionListener{

    private Context context = BluetoothReader.this;
    private FragmentManager fragmentManager;
    private com.praescient.components.bluetoothservice.BluetoothReader bluetoothReader;
    private Popup popup;

    private BluetoothDevice bluetoothDevice;
    private BluetoothSocket bluetoothSocket;

    //HF-7000
    private byte mDeviceCmd=0x00;
    private boolean mIsWork=false;
    private byte  mCmdData[]=new byte[10240];
    private int	  mCmdSize=0;

    private Timer mTimerTimeout=null;
    private TimerTask mTaskTimeout=null;
    private Handler mHandlerTimeout;

    //Command Reference
    private final static byte CMD_CAPTUREHOST=0x08;
    private final static byte CMD_MATCH=0x09;		//Match
    private final static byte CMD_WRITECARD=0x0A;	//Write Card Data
    private final static byte CMD_READCARD=0x0B;	//Read Card Data
    private final static byte CMD_CARDSN=0x0E;		//Read Card Sn
    private final static byte CMD_GETBAT=0x21;
    private final static byte CMD_GETIMAGE=0x30;
    private final static byte CMD_GETCHAR=0x31;

    public byte mRefData[]=new byte[512];
    public int mRefSize=0;
    public byte mMatData[]=new byte[512];
    public int mMatSize=0;

    public byte mCardSn[]=new byte[4];
    public byte mCardData[]=new byte[4096];
    public int mCardSize=0;
    public byte mTempData[] = new byte[4];

    public byte mBat[]=new byte[2];
    public byte mUpImage[]=new byte[73728];//36864
    public int mUpImageSize=0;

    byte[] fingerprint = new byte[512];

    // Debugging
    private static final String TAG = "BluetoothReader";
    private static final boolean D = true;

    private Runnable sendCommand;
    private Runnable receiveCommand;
    private boolean newConnection = true;

    private OutputStream outputStream;
    private Bundle extras;
    private String action = null;
    private String template = null;
    private String[] templates = null;
    private byte command;

    private Handler pHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        fragmentManager = getFragmentManager();
        bluetoothReader = com.praescient.components.bluetoothservice.BluetoothReader.newInstance(this, "Bluetooth Reader");
        popup = Popup.newInstance(this, "Reader Dialog");

        extras = getIntent().getExtras();

        if (extras != null) {
            action = extras.getString("action");
            template = extras.getString("template");
            templates = extras.getStringArray("templates");
        }

        FilterCommand();

        receiveCommand = new Runnable() {
            @Override
            public void run() {

                Message msg = bluetoothReader.getMessage();
                byte[] readBuf = (byte[]) msg.obj;
                ReceiveCommand(readBuf, msg.arg1 );
            }
        };

        bluetoothReader.setReadCommand(receiveCommand);


        if(ConnectionService.getBluetoothDevice() == null && !bluetoothReader.isConnected){
           bluetoothReader.show(fragmentManager, "bluetooth_fragment");
        } else {
            Init();
        }
    }

    private void Init(){

        if(action != null){

            if(action.equalsIgnoreCase("capture") || action.equalsIgnoreCase("match")){

                if(!popup.isShowing()){
                    popup.show(fragmentManager, "bluetooth_fragment");
                }
            }
        } else {

            if(!popup.isShowing()){
                popup.setType("nfc");
                popup.show(fragmentManager, "bluetooth_fragment");
            }
        }

        SendCommand(command,null,0,"repeat");
    }

    private void FilterCommand(){

        if(action != null){

            if(action.equalsIgnoreCase("capture") || action.equalsIgnoreCase("match")){
                command = 0x08;
            }

        } else {
            command = 0x0E;
        }
    }

    //Send Actual Command
    private void SendCommand(byte cmdid,byte[] data,int size, String... action) {

        if(mIsWork)return;

        String actionType;

        if(action.length > 0){
            actionType = action[0];
        } else {
            actionType = "DEFAULT";
        }

        if(actionType.equalsIgnoreCase("REPEAT")){

            try {
                outputStream = ConnectionService.getBluetoothSocket().getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        int sendsize=9+size;
        byte[] sendbuf = new byte[sendsize];
        sendbuf[0]='F';
        sendbuf[1]='T';
        sendbuf[2]=0;
        sendbuf[3]=0;
        sendbuf[4]=cmdid;
        sendbuf[5]=(byte)(size);
        sendbuf[6]=(byte)(size>>8);

        if(size>0) {
            for(int i=0;i<size;i++) {
                sendbuf[7+i]=data[i];
            }
        }

        int sum=calcCheckSum(sendbuf,(7+size));
        sendbuf[7+size]=(byte)(sum);
        sendbuf[8+size]=(byte)(sum>>8);

        mIsWork=true;
        TimeOutStart();
        mDeviceCmd=cmdid;
        mCmdSize=0;

        if(actionType.equalsIgnoreCase("REPEAT")){
            try {
                outputStream.write(sendbuf);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

//        else {
//            bluetoothReader.bluetoothService.write(sendbuf);
//        }

        switch(sendbuf[4]) {

            case CMD_CAPTUREHOST:

                break;
            case CMD_MATCH:

                break;
            case CMD_WRITECARD:

                break;
            case CMD_READCARD:

                break;

            case CMD_CARDSN:

                break;

            case CMD_GETBAT:

                break;
            case CMD_GETIMAGE:
                mUpImageSize=0;

                break;
            case CMD_GETCHAR:

                break;
        }
    }

    private void ReceiveCommand(byte[] databuf,int datasize) {

        if(mDeviceCmd==CMD_GETIMAGE) {

            memcpy(mUpImage,mUpImageSize,databuf,0,datasize);
            mUpImageSize=mUpImageSize+datasize;
            if(mUpImageSize>=15200){
                byte[] bmpdata = getFingerprintImage(mUpImage,152,200);
                Bitmap image = BitmapFactory.decodeByteArray(bmpdata, 0,bmpdata.length);
                //fingerprintImage.setImageBitmap(image);
                mUpImageSize=0;
                mIsWork=false;
            }

        }else{

            memcpy(mCmdData,mCmdSize,databuf,0,datasize);
            mCmdSize=mCmdSize+datasize;
            int totalsize=(byte)(mCmdData[5])+((mCmdData[6]<<8)&0xFF00)+9;

            if(mCmdSize>=totalsize){
                mCmdSize=0;
                mIsWork=false;
                TimeOutStop();
                if((mCmdData[0]=='F')&&(mCmdData[1]=='T'))	{

                    switch(mCmdData[4]) {

                        case CMD_CAPTUREHOST: {

                            int size=(byte)(mCmdData[5])+((mCmdData[6]<<8)&0xFF00)-1;

                            if(mCmdData[7]==1) {

                                memcpy(mMatData,0,mCmdData,8,size);
                                mMatSize=size;

                                if(action != null){

                                    if(action.equalsIgnoreCase("capture")){

                                        final String template = Base64.encodeToString(mMatData, Base64.DEFAULT);

                                        if(template != null && !template.isEmpty() && !template.equalsIgnoreCase("null")){

                                            sendMessage("Capture Ok!");

                                        } else {

                                            sendMessage("Capture Failed!");

                                        }

                                        pHandler.postDelayed(new Runnable() {
                                            @Override
                                            public void run() {
                                                sendBroadcast(template, "THUMB");
                                                finish();
                                            }
                                        }, 2000);

                                    } else if(action.equalsIgnoreCase("match")) {

                                        FPMatch.getInstance().InitMatch(1, "http://www.hfcctv.com/");

                                        if(template != null && !template.isEmpty() && !template.equals("null")){

                                            fingerprint = Base64.decode(template, Base64.DEFAULT);

                                            if(FPMatch.getInstance().MatchTemplate(mMatData, fingerprint)>60){

                                                sendMessage("Match Ok!");
                                                pHandler.postDelayed(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        sendBroadcast(true);
                                                        finish();
                                                    }
                                                }, 2000);

                                            } else {

                                                sendMessage("Match Failed!");
                                                pHandler.postDelayed(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        sendBroadcast(false);
                                                        finish();
                                                    }
                                                }, 2000);
                                            }

                                        } else if(templates != null && templates.length > 0){

                                            boolean match = false;

                                            for (int i = 0; i < templates.length; i++) {

                                                try {
                                                    fingerprint = Base64.decode(templates[i], Base64.DEFAULT);

                                                    if (FPMatch.getInstance().MatchTemplate(mMatData, fingerprint) > 60) {
                                                        match = true;
                                                        sendMessage("Match Ok!");
                                                    }

                                                } catch (Exception e) {
                                                    e.printStackTrace();
                                                }
                                            }

                                            final boolean finalMatch = match;

                                            if(!match){
                                                sendMessage("Match Failed!");
                                            }

                                            pHandler.postDelayed(new Runnable() {
                                                @Override
                                                public void run() {
                                                    sendBroadcast(finalMatch);
                                                    finish();
                                                }
                                            }, 2000);

                                        }  else {

                                            sendMessage("Match Failed!");
                                            pHandler.postDelayed(new Runnable() {
                                                @Override
                                                public void run() {
                                                    sendBroadcast(false);
                                                    finish();
                                                }
                                            }, 2000);
                                        }

                                    }

                                }

                            } else {
                                sendMessage("null");
                            }
                        }
                        break;
                        case CMD_CARDSN: {

                            int size=(byte)(mCmdData[5])+((mCmdData[6]<<8)&0xF0)-1;

                            if(size>0) {

                                memcpy(mCardSn,0,mCmdData,8,size);

                                final String Serial = Integer.toHexString(mCardSn[0]&0xFF)+Integer.toHexString(mCardSn[1]&0xFF)+Integer.toHexString(mCardSn[2]&0xFF)+Integer.toHexString(mCardSn[3]&0xFF);

                                sendMessage("Read OK!");
                                pHandler.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        sendBroadcast(Serial, "CARD");
                                        finish();
                                    }
                                }, 2000);
                            }
                        }
                        break;

                    }
                }
            }
        }
    }

    private void memcpy(byte[] dstbuf,int dstoffset,byte[] srcbuf,int srcoffset,int size) {
        for(int i=0;i<size;i++) {
            dstbuf[dstoffset+i]=srcbuf[srcoffset+i];
        }
    }

    public byte[] getFingerprintImage(byte[] data,int width,int height) {
        if (data == null) {
            return null;
        }
        byte[] imageData = new byte[data.length * 2];
        for (int i = 0; i < data.length; i++) {
            imageData[i * 2] = (byte) (data[i] & 0xf0);
            imageData[i * 2 + 1] = (byte) (data[i] << 4 & 0xf0);
        }
        byte[] bmpData = toBmpByte(width, height, imageData);
        return bmpData;
    }

    private int calcCheckSum(byte[] buffer,int size) {
        int sum=0;
        for(int i=0;i<size;i++) {
            sum=sum+buffer[i];
        }
        return (sum & 0x00ff);
    }

    private byte[] changeByte(int data) {
        byte b4 = (byte) ((data) >> 24);
        byte b3 = (byte) (((data) << 8) >> 24);
        byte b2 = (byte) (((data) << 16) >> 24);
        byte b1 = (byte) (((data) << 24) >> 24);
        byte[] bytes = { b1, b2, b3, b4 };
        return bytes;
    }

    private byte[] toBmpByte(int width, int height, byte[] data) {

        byte[] buffer = null;

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);

            int bfType = 0x424d;
            int bfSize = 54 + 1024 + width * height;
            int bfReserved1 = 0;
            int bfReserved2 = 0;
            int bfOffBits = 54 + 1024;

            dos.writeShort(bfType);
            dos.write(changeByte(bfSize), 0, 4);
            dos.write(changeByte(bfReserved1), 0, 2);
            dos.write(changeByte(bfReserved2), 0, 2);
            dos.write(changeByte(bfOffBits), 0, 4);

            int biSize = 40;
            int biWidth = width;
            int biHeight = height;
            int biPlanes = 1;
            int biBitcount = 8;
            int biCompression = 0;
            int biSizeImage = width * height;
            int biXPelsPerMeter = 0;
            int biYPelsPerMeter = 0;
            int biClrUsed = 256;
            int biClrImportant = 0;

            dos.write(changeByte(biSize), 0, 4);
            dos.write(changeByte(biWidth), 0, 4);
            dos.write(changeByte(biHeight), 0, 4);
            dos.write(changeByte(biPlanes), 0, 2);
            dos.write(changeByte(biBitcount), 0, 2);
            dos.write(changeByte(biCompression), 0, 4);
            dos.write(changeByte(biSizeImage), 0, 4);
            dos.write(changeByte(biXPelsPerMeter), 0, 4);
            dos.write(changeByte(biYPelsPerMeter), 0, 4);
            dos.write(changeByte(biClrUsed), 0, 4);
            dos.write(changeByte(biClrImportant), 0, 4);

            byte[] palatte = new byte[1024];
            for (int i = 0; i < 256; i++) {
                palatte[i * 4] = (byte) i;
                palatte[i * 4 + 1] = (byte) i;
                palatte[i * 4 + 2] = (byte) i;
                palatte[i * 4 + 3] = 0;
            }
            dos.write(palatte);

            dos.write(data);
            dos.flush();
            buffer = baos.toByteArray();
            dos.close();
            baos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return buffer;
    }

    public void TimeOutStart() {
        if(mTimerTimeout!=null){
            return;
        }
        mTimerTimeout = new Timer();
        mHandlerTimeout = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                TimeOutStop();
                if(mIsWork){
                    mIsWork=false;
                    //Toast.makeText(context, "Time Out", Toast.LENGTH_SHORT).show();
                }
                super.handleMessage(msg);
            }
        };
        mTaskTimeout = new TimerTask() {
            @Override
            public void run() {
                Message message = new Message();
                message.what = 1;
                mHandlerTimeout.sendMessage(message);
            }
        };
        mTimerTimeout.schedule(mTaskTimeout, 10000, 10000);
    }

    public void TimeOutStop() {
        if (mTimerTimeout!=null) {
            mTimerTimeout.cancel();
            mTimerTimeout = null;
            mTaskTimeout.cancel();
            mTaskTimeout=null;
        }
    }

    @Override
    public void isConnected(boolean value) {

        if(value){
            bluetoothReader.dismiss();
            Init();
        }
    }

    //Send Broadcast
    private void sendBroadcast(String value, String type){

        Intent intent = new Intent();
        intent.putExtra("result", value);

        if(type.equalsIgnoreCase("CARD")){

            intent.setAction("com.reader.card");

        } else if(type.equalsIgnoreCase("THUMB")){

            intent.setAction("com.reader.capture");
        }

        sendBroadcast(intent);
    }

    private void sendBroadcast(Boolean value){
        Intent intent = new Intent();
        intent.putExtra("result", value);
        intent.setAction("com.reader.match");
        sendBroadcast(intent);
    }

    private void sendMessage(String message){
        Intent intent = new Intent("com.popup.status");
        intent.putExtra("message", message);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    private void sendMessage(byte[] image){
        Intent intent = new Intent("com.popup.status");
        intent.putExtra("image", image);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    @Override
    protected void onPause() {
        super.onPause();

        if(popup.isShowing()){
            popup.dismiss();
        }
    }
}
