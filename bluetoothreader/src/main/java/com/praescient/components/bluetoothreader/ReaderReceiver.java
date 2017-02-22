package com.praescient.components.bluetoothreader;


import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.Switch;
import android.widget.Toast;

public class ReaderReceiver extends BroadcastReceiver{

    //Global Variables
    public static CaptureListener captureListener;
    public static CardListener cardListener;
    public static MatchListener matchListener;

    public interface CaptureListener{
        void isCaptured(String template);
    }

    public interface CardListener{
        void isRead(String value);
    }

    public interface MatchListener{
        void isMatch(boolean value);
    }


    @Override
    public void onReceive(Context context, Intent intent) {

       // context.unregisterReceiver(this);

        String action = intent.getAction();

        switch (action){

            case "com.reader.capture":

                String template = intent.getStringExtra("result");
                captureListener.isCaptured(template);
                break;

            case "com.reader.card":

                String serial = intent.getStringExtra("result");
                cardListener.isRead(serial);
                break;

            case "com.reader.match":

                Boolean value = intent.getBooleanExtra("result",false);
                matchListener.isMatch(value);

                break;
        }

        //context.unregisterReceiver(this);
       // LocalBroadcastManager.getInstance(context).unregisterReceiver(this);
    }
}
