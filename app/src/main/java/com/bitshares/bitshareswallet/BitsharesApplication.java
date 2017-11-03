package com.bitshares.bitshareswallet;

import android.arch.persistence.room.Room;
import android.support.multidex.MultiDexApplication;

import com.bitshares.bitshareswallet.room.BitsharesDatabase;
import com.crashlytics.android.Crashlytics;

import org.spongycastle.jce.provider.BouncyCastleProvider;

import java.security.Security;

import io.fabric.sdk.android.Fabric;


public class BitsharesApplication extends MultiDexApplication {
    private static BitsharesApplication theApp;
    private BitsharesDatabase bitsharesDatabase;
    /*
    * 是否需要把涨跌的颜色互换
     */
    public static BitsharesApplication getInstance() {
        return theApp;
    }

    public BitsharesApplication() {
        theApp = this;
    }

    public BitsharesDatabase getBitsharesDatabase() {
        return bitsharesDatabase;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Fabric.with(this, new Crashlytics());
        Security.insertProviderAt(new BouncyCastleProvider(), 1);

        bitsharesDatabase = Room.databaseBuilder(
                this,
                BitsharesDatabase.class,
                "bitshares.db"
        ).build();

        // 注册回调，保证数据更新
    }
}
