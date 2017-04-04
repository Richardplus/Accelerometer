package xyz.richardplus.acceleration;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;

import java.io.File;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.Date;


public class MainActivity extends AppCompatActivity implements SensorEventListener, View.OnClickListener {
    SensorManager mSensorMagager;
    Sensor sr;

    int ACCE_FILTER_DATA_MIN_TIME = 500;
    long lastSaved = System.currentTimeMillis();
    //设置文本显示
    private TextView textviewX, textviewY, textviewZ, phoneStatus, setFreq;
    //文本输入框
    private EditText getName;
    //button 开始/暂停、保存、读取、记录频率
    private Button btnToggle, btnSave, btnRead, btnUp, btnDown, btnStart;
    //开始/暂停 flag
    private boolean flag = true,recording = false;
    //w文件名和文件内容
    String filename, message = "";
    //三维加速度数据
    float xValue, yValue, zValue;
    //保存文件计数器
    private int count = 0, freq = 2;
    //三个linechart
    LineChart lineChartX, lineChartY, lineChartZ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d("MainActivity", "onCreate execute");
        //findViewById()让组件和相应的ID对应起来
        linkViews();

        mSensorMagager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);//从系统服务获取传感器管理器
        sr = mSensorMagager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER); //获取加速度感应器

    }

    private void linkViews() {
        btnToggle = (Button) findViewById(R.id.btnToggle);         //开始/暂停
        btnToggle.setOnClickListener(this);
        btnSave = (Button) findViewById(R.id.btnSave);             //保存
        btnSave.setOnClickListener(this);
        btnRead = (Button) findViewById(R.id.btnRead);             //读取
        btnRead.setOnClickListener(this);

        btnUp = (Button) findViewById(R.id.btnUp);
        btnUp.setOnClickListener(this);
        btnDown = (Button) findViewById(R.id.btnDown);
        btnDown.setOnClickListener(this);
        btnStart = (Button) findViewById(R.id.btnStart);
        btnStart.setOnClickListener(this);

        textviewX = (TextView) findViewById(R.id.textviewX);
        textviewY = (TextView) findViewById(R.id.textviewY);
        textviewZ = (TextView) findViewById(R.id.textviewZ);
        phoneStatus = (TextView) findViewById(R.id.phoneStatus);
        getName = (EditText) findViewById(R.id.edit_message);
        setFreq = (TextView) findViewById(R.id.edit_freq);

        //绑定chart
        lineChartX = (LineChart) findViewById(R.id.lineChartX);
        lineChartY = (LineChart) findViewById(R.id.lineChartY);
        lineChartZ = (LineChart) findViewById(R.id.lineChartZ);
    }

    @Override
    //复写onSensorChanged方法（传感器数据变动，用于数据刷新）
    public void onSensorChanged(SensorEvent sensorEvent) {

        if ((System.currentTimeMillis() - lastSaved) > ACCE_FILTER_DATA_MIN_TIME) {
            lastSaved = System.currentTimeMillis();
            xValue = sensorEvent.values[0];// values[0]\[1]\[2]分别表示加速度传感器的三个值
            yValue = sensorEvent.values[1];
            zValue = sensorEvent.values[2];
            //只显示两位小数
            textviewX.setText(String.format("x轴:%1.2f,m/s²", xValue));
            textviewY.setText(String.format("y轴:%1.2f,m/s²", yValue));
            textviewZ.setText(String.format("z轴:%1.2f,m/s²", zValue));
            getState(); //手机状态
            if (recording) {
                writeMessage();// 写入message 字符串
            }
        }


    }

    @Override
    public void onClick(View v){
        switch(v.getId()){
            case R.id.btnToggle:
                flag=!flag;
                if(flag){btnToggle.setText("暂停");onResume();}
                else{btnToggle.setText("开始");onPause();}
                break;
            case R.id.btnStart:
                if(recording)btnStart.setText("开始记录");
                else btnStart.setText("停止记录");
                recording =!recording;
                break;
            case R.id.btnSave://保存按钮
                if(recording){Toast.makeText(this,"请先停止记录",Toast.LENGTH_SHORT).show();break;}
                if(message.equals("")){Toast.makeText(this,"还没开始记录",Toast.LENGTH_SHORT).show();break;}
                if(!getName.getText().toString().equals("")){
                    filename = getName.getText().toString();
                    writeFileSdcard(message);
                    Toast.makeText(this,String.format("已保存在 根目录/Acc/%s%d.txt",filename,count++),Toast.LENGTH_SHORT).show();
                }
                else Toast.makeText(this,"请输入文件名",Toast.LENGTH_SHORT).show();
            case R.id.btnRead: //读取按钮
                // TODO: 2017/4/2
                break;
            case R.id.btnUp:
                if(freq==2){
                    ACCE_FILTER_DATA_MIN_TIME=250;
                    setFreq.setText("快");
                    freq++;
                }else if(freq==3){
                    Toast.makeText(this,"最快了！",Toast.LENGTH_SHORT).show();
                }else if(freq==1){
                    ACCE_FILTER_DATA_MIN_TIME=500;
                    setFreq.setText("中");
                    freq++;
                }
                break;
            case R.id.btnDown:
                if(freq==2){
                    ACCE_FILTER_DATA_MIN_TIME=1000;
                    setFreq.setText("慢");
                    freq--;
                }else if(freq==3){
                    ACCE_FILTER_DATA_MIN_TIME=500;
                    setFreq.setText("中");
                    freq--;
                }else if(freq==1){
                    Toast.makeText(this,"最慢了！",Toast.LENGTH_SHORT).show();
                }
            default:
                break;
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        mSensorMagager.registerListener(this, sr, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorMagager.unregisterListener(this);
    }

    //判断设备的状态
    private void getState(){
        if(xValue>9&&yValue< 10){
            phoneStatus.setText("重力指向设备x轴下方");
        }else if(xValue>-10&&xValue< -9){
            phoneStatus.setText("重力指向设备x轴上");
        }
        if(yValue>9&&yValue< 10){
            phoneStatus.setText("重力指向设备y轴下方");
        }else if(yValue>-10&&yValue< -9){
            phoneStatus.setText("重力指向设备y轴上方");
        }
        if(zValue>9&&zValue< 10){
            phoneStatus.setText("屏幕朝上");
        }else if(zValue>-10&&zValue< -9){
            phoneStatus.setText("屏幕朝下");
        }
    }

    private void writeMessage(){
        SimpleDateFormat sdf=new SimpleDateFormat("MM月dd日   HH:mm:ss"); //yyyy年
        String str=sdf.format(new Date());
        message+=str+"    ";
        message+=(String.format("%1.2f",xValue))+"  ";
        message+=(String.format("%1.2f",yValue))+"  ";
        message+=(String.format("%1.2f",zValue))+"\n";
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }//空函数

    //写入数据
    private void writeFileSdcard(String message) {
        try{
            if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
                File sdCardDir=Environment.getExternalStorageDirectory();
                File targitFile=new File(sdCardDir.getCanonicalPath()+"/"+filename+count+".txt");
                RandomAccessFile raf=new RandomAccessFile(targitFile,"rw");
                raf.seek(targitFile.length());
                raf.write(message.getBytes());
                raf.close();
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}