package com.example.sensorvalue_measurement;

//import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.WindowManager;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import java.util.*;

class Values{
    float time;
    float x;
    float y;
    float z;
}

public class MainActivity extends Activity implements SensorEventListener {
    private final String TAG = MainActivity.class.getName();
    private TextView mTextView;
    private TextView mTextView2;
    private TextView textView3;
    private SensorManager mSensorManager;
    private Sensor acc_sensor;
    private Sensor gyr_sensor;
    private int button_flag = 0; //1でスタート,2で終わり
    ArrayList<Float> acc_x = new ArrayList<Float>();
    static ArrayList<Values> acc_values = new ArrayList<Values>();
    static ArrayList<Values> gyr_values = new ArrayList<Values>();
    float t0,x1,y1,z1,x2,y2,z2;
    long starttime;
    private int prev;
    private int i = 0;

    public int count_Ltap = 0;
    public float prev_x = 0;
    public float diffaccx = 0;
    public float diffaccxprev = 0;

    private String person_name = "admin";
    private String filename = "Test.csv";


    String[] spinnerItems = {
            "Test",
            "Free",
            "Ltap",
            "Ctap",
            "Rtap",
            "UPtwist",
            "DOWNtwist",
    };

    class getValueThread extends Thread {

        public Values setValues(float st, float sx, float sy, float sz){
            Values tmp = new Values();
            tmp.time = st;
            tmp.x = sx;
            tmp.y = sy;
            tmp.z = sz;
            return tmp;
        }
        @Override
        public void run() {
            Calendar cal = Calendar.getInstance(TimeZone.getDefault());
            int sec=0;
            int ss=0;
            while(button_flag == 1){
                cal.setTime(new Date());
                sec = cal.get(Calendar.SECOND);
                ss = cal.get(Calendar.MILLISECOND);

                if (ss % 20 == 0 && ss != prev) {//前のデータから20ms以上経過している時
                    acc_x.add(x1);
                    acc_values.add(setValues(sec+((float)ss/1000), x1, y1, z1));
                    gyr_values.add(setValues(sec+((float)ss/1000), x2, y2, z2));

                    prev = ss;
                }

            }

        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mTextView = (TextView) findViewById(R.id.text);
        mTextView2 = findViewById(R.id.text2);
        mTextView2.setTextSize(15.0f);
        mTextView2.setText(person_name);
        //mTextView2.setText(String.format("%s", person_name));

        mSensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);

        //Spinner設定
        //super.onCreate(savedInstanceState);
        textView3 = findViewById(R.id.text3);
        Spinner spinner = findViewById(R.id.spinner);
        ArrayAdapter<String> adapter
                = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, spinnerItems);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // spinner に adapter をセット
        spinner.setAdapter(adapter);
        // リスナーを登録
        spinner.setOnItemSelectedListener(new OnItemSelectedListener() {
            //　アイテムが選択された時
            @Override
            public void onItemSelected(AdapterView<?> parent,
                                       View view, int position, long id) {
                Spinner spinner = (Spinner)parent;
                String item = (String)spinner.getSelectedItem();
                textView3.setText(item);
                filename = String.format("%s_%s.csv",person_name, item);
                //start & stop リセット
                button_flag = 0;
                TextView start_text = findViewById(R.id.button1);
                start_text.setText("START");
                TextView stop_text = findViewById(R.id.button2);
                stop_text.setText("STOP");

                mTextView.setText("STARTを押してください");
                //mTextView.setText(String.format("Ltap:%d, Ctap:%d, Rtap:%d",count.ltap, count.ctap, count.rtap));
            }

            //　アイテムが選択されなかった
            public void onNothingSelected(AdapterView<?> parent) {
                //
            }
        });
        //Spiner設定終了



        Button start_btn = findViewById(R.id.button1);
        start_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                starttime = System.currentTimeMillis();
                button_flag = 1;
                TextView start_text = findViewById(R.id.button1);
                start_text.setText("計測中");
                mTextView = (TextView) findViewById(R.id.text);
                //mTextView.setTextSize(15.0f);
                mTextView.setText("計測中");
                getValueThread thread1 = new getValueThread();
                thread1.start();
            }

        });

        Button stop_btn = findViewById(R.id.button2);
        stop_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                button_flag = 2;

                //　ファイルに出力
                try {
                    FileOutputStream fos = openFileOutput(filename, Context.MODE_APPEND);
                    OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF-8");
                    BufferedWriter bw = new BufferedWriter(osw);
                    bw.write(String.format("size : %d\n", acc_values.size()));
                    bw.write("time,x,y,z,time,x,y,z\n");
                    for (i = 0; i < acc_values.size(); i++) {
                        t0 = acc_values.get(i).time;
                        x1 = acc_values.get(i).x;
                        y1 = acc_values.get(i).y;
                        z1 = acc_values.get(i).z;
                        t0 = gyr_values.get(i).time;
                        x2 = gyr_values.get(i).x;
                        y2 = gyr_values.get(i).y;
                        z2 = gyr_values.get(i).z;
                        bw.write(String.format("%f,%f,%f,%f,%f,%f,%f,%f\n",t0,x1,y1,z1,t0,x2,y2,z2));
                    }
                    bw.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                acc_values.clear();
                gyr_values.clear();
                TextView stop_text = findViewById(R.id.button2);
                stop_text.setText("計測完了");
                mTextView.setText("次のファイルを選んで下さい");
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        acc_sensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyr_sensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        mSensorManager.registerListener(this, acc_sensor, SensorManager.SENSOR_DELAY_FASTEST);
        mSensorManager.registerListener(this, gyr_sensor, SensorManager.SENSOR_DELAY_FASTEST);
    }
    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            //スタートしていたら加速度記録
            x1 = event.values[0];
            y1 = event.values[1];
            z1 = event.values[2];

        }

        if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            //スタートしていたら加速度記録
            x2 = event.values[0];
            y2 = event.values[1];
            z2 = event.values[2];
        }
        if(button_flag == 1) {
            //mTextView.setText(String.format("Ltap:%d, Ctap:%d, Rtap:%d",count.ltap, count.ctap, count.rtap));
        }
    }



    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }


}