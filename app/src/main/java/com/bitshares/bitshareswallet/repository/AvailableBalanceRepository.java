package com.bitshares.bitshareswallet.repository;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MediatorLiveData;

import com.bitshares.bitshareswallet.BitsharesApplication;
import com.bitshares.bitshareswallet.market.MarketTicker;
import com.bitshares.bitshareswallet.room.BitsharesAsset;
import com.bitshares.bitshareswallet.room.BitsharesAssetObject;
import com.bitshares.bitshareswallet.room.BitsharesDao;
import com.bitshares.bitshareswallet.room.BitsharesMarketTicker;
import com.bitshares.bitshareswallet.wallet.BitsharesWalletWraper;
import com.bitshares.bitshareswallet.wallet.account_balance_object;
import com.bitshares.bitshareswallet.wallet.account_object;
import com.bitshares.bitshareswallet.wallet.exception.NetworkStatusException;
import com.bitshares.bitshareswallet.wallet.full_account_object;
import com.bitshares.bitshareswallet.wallet.graphene.chain.asset_object;
import com.bitshares.bitshareswallet.wallet.graphene.chain.limit_order_object;
import com.bitshares.bitshareswallet.wallet.graphene.chain.object_id;
import com.bituniverse.network.Resource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.reactivex.Flowable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by lorne on 03/11/2017.
 */

public class AvailableBalanceRepository {
    private String mCurrency;
    private BitsharesDao bitsharesDao;

    private MediatorLiveData<Resource<BitsharesAsset>> result = new MediatorLiveData<>();

    public AvailableBalanceRepository() {
        bitsharesDao = BitsharesApplication.getInstance().getBitsharesDatabase().getBitsharesDao();
    }

    public LiveData<Resource<BitsharesAsset>> getTargetAvaliableBlance(String currency) {
        mCurrency = currency;
        LiveData<BitsharesAsset> balanceAssetListLiveData = bitsharesDao.queryTargetAvalaliableBalance(currency);
        result.addSource(
                balanceAssetListLiveData,
                data -> {
                    result.removeSource(balanceAssetListLiveData);
                    if (shouldFetch(data)) {
                        fetchFromNetwork(balanceAssetListLiveData);
                    } else {
                        result.addSource(balanceAssetListLiveData, newData -> result.setValue(Resource.success(newData)));
                    }
                });

        return result;
    }

    private boolean shouldFetch(BitsharesAsset bitsharesBalanceAsset) {
        return true;
    }

    private void fetchFromNetwork(final LiveData<BitsharesAsset> dbSource) {
        result.addSource(dbSource, newData -> result.setValue(Resource.loading(newData)));
        // 向远程获取数据，并进行存储
        Flowable.just(0)
                .subscribeOn(Schedulers.io())
                .map(integer -> { // 获取asset list
                    fetchBalanceData();
                    return 0;
                }).observeOn(AndroidSchedulers.mainThread())
                .subscribe(retCode -> {
                    LiveData<BitsharesAsset> listLiveData = bitsharesDao.queryTargetAvalaliableBalance(mCurrency);
                    result.removeSource(dbSource);
                    result.addSource(listLiveData, newData -> result.setValue(Resource.success(newData)));
                }, throwable -> {
                    result.removeSource(dbSource);
                    result.addSource(dbSource, newData -> result.setValue(Resource.error(throwable.getMessage(), newData)));
                });
    }

