package com.bitshares.bitshareswallet.wallet;

import android.content.SharedPreferences;
import android.support.v7.preference.PreferenceManager;
import android.util.Pair;

import com.bitshares.bitshareswallet.BitsharesApplication;
import com.bitshares.bitshareswallet.R;
import com.bitshares.bitshareswallet.market.MarketTicker;
import com.bitshares.bitshareswallet.market.MarketTrade;
import com.bitshares.bitshareswallet.wallet.common.ErrorCode;
import com.bitshares.bitshareswallet.wallet.exception.NetworkStatusException;
import com.bitshares.bitshareswallet.wallet.faucet.CreateAccountException;
import com.bitshares.bitshareswallet.wallet.graphene.chain.asset_object;
import com.bitshares.bitshareswallet.wallet.graphene.chain.block_header;
import com.bitshares.bitshareswallet.wallet.graphene.chain.bucket_object;
import com.bitshares.bitshareswallet.wallet.graphene.chain.dynamic_global_property_object;
import com.bitshares.bitshareswallet.wallet.graphene.chain.global_property_object;
import com.bitshares.bitshareswallet.wallet.graphene.chain.limit_order_object;
import com.bitshares.bitshareswallet.wallet.graphene.chain.memo_data;
import com.bitshares.bitshareswallet.wallet.graphene.chain.object_id;
import com.bitshares.bitshareswallet.wallet.graphene.chain.operation_history_object;
import com.bitshares.bitshareswallet.wallet.graphene.chain.operations;
import com.bitshares.bitshareswallet.wallet.graphene.chain.signed_transaction;
import com.bitshares.bitshareswallet.wallet.graphene.chain.utils;
import com.google.common.collect.Sets;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import de.bitsharesmunich.graphenej.FileBin;
import de.bitsharesmunich.graphenej.models.backup.LinkedAccount;
import de.bitsharesmunich.graphenej.models.backup.WalletBackup;

public class BitsharesWalletWraper {
    public interface BitsharesDataObserver {
        void onDisconnect();
        void onMarketFillUpdate(object_id<asset_object> base, object_id<asset_object> quote);
        void onAccountChanged();
    }

    private static BitsharesWalletWraper bitsharesWalletWraper = new BitsharesWalletWraper();
    private wallet_api mWalletApi;
    private Map<object_id<account_object>, account_object> mMapAccountId2Object = new ConcurrentHashMap<>();
    private Map<object_id<account_object>, List<asset>> mMapAccountId2Asset = new ConcurrentHashMap<>();
    private Map<object_id<account_object>, List<operation_history_object>> mMapAccountId2History = new ConcurrentHashMap<>();
    private Map<object_id<asset_object>, asset_object> mMapAssetId2Object = new ConcurrentHashMap<>();
    private Set<BitsharesDataObserver> msetDataObserver;

    private String mstrWalletFilePath;

    private int mnStatus = STATUS_INVALID;

    private static final int STATUS_INVALID = -1;
    private static final int STATUS_INITIALIZED = 0;

    private BitsharesWalletWraper() {
        mstrWalletFilePath = BitsharesApplication.getInstance().getFilesDir().getPath();
        mstrWalletFilePath += "/wallet.json";
        msetDataObserver = Sets.newConcurrentHashSet();

        initializeWalletapi();
    }

    public static BitsharesWalletWraper getInstance() {
        return bitsharesWalletWraper;
    }

    public void registerDataObserver(BitsharesDataObserver observer) {
        msetDataObserver.add(observer);
    }

    public void unregisterDataObserver(BitsharesDataObserver observer) {
        msetDataObserver.remove(observer);
    }

    private void initializeWalletapi() {
        mWalletApi = new wallet_api(new websocket_api.BitsharesNoticeListener() {
            @Override
            public void onNoticeMessage(BitsharesNoticeMessage message) {
                if (message.listFillOrder != null) {
                    // market发生变化，需要进行对应的数据更新，查看里面对应的id，然后进行数据更新
                    for (operations.operation_type operationType : message.listFillOrder) {
                        if (operationType.nOperationType == operations.ID_FILL_LMMIT_ORDER_OPERATION) {
                            for (BitsharesDataObserver observer : msetDataObserver) {
                                    operations.fill_order_operation operation = (operations.fill_order_operation)operationType.operationContent;
                                    observer.onMarketFillUpdate(operation.pays.asset_id, operation.receives.asset_id);
                            }
                        }
                    }
                } else if (message.bAccountChanged) {
                    for (BitsharesDataObserver observer : msetDataObserver) {
                        observer.onAccountChanged();
                    }
                }
            }

            @Override
            public void onDisconnect() {
                for (BitsharesDataObserver observer : msetDataObserver) {
                    observer.onDisconnect();
                }
            }
        });
    }

