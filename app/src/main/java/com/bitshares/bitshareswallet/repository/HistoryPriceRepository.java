package com.bitshares.bitshareswallet.repository;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.text.format.DateUtils;
import android.util.Pair;

import com.bitshares.bitshareswallet.BitsharesApplication;
import com.bitshares.bitshareswallet.data.HistoryPrice;
import com.bitshares.bitshareswallet.room.BitsharesAssetObject;
import com.bitshares.bitshareswallet.room.BitsharesDao;
import com.bitshares.bitshareswallet.wallet.BitsharesWalletWraper;
import com.bitshares.bitshareswallet.wallet.exception.NetworkStatusException;
import com.bitshares.bitshareswallet.wallet.graphene.chain.bucket_object;
import com.bitshares.bitshareswallet.wallet.graphene.chain.utils;
import com.bituniverse.network.Resource;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import io.reactivex.Flowable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by lorne on 02/11/2017.
 */

public class HistoryPriceRepository {
    private BitsharesDao bitsharesDao;
    private Pair<String, String> currencyPair;
    private MutableLiveData<Resource<List<HistoryPrice>>> result = new MutableLiveData<>();
    public HistoryPriceRepository(Pair<String, String> currencyPair) {
        this.currencyPair = currencyPair;
        bitsharesDao = BitsharesApplication.getInstance().getBitsharesDatabase().getBitsharesDao();
    }

    public LiveData<Resource<List<HistoryPrice>>> getHistoryPrice() {
        result.setValue(Resource.loading(null));
        Flowable.just(0)
                .subscribeOn(Schedulers.io())
                .map(integer -> {
                    return fetchFromNetwork(currencyPair);
                }).observeOn(AndroidSchedulers.mainThread())
                .subscribe(historyPriceList -> {
                    result.setValue(Resource.success(historyPriceList));
                }, throwable -> {
                    result.setValue(Resource.error(throwable.getMessage(), null));
                });

        return result;
    }

    private List<HistoryPrice> fetchFromNetwork(Pair<String, String> currencyPair) throws NetworkStatusException {
        int nRet = BitsharesWalletWraper.getInstance().build_connect();
        if (nRet == -1) {
            throw new NetworkStatusException("It can't connect to the server");
        }

        String[] symbolArray = {currencyPair.second, currencyPair.first};
        List<BitsharesAssetObject> bitsharesAssetObjectList = bitsharesDao.queryAssetObject(symbolArray);
        if (bitsharesAssetObjectList.get(0).symbol.compareTo(currencyPair.second) == 0) {
            return prepare_k_line_data(bitsharesAssetObjectList.get(0), bitsharesAssetObjectList.get(1));
        } else {
            return prepare_k_line_data(bitsharesAssetObjectList.get(1), bitsharesAssetObjectList.get(0));
        }
    }

    /*public void process_market_fill_change(operations.fill_order_operation fillOrderOperation) {
        asset_object assetObjectPay = mapId2AssetObject.get(fillOrderOperation.pays.asset_id);
        asset_object assetObjectReceive = mapId2AssetObject.get(fillOrderOperation.receives.asset_id);

        try {
            // 针对当前的ticker数据进行更新
            Pair<String, String> pairCurrency = new Pair<>(
                    utils.getAssetSymbolDisply(assetObjectPay.symbol),
                    utils.getAssetSymbolDisply(assetObjectReceive.symbol)
            );
            if (mapMarketCurrencyPair.containsKey(pairCurrency)) {
                MarketTicker marketTicker = BitsharesWalletWraper.getInstance().get_ticker(assetObjectReceive.symbol, assetObjectPay.symbol);
                mapMarketCurrencyPair.put(pairCurrency, marketTicker);

                prepare_k_line_data(assetObjectPay, assetObjectReceive);

            } else {
                MarketTicker marketTicker = BitsharesWalletWraper.getInstance().get_ticker(assetObjectPay.symbol, assetObjectReceive.symbol);
                mapMarketCurrencyPair.put(
                        new Pair<>(utils.getAssetSymbolDisply(assetObjectReceive.symbol), utils.getAssetSymbolDisply(assetObjectPay.symbol)),
                        marketTicker
                );
                prepare_k_line_data(assetObjectReceive, assetObjectPay);
            }
        } catch (NetworkStatusException e) {
            e.printStackTrace();
        }
    }*/

