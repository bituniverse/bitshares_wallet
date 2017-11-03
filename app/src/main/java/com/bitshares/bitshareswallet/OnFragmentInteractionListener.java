package com.bitshares.bitshareswallet;

import android.net.Uri;

import com.bitshares.bitshareswallet.wallet.graphene.chain.signed_transaction;

public interface OnFragmentInteractionListener {
    void onFragmentInteraction(Uri uri);
    void notifyTransferComplete(signed_transaction signedTransaction);
    void notifyCurrencyPairChange();
}
