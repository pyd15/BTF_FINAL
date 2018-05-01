package com.example.btf_final.activity;

import android.Manifest;
import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.example.btf.R;
import com.example.btf.util.ActivityCollector;
import com.example.btf.util.Constants;
import com.example.btf.util.HttpAction;
import com.example.btf.util.ImageUtil;
import com.example.btf.util.SQLUtil;
import com.example.btf.util.download.DownloadService;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;

import static com.example.btf.util.Constants.CHOOSE_PHOTO;
import static com.example.btf.util.Constants.CROP_PHOTO_FORCAMERA;
import static com.example.btf.util.Constants.OVERLAY_PERMISSION_REQ_CODE;
import static com.example.btf.util.Constants.TAKE_PHOTO_PATH;

/**
 * Created by Dr.P on 2017/10/10.
 * 2017/12/22
 * 待优化问题：
 * 1.识别图片界面：
 * 加上标题栏较好（完成）
 * 图片最好填充整个屏幕，被遮挡部分通过手指拖动查看（已解决）
 * 识别结果对话框的美化，且受到的识别数据应存储起来以方便查看详细结果后返回识别界面再次查看（已解决）
 * 底部按钮的美化（已解决）
 * 拍照或选图的图片的加载速度（已解决）
 * 增加一个分享识别结果的功能（保存屏幕截图分享即可）
 * 2.整个应用的图标设计和美化
 * 3.主界面三个按钮的触摸反馈
 * 4.图片相关匹配率前五的列表（可能要做）
 * 5.搜索界面：
 * 搜索栏过滤的优化，目前问题：添加对拉丁名的过滤以实现对拉丁名的匹配搜索（已解决）
 * 下拉刷新：获取数据后先和数据库中内容比较，没有的才添加，多余的删除（已解决）
 * 更新时某种蝴蝶无图片的问题、须更新data.txt文件
 * 6.不同分辨率屏幕间适配
 * 7.第一次使用时的引导界面
 * 8.滑动菜单中各项子菜单的具体内容
 */

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private Uri imageUri = null;
    Uri imageUri1 = null;
    private String takePath;
    private String imagePath;
    private File outputImage;
    private static boolean dataFlag = true;
    private static boolean netFlag = true;
    private int net_type=0;

    private Button search;
    private Button takePhoto;
    private Button choosePhoto;
    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle drawerToggle;
    private NavigationView navigationView;

    private SharedPreferences sp;
    private SharedPreferences.Editor editor;
    private ProgressDialog progressDialog;
    private IntentFilter intentFilter;
    private MainActivity.NetworkChangeReciver networkChangeReciver;

    private boolean readImgFlag = false;
    private boolean readDBFlag = false;
    private int statusBarColor = 0xff000000;
    private DownloadService.DownloadBinder downloadBinder;
    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            downloadBinder = (DownloadService.DownloadBinder) service;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {

        }
    };

    public static void setWindowStatusBarColor(Activity activity, int colorResId) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Window window = activity.getWindow();
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                window.setStatusBarColor(activity.getResources().getColor(colorResId));

                //底部导航栏
                //window.setNavigationBarColor(activity.getResources().getColor(colorResId));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            imageUri = Uri.parse(savedInstanceState.getString(TAKE_PHOTO_PATH, ""));
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ActivityCollector.AddActivity(this);
        //        setWindowStatusBarColor(this,R.color.toolbar_edge);
        //        StatusBarUtil.setColor(this,getResources().getColor(R.color.toolbar_edge));
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_main);
        toolbar.setTitle("");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            toolbar.setElevation(20);
        }
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.menu2);
        }
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.string.open_drawer, R.string.close_drawer);
        drawerToggle.setDrawerIndicatorEnabled(true);
        drawerToggle.setDrawerSlideAnimationEnabled(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            drawerToggle.getDrawerArrowDrawable().setColor(getColor(R.color.background));//设置小汉堡颜色
        }
        drawerLayout.addDrawerListener(drawerToggle);
        drawerToggle.syncState();
        navigationView = (NavigationView) findViewById(R.id.nav_view);

        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.nav_call:
                        Toast.makeText(MainActivity.this, "请联系#18927512657#", Toast.LENGTH_SHORT).show();
                        break;
                    case R.id.nav_friends:
                        Toast.makeText(MainActivity.this, "#蝴蝶识别项目组#", Toast.LENGTH_SHORT).show();
                        break;
                    case R.id.nav_version:
                        String url = "http://40.125.207.182:8080/apk/btfrg_V1.0.apk";
                        //https://raw.githubusercontent.com/guolindev/eclipse/master/eclipse-inst-win64.exe
                        askForPermission();
                        if (net_type == 1) {
                            downloadBinder.startDownload(url);
//                            installAPK(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath()+"/btfrg.apk");
                        } else {
                            Toast.makeText(MainActivity.this, "请注意流量哟，连接至Wifi网络再更新吧" + getVersionCode(getApplicationContext()), Toast.LENGTH_SHORT).show();
                        }
                        Toast.makeText(MainActivity.this, "#当前版本#-" + getVersionCode(getApplicationContext()), Toast.LENGTH_SHORT).show();

                        break;
                    case R.id.nav_mail:
                        Toast.makeText(MainActivity.this, "#拍照或选图上传即可#", Toast.LENGTH_SHORT).show();
                        break;
                    case R.id.nav_task:
                        Toast.makeText(MainActivity.this, "#任务完成#", Toast.LENGTH_SHORT).show();
                        break;
                    default:
                        break;
                }
                return true;
            }
        });

        takePhoto = (Button) findViewById(R.id.take_photo);
        choosePhoto = (Button) findViewById(R.id.choose_from_album);
        search = (Button) findViewById(R.id.search);

        takePhoto.setOnClickListener(this);
        choosePhoto.setOnClickListener(this);
        search.setOnClickListener(this);

        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            //动态申请WRITE_EXTERNAL_STORAGE权限
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        }
        sp = getSharedPreferences(Constants.BTF_DATA, MODE_PRIVATE);
        editor = getSharedPreferences(Constants.BTF_DATA, MODE_PRIVATE).edit();
        readImgFlag = sp.getBoolean(Constants.IMG_EXIST, false);
        readDBFlag = sp.getBoolean(Constants.DB_EXIST, false);
        if (!readDBFlag) {
            readDBFlag = SQLUtil.createDatabase(getApplicationContext());
            editor.putBoolean(Constants.DB_EXIST, readDBFlag);
        }
        if (!readImgFlag) {
            ImageUtil.saveImageToLocal(this);
            readImgFlag = true;
            editor.putBoolean(Constants.IMG_EXIST, readImgFlag);
        }
        editor.apply();

        Intent intent = new Intent(this, DownloadService.class);
        startService(intent);//启动服务
        bindService(intent, connection, BIND_AUTO_CREATE);

        intentFilter = new IntentFilter();
        intentFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        networkChangeReciver = new NetworkChangeReciver();
        registerReceiver(networkChangeReciver, intentFilter);
    }

    private Notification getNotification(String title, int progress) {
        Notification notification;
        RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.notification_layout);
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setSmallIcon(R.mipmap.ic_btf_launcher);
        //        builder.setContent(remoteViews);//设置自定义布局
        builder.setContentTitle(title);
        builder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher));
        builder.setPriority(Notification.PRIORITY_MAX);
        builder.setDefaults(~0);
        builder.setTicker("悬浮通知");
        builder.setContentIntent(pendingIntent);
        if (progress > 0) {
            //当progress>0时才显示下载进度
            builder.setContentText(progress + "%");
            builder.setProgress(100, progress, false);
        }
        notification = builder.build();
        return notification;
    }

    private void installAPK(String filePath) {
        File file = new File(filePath);
        Intent intent = new Intent();
        intent.setAction("android.intent.action.VIEW");
        intent.addCategory("android.intent.category.DEFAULT");
        intent.setDataAndType(Uri.fromFile(file),
                "application/vnd.android.package-archive");
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.take_photo:
                //创建File对象用于存储拍摄的照片
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                String date = null;
                outputImage = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), (int) (Math.random() * 1000) + ".jpg");///storage/emulated/0/tempImage.jpg Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                //                        getExternalCacheDir()
                try {
                    if (outputImage.exists()) {
                        outputImage.delete();
                    }
                    outputImage.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                takePath = outputImage.getAbsolutePath();
                if (Build.VERSION.SDK_INT >= 24) {
                    //别忘了注册FileProvider内容提供器
                    imageUri = FileProvider.getUriForFile(MainActivity.this, "com.example.btf_final.fileProvider", outputImage);
                } else {
                    imageUri = Uri.fromFile(outputImage);
                }
                //启动相机
                Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
                intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                startActivityForResult(intent, CROP_PHOTO_FORCAMERA);
                break;
            case R.id.choose_from_album:
                openAlbum();
                break;
            case R.id.search:
                Intent intent1 = new Intent(MainActivity.this, ButterflyActivity.class);
                intent1.putExtra("activity", MainActivity.class.getSimpleName());
                startActivity(intent1);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                break;
            default:
                break;
        }
    }

    private void openAlbum() {
        Intent intent1 = new Intent(Intent.ACTION_PICK);//ACTION_PICK Intent.ACTION_GET_CONTENT//"android.intent.action.GET_CONTENT"
        intent1.setType("image/*");
        startActivityForResult(intent1, CHOOSE_PHOTO);//打开相册
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (imageUri != null) {
            outState.putString(TAKE_PHOTO_PATH, imageUri.toString());
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }


    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "拒绝权限将无法使用程序", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
            default:
                break;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                drawerLayout.openDrawer(Gravity.START);
                break;
            default:
        }
        return true;
    }

    /**
     * 请求用户给予悬浮窗的权限
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    public void askForPermission() {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(MainActivity.this, "当前无悬浮通知权限，请授权！", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, OVERLAY_PERMISSION_REQ_CODE);
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case CHOOSE_PHOTO:
                if (resultCode == RESULT_OK) {
                    //判断手机系统版本号
                    if (Build.VERSION.SDK_INT >= 19) {
                        //4.4以上系统用此方法处理图片
                        imagePath = ImageUtil.handleImageOnKitKat(this,data);
                        imageUri1 = data.getData();
                    } else {
                        //其他系统用此方法处理图片
                        imageUri1 = ImageUtil.handleImageBeforeKitKat(this,data);
                        imageUri = imageUri1;
                    }
                    Intent intent = new Intent(this, ImageActivity.class);
                    intent.setDataAndType(imageUri1, "image/*");
                    //                    intent.putExtra("crop", true);//允许裁剪/
                    //                    intent.putExtra("scale", true);//允许缩放
                    if (imagePath != null) {
                        intent.putExtra("imagePath_Album", imagePath);
                    } else {
                        intent.putExtra("imagePath_Album", ImageUtil.getImagePath(this,imageUri1, null));
                    }
                    startActivity(intent);
                }
                break;
            case CROP_PHOTO_FORCAMERA:
                if (resultCode == RESULT_OK) {
                    Intent intent2 = new Intent(this, ImageActivity.class);
                    intent2.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                    intent2.putExtra("imagePath_camera", imageUri.toString());
                    startActivity(intent2);
                }

                break;

            case OVERLAY_PERMISSION_REQ_CODE:
               if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                   if (!Settings.canDrawOverlays(this)) {
                       Toast.makeText(MainActivity.this, "权限授予失败，无法开启悬浮窗", Toast.LENGTH_SHORT).show();
                   } else {
                       Toast.makeText(MainActivity.this, "权限授予成功！", Toast.LENGTH_SHORT).show();
                   }
               }
               break;

            default:
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(connection);
        unregisterReceiver(networkChangeReciver);
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this).setTitle("确认退出吗？")
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ActivityCollector.finishAll();
                    }
                })
                .setNegativeButton("返回", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .show();
    }

    class NetworkChangeReciver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            if (networkInfo != null && networkInfo.isAvailable()) {
                netFlag = true;
                net_type = HttpAction.getNetWorkStatus(context);
            } else {
                netFlag = false;
                net_type= HttpAction.getNetWorkStatus(context);
                Toast.makeText(context, "无法连接至服务器，请稍后再试", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * get App versionCode
     *
     * @param context
     * @return
     */
    public String getVersionCode(Context context) {
        PackageManager packageManager = context.getPackageManager();
        PackageInfo packageInfo;
        String versionCode = "";
        try {
            packageInfo = packageManager.getPackageInfo(context.getPackageName(), 0);
            versionCode = packageInfo.versionCode + "";
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return versionCode;
    }

    /**
     * get App versionName
     *
     * @param context
     * @return
     */
    public String getVersionName(Context context) {
        PackageManager packageManager = context.getPackageManager();
        PackageInfo packageInfo;
        String versionName = "";
        try {
            packageInfo = packageManager.getPackageInfo(context.getPackageName(), 0);
            versionName = packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return versionName;
    }
}
