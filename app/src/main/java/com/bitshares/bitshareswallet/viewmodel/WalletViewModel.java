package com.bitshares.bitshareswallet.viewmodel;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MediatorLiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.Transformations;
import android.arch.lifecycle.ViewModel;

import com.bitshares.bitshareswallet.livedata.StatusChangeLiveData;
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
    private StatusChangeLiveData statusChangeLiveData = new StatusChangeLiveData();
    MediatorLiveData<Resource<List<BitsharesBalanceAsset>>> resultData = new MediatorLiveData<>();

    public WalletViewModel() {
        retryData.setValue(0);

        resultData.addSource(statusChangeLiveData, statusChange -> {
            LiveData<Resource<List<BitsharesBalanceAsset>>> balanceData = Transformations.switchMap(
                    Transformations.switchMap(currencyData, input -> {
                        retryData.setValue(retryData.getValue());
                        return retryData;
                    }),
                    retryCount -> {
                        return new BalanceRepository().getBalances(currencyData.getValue());
                    });

            resultData.addSource(balanceData, result -> resultData.setValue(result));
        });

    }

    public void changeCurrency(String currency) {
        currencyData.setValue(currency);
    }

    public LiveData<Resource<List<BitsharesBalanceAsset>>> getBalanceData() {
        return resultData;
    }

    public void retry() {
        int nValue = retryData.getValue();
        retryData.setValue(nValue++);
    }
}
