package com.bitshares.bitshareswallet.viewmodel;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MediatorLiveData;
import android.arch.lifecycle.Transformations;
import android.arch.lifecycle.ViewModel;

import com.bitshares.bitshareswallet.BitsharesApplication;
import com.bitshares.bitshareswallet.livedata.StatusChangeLiveData;
import com.bitshares.bitshareswallet.repository.OperationHistoryRepository;
import com.bitshares.bitshareswallet.room.BitsharesAccountObject;
import com.bitshares.bitshareswallet.room.BitsharesAssetObject;
import com.bitshares.bitshareswallet.room.BitsharesDao;
import com.bitshares.bitshareswallet.room.BitsharesOperationHistory;
import com.bitshares.bitshareswallet.wallet.account_object;
import com.bitshares.bitshareswallet.wallet.graphene.chain.asset_object;
import com.bitshares.bitshareswallet.wallet.graphene.chain.object_id;
import com.bituniverse.network.Resource;
import com.bituniverse.network.Status;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by lorne on 01/11/2017.
 */

public class TransactionViewModel extends ViewModel {
    public class OperationHistoryWrapper {
        public List<BitsharesOperationHistory> bitsharesOperationHistoryList;
        public Map<object_id<account_object>, BitsharesAccountObject> mapId2AccountObject;
        public Map<object_id<asset_object>, BitsharesAssetObject> mapId2AssetObject;
    };

    private BitsharesDao bitsharesDao;
    private StatusChangeLiveData statusChangeLiveData = new StatusChangeLiveData();
    private MediatorLiveData<Resource<OperationHistoryWrapper>> result = new MediatorLiveData<>();
    private List<BitsharesOperationHistory> bitsharesOperationHistoryList;
    private List<BitsharesAccountObject> bitsharesAccountObjectList;
    private List<BitsharesAssetObject> bitsharesAssetObjectList;

    public TransactionViewModel() {
        bitsharesDao = BitsharesApplication.getInstance().getBitsharesDatabase().getBitsharesDao();
    }

    public LiveData<Resource<OperationHistoryWrapper>> getOperationHistory() {
        LiveData<Resource<List<BitsharesOperationHistory>>> resourceLiveDataHistoryList = Transformations.switchMap(
                statusChangeLiveData, statusChange -> new OperationHistoryRepository().getOperationHistory()
        );
        result.addSource(resourceLiveDataHistoryList, resourceList -> {
            if (resourceList.status == Status.ERROR) {
                result.setValue(Resource.error(resourceList.message, null));
            } else if (resourceList.status == Status.LOADING) {
                if (resourceList.data != null && !resourceList.data.isEmpty()) {
                    setHistoryValue(resourceList.data, true);
                    LiveData<List<BitsharesAccountObject>> accountObjectListLiveData = bitsharesDao.queryAccountObject();
                    result.addSource(accountObjectListLiveData, accountList -> {
                        setAccountValue(accountList, true);
                        result.removeSource(accountObjectListLiveData);
                    });
                    LiveData<List<BitsharesAssetObject>> assetObjectListLiveData = bitsharesDao.queryAssetObjectData();
                    result.addSource(assetObjectListLiveData, assetList -> {
                        setAssetValue(assetList, true);
                        result.removeSource(assetObjectListLiveData);
                    });
                } else {
                    result.setValue(Resource.loading(null));
                }
            } else {
                // 继续加载另外两个部分
                setHistoryValue(resourceList.data, false);

                LiveData<List<BitsharesAccountObject>> accountObjectListLiveData = bitsharesDao.queryAccountObject();
                result.addSource(accountObjectListLiveData, accountList -> {
                    setAccountValue(accountList, false);
                });
                LiveData<List<BitsharesAssetObject>> assetObjectListLiveData = bitsharesDao.queryAssetObjectData();
                result.addSource(assetObjectListLiveData, assetList -> {
                    setAssetValue(assetList, false);
                });
            }
        });

        return result;
    }

    private void setHistoryValue(List<BitsharesOperationHistory> bitsharesOperationHistoryList, boolean loading) {
        this.bitsharesOperationHistoryList = bitsharesOperationHistoryList;
        processReturnData(loading);
    }

    private void setAccountValue(List<BitsharesAccountObject> bitsharesAccountObjectList, boolean loading) {
        this.bitsharesAccountObjectList = bitsharesAccountObjectList;
        processReturnData(loading);
    }

    private void setAssetValue(List<BitsharesAssetObject> bitsharesAssetObjectList, boolean loading) {
        this.bitsharesAssetObjectList = bitsharesAssetObjectList;
        processReturnData(loading);
    }

    private void processReturnData(boolean loading) {
        if (bitsharesAssetObjectList != null &&
                bitsharesAccountObjectList != null &&
                bitsharesOperationHistoryList != null) {
            OperationHistoryWrapper operationHistoryWrapper = new OperationHistoryWrapper();
            operationHistoryWrapper.bitsharesOperationHistoryList = bitsharesOperationHistoryList;
            operationHistoryWrapper.mapId2AccountObject = new HashMap<>();
            for (BitsharesAccountObject accountObject : bitsharesAccountObjectList) {
                operationHistoryWrapper.mapId2AccountObject.put(accountObject.account_id, accountObject);
            }
            operationHistoryWrapper.mapId2AssetObject = new HashMap<>();
            for (BitsharesAssetObject assetObject : bitsharesAssetObjectList) {
                operationHistoryWrapper.mapId2AssetObject.put(assetObject.asset_id, assetObject);
            }
            if (loading) {
                result.setValue(Resource.loading(operationHistoryWrapper));
            } else {
                result.setValue(Resource.success(operationHistoryWrapper));
            }
        }
    }
}
