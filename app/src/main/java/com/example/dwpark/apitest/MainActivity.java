package com.example.dwpark.apitest;

import android.databinding.DataBindingUtil;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.example.dwpark.apitest.Model.Dummy;
import com.example.dwpark.apitest.Model.Dworks;
import com.example.dwpark.apitest.Rf.DxApi;
import com.example.dwpark.apitest.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity implements DxApi.onApiCallback{
    private static ActivityMainBinding b;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = DataBindingUtil.setContentView(this, R.layout.activity_main);

        View v = null;
        String s;

        //todo sync
        final Dworks d = DxApi.rf().getDworks().sync();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                test(d.getResultCode(), "[h]"+d.getApiVersion(), "[h]"+d.getApiName());
            }
        },5000);

        //todo async-weak listener
        DxApi.rf().getDworks().async("weakListener");

        //todo async-strong listener
        DxApi.rf().getDworks().async(strongListener);
        DxApi.rf().getDworks().async(this);

        //todo add header
        DxApi.rf().getDworks().addHeader("a=a1").async(strongListener);
    }

    //todo weak callback
    public void weakListener(Object obj){
        Dummy d = (Dummy) obj;
        test(d.getResultCode(), "[w]"+d.getApiVersion(), "[w]"+d.getApiName());
    }

    //todo strong callback
    private DxApi.onApiCallback strongListener = new DxApi.onApiCallback() {
        @Override
        public void result(Object obj) {
            Dworks d = (Dworks) obj;
            test(d.getResultCode(), "[s1]"+d.getApiVersion(), "[s1]"+d.getApiName());
        }
    };

    //todo implements callback
    @Override
    public void result(Object obj) {
        Dummy d = (Dummy) obj;
        test(d.getResultCode(), "[s2]"+d.getApiVersion(), "[s2]"+d.getApiName());
    }

    private static void test(String code, String apiVersion, String apiName){
        b.tvResponseTest.setText(b.tvResponseTest.getText()+"\n version-> "+apiVersion+" / code-> "+code+" / name-> "+apiName+"");
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }
}