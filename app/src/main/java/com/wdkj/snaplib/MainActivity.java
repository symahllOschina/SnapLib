package com.wdkj.snaplib;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import com.geekmaker.paykeyboard.DefaultKeyboardListener;
import com.geekmaker.paykeyboard.ICheckListener;
import com.geekmaker.paykeyboard.IPayRequest;
import com.geekmaker.paykeyboard.PayKeyboard;
import com.geekmaker.paykeyboard.USBDetector;
import com.wdkj.snaplib.mylibrary.SnapLibrary;

import java.math.BigDecimal;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity implements ICheckListener {

    private Handler handler = new Handler();
    private EditText eventLog;
    private PayKeyboard keyboard;
    private EditText  wifi,baudrate;
    private EditText gprs;
    private USBDetector detector;
    private Spinner spinner;/**/





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.main_open_camera).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SnapLibrary.openCamera();
                startActivity(new Intent(MainActivity.this,TestActivity.class));
            }
        });
        findViewById(R.id.main_close_camera).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SnapLibrary.closeCamera();
                startActivity(new Intent(MainActivity.this,TestActivity.class));
            }
        });
        eventLog = findViewById(R.id.eventLog);

        wifi = findViewById(R.id.wifi);
        gprs = findViewById(R.id.gprs);



        baudrate = findViewById(R.id.baudrate);

        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                //Toast.makeText(ShowAddressActivity.this,"beforeTextChanged ",Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                //Toast.makeText(ShowAddressActivity.this,"onTextChanged ",Toast.LENGTH_SHORT).show();
            }

            @Override
            public void afterTextChanged(Editable s) {
                //Toast.makeText(ShowAddressActivity.this,"afterTextChanged ",Toast.LENGTH_SHORT).show();
                updateSignal();
            }
        };
        wifi.addTextChangedListener(textWatcher);
        gprs.addTextChangedListener(textWatcher);

        detector =  PayKeyboard.getDetector(this);
        detector.setListener(this);
        spinner = findViewById(R.id.layoutList);
        spinner.setAdapter(new ArrayAdapter<String>(this,
                R.layout.support_simple_spinner_dropdown_item,
                Arrays.asList("默认布局","布局1")));
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if(keyboard!=null) {
                    keyboard.setLayout(position);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        double result = 0.0D;
        double num1  = Double.parseDouble( "2176.39");
        double num2 =  Double.parseDouble("46.46");

        result = num1 + num2;
        String ret = (BigDecimal.valueOf(result)).toPlainString();
        Log.i("Calc",ret);
    }

    @Override
    protected void onResume() {
        super.onResume();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        Log.i("KeyboardUI","activity start!!!!!!");
        openKeyboard();
    }

    private void openKeyboard(){
        if(keyboard==null||keyboard.isReleased()){
            keyboard = PayKeyboard.get(getApplicationContext());
            if(keyboard!=null) {
                if(spinner.getSelectedItemPosition()>=0) {
                    keyboard.setLayout(spinner.getSelectedItemPosition());
                }
                if(baudrate.getText().length()>0){
                    keyboard.setBaudRate(Integer.parseInt(baudrate.getText().toString()));
                }
                keyboard.setListener(new DefaultKeyboardListener() {
                    @Override
                    public void onRelease() {
                        super.onRelease();
                        keyboard = null;
                        Log.e("KeyboardUI", "Keyboard release!!!!!!");
                    }

                    @Override
                    public void onDisplayUpdate(final String text) {
                        super.onDisplayUpdate(text);
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                eventLog.setMovementMethod(ScrollingMovementMethod.getInstance());
                                eventLog.setSelection(eventLog.getText().length(), eventLog.getText().length());
                                eventLog.getText().append(String.format("lastupdate  : %s \n ",text));
                            }
                        });
                        Log.e("KeyboardUI",String.format("display update %s",text));
                    }

                    @Override
                    public void onAvailable() {
                        super.onAvailable();
                        if(keyboard==null){
                            return;
                        }
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                keyboard.showTip("0");
                            }
                        },1000);
                        Log.e("KeyboardUI","键盘可用！");
                    }

                    @Override
                    public void onException(Exception e) {
                        Log.e("KeyboardUI", "usb exception!!!!");
                        keyboard = null;
                        super.onException(e);
                    }

                    @Override
                    public void onPay(final IPayRequest request) {
                        super.onPay(request);
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                final AlertDialog.Builder normalDialog =
                                        new AlertDialog.Builder(MainActivity.this);
                                normalDialog.setTitle("支付提示");
                                normalDialog.setMessage(String.format("请支付 %.2f 元", request.getMoney()));
                                normalDialog.setPositiveButton("支付成功",
                                        new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                request.setResult(true);
                                            }
                                        });
                                normalDialog.setNegativeButton("支付失败",
                                        new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                request.setResult(false);
                                            }
                                        });
                                normalDialog.show();
                            }
                        });
                    }

                    @Override
                    public void onKeyDown(final int keyCode, final String keyName) {
                        super.onKeyDown(keyCode, keyName);
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                eventLog.setMovementMethod(ScrollingMovementMethod.getInstance());
                                eventLog.setSelection(eventLog.getText().length(), eventLog.getText().length());
                                eventLog.getText().append(String.format("key down event code : %s, name: %s \n ", keyCode, keyName));
                                Log.e("KeyboardUI","键盘可用！");
                            }
                        });


                    }


                    @Override
                    public void onKeyUp(int keyCode, String keyName) {
                        super.onKeyUp(keyCode, keyName);
                    }
                });
                keyboard.open();

            }
        }else{
            Log.e("KeyboardUI","keyboard exists!!!");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }



    @Override
    protected void onStop() {
        Log.e("KeyboardUI","activity destroy!!!!!!");
        super.onStop();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(keyboard!=null){
            // keyboard.release();
            keyboard.release();
            keyboard=null;

        }
        if(detector!=null){
            detector.release();
            detector = null;
        }
    }

    public void updateSignal(){
        int w = 0;
        int g = 0;
        if(wifi.getText().length()>0){
            w =  Integer.parseInt(wifi.getText().toString());
        }
        if(gprs.getText().length()>0){
            g = Integer.parseInt(gprs.getText().toString());
        }

        if(keyboard!=null && !keyboard.isReleased()){
            keyboard.updateSign(w,g);
        }
    }

    @Override
    public void onAttach() {
        openKeyboard();
    }
}
