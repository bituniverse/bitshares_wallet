package com.bitshares.bitshareswallet.viewmodel;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MediatorLiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.Transformations;
import android.arch.lifecycle.ViewModel;

import com.bitshares.bitshareswallet.BitsharesApplication;
import com.bitshares.bitshareswallet.livedata.StatusChangeLiveData;
import com.bitshares.bitshareswallet.repository.AvailableBalanceRepository;
import com.bitshares.bitshareswallet.repository.BalanceRepository;
import com.bitshares.bitshareswallet.room.BitsharesAsset;
import com.bitshares.bitshareswallet.room.BitsharesBalanceAsset;
import com.bitshares.bitshareswallet.room.BitsharesDao;
import com.bituniverse.network.Resource;

import java.util.List;

/**
 * Created by lorne on 02/11/2017.
 */

public class SellBuyViewModel extends ViewModel {
    private MutableLiveData<String> currencyData = new MutableLiveData<>();
    private StatusChangeLiveData statusChangeLiveData = new StatusChangeLiveData();

    public SellBuyViewModel() {

    }

    public void changeBalanceAsset(String currency) {
        currencyData.setValue(currency);
    }

    public LiveData<Resource<BitsharesAsset>> getAvaliableBalance() {
        LiveData<Resource<BitsharesAsset>> balanceData = Transformations.switchMap(
                Transformations.switchMap(currencyData, input -> {
                    return statusChangeLiveData;
                }),
                statusChange -> {
                    return new AvailableBalanceRepository().getTargetAvaliableBlance(currencyData.getValue());
                });


        return balanceData;
    }
}
