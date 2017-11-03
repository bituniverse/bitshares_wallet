package com.bitshares.bitshareswallet.viewmodel;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.Transformations;
import android.arch.lifecycle.ViewModel;
import android.content.SharedPreferences;
import android.support.v7.preference.PreferenceManager;
import android.util.Pair;

import com.bitshares.bitshareswallet.BitsharesApplication;
import com.bitshares.bitshareswallet.data.HistoryPrice;
import com.bitshares.bitshareswallet.livedata.MarketChangeLiveData;
import com.bitshares.bitshareswallet.repository.HistoryPriceRepository;
import com.bitshares.bitshareswallet.repository.MarketTickerRepository;
import com.bitshares.bitshareswallet.room.BitsharesMarketTicker;
import com.bitshares.bitshareswallet.wallet.graphene.chain.utils;
import com.bituniverse.network.Resource;

import java.util.List;

/**
 * Created by lorne on 02/11/2017.
 */

public class QuotationViewModel extends ViewModel {
    private MutableLiveData<Pair<String, String>> mutableLiveDataSelected = new MutableLiveData<>();
    private MarketChangeLiveData marketChangeLiveData = new MarketChangeLiveData();

    public LiveData<Resource<List<BitsharesMarketTicker>>> getMarketTicker() {
        return Transformations.switchMap(marketChangeLiveData, marketChange -> {
            return new MarketTickerRepository().queryMarketTicker();
        });
    }

    public void selectedMarketTicker(Pair<String, String> currencyPair) {
        Pair<String, String> current = mutableLiveDataSelected.getValue();
        if (current != null && current.equals(currencyPair)) {
            return;
        }

        mutableLiveDataSelected.setValue(currencyPair);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(BitsharesApplication.getInstance());
        String strPair = utils.getAssetSymbolDisply(currencyPair.second) + ":" +
                utils.getAssetSymbolDisply(currencyPair.first);
        prefs.edit().
                putString("quotation_currency_pair", strPair)
                .apply();
    }

    public LiveData<Pair<String, String>> getSelectedMarketTicker() {
        return mutableLiveDataSelected;
    }

    public LiveData<Resource<List<HistoryPrice>>> getHistoryPrice() {
        return Transformations.switchMap(mutableLiveDataSelected, currencyPair -> {
            return new HistoryPriceRepository(currencyPair).getHistoryPrice();
        });
    }
}
