package com.example.btf_final.util;

import android.annotation.TargetApi;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;

import org.litepal.LitePal;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by Dr.P on 2017/11/19.
 * runas /user:Dr.P "cmd /k"
 */

public class ImageUtil {
    //4.4及以上系统使用这个方法处理图片
    @TargetApi(19)
    public static String handleImageOnKitKat(Context context, Intent data) {
        String imagePath = null;
        Uri uri = data.getData();
        if (DocumentsContract.isDocumentUri(context, uri)) {
            //如果是document类型的Uri,则通过document id处理
            String docId = DocumentsContract.getDocumentId(uri);
            if ("com.android.providers.media.documents".equals(uri.getAuthority())) {
                String id = docId.split(":")[1];  //解析出数字格式的id
                String selection = MediaStore.Images.Media._ID + "=" + id;
                imagePath = getImagePath(context, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, selection);
            } else if ("com.android.providers.downloads.documents".equals(uri.getAuthority())) {
                Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(docId));
                imagePath = getImagePath(context, contentUri, null);
            }
        } else if ("content".equalsIgnoreCase(uri.getScheme())) {
            //如果不是document类型的Uri,则使用普通方式处理
            imagePath = getImagePath(context, uri, null);
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            //若为file类型的uri，直接获取图片路径即可
            imagePath = uri.getPath();
        }
//        return getImage(imagePath);
        return imagePath;
    }

    //4.4以下系统使用这个方法处理图片
    public static Uri handleImageBeforeKitKat(Context context, Intent data) {
        Uri uri = data.getData();
//        String imagePath = getImagePath(context, uri, null);
//        return getImage(imagePath);
        return uri;
    }

    public static String getImagePath(Context context, Uri uri, String selection) {
        String path = null;
        //通过Uri和selection来获取真实的图片路径
        Cursor cursor = context.getContentResolver().query(uri, null, selection, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
            }
            cursor.close();
        }
        return path;
    }

    public static void saveImageToLocal(Context context) {
        LitePal.initialize(context);
        InputStream assetFile = null;
        FileOutputStream fileOutputStream;
        File imageFile;
        String[] images = null;
        byte[] data;
        long start_time = System.currentTimeMillis();
        String directory = context.getFilesDir().getAbsolutePath();
        File dir = new File(directory + "/btf/");
        if (!dir.exists()) {
            dir.mkdir();
        }
        try {
            images = context.getAssets().list("btf");
        for (String imageName : images) {
            assetFile = context.getAssets().open("btf/" + imageName);
            if (dir.exists() && dir.canWrite()) {
                imageFile = new File(dir.getAbsolutePath() + "/" + imageName);//directory
                if (imageFile.exists()) {
                    continue;
                }
                data = DownImage.readInputStream(assetFile);
                fileOutputStream = new FileOutputStream(imageFile);
                fileOutputStream.write(data);
                fileOutputStream.close();
            }
        }
            assetFile = context.getAssets().open("zanwu.jpg");
            imageFile = new File(dir.getAbsolutePath() + "/zanwu.jpg");//directory
            if (!imageFile.exists()) {
                data = DownImage.readInputStream(assetFile);
                fileOutputStream = new FileOutputStream(imageFile);
                fileOutputStream.write(data);
                fileOutputStream.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        long end_time = System.currentTimeMillis();
        Log.d("file", String.valueOf(end_time - start_time)+"ms");
    }

    //对bitmap进行质量压缩
    public static Bitmap compressImage(Bitmap image) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.JPEG, 100, baos);//质量压缩方法，这里100表示不压缩，把压缩后的数据存放到baos中
        int options = 100;
        while (baos.toByteArray().length / 1024 > 100) {    //循环判断如果压缩后图片是否大于100kb,大于继续压缩
            baos.reset();//重置baos即清空baos
            image.compress(Bitmap.CompressFormat.JPEG, options, baos);//这里压缩options%，把压缩后的数据存放到baos中
            options -= 10;//每次都减少10
        }
        ByteArrayInputStream isBm = new ByteArrayInputStream(baos.toByteArray());//把压缩后的数据baos存放到ByteArrayInputStream中
        Bitmap bitmap = BitmapFactory.decodeStream(isBm, null, null);//把ByteArrayInputStream数据生成图片
        return bitmap;
    }

    //传入图片路径，返回压缩后的bitmap
    public static Bitmap getImage(String srcPath) {
        if (TextUtils.isEmpty(srcPath))  //如果图片路径为空 直接返回
            return null;
        BitmapFactory.Options newOpts = new BitmapFactory.Options();
        //开始读入图片，此时把options.inJustDecodeBounds 设回true了
        newOpts.inJustDecodeBounds = true;
        Bitmap bitmap = BitmapFactory.decodeFile(srcPath, newOpts);//此时返回bm为空

        newOpts.inJustDecodeBounds = false;
        int w = newOpts.outWidth;
        int h = newOpts.outHeight;
        //现在主流手机比较多是800*480分辨率，所以高和宽我们设置为
        float hh = 800f;//这里设置高度为800f
        float ww = 480f;//这里设置宽度为480f
        //缩放比。由于是固定比例缩放，只用高或者宽其中一个数据进行计算即可
        int be = 1;//be=1表示不缩放
        if (w > h && w > ww) {//如果宽度大的话根据宽度固定大小缩放
            be = (int) (newOpts.outWidth / ww);
        } else if (w < h && h > hh) {//如果高度高的话根据宽度固定大小缩放
            be = (int) (newOpts.outHeight / hh);
        }
        if (be <= 0)
            be = 1;
        newOpts.inSampleSize = be;//设置缩放比例
        //重新读入图片，注意此时已经把options.inJustDecodeBounds 设回false了
        bitmap = BitmapFactory.decodeFile(srcPath, newOpts);
        return compressImage(bitmap);//压缩好比例大小后再进行质量压缩
    }
}
