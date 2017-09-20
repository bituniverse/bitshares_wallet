package com.bitshares.bitshareswallet.wallet;

import android.util.Pair;

import com.bitshares.bitshareswallet.wallet.account_object;
import com.bitshares.bitshareswallet.wallet.asset;
import com.bitshares.bitshareswallet.wallet.graphene.chain.asset_object;
import com.bitshares.bitshareswallet.wallet.graphene.chain.bucket_object;
import com.bitshares.bitshareswallet.wallet.graphene.chain.object_id;
import com.bitshares.bitshareswallet.wallet.graphene.chain.operation_history_object;

import java.math.BigInteger;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BitshareData {
    public List<asset> listBalances;
    public List<Pair<operation_history_object, Date>> listHistoryObject;
    public Map<object_id<account_object>, account_object> mapId2AccountObject;
    public Map<object_id<asset_object>, asset_object> mapId2AssetObject;
    public asset_object assetObjectCurrency; // 当前做汇率标志的货币
    public Map<object_id<asset_object>, bucket_object> mapAssetId2Bucket;

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

        bucket_object bucketObject = mapAssetId2Bucket.get(assetAmount.asset_id);

        // // TODO: 06/09/2017 这里需要用整数计算提高精度
        
        long lBaseAmount = (long)(assetAmount.amount * ((double)bucketObject.close_base / bucketObject.close_quote));
        
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
}
