package com.bitshares.bitshareswallet.livedata;

import android.arch.lifecycle.LiveData;
import android.util.Pair;

import com.bitshares.bitshareswallet.wallet.BitsharesWalletWraper;
import com.bitshares.bitshareswallet.wallet.graphene.chain.asset_object;
import com.bitshares.bitshareswallet.wallet.graphene.chain.object_id;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by lorne on 03/11/2017.
 */

public class MarketChangeLiveData extends LiveData<List<Pair<object_id<asset_object>, object_id<asset_object>>>> {
    private BitsharesWalletWraper.BitsharesDataObserver bitsharesDataObserver = new BitsharesWalletWraper.BitsharesDataObserver() {
        @Override
        public void onDisconnect() {
        }

        @Override
        public void onMarketFillUpdate(object_id<asset_object> base, object_id<asset_object> quote) {
            postValue(Collections.singletonList(new Pair<>(base, quote)));
        }

        @Override
        public void onAccountChanged() {

        }
    };

    public MarketChangeLiveData() {
        setValue(new ArrayList<>());
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
