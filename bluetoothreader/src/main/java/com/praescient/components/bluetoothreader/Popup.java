package com.praescient.components.bluetoothreader;


import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;


public class Popup extends DialogFragment {

    private static Activity activity;
    private static Bundle args;
    private boolean showTitle = false;
    private boolean setCancelable = false;
    private boolean shown = false;

    //Interactives
    private LinearLayout thepop;
    private ImageView placeholder;
    private TextView status;

    private Handler dHandler = new Handler();

    // Constructor
    public Popup(){
        // Empty constructor is required for DialogFragment
        // Make sure not to add arguments to the constructor
        // Use `newInstance` instead as shown below
    }

    public static Popup newInstance(Activity act, String title){
        Popup fragment = new Popup();
        args = new Bundle();
        args.putString("title", title);
        fragment.setArguments(args);
        activity = act;
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.popup, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        thepop = (LinearLayout) view.findViewById(R.id.popup);
        placeholder = (ImageView) view.findViewById(R.id.placeholder);
        status = (TextView) view.findViewById(R.id.status);
        placeholder.setImageResource(R.drawable.ic_print);

        LocalBroadcastManager.getInstance(activity.getApplicationContext()).registerReceiver(statusReceiver, new IntentFilter("com.popup.status"));

        //Fetch argument from bundle and set title
        String title = getArguments().getString("title");
        getDialog().setTitle(title);

        if(args.containsKey("status")){
            status.setText(getArguments().getString("status"));
        }

        if(args.containsKey("color")){
           thepop.setBackgroundColor(activity.getApplicationContext().getResources().getColor(getArguments().getInt("color")));
        }

        if(args.containsKey("textSize")){
            status.setTextSize(getArguments().getFloat("textSize"));
        }

        if(args.containsKey("type")){

            String type = getArguments().getString("type");

            if(type != null && !type.isEmpty() && !type.equalsIgnoreCase("null")){

                if(type.equalsIgnoreCase("nfc")){
                    placeholder.setImageResource(R.drawable.ic_nfc);
                    status.setText(R.string.reader);
                }
            }

        }

    }

    @Override
    public void onStart() {
        super.onStart();

        //safety check
        if (getDialog() == null){
            return;
        }

        int width = 600;
        int height = 912;

        if(args.containsKey("width")){
            width = getArguments().getInt("width");
        }

        if(args.containsKey("height")){
            height = getArguments().getInt("height");
        }

        try {
            getDialog().getWindow().setLayout(width, height);
        } catch (Exception e){
            e.printStackTrace();
        }

    }

    @Override
    public void show(FragmentManager manager, String tag) {
        if (shown) return;
        super.show(manager, tag);
        shown = true;
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        shown = false;
        LocalBroadcastManager.getInstance(activity.getApplicationContext()).unregisterReceiver(statusReceiver);
        super.onDismiss(dialog);
    }

    public boolean isShowing(){
        return shown;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        Dialog dialog = super.onCreateDialog(savedInstanceState);

        if(!showTitle){
            dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        }

        if(!setCancelable){
            dialog.setCanceledOnTouchOutside(setCancelable);
        }

        return dialog;
    }

    public boolean showTitle(boolean value){
        return showTitle = value;
    }

    public boolean cancelable(boolean value){
        return setCancelable = value;
    }

    public void setStatus(String value){

        if (status != null){
            status.setText(value);
        } else {
            args.putString("status", value);
        }
    }

    public void setBackgroundColor(int color){
        args.putInt("color", color);
    }

    public void setTextSize(float size) { args.putFloat("textSize", size); }

    public void setWidth(int width) { args.putInt("width", width); }

    public void setHeight(int height) { args.putInt("height", height); }

    public void setType(String value) {
        args.putString("type", value);
    }

    private BroadcastReceiver statusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            Bundle extras = intent.getExtras();
            String message = null;
            byte[] image = null;

            if(extras != null){
                message = extras.getString("message");
                image = extras.getByteArray("image");
            }

            if(message != null && !message.isEmpty() && !message.equals("null")){
                setStatus(message);
            }

            if(isShowing()){
                dHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        dismiss();
                    };
                }, 1000);
            }
        }
    };

}