    public void reset() {
        mWalletApi.reset();
        initializeWalletapi();
        mMapAccountId2Object.clear();;
        mMapAccountId2Asset.clear();;
        mMapAccountId2History.clear();
        mMapAssetId2Object.clear();;
        msetDataObserver.clear();;

        File file = new File(mstrWalletFilePath);
        file.delete();

        mnStatus = STATUS_INVALID;
    }

    public account_object get_account() {
        List<account_object> listAccount = mWalletApi.list_my_accounts();
        if (listAccount == null || listAccount.isEmpty()) {
            return null;
        }

        return listAccount.get(0);
    }

    public boolean is_new() {
        return mWalletApi.is_new();
    }

    public  boolean is_locked() {
        return mWalletApi.is_locked();
    }

    public int load_wallet_file() {
        return mWalletApi.load_wallet_file(mstrWalletFilePath);
    }

    private int save_wallet_file() {
        return mWalletApi.save_wallet_file(mstrWalletFilePath);
    }

    public synchronized int build_connect() {
        if (mnStatus == STATUS_INITIALIZED) {
            return 0;
        }

        int nRet = mWalletApi.initialize();
        if (nRet != 0) {
            return nRet;
        }

        mnStatus = STATUS_INITIALIZED;
        return 0;
    }


    public List<account_object> list_my_accounts() {
        return mWalletApi.list_my_accounts();
    }

    public int import_key(String strAccountNameOrId,
                          String strPassword,
                          String strPrivateKey) {

        mWalletApi.set_passwrod(strPassword);

        try {
            int nRet = mWalletApi.import_key(strAccountNameOrId, strPrivateKey);
            if (nRet != 0) {
                return nRet;
            }
        } catch (NetworkStatusException e) {
            e.printStackTrace();
            return -1;
        }

        save_wallet_file();

        for (account_object accountObject : list_my_accounts()) {
            mMapAccountId2Object.put(accountObject.id, accountObject);
        }

        return 0;
    }

    public int import_keys(String strAccountNameOrId,
                           String strPassword,
                           String strPrivateKey1,
                           String strPrivateKey2) {

        mWalletApi.set_passwrod(strPassword);

        try {
            int nRet = mWalletApi.import_keys(strAccountNameOrId, strPrivateKey1, strPrivateKey2);
            if (nRet != 0) {
                return nRet;
            }
        } catch (NetworkStatusException e) {
            e.printStackTrace();
            return -1;
        }

        save_wallet_file();

        for (account_object accountObject : list_my_accounts()) {
            mMapAccountId2Object.put(accountObject.id, accountObject);
        }

        return 0;
    }

    public int import_brain_key(String strAccountNameOrId,
                                String strPassword,
                                String strBrainKey) {
        mWalletApi.set_passwrod(strPassword);
        try {
            int nRet = mWalletApi.import_brain_key(strAccountNameOrId, strBrainKey);
            if (nRet != 0) {
                return nRet;
            }
        } catch (NetworkStatusException e) {
            e.printStackTrace();
            return ErrorCode.ERROR_NETWORK_FAIL;
        }

        save_wallet_file();

        for (account_object accountObject : list_my_accounts()) {
            mMapAccountId2Object.put(accountObject.id, accountObject);
        }

        return 0;
    }

    public int import_file_bin(String strPassword,
                               String strFilePath) {
        File file = new File(strFilePath);
        if (file.exists() == false) {
            return ErrorCode.ERROR_FILE_NOT_FOUND;
        }

        int nSize = (int)file.length();

        final byte[] byteContent = new byte[nSize];

        FileInputStream fileInputStream;
        try {
            fileInputStream = new FileInputStream(file);
            fileInputStream.read(byteContent, 0, byteContent.length);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return ErrorCode.ERROR_FILE_NOT_FOUND;
        } catch (IOException e) {
            e.printStackTrace();
            return ErrorCode.ERROR_FILE_READ_FAIL;
        }

        WalletBackup walletBackup = FileBin.deserializeWalletBackup(byteContent, strPassword);
        if (walletBackup == null) {
            return ErrorCode.ERROR_FILE_BIN_PASSWORD_INVALID;
        }

        String strBrainKey = walletBackup.getWallet(0).decryptBrainKey(strPassword);
        //LinkedAccount linkedAccount = walletBackup.getLinkedAccounts()[0];

        int nRet = ErrorCode.ERROR_IMPORT_NOT_MATCH_PRIVATE_KEY;
        for (LinkedAccount linkedAccount : walletBackup.getLinkedAccounts()) {
            nRet = import_brain_key(linkedAccount.getName(), strPassword, strBrainKey);
            if (nRet == 0) {
                break;
            }
        }

        return nRet;
    }

