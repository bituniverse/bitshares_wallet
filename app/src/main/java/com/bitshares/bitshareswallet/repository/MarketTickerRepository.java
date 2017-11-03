package com.bitshares.bitshareswallet.repository;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MediatorLiveData;
import android.util.Pair;

import com.bitshares.bitshareswallet.BitsharesApplication;
import com.bitshares.bitshareswallet.R;
import com.bitshares.bitshareswallet.market.MarketTicker;
import com.bitshares.bitshareswallet.room.BitsharesAssetObject;
import com.bitshares.bitshareswallet.room.BitsharesBalanceAsset;
import com.bitshares.bitshareswallet.room.BitsharesDao;
import com.bitshares.bitshareswallet.room.BitsharesMarketTicker;
import com.bitshares.bitshareswallet.wallet.BitsharesWalletWraper;
import com.bitshares.bitshareswallet.wallet.exception.NetworkStatusException;
import com.bitshares.bitshareswallet.wallet.graphene.chain.asset_object;
import com.bitshares.bitshareswallet.wallet.graphene.chain.object_id;
import com.bitshares.bitshareswallet.wallet.graphene.chain.utils;
import com.bituniverse.network.Resource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.Flowable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by lorne on 02/11/2017.
 */

public class MarketTickerRepository {
    BitsharesDao bitsharesDao;
    private MediatorLiveData<Resource<List<BitsharesMarketTicker>>> result = new MediatorLiveData<>();

    public MarketTickerRepository() {
        bitsharesDao = BitsharesApplication.getInstance().getBitsharesDatabase().getBitsharesDao();
    }

    public LiveData<Resource<List<BitsharesMarketTicker>>> queryMarketTicker() {
        LiveData<List<BitsharesMarketTicker>> marketTickerListData = bitsharesDao.queryMarketTicker();
        result.addSource(marketTickerListData, data -> {
            result.removeSource(marketTickerListData);
            if (shouldFetch(data)) {
                fetchFromNetwork(marketTickerListData);
            } else {
                result.addSource(marketTickerListData, newData -> result.setValue(Resource.success(newData)));
            }
        });

        return result;
    }

    private boolean shouldFetch(List<BitsharesMarketTicker> bitsharesMarketTickerList) {
        return true;
    }

    private void fetchFromNetwork(final LiveData<List<BitsharesMarketTicker>> dbSource) {
        result.addSource(dbSource, newData -> result.setValue(Resource.loading(newData)));
        // 向远程获取数据，并进行存储
        Flowable.just(0)
                .subscribeOn(Schedulers.io())
                .map(integer -> { // 获取asset list
                    fetchMarketTicker();
                    return 0;
                }).observeOn(AndroidSchedulers.mainThread())
                .subscribe(retCode -> {
                    LiveData<List<BitsharesMarketTicker>> listLiveData = bitsharesDao.queryMarketTicker();
                    result.removeSource(dbSource);
                    result.addSource(listLiveData, newData -> result.setValue(Resource.success(newData)));
                }, throwable -> {
                    result.removeSource(dbSource);
                    result.addSource(dbSource, newData -> result.setValue(Resource.error(throwable.getMessage(), newData)));
                });
    }

    private synchronized void fetchMarketTicker() throws NetworkStatusException {
        int nRet = BitsharesWalletWraper.getInstance().build_connect();
        if (nRet == -1) {
            throw new NetworkStatusException("It can't connect to the server");
        }

        List<BitsharesAssetObject> bitsharesAssetObjectList = bitsharesDao.queryAssetObject();
        Map<String, BitsharesAssetObject>  mapSymbol2Object = new HashMap<>();
        for (BitsharesAssetObject bitsharesAssetObject : bitsharesAssetObjectList) {
            mapSymbol2Object.put(bitsharesAssetObject.symbol, bitsharesAssetObject);
        }

        bitsharesAssetObjectList.clear();
        List<BitsharesMarketTicker> bitsharesMarketTickerList = new ArrayList<>();
        String[] strArrayValue = BitsharesApplication.getInstance().getResources().getStringArray(R.array.quotation_currency_pair_values);
        for (String strValue : strArrayValue) {
            String[] strAssetArray = strValue.split(":");
            BitsharesAssetObject assetObjectBase;
            if (mapSymbol2Object.containsKey(strAssetArray[0])) {
                assetObjectBase = mapSymbol2Object.get(strAssetArray[0]);
            } else {
                asset_object assetObject = BitsharesWalletWraper.getInstance().list_assets(strAssetArray[0], 1).get(0);
                assetObjectBase = new BitsharesAssetObject();
                assetObjectBase.id = 0;
                assetObjectBase.asset_id = assetObject.id;
                assetObjectBase.symbol = assetObject.symbol;
                assetObjectBase.precision = assetObject.get_scaled_precision();

                mapSymbol2Object.put(assetObject.symbol, assetObjectBase);
                bitsharesAssetObjectList.add(assetObjectBase);
            }

            BitsharesAssetObject assetObjectQuote;
            if (mapSymbol2Object.containsKey(strAssetArray[1])) {
                assetObjectQuote = mapSymbol2Object.get(strAssetArray[1]);
            } else {
                asset_object assetObject = BitsharesWalletWraper.getInstance().list_assets(strAssetArray[1], 1).get(0);
                assetObjectQuote = new BitsharesAssetObject();
                assetObjectQuote.id = 0;
                assetObjectQuote.asset_id = assetObject.id;
                assetObjectQuote.symbol = assetObject.symbol;
                assetObjectQuote.precision = assetObject.get_scaled_precision();

                mapSymbol2Object.put(assetObject.symbol, assetObjectQuote);
                bitsharesAssetObjectList.add(assetObjectQuote);
            }

            MarketTicker marketTicker = BitsharesWalletWraper.getInstance().get_ticker(strAssetArray[1], strAssetArray[0]);
            BitsharesMarketTicker bitsharesMarketTicker = new BitsharesMarketTicker();
            bitsharesMarketTicker.id = 0;
            bitsharesMarketTicker.marketTicker = marketTicker;

            BitsharesWalletWraper.getInstance().subscribe_to_market(assetObjectQuote.asset_id, assetObjectBase.asset_id);

            bitsharesMarketTickerList.add(bitsharesMarketTicker);
        }
        bitsharesDao.insertAssetObject(bitsharesAssetObjectList);
        bitsharesDao.insertMarketTicker(bitsharesMarketTickerList);
    }

}