    public List<HistoryPrice> prepare_k_line_data(BitsharesAssetObject assetObjectBase,
                                                  BitsharesAssetObject assetObjectQuote) throws NetworkStatusException {
        Date startDate = new Date(System.currentTimeMillis() - 24 * 3600 * 7 * 1000);
        Date endDate = new Date(System.currentTimeMillis() + DateUtils.DAY_IN_MILLIS);

        List<bucket_object> bucketObjectList = BitsharesWalletWraper.getInstance().get_market_history(
                assetObjectBase.asset_id,
                assetObjectQuote.asset_id,
                3600,
                startDate,
                endDate
        );

        if (bucketObjectList != null) {
            List<HistoryPrice> listHistoryPrices = new ArrayList<>();
            long lastBucketTime = 0;
            for (bucket_object bucketObject : bucketObjectList) {
                if (lastBucketTime == 0) {
                    lastBucketTime = bucketObject.key.open.getTime();
                }

                long differenceTime = bucketObject.key.open.getTime() - lastBucketTime;
                if (differenceTime > bucketObject.key.seconds * 1000) {
                    long lCount = differenceTime / (bucketObject.key.seconds * 1000) - 1;

                    fillHistoryPrice(listHistoryPrices, lCount, bucketObject.key.seconds);
                }

                listHistoryPrices.add(priceFromBucket(bucketObject, assetObjectBase, assetObjectQuote));
                lastBucketTime = bucketObject.key.open.getTime();
            }

            // 最后一个数据如果不完整，则依然需要进行补全
            bucket_object bucketObjectLast = bucketObjectList.get(bucketObjectList.size() - 1);
            long differenceTime = System.currentTimeMillis() - bucketObjectLast.key.open.getTime();
            if (differenceTime > bucketObjectLast.key.seconds * 1000) {
                long lCount = differenceTime / (bucketObjectLast.key.seconds * 1000) - 1;
                fillHistoryPrice(listHistoryPrices, lCount, bucketObjectLast.key.seconds);
            }
            return listHistoryPrices;
        } else {
            return new ArrayList<>();
        }
    }

    private void fillHistoryPrice(List<HistoryPrice> listHistoryPrices, long lCount, long lSeconds) {
        HistoryPrice lastHistoryPrice = listHistoryPrices.get(listHistoryPrices.size() - 1);
        for (int i = 0; i < lCount; ++i) {
            HistoryPrice historyPrice = new HistoryPrice();
            historyPrice.open = lastHistoryPrice.close;
            historyPrice.close = lastHistoryPrice.close;
            historyPrice.high = lastHistoryPrice.close;
            historyPrice.low = lastHistoryPrice.close;
            historyPrice.volume = 0;
            historyPrice.date = new Date(lastHistoryPrice.date.getTime() + lSeconds * 1000 * (i + 1));
            listHistoryPrices.add(historyPrice);
        }
    }

    private HistoryPrice priceFromBucket(bucket_object bucket,
                                         BitsharesAssetObject assetObjectBase,
                                         BitsharesAssetObject assetObjectQuote) {
        HistoryPrice price = new HistoryPrice();
        price.date = bucket.key.open;
        if (bucket.key.quote.equals(assetObjectBase.asset_id)) {
            price.high = utils.get_asset_price(bucket.high_base, assetObjectQuote,
                    bucket.high_quote, assetObjectBase);
            price.low = utils.get_asset_price(bucket.low_base, assetObjectQuote,
                    bucket.low_quote, assetObjectBase);
            price.open = utils.get_asset_price(bucket.open_base, assetObjectQuote,
                    bucket.open_quote, assetObjectBase);
            price.close = utils.get_asset_price(bucket.close_base, assetObjectQuote,
                    bucket.close_quote, assetObjectBase);
            price.volume = utils.get_asset_amount(bucket.quote_volume, assetObjectBase);
        } else {
            price.low = utils.get_asset_price(bucket.high_quote, assetObjectQuote,
                    bucket.high_base, assetObjectBase);
            price.high = utils.get_asset_price(bucket.low_quote, assetObjectQuote,
                    bucket.low_base, assetObjectBase);
            price.open = utils.get_asset_price(bucket.open_quote, assetObjectQuote,
                    bucket.open_base, assetObjectBase);
            price.close = utils.get_asset_price(bucket.close_quote, assetObjectQuote,
                    bucket.close_base, assetObjectBase);
            price.volume = utils.get_asset_amount(bucket.base_volume, assetObjectBase);
        }
        if (price.low == 0) {
            price.low = findMin(price.open, price.close);
        }
        if (price.high == Double.NaN || price.high == Double.POSITIVE_INFINITY) {
            price.high = findMax(price.open, price.close);
        }
        if (price.close == Double.POSITIVE_INFINITY || price.close == 0) {
            price.close = price.open;
        }
        if (price.open == Double.POSITIVE_INFINITY || price.open == 0) {
            price.open = price.close;
        }
        if (price.high > 1.3 * ((price.open + price.close) / 2)) {
            price.high = findMax(price.open, price.close);
        }
        if (price.low < 0.7 * ((price.open + price.close) / 2)) {
            price.low = findMin(price.open, price.close);
        }
        return price;
    }

    private static double findMax(double a, double b) {
        if (a != Double.POSITIVE_INFINITY && b != Double.POSITIVE_INFINITY) {
            return Math.max(a, b);
        } else if (a == Double.POSITIVE_INFINITY) {
            return b;
        } else {
            return a;
        }
    }

    private static double findMin(double a, double b) {
        if (a != 0 && b != 0) {
            return Math.min(a, b);
        } else if (a == 0) {
            return b;
        } else {
            return a;
        }
    }
}