    public int import_account_password(String strAccountName,
                                       String strPassword) {
        mWalletApi.set_passwrod(strPassword);
        try {
            int nRet = mWalletApi.import_account_password(strAccountName, strPassword);
            if (nRet != 0) {
                return nRet;
            }
        } catch (NetworkStatusException e) {
            e.printStackTrace();
            return -1;
        }

        save_wallet_file();

        for (account_object accountObject : list_my_accounts()) {
            mMapAccountId2Object.put(accountObject.id, accountObject);
        }

        return 0;

    }

    public int unlock(String strPassword) {
        return mWalletApi.unlock(strPassword);
    }

    public int lock() {
        return mWalletApi.lock();
    }

    public List<asset> list_balances(boolean bRefresh) throws NetworkStatusException {
        List<asset> listAllAsset = new ArrayList<>();
        for (account_object accountObject : list_my_accounts()) {
            List<asset> listAsset = list_account_balance(accountObject.id, bRefresh);

            listAllAsset.addAll(listAsset);
        }

        return listAllAsset;
    }

    public List<asset> list_account_balance(object_id<account_object> accountObjectId,
                                            boolean bRefresh) throws NetworkStatusException {
        List<asset> listAsset = mMapAccountId2Asset.get(accountObjectId);
        if (bRefresh || listAsset == null) {
            listAsset = mWalletApi.list_account_balance(accountObjectId);
            mMapAccountId2Asset.put(accountObjectId, listAsset);
        }

        return listAsset;
    }

    public List<operation_history_object> get_history(boolean bRefresh) throws NetworkStatusException {
        List<operation_history_object> listAllHistoryObject = new ArrayList<>();
        for (account_object accountObject : list_my_accounts()) {
            List<operation_history_object> listHistoryObject = get_account_history(
                    accountObject.id,
                    100,
                    bRefresh
            );

            listAllHistoryObject.addAll(listHistoryObject);
        }

        return listAllHistoryObject;
    }

    public List<operation_history_object> get_account_history(object_id<account_object> accountObjectId,
                                                              int nLimit,
                                                              boolean bRefresh) throws NetworkStatusException {
        List<operation_history_object> listHistoryObject = mMapAccountId2History.get(accountObjectId);
        if (listHistoryObject == null || bRefresh) {
            listHistoryObject = mWalletApi.get_account_history(
                    accountObjectId,
                    new object_id<operation_history_object>(0, operation_history_object.class),
                    nLimit
            );
            mMapAccountId2History.put(accountObjectId, listHistoryObject);
        }
        return listHistoryObject;
    }

    public List<operation_history_object> get_account_history(object_id<account_object> accountObjectId,
                                                              object_id<operation_history_object> startId,
                                                              int nLimit) throws NetworkStatusException {
        List<operation_history_object> listHistoryObject =
                mWalletApi.get_account_history(accountObjectId, startId, nLimit);
        return listHistoryObject;
    }

    public List<asset_object> list_assets(String strLowerBound, int nLimit) throws NetworkStatusException {
        return mWalletApi.list_assets(strLowerBound, nLimit);
    }

    public Map<object_id<asset_object>, asset_object> get_assets(List<object_id<asset_object>> listAssetObjectId) throws NetworkStatusException {
        Map<object_id<asset_object>, asset_object> mapId2Object = new HashMap<>();

        List<object_id<asset_object>> listRequestId = new ArrayList<>();
        for (object_id<asset_object> objectId : listAssetObjectId) {
            asset_object assetObject = mMapAssetId2Object.get(objectId);
            if (assetObject != null) {
                mapId2Object.put(objectId, assetObject);
            } else {
                listRequestId.add(objectId);
            }
        }

        if (listRequestId.isEmpty() == false) {
            List<asset_object> listAssetObject = mWalletApi.get_assets(listRequestId);
            for (asset_object assetObject : listAssetObject) {
                mapId2Object.put(assetObject.id, assetObject);
                mMapAssetId2Object.put(assetObject.id, assetObject);
            }
        }

        return mapId2Object;
    }

