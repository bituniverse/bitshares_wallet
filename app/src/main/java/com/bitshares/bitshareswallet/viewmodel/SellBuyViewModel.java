package com.bitshares.bitshareswallet.viewmodel;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.Transformations;
import android.arch.lifecycle.ViewModel;

import com.bitshares.bitshareswallet.BitsharesApplication;
import com.bitshares.bitshareswallet.room.BitsharesAsset;
import com.bitshares.bitshareswallet.room.BitsharesDao;

/**
 * Created by lorne on 02/11/2017.
 */

public class SellBuyViewModel extends ViewModel {
    private BitsharesDao bitsharesDao;
    private MutableLiveData<String> currencyData = new MutableLiveData<>();
    private LiveData<BitsharesAsset> assetData;

    public SellBuyViewModel() {
        bitsharesDao = BitsharesApplication.getInstance().getBitsharesDatabase().getBitsharesDao();
        assetData = Transformations.switchMap(
                currencyData,
                currency -> bitsharesDao.queryTargetAvalaliableBalance(currency)
        );
    }

    public void changeBalanceAsset(String currency) {
        currencyData.setValue(currency);
    }

    public LiveData<BitsharesAsset> getAvaliableBalance() {
        return assetData;
    }
}
