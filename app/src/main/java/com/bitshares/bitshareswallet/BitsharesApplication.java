package com.bitshares.bitshareswallet;

import android.app.Application;
import android.support.multidex.MultiDexApplication;

import com.crashlytics.android.Crashlytics;

import org.spongycastle.jce.provider.BouncyCastleProvider;

import java.security.Security;

import io.fabric.sdk.android.Fabric;


public class BitsharesApplication extends MultiDexApplication {
    private static BitsharesApplication theApp;
    /*
    * 是否需要把涨跌的颜色互换
     */
    public static BitsharesApplication getInstance() {
        return theApp;
    }

    public BitsharesApplication() {
        theApp = this;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Fabric.with(this, new Crashlytics());
        Security.insertProviderAt(new BouncyCastleProvider(), 1);
    }
}
