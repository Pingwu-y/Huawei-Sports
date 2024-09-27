package me.w1049.hsports;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Build;
import android.Manifest;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.Overlay;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.map.PolylineOptions;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.utils.DistanceUtil;

import java.util.ArrayDeque;
import java.util.ArrayList;

public class MainActivity extends Activity {
    private MapView mMapView = null;
    private BaiduMap mBaiduMap = null;
    private LocationClient mLocationClient = null;
    private boolean isPermissionRequested = false;
    private String mFileName;
    private FileManager fileManager;

    private SportsTimer mTimer;

    // For time display
    private TextView mSpdView;

    private final ArrayDeque<Float> mSpeedQueue = new ArrayDeque<>(5);
    private final ArrayList<LatLng> mLastTwoPoints = new ArrayList<>();

    private void drawPoint(double lat, double lon) {
        LatLng point = new LatLng(lat, lon);
        mLastTwoPoints.add(point);
        if (mLastTwoPoints.size() < 2)
            return;

        OverlayOptions mOverlayOptions = new PolylineOptions().width(10).color(0xAAFF0000).points(mLastTwoPoints);
        Overlay _mPolyline = mBaiduMap.addOverlay(mOverlayOptions);
        mBaiduMap.setMyLocationEnabled(true);

        double deltaDis = DistanceUtil.getDistance(mLastTwoPoints.get(0), mLastTwoPoints.get(1));
        mTimer.addDistance(deltaDis);
        mLastTwoPoints.remove(0);
    }

    public class MyLocationListener extends BDAbstractLocationListener {
        @SuppressLint("DefaultLocale")
        @Override
        public void onReceiveLocation(BDLocation location) {
            // mapView 销毁后不在处理新接收的位置
            if (location == null || mMapView == null) {
                return;
            }
            MyLocationData locData = new MyLocationData.Builder().accuracy(location.getRadius())
                    // 此处设置开发者获取到的方向信息，顺时针0-360
                    .direction(location.getDirection()).latitude(location.getLatitude())
                    .longitude(location.getLongitude()).build();
            mBaiduMap.setMyLocationData(locData);
            mSpdView.setText(String.format("%.2f", location.getSpeed()));
            // 绘制路径
            drawPoint(location.getLatitude(), location.getLongitude());

            fileManager.writeToFile(mFileName, String.format("%d,%f,%f,%f\n", System.currentTimeMillis(),
                    location.getLatitude(), location.getLongitude(), location.getSpeed()));
            tryAutoPause(location.getSpeed());
        }

        private void tryAutoPause(float speed) {
            // 记录最近5次速度
            mSpeedQueue.add(speed);
            if (mSpeedQueue.size() > 5) {
                mSpeedQueue.poll();
            }
            SportsTimer.TimerState timerState = mTimer.getState();
            if (timerState == SportsTimer.TimerState.RUNNING) {
                if (isSlowingDown()) {
                    mTimer.autoPause();
                    Toast.makeText(getApplicationContext(), "自动暂停", Toast.LENGTH_SHORT).show();
                }
            } else if (timerState == SportsTimer.TimerState.AUTO_PAUSED) {
                // 仅在自动暂停状态下检查是否需要自动恢复
                if (isSpeedingUp()) {
                    mTimer.start();
                    Toast.makeText(getApplicationContext(), "自动恢复", Toast.LENGTH_SHORT).show();
                }
            }
        }

        private boolean isSlowingDown() {
            // 最后一次速度小于1，且有至少2次下降
            if (mSpeedQueue.size() < 3 || mSpeedQueue.peekLast() >= 1)
                return false;
            ArrayList<Float> speedList = new ArrayList<>(mSpeedQueue);
            int count = 0;
            for (int i = 1; i < speedList.size(); i++) {
                if (speedList.get(i) < speedList.get(i - 1)) {
                    count++;
                }
            }
            return count >= 2;
        }

        private boolean isSpeedingUp() {
            // 最后一次速度大于1，且有至少2次上升
            if (mSpeedQueue.size() < 3 || mSpeedQueue.peekLast() < 1)
                return false;
            ArrayList<Float> speedList = new ArrayList<>(mSpeedQueue);
            int count = 0;
            for (int i = 1; i < speedList.size(); i++) {
                if (speedList.get(i) > speedList.get(i - 1)) {
                    count++;
                }
            }
            return count >= 2;
        }
    }

