package com.bitshares.bitshareswallet.repository;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MediatorLiveData;
import android.text.TextUtils;
import android.util.Pair;

import com.bitshares.bitshareswallet.BitsharesApplication;
import com.bitshares.bitshareswallet.room.BitsharesAccountObject;
import com.bitshares.bitshareswallet.room.BitsharesAssetObject;
import com.bitshares.bitshareswallet.room.BitsharesBalanceAsset;
import com.bitshares.bitshareswallet.room.BitsharesDao;
import com.bitshares.bitshareswallet.room.BitsharesOperationHistory;
import com.bitshares.bitshareswallet.wallet.BitsharesWalletWraper;
import com.bitshares.bitshareswallet.wallet.account_object;
import com.bitshares.bitshareswallet.wallet.exception.NetworkStatusException;
import com.bitshares.bitshareswallet.wallet.graphene.chain.asset_object;
import com.bitshares.bitshareswallet.wallet.graphene.chain.block_header;
import com.bitshares.bitshareswallet.wallet.graphene.chain.object_id;
import com.bitshares.bitshareswallet.wallet.graphene.chain.operation_history_object;
import com.bitshares.bitshareswallet.wallet.graphene.chain.operations;
import com.bituniverse.network.Resource;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import io.reactivex.Flowable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by lorne on 31/10/2017.
 */

public class OperationHistoryRepository {
    private BitsharesDao bitsharesDao;
    private MediatorLiveData<Resource<List<BitsharesOperationHistory>>> result = new MediatorLiveData<>();

    public OperationHistoryRepository() {
        bitsharesDao = BitsharesApplication.getInstance().getBitsharesDatabase().getBitsharesDao();
        LiveData<List<BitsharesOperationHistory>> operationHistoryLiveData = bitsharesDao.queryOperationHistory();
        result.addSource(
                operationHistoryLiveData,
                data -> {
                    result.removeSource(operationHistoryLiveData);
                    if (shouldFetch(data)) {
                        fetchFromNetwork(operationHistoryLiveData);
                    } else {
                        result.addSource(operationHistoryLiveData, newData -> result.setValue(Resource.success(newData)));
                    }
                });

    }

    private boolean shouldFetch(List<BitsharesOperationHistory> bitsharesOperationHistoryList) {
        return true;
    }

    private void fetchFromNetwork(final LiveData<List<BitsharesOperationHistory>> dbSource) {
        result.addSource(dbSource, newData -> result.setValue(Resource.loading(newData)));
        // 开始异步获取数据
        Flowable.just(0)
                .subscribeOn(Schedulers.io())
                .map(integer -> {
                    fetchOperationHistory(dbSource);
                    return 0;
                }).observeOn(AndroidSchedulers.mainThread())
                .subscribe(retCode -> {
                    result.removeSource(dbSource);
                    LiveData<List<BitsharesOperationHistory>> operationHistoryLiveData = bitsharesDao.queryOperationHistory();
                    result.addSource(operationHistoryLiveData, newData -> result.setValue(Resource.success(newData)));
                }, throwable -> {
                    throwable.printStackTrace();
                    result.removeSource(dbSource);
                    result.addSource(dbSource, newData -> result.setValue(Resource.error(throwable.getMessage(), newData)));
                });
    }

    public LiveData<Resource<List<BitsharesOperationHistory>>> getOperationHistory() {
        return result;
    }

    private void fetchOperationHistory(LiveData<List<BitsharesOperationHistory>> dbSource) throws NetworkStatusException {
        account_object accountObject = BitsharesWalletWraper.getInstance().get_account();
        object_id<operation_history_object> startId;
        if (dbSource != null && dbSource.getValue() != null && dbSource.getValue().isEmpty() == false) {
            startId = object_id.create_from_string(dbSource.getValue().get(0).operationHistoryObject.id);
        } else {
            String strId = bitsharesDao.queryOperationHistoryLatestId();
            if (!TextUtils.isEmpty(strId)) {
                startId = object_id.create_from_string(strId);
            } else {
                startId = new object_id<>(0, operation_history_object.class);
            }
        }

        List<operation_history_object> operationHistoryObjectList =
                BitsharesWalletWraper.getInstance().get_account_history(
                        accountObject.id,
                        startId,
                        100
                );

        // 去掉我们不展现的operation
        List<operation_history_object> operationHistoryObjectFilter = new ArrayList<>();
        for (operation_history_object object : operationHistoryObjectList) {
            if (operations.operations_map.getOperationFeeObjectById(object.op.nOperationType) != null) {
                operationHistoryObjectFilter.add(object);
            }
        }
        operationHistoryObjectList = operationHistoryObjectFilter;

        List<BitsharesOperationHistory> bitsharesOperationHistoryList = new ArrayList<>();

        HashSet<object_id<account_object>> accountObjectIdSet = new HashSet<object_id<account_object>>();
        HashSet<object_id<asset_object>> assetObjectIdSet = new HashSet<object_id<asset_object>>();
        for (operation_history_object historyObject : operationHistoryObjectList) {
            block_header blockHeader = BitsharesWalletWraper.getInstance().get_block_header(historyObject.block_num);
            BitsharesOperationHistory bitsharesOperationHistory = new BitsharesOperationHistory();
            bitsharesOperationHistory.id = 0;
            bitsharesOperationHistory.timestamp = blockHeader.timestamp.getTime();
            bitsharesOperationHistory.operationHistoryObject = historyObject;
            bitsharesOperationHistoryList.add(bitsharesOperationHistory);

            if (historyObject.op.nOperationType <= operations.ID_CREATE_ACCOUNT_OPERATION) {
                operations.base_operation operation = (operations.base_operation) historyObject.op.operationContent;
                accountObjectIdSet.addAll(operation.get_account_id_list());
                assetObjectIdSet.addAll(operation.get_asset_id_list());
            }
        }

        List<object_id<asset_object>> assetObjectIdList = new ArrayList<>();
        assetObjectIdList.addAll(assetObjectIdSet);
        Map<object_id<asset_object>, asset_object> mapId2AssetObject =
                BitsharesWalletWraper.getInstance().get_assets(assetObjectIdList);
        List<BitsharesAssetObject> bitsharesAssetObjectList = new ArrayList<>();
        for (asset_object assetObject : mapId2AssetObject.values()) {
            BitsharesAssetObject bitsharesAssetObject = new BitsharesAssetObject();
            bitsharesAssetObject.asset_id = assetObject.id;
            bitsharesAssetObject.precision = assetObject.get_scaled_precision();
            bitsharesAssetObject.symbol = assetObject.symbol;
            bitsharesAssetObjectList.add(bitsharesAssetObject);
        }
        bitsharesDao.insertAssetObject(bitsharesAssetObjectList);

        List<object_id<account_object>> accountObjectIdList = new ArrayList<>();
        accountObjectIdList.addAll(accountObjectIdSet);

        Map<object_id<account_object>, account_object> mapId2AccountObject =
                BitsharesWalletWraper.getInstance().get_accounts(accountObjectIdList);
        List<BitsharesAccountObject> bitsharesAccountObjectList = new ArrayList<>();
        for (account_object object : mapId2AccountObject.values()) {
            BitsharesAccountObject bitsharesAccountObject = new BitsharesAccountObject();
            bitsharesAccountObject.id = 0;
            bitsharesAccountObject.account_id = object.id;
            bitsharesAccountObject.name = object.name;
            bitsharesAccountObjectList.add(bitsharesAccountObject);
        }
        bitsharesDao.insertAccountObject(bitsharesAccountObjectList);
        bitsharesDao.insertHistoryObject(bitsharesOperationHistoryList);
    }
}