    public asset_object lookup_asset_symbols(String strAssetSymbol) throws NetworkStatusException {
        return mWalletApi.lookup_asset_symbols(strAssetSymbol);
    }

    public Map<object_id<account_object>, account_object> get_accounts(List<object_id<account_object>> listAccountObjectId) throws NetworkStatusException {
        Map<object_id<account_object>, account_object> mapId2Object = new HashMap<>();

        List<object_id<account_object>> listRequestId = new ArrayList<>();
        for (object_id<account_object> objectId : listAccountObjectId) {
            account_object accountObject = mMapAccountId2Object.get(objectId);
            if (accountObject != null) {
                mapId2Object.put(objectId, accountObject);
            } else {
                listRequestId.add(objectId);
            }
        }

        if (listRequestId.isEmpty() == false) {
            List<account_object> listAccountObject = mWalletApi.get_accounts(listRequestId);
            for (account_object accountObject : listAccountObject) {
                mapId2Object.put(accountObject.id, accountObject);
                mMapAccountId2Object.put(accountObject.id, accountObject);
            }
        }

        return mapId2Object;
    }

    public block_header get_block_header(int nBlockNumber) throws NetworkStatusException {
        return mWalletApi.get_block_header(nBlockNumber);
    }

    public signed_transaction transfer(String strFrom,
                         String strTo,
                         String strAmount,
                         String strAssetSymbol,
                         String strMemo) throws NetworkStatusException {
        signed_transaction signedTransaction = mWalletApi.transfer(
                strFrom,
                strTo,
                strAmount,
                strAssetSymbol,
                strMemo
        );
        return signedTransaction;
    }
    
    // 获取对于基础货币的所有市场价格
    public Map<object_id<asset_object>, bucket_object> get_market_histories_base(List<object_id<asset_object>> listAssetObjectId) throws NetworkStatusException {
        dynamic_global_property_object dynamicGlobalPropertyObject = mWalletApi.get_dynamic_global_properties();

        Date dateObject = dynamicGlobalPropertyObject.time;
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(dateObject);
        calendar.add(Calendar.HOUR, -24);

        Date dateObjectStart = calendar.getTime();

        calendar.setTime(dateObject);
        calendar.add(Calendar.HOUR, 1);

        Date dateObjectEnd = calendar.getTime();
        
        Map<object_id<asset_object>, bucket_object> mapId2BucketObject = new HashMap<>();

        object_id<asset_object> assetObjectBase = new object_id<asset_object>(0, asset_object.class);
        for (object_id<asset_object> objectId : listAssetObjectId) {
            if (objectId.equals(assetObjectBase)) {
                continue;
            }
            List<bucket_object> listBucketObject = mWalletApi.get_market_history(
                    objectId,
                    assetObjectBase,
                    3600,
                    dateObjectStart,
                    dateObjectEnd
            );

            if (listBucketObject.isEmpty() == false) {
                bucket_object bucketObject = listBucketObject.get(listBucketObject.size() - 1);
                mapId2BucketObject.put(objectId, bucketObject);
            }
        }
        
        return mapId2BucketObject;
    }

    private bucket_object get_market_history(object_id<asset_object> baseAsset, object_id<asset_object> quoteAsset) throws NetworkStatusException {
        dynamic_global_property_object dynamicGlobalPropertyObject = mWalletApi.get_dynamic_global_properties();

        Date dateObject = dynamicGlobalPropertyObject.time;
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(dateObject);
        calendar.add(Calendar.HOUR, -24);

        Date dateObjectStart = calendar.getTime();

        calendar.setTime(dateObject);
        calendar.add(Calendar.HOUR, 1);

        Date dateObjectEnd = calendar.getTime();

        Map<object_id<asset_object>, bucket_object> mapId2BucketObject = new HashMap<>();

        List<bucket_object> listBucketObject = mWalletApi.get_market_history(
                baseAsset,
                quoteAsset,
                3600,
                dateObjectStart,
                dateObjectEnd
        );

        if (listBucketObject.isEmpty() == false) {
            return listBucketObject.get(0);
        } else {
            return null;
        }
    }

