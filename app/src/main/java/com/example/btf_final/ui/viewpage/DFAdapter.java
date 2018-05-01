package com.example.btf_final.ui.viewpage;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.PagerAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.btf.R;
import com.example.btf.entity.InfoDetail;
import com.github.chrisbanes.photoview.PhotoView;

import java.util.ArrayList;

/**
 * Created by Dr.P on 2018/3/18.
 * runas /user:Dr.P "cmd /k"
 */

public class DFAdapter extends PagerAdapter {

    private Context context;
    private Activity activity;
    private Dialog dialog;
    private FragmentManager fm;
    private ArrayList<String> imgs;
    private InfoDetail infoDetail;
    private int img_position;

    public DFAdapter(Context context, Activity activity, Dialog dialog, FragmentManager fm, ArrayList<String> imageList, InfoDetail infoDetail) {
        this.context = context;
        this.activity=activity;
        this.dialog=dialog;
        this.fm = fm;
        imgs = new ArrayList<>();
        this.infoDetail = infoDetail;
        for (int i = 0; i < imageList.size(); i++) {
            imgs.add(imageList.get(i));
        }
    }

    @Override
    public int getCount() {
//        return Integer.MAX_VALUE;
        return imgs!=null?imgs.size():0;
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    @Override
    public View instantiateItem(final ViewGroup container, int position) {
        final View view = LayoutInflater.from(context).inflate(R.layout.df_vp_layout_item, null);
        PhotoView iv = new PhotoView(context);
        Log.e("position", position + "");
        RequestOptions options = new RequestOptions().fitCenter().
                placeholder(R.drawable.loading).error(R.drawable.fail_diy);
        if (imgs.size() > 1) {
            img_position = position;
            Glide.with(context).load("http://40.125.207.182:8080" + imgs.get(position%imgs.size())).apply(options).into(iv);
//            Glide.with(context).load(imgs.get(position % imgs.size())).apply(options).into(iv);
        } else if (imgs.size() == 1) {
            Glide.with(context).load("http://40.125.207.182:8080" + imgs.get(0)).apply(options).into(iv);
//            Glide.with(context).load(imgs.get(0)).apply(options).into(iv);
        } else {
            Glide.with(context).load(context.getFilesDir().getAbsolutePath() + "/btf/zanwu.jpg").into(iv);
        }
        iv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        container.addView(iv);
        return iv;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((View) object);
    }

}
