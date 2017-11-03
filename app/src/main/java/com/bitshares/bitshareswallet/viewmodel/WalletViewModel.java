package com.bitshares.bitshareswallet.viewmodel;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.Transformations;
import android.arch.lifecycle.ViewModel;

import com.bitshares.bitshareswallet.repository.BalanceRepository;
import com.bitshares.bitshareswallet.room.BitsharesBalanceAsset;
import com.bituniverse.network.Resource;

import java.util.List;

/**
 * Created by lorne on 31/10/2017.
 */

public class WalletViewModel extends ViewModel {
    private MutableLiveData<String> currencyData = new MutableLiveData<>();
    private MutableLiveData<Integer> retryData = new MutableLiveData<>();

    public WalletViewModel() {
        retryData.setValue(0);
    }

    public void changeCurrency(String currency) {
        currencyData.setValue(currency);
    }

    public LiveData<Resource<List<BitsharesBalanceAsset>>> getBalanceData() {
        LiveData<Resource<List<BitsharesBalanceAsset>>> resultData = Transformations.switchMap(
                Transformations.switchMap(currencyData, input -> {
                    retryData.setValue(retryData.getValue());
                    return retryData;
                }),
                retryCount -> {
                    return new BalanceRepository().getBalances(currencyData.getValue());
                });
        return resultData;
    }

    public void retry() {
        int nValue = retryData.getValue();
        retryData.setValue(nValue++);
    }
}