    private void fetchBalanceData() throws NetworkStatusException {
        int nRet = BitsharesWalletWraper.getInstance().build_connect();
        if (nRet == -1) {
            throw new NetworkStatusException("It can't connect to the server");
        }

        account_object accountObject = BitsharesWalletWraper.getInstance().get_account();
        List<full_account_object> fullAccountObjectList = BitsharesWalletWraper.getInstance().get_full_accounts(
                Collections.singletonList(accountObject.name),
                true
        );

        full_account_object fullAccountObject = fullAccountObjectList.get(0);

        Set<object_id<asset_object>> objectIdSet = new HashSet<>();
        for (account_balance_object object : fullAccountObject.balances) {
            objectIdSet.add(object.asset_type);
        }

        for (limit_order_object object : fullAccountObject.limit_orders) {
            objectIdSet.add(object.sell_price.base.asset_id);
            objectIdSet.add(object.sell_price.quote.asset_id);
        }

        List<object_id<asset_object>> objectIdList = new ArrayList<>(objectIdSet);
        Map<object_id<asset_object>, asset_object> mapId2Object =
                BitsharesWalletWraper.getInstance().get_assets(objectIdList);

        List<BitsharesAsset> orderAssetList = bitsharesDao.queryBalanceList();
        if (orderAssetList.isEmpty() == false) {
            bitsharesDao.deleteBalance(orderAssetList);
        }

        List<BitsharesAsset> bitsharesAssetList = new ArrayList<>();
        for (account_balance_object object : fullAccountObject.balances) {
            BitsharesAsset bitsharesAsset = new BitsharesAsset();
            bitsharesAsset.id = 0;
            bitsharesAsset.currency = mapId2Object.get(object.asset_type).symbol;
            bitsharesAsset.amount = object.balance;
            bitsharesAsset.asset_id = object.asset_type;
            bitsharesAsset.precision = mapId2Object.get(object.asset_type).get_scaled_precision();
            bitsharesAsset.type = BitsharesAsset.TYPE_AVALIABLE;
            bitsharesAssetList.add(bitsharesAsset);
        }

        for (limit_order_object limitOrderObject : fullAccountObject.limit_orders) {
            BitsharesAsset bitsharesAsset = new BitsharesAsset();
            bitsharesAsset.id = 0;
            bitsharesAsset.currency = mapId2Object.get(limitOrderObject.sell_price.base.asset_id).symbol;
            bitsharesAsset.amount = limitOrderObject.sell_price.base.amount;
            bitsharesAsset.asset_id = limitOrderObject.sell_price.base.asset_id;
            bitsharesAsset.precision = mapId2Object.get(limitOrderObject.sell_price.base.asset_id).get_scaled_precision();
            bitsharesAsset.type = BitsharesAsset.TYPE_SELL_ORDER;
            bitsharesAssetList.add(bitsharesAsset);
        }

        bitsharesDao.insertBlance(bitsharesAssetList);
        List<BitsharesAssetObject> bitsharesAssetObjectList = new ArrayList<>();
        for (asset_object assetObject : mapId2Object.values()) {
            BitsharesAssetObject bitsharesAssetObject = new BitsharesAssetObject();
            bitsharesAssetObject.asset_id = assetObject.id;
            bitsharesAssetObject.precision = assetObject.get_scaled_precision();
            bitsharesAssetObject.symbol = assetObject.symbol;
            bitsharesAssetObjectList.add(bitsharesAssetObject);
        }

        asset_object assetObjectCurrency = BitsharesWalletWraper.getInstance()
                .list_assets("USD", 1).get(0);
        BitsharesAssetObject bitsharesAssetObject = new BitsharesAssetObject();
        bitsharesAssetObject.symbol = assetObjectCurrency.symbol;
        bitsharesAssetObject.precision = assetObjectCurrency.get_scaled_precision();
        bitsharesAssetObject.asset_id = assetObjectCurrency.id;
        bitsharesAssetObjectList.add(bitsharesAssetObject);
        bitsharesDao.insertAssetObject(bitsharesAssetObjectList);

        List<BitsharesMarketTicker> bitsharesMarketTickerList = new ArrayList<>();
        for (BitsharesAsset bitsharesAsset : bitsharesAssetList) {
            if (bitsharesAsset.currency.compareTo("BTS") != 0) {
                MarketTicker marketTicker = BitsharesWalletWraper.getInstance().get_ticker(
                        "BTS",
                        bitsharesAsset.currency
                );
                BitsharesMarketTicker bitsharesMarketTicker = new BitsharesMarketTicker();
                bitsharesMarketTicker.id = 0;
                bitsharesMarketTicker.marketTicker = marketTicker;
                bitsharesMarketTickerList.add(bitsharesMarketTicker);
            }
        }
        BitsharesMarketTicker bitsharesMarketTicker = new BitsharesMarketTicker();
        bitsharesMarketTicker.marketTicker = new MarketTicker();
        bitsharesMarketTicker.id = 0;
        bitsharesMarketTicker.marketTicker.base = "BTS";
        bitsharesMarketTicker.marketTicker.quote = "BTS";
        bitsharesMarketTicker.marketTicker.latest = 1;
        bitsharesMarketTicker.marketTicker.lowest_ask = 1;
        bitsharesMarketTicker.marketTicker.highest_bid = 1;
        bitsharesMarketTicker.marketTicker.percent_change = "0";
        bitsharesMarketTicker.marketTicker.base_volume = 1;
        bitsharesMarketTicker.marketTicker.quote_volume = 1;
        bitsharesMarketTickerList.add(bitsharesMarketTicker);

        MarketTicker marketTicker = BitsharesWalletWraper.getInstance().get_ticker(
                "USD",
                "BTS"
        );
        bitsharesMarketTicker = new BitsharesMarketTicker();
        bitsharesMarketTicker.id = 0;
        bitsharesMarketTicker.marketTicker = marketTicker;
        bitsharesMarketTickerList.add(bitsharesMarketTicker);

        marketTicker = BitsharesWalletWraper.getInstance().get_ticker(
                "CNY",
                "BTS"
        );
        bitsharesMarketTicker = new BitsharesMarketTicker();
        bitsharesMarketTicker.id = 0;
        bitsharesMarketTicker.marketTicker = marketTicker;
        bitsharesMarketTickerList.add(bitsharesMarketTicker);

        bitsharesDao.insertMarketTicker(bitsharesMarketTickerList);
    }
}
