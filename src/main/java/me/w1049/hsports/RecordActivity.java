package me.w1049.hsports;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
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
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.map.Overlay;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.map.PolylineOptions;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.utils.DistanceUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;

public class RecordActivity extends Activity {
    private MapView mMapView = null;
    private BaiduMap mBaiduMap = null;
    private boolean isPermissionRequested = false;
    private String mFileName;

    // For time display
    private TextView mDisView;
    private TextView mTimeView;
    private TextView mAvgSpdView;
    private int mSeconds = 0;
    private double mDistance = 0;

    private ArrayList<LatLng> points = new ArrayList<>();

    private void drawPoint(double lat, double lon) {
        LatLng point = new LatLng(lat, lon);
        mLastTwoPoints.add(point);
        if (mLastTwoPoints.size() < 2)
            return;

        OverlayOptions mOverlayOptions = new PolylineOptions().width(10).color(0xAAFF0000).points(mLastTwoPoints);
        Overlay _mPolyline = mBaiduMap.addOverlay(mOverlayOptions);
        mBaiduMap.setMyLocationEnabled(true);
        mDistance += DistanceUtil.getDistance(points.get(0), points.get(1));

        double deltaDis = DistanceUtil.getDistance(mLastTwoPoints.get(0), mLastTwoPoints.get(1));
        mTimer.addDistance(deltaDis);
        mLastTwoPoints.remove(0);
    }

    @SuppressLint("DefaultLocale")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LocationClient.setAgreePrivacy(true);
        setContentView(R.layout.activity_record);
        // 获取地图控件引用
        mMapView = (MapView) findViewById(R.id.bmapView);
        mBaiduMap = mMapView.getMap();
        mBaiduMap.setMyLocationEnabled(true);

        // 设置缩放级别 50m
        MapStatus.Builder builder = new MapStatus.Builder();
        builder.zoom(18.0f);
        mBaiduMap.setMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()));

        // 获取相关TextView
        mDisView = findViewById(R.id.mDisView);
        mTimeView = findViewById(R.id.mTimeView);
        mAvgSpdView = findViewById(R.id.mAvgSpdView);

        fileInit();
        int hours = mSeconds / 3600;
        int minutes = (mSeconds % 3600) / 60;
        int secs = mSeconds % 60;
        String time = String.format("%02d:%02d:%02d", hours, minutes, secs);
        mTimeView.setText(time);
        mAvgSpdView.setText(String.format("%.2f", mDistance / mSeconds * 3.6));
        mDisView.setText(String.format("%.2f", mDistance / 1000));
        for (LatLng latLng : latLngs) {
            drawPoint(latLng.latitude, latLng.longitude);
        }
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
        mMapView.onDestroy();
        mMapView = null;
        super.onDestroy();

    }

    private ArrayList<LatLng> latLngs = new ArrayList<>();

    private void fileInit() {
        // file name start with time
        mFileName = "gps_file.csv";
        File dir = getApplicationContext().getExternalFilesDir("");
        if (dir != null) {
            String path = dir.getAbsoluteFile() + "/" + mFileName;
            Log.i("map", "gps_file path is:" + path);
            File gps_file = new File(path);
            try (BufferedReader br = new BufferedReader(new FileReader(gps_file))) {
                String line = br.readLine(); // skip the first line
                while ((line = br.readLine()) != null) {
                    String[] data = line.split(",");
                    Log.d("READING", data[0] + " " + data[1] + " " + data[2]);
                    if (data[0].equals("total")) {
                        mSeconds = Integer.parseInt(data[1]);
                        mDistance = Double.parseDouble(data[2]);
                        break;
                    } else {
                        double lat = Double.parseDouble(data[1]);
                        double lon = Double.parseDouble(data[2]);
                        latLngs.add(new LatLng(lat, lon));
                    }
                }
                Log.d("READING", latLngs.toString());
                Log.d("READING", "mSeconds: " + mSeconds + " mDistance: " + mDistance);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            Log.e("map", "获取文件出错");
        }
    }
}