    public List<bucket_object> get_market_history(object_id<asset_object> assetObjectId1,
                                                  object_id<asset_object> assetObjectId2,
                                                  int nBucket, Date dateStart,
                                                  Date dateEnd) throws NetworkStatusException {
        return mWalletApi.get_market_history(
                assetObjectId1, assetObjectId2, nBucket, dateStart, dateEnd);
    }

    public MarketTicker get_ticker(String base, String quote) throws NetworkStatusException {
        return mWalletApi.get_ticker(base, quote);
    }

    public List<MarketTrade> get_trade_history(String base, String quote, Date start, Date end, int limit)
            throws NetworkStatusException {
        return mWalletApi.get_trade_history(base, quote, start, end, limit);
    }

    public List<limit_order_object> get_limit_orders(object_id<asset_object> base,
                                                     object_id<asset_object> quote,
                                                     int limit) throws NetworkStatusException {
        return mWalletApi.get_limit_orders(base, quote, limit);
    }

    public signed_transaction sell_asset(String amountToSell, String symbolToSell,
                                         String minToReceive, String symbolToReceive,
                                         int timeoutSecs, boolean fillOrKill)
            throws NetworkStatusException {
        return mWalletApi.sell_asset(amountToSell, symbolToSell, minToReceive, symbolToReceive,
                timeoutSecs, fillOrKill);
    }

    public asset calculate_sell_fee(asset_object assetToSell, asset_object assetToReceive,
                                    double rate, double amount,
                                    global_property_object globalPropertyObject) {
        return mWalletApi.calculate_sell_fee(assetToSell, assetToReceive, rate, amount,
                globalPropertyObject);
    }

    public asset calculate_buy_fee(asset_object assetToReceive, asset_object assetToSell,
                                   double rate, double amount,
                                   global_property_object globalPropertyObject) {
        return mWalletApi.calculate_buy_fee(assetToReceive, assetToSell, rate, amount,
                globalPropertyObject);
    }

    public signed_transaction sell(String base, String quote, double rate, double amount)
            throws NetworkStatusException {
        return mWalletApi.sell(base, quote, rate, amount);
    }

    public signed_transaction sell(String base, String quote, double rate, double amount,
                                   int timeoutSecs) throws NetworkStatusException {
        return mWalletApi.sell(base, quote, rate, amount, timeoutSecs);
    }

    public signed_transaction buy(String base, String quote, double rate, double amount)
            throws NetworkStatusException {
        return mWalletApi.buy(base, quote, rate, amount);
    }

    public signed_transaction buy(String base, String quote, double rate, double amount,
                                  int timeoutSecs) throws NetworkStatusException {
        return mWalletApi.buy(base, quote, rate, amount, timeoutSecs);
    }

    /*public BitshareData getBitshareData() {
        return mBitshareData;
    }*/

    public account_object get_account_object(String strAccount) throws NetworkStatusException {
        return mWalletApi.get_account(strAccount);
    }

    public asset transfer_calculate_fee(String strAmount,
                                        String strAssetSymbol,
                                        String strMemo) throws NetworkStatusException {
        return mWalletApi.transfer_calculate_fee(strAmount, strAssetSymbol, strMemo);
    }

    public String get_plain_text_message(memo_data memoData) {
        return mWalletApi.decrypt_memo_message(memoData);
    }

    public List<full_account_object> get_full_accounts(List<String> names, boolean subscribe)
            throws NetworkStatusException {
        return mWalletApi.get_full_accounts(names, subscribe);
    }

    public signed_transaction cancel_order(object_id<limit_order_object> id)
            throws NetworkStatusException {
        return mWalletApi.cancel_order(id);
    }

    public global_property_object get_global_properties() throws NetworkStatusException {
        return mWalletApi.get_global_properties();
    }

    public int create_account_with_password(String strAccountName,
                                            String strPassword) throws CreateAccountException {
        try {
            return mWalletApi.create_account_with_password(strAccountName, strPassword);
        } catch (NetworkStatusException e) {
            e.printStackTrace();
            return ErrorCode.ERROR_NETWORK_FAIL;
        }
    }

    public int subscribe_to_market(object_id<asset_object> a, object_id<asset_object> b) {
        try {
            mWalletApi.subscribe_to_market(a, b);
        } catch (NetworkStatusException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public int set_subscribe_callback() {
        try {
            mWalletApi.set_subscribe_callback();
        } catch (NetworkStatusException e) {
            e.printStackTrace();
        }
        return 0;
    }
}