    // 动态申请权限
    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= 23 && !isPermissionRequested) {
            isPermissionRequested = true;
            ArrayList<String> permissionsList = new ArrayList<>();
            String[] permissions = { Manifest.permission.ACCESS_NETWORK_STATE, Manifest.permission.INTERNET,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_WIFI_STATE, };

            for (String perm : permissions) {
                if (PackageManager.PERMISSION_GRANTED != checkSelfPermission(perm)) {
                    permissionsList.add(perm);
                    // 进入到这里代表没有权限.
                }
            }

            if (!permissionsList.isEmpty()) {
                String[] strings = new String[permissionsList.size()];
                requestPermissions(permissionsList.toArray(strings), 0);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestPermission();
        LocationClient.setAgreePrivacy(true);
        setContentView(R.layout.activity_main);
        // 获取地图控件引用
        mMapView = (MapView) findViewById(R.id.bmapView);
        mBaiduMap = mMapView.getMap();
        mBaiduMap.setMyLocationEnabled(true);

        // 设置缩放级别 50m
        MapStatus.Builder builder = new MapStatus.Builder();
        builder.zoom(18.0f);
        mBaiduMap.setMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()));

        // 设置定位为跟随模式
        mBaiduMap.setMyLocationConfiguration(
                new MyLocationConfiguration(MyLocationConfiguration.LocationMode.FOLLOWING, true, null));

        // 定位初始化
        try {
            mLocationClient = new LocationClient(this);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // 通过LocationClientOption设置LocationClient相关参数
        LocationClientOption option = new LocationClientOption();
        option.setOpenGnss(true); // 打开gps
        option.setCoorType("bd09ll"); // 设置坐标类型
        option.setScanSpan(1000);
        // 可选，默认false，设置是否当gps有效时按照1S1次频率输出GPS结果
        option.setLocationNotify(true);

        // 设置locationClientOption
        mLocationClient.setLocOption(option);

        // 注册LocationListener监听器
        MyLocationListener myLocationListener = new MyLocationListener();
        mLocationClient.registerLocationListener(myLocationListener);
        // 开启地图定位图层
        mLocationClient.start();

        // 获取相关TextView
        // Buttons
        Button mStartButton = findViewById(R.id.startButton);
        Button mStopButton = findViewById(R.id.stopButton);
        mSpdView = findViewById(R.id.speedTextView);
        TextView mDisView = findViewById(R.id.distanceTextView);
        TextView mTimeView = findViewById(R.id.timeTextView);
        TextView mAvgSpdView = findViewById(R.id.averageSpeedTextView);

        mTimer = new SportsTimer(mStartButton, mStopButton, mTimeView, mAvgSpdView, mDisView);

        fileManager = new FileManager(getApplicationContext());
    }

    @Override
    protected void onResume() {
        // 在activity执行onResume时执行mMapView. onResume ()，实现地图生命周期管理
        mMapView.onResume();
        super.onResume();
    }

    @Override
    protected void onPause() {
        // 在activity执行onPause时执行mMapView. onPause ()，实现地图生命周期管理
        mMapView.onPause();
        super.onPause();

    }

    @Override
    protected void onDestroy() {
        // 在activity执行onDestroy时执行mMapView.onDestroy()，实现地图生命周期管理
        mLocationClient.stop();
        mBaiduMap.setMyLocationEnabled(false);
        mMapView.onDestroy();
        mMapView = null;
        super.onDestroy();

    }

    public void startTimer(View view) {
        switch (mTimer.getState()) {
            case INIT:
                fileInit();
                mSpeedQueue.clear();
            case PAUSED:
            case AUTO_PAUSED:
                mTimer.start();
                break;
            case RUNNING:
                mTimer.pause();
                break;
        }
    }

    public void stopTimer(View view) {
        fileManager.writeToFile(mFileName, "total" + "," + mTimer.getSeconds() + "," +
                mTimer.getDistance() + "\n");
        mTimer.reset();
    }

    private void fileInit() {
        mFileName = "gps_file.csv";
        getApplicationContext().deleteFile(mFileName);
        fileManager.writeToFile(mFileName, "time,latitude,longitude,speed\n");
    }
}
