package com.bitshares.bitshareswallet.wallet;

import android.text.format.DateUtils;
import android.util.Pair;

import com.bitshares.bitshareswallet.market.MarketStat;
import com.bitshares.bitshareswallet.market.MarketTicker;
import com.bitshares.bitshareswallet.wallet.account_object;
import com.bitshares.bitshareswallet.wallet.asset;
import com.bitshares.bitshareswallet.wallet.exception.NetworkStatusException;
import com.bitshares.bitshareswallet.wallet.graphene.chain.asset_object;
import com.bitshares.bitshareswallet.wallet.graphene.chain.bucket_object;
import com.bitshares.bitshareswallet.wallet.graphene.chain.object_id;
import com.bitshares.bitshareswallet.wallet.graphene.chain.operation_history_object;
import com.bitshares.bitshareswallet.wallet.graphene.chain.operations;
import com.bitshares.bitshareswallet.wallet.graphene.chain.utils;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BitshareData {
    public List<asset> listBalances;
    public List<Pair<operation_history_object, Date>> listHistoryObject;
    public Map<object_id<account_object>, account_object> mapId2AccountObject = new ConcurrentHashMap<>();
    public Map<object_id<asset_object>, asset_object> mapId2AssetObject = new ConcurrentHashMap<>();
    public asset_object assetObjectCurrency; // 当前做汇率标志的货币
    public Map<object_id<asset_object>, bucket_object> mapAssetId2Bucket = new ConcurrentHashMap<>();
    public Map<String, object_id<asset_object>> mapAssetSymbol2Id = new ConcurrentHashMap<>();

    public Map<Pair<String, String>, MarketTicker> mapMarketCurrencyPair = new ConcurrentHashMap<>();
    public Map<Pair<String, String>, List<MarketStat.HistoryPrice>> mapPair2HistoryPrices = new ConcurrentHashMap<>();

    public class TotalBalances {
        public String strTotalBalances;
        public String strTotalCurrency;
        public String strExchangeRate;
    };

    public TotalBalances getTotalAmountBalances() {
        final asset_object assetObjectBase = mapId2AssetObject.get(new object_id<asset_object>(0, asset_object.class));

        long lTotalAmount = 0;
        for (asset assetBalances : listBalances) {
            long lBaseAmount = convert_asset_to_base(assetBalances, assetObjectBase).amount;
            lTotalAmount += lBaseAmount;
        }

        final asset_object.asset_object_legible assetObjectLegible = assetObjectBase.get_legible_asset_object(lTotalAmount);
        final double fResult = (double)assetObjectLegible.lDecimal / assetObjectLegible.scaled_precision + assetObjectLegible.lCount;
        int nResult = (int)Math.rint(fResult);

        TotalBalances totalBalances = new TotalBalances();
        totalBalances.strTotalBalances = String.format(Locale.ENGLISH, "%d %s", nResult, assetObjectBase.symbol);

        long lTotalCurrency = assetObjectCurrency.convert_exchange_from_base(lTotalAmount);
        asset_object.asset_object_legible legibleCurrency = assetObjectCurrency.get_legible_asset_object(lTotalCurrency);
        double fCurrency = (double)legibleCurrency.lDecimal / legibleCurrency.scaled_precision + legibleCurrency.lCount;
        int nCurrencyResult = (int)Math.rint(fCurrency);
        totalBalances.strTotalCurrency = String.format(Locale.ENGLISH, "%d %s", nCurrencyResult, assetObjectCurrency.symbol);

        double fExchange = get_base_exchange_rate(assetObjectCurrency, assetObjectBase);

        totalBalances.strExchangeRate = String.format(
                Locale.ENGLISH,
                "%.4f %s/%s",
                fExchange,
                assetObjectCurrency.symbol,
                assetObjectBase.symbol
        );

        return totalBalances;
    }
    
    public asset convert_asset_to_base(asset assetAmount, asset_object assetObjectBase) {
        if (assetAmount.asset_id.equals(assetObjectBase.id)) {
            return assetAmount;
        }

        long lBaseAmount;

        bucket_object bucketObject = mapAssetId2Bucket.get(assetAmount.asset_id);
        if (bucketObject == null) {
            lBaseAmount = 0L;
        } else {
            //TODO: 06/09/2017 这里需要用整数计算提高精度
            lBaseAmount = (long) (assetAmount.amount * ((double) bucketObject.close_base / bucketObject.close_quote));
        }
        
        return new asset(lBaseAmount, assetObjectBase.id);
    }

    public double get_base_exchange_rate(asset_object assetObject, asset_object assetObjectBase) {
        bucket_object bucketObject = mapAssetId2Bucket.get(assetObject.id);

        double fExchange =
                (double)bucketObject.close_quote /
                        bucketObject.close_base *
                        assetObjectBase.get_scaled_precision() /
                        assetObject.get_scaled_precision();

        return fExchange;

    }

    public void process_market_fill_change(operations.fill_order_operation fillOrderOperation) {
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
    }

    public void prepare_k_line_data(object_id<asset_object> assetIdBase, object_id<asset_object> assetIdQuote) {
        asset_object assetObjectBase = mapId2AssetObject.get(assetIdBase);
        asset_object assetObjectQuote = mapId2AssetObject.get(assetIdQuote);
        prepare_k_line_data(assetObjectBase, assetObjectQuote);
    }

    public void prepare_k_line_data(asset_object assetObjectBase, asset_object assetObjectQuote) {
        Pair<String, String> pairCurrency = new Pair<>(
                utils.getAssetSymbolDisply(assetObjectQuote.symbol),
                utils.getAssetSymbolDisply(assetObjectBase.symbol)
        );

        Date startDate = new Date(System.currentTimeMillis() - 24 * 3600 * 7 * 1000);
        Date endDate = new Date(System.currentTimeMillis() + DateUtils.DAY_IN_MILLIS);
        List<bucket_object> bucketObjectList = null;
        try {
            bucketObjectList = BitsharesWalletWraper.getInstance().get_market_history(
                    assetObjectBase.id,
                    assetObjectQuote.id,
                    3600,
                    startDate,
                    endDate
            );
        } catch (NetworkStatusException e) {
            e.printStackTrace();
        }

        if (bucketObjectList != null) {
            synchronized (mapPair2HistoryPrices) {
                List<MarketStat.HistoryPrice> listHistoryPrices = new ArrayList<>();
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


                mapPair2HistoryPrices.put(
                        pairCurrency,
                        listHistoryPrices
                );
            }
        }
    }

    private void fillHistoryPrice(List<MarketStat.HistoryPrice> listHistoryPrices, long lCount, long lSeconds) {
        MarketStat.HistoryPrice lastHistoryPrice = listHistoryPrices.get(listHistoryPrices.size() - 1);
        for (int i = 0; i < lCount; ++i) {
            MarketStat.HistoryPrice historyPrice = new MarketStat.HistoryPrice();
            historyPrice.open = lastHistoryPrice.close;
            historyPrice.close = lastHistoryPrice.close;
            historyPrice.high = lastHistoryPrice.close;
            historyPrice.low = lastHistoryPrice.close;
            historyPrice.volume = 0;
            historyPrice.date = new Date(lastHistoryPrice.date.getTime() + lSeconds * 1000 * (i + 1));
            listHistoryPrices.add(historyPrice);
        }
    }

    private MarketStat.HistoryPrice priceFromBucket(bucket_object bucket,
                                                    asset_object assetObjectBase,
                                                    asset_object assetObjectQuote) {
        MarketStat.HistoryPrice price = new MarketStat.HistoryPrice();
        price.date = bucket.key.open;
        if (bucket.key.quote.equals(assetObjectBase.id)) {
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
