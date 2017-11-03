package com.bitshares.bitshareswallet.livedata;

import android.arch.lifecycle.LiveData;
import android.util.Pair;

import com.bitshares.bitshareswallet.wallet.BitsharesWalletWraper;
import com.bitshares.bitshareswallet.wallet.graphene.chain.asset_object;
import com.bitshares.bitshareswallet.wallet.graphene.chain.object_id;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by lorne on 03/11/2017.
 */

public class StatusChangeLiveData extends LiveData<StatusChangeLiveData.StatusChange> {
    public static class StatusChange {
        boolean bInitialize;
        boolean bDisconnect;
        boolean bAccountChange;
    }

    private BitsharesWalletWraper.BitsharesDataObserver bitsharesDataObserver = new BitsharesWalletWraper.BitsharesDataObserver() {
        @Override
        public void onDisconnect() {
            StatusChange statusChange = new StatusChange();
            statusChange.bInitialize = false;
            statusChange.bDisconnect = true;
            statusChange.bAccountChange = false;
            postValue(statusChange);
        }

        @Override
        public void onMarketFillUpdate(object_id<asset_object> base, object_id<asset_object> quote) {
        }

        @Override
        public void onAccountChanged() {
            StatusChange statusChange = new StatusChange();
            statusChange.bInitialize = false;
            statusChange.bDisconnect = false;
            statusChange.bAccountChange = true;
            postValue(statusChange);
        }
    };

    public StatusChangeLiveData() {
        StatusChange statusChange = new StatusChange();
        statusChange.bInitialize = true;
        statusChange.bDisconnect = false;
        statusChange.bAccountChange = false;
        setValue(statusChange);
    }

    @Override
    protected void onInactive() {
        super.onInactive();
        BitsharesWalletWraper.getInstance().unregisterDataObserver(bitsharesDataObserver);
    }

    @Override
    protected void onActive() {
        super.onActive();
        BitsharesWalletWraper.getInstance().registerDataObserver(bitsharesDataObserver);
    }
}
