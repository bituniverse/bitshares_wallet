package com.bitshares.bitshareswallet.viewmodel;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;

import com.bitshares.bitshareswallet.BitsharesApplication;
import com.bitshares.bitshareswallet.room.BitsharesAsset;
import com.bitshares.bitshareswallet.room.BitsharesBalanceAsset;
import com.bitshares.bitshareswallet.room.BitsharesDao;

import java.util.List;

/**
 * Created by lorne on 02/11/2017.
 */

public class SendViewModel extends ViewModel {
    private BitsharesDao bitsharesDao;
    private LiveData<List<BitsharesBalanceAsset>> balancesList;

    public SendViewModel() {
        bitsharesDao = BitsharesApplication.getInstance().getBitsharesDatabase().getBitsharesDao();
        balancesList = bitsharesDao.queryAvaliableBalances("USD");
    }

    public LiveData<List<BitsharesBalanceAsset>> getBalancesList() {
        return balancesList;
    }

}
