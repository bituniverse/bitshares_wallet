package com.bitshares.bitshareswallet;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.bitshares.bitshareswallet.wallet.BitshareData;
import com.bitshares.bitshareswallet.wallet.BitsharesWalletWraper;

import java.text.DecimalFormat;
import com.bitshares.bitshareswallet.market.MarketStat;
import com.bitshares.bitshareswallet.wallet.Broadcast;
import com.bitshares.bitshareswallet.wallet.asset;
import com.bitshares.bitshareswallet.wallet.graphene.chain.asset_object;
import com.bitshares.bitshareswallet.wallet.graphene.chain.global_property_object;
import com.bitshares.bitshareswallet.wallet.graphene.chain.object_id;
import com.bitshares.bitshareswallet.wallet.graphene.chain.utils;
import com.kaopiz.kprogresshud.KProgressHUD;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;


public class TransactionSellBuyFragment extends BaseFragment
        implements View.OnClickListener
        , TextWatcher
        , MarketStat.OnMarketStatUpdateListener
        , ConfirmOrderDialog.OnDialogInterationListener {

    private static final long MARKET_STAT_INTERVAL_MILLIS = TimeUnit.SECONDS.toMillis(5);
    private static final String TRANSACTION_TYPE = "transaction_type";

    private static int buyTimeSec = 5 * 365 * 24 * 60 * 60;

    private TransactionSellBuyRecyclerViewAdapter buyRecyclerViewAdapter;
    private TransactionSellBuyRecyclerViewAdapter sellRecyclerViewAdapter;
    private RecyclerView buyRecyclerView;
    private RecyclerView sellRecyclerView;
    private int maxOrderCount = 10;

    public static final int TRANSACTION_BUY = 1;
    public static final int TRANSACTION_SELL = 2;
    private int transactionType;
    private MarketStat orderStat;
    private String baseAsset;
    private String quoteAsset;
    private double lowestSellPrice = -1;
    private double higgestBuyPrice = -1;
    private double balance = 0;
    private Handler mHandler = new Handler();
    private OnFragmentInteractionListener mListener;

    private EditText qEditText;
    private EditText pEditText;
    private EditText tEditText;
    private EditText fEditText;

    private TextView pTextLastView;
    private TextView qTextLastView;
    private TextView tTextLastView;

    private TextView balanceText;
    private TextView askText;

    private TextView balanceTextBase;
    private TextView askTextBase;
    private TextView askTextInfo;

    private KProgressHUD mProcessHud;

    private asset_object baseAssetObj;
    private asset_object quoteAssetObj;
    private global_property_object globalPropertyObject;
    private boolean isAssetObjIsInit;
    private boolean isInitPriceValue;

    public TransactionSellBuyFragment() {
        orderStat = new MarketStat();
    }

    public static TransactionSellBuyFragment newInstance(int transactionType) {
        TransactionSellBuyFragment fragment = new TransactionSellBuyFragment();
        Bundle args = new Bundle();
        args.putInt(TRANSACTION_TYPE, transactionType);
        fragment.setArguments(args);
        return fragment;
    }

    private BroadcastReceiver currencyUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            orderStat.unsubscribe(baseAsset, quoteAsset);

            updateCurency();
            orderStat.subscribe(baseAsset, quoteAsset, MarketStat.STAT_MARKET_ORDER_BOOK,
                    MARKET_STAT_INTERVAL_MILLIS, TransactionSellBuyFragment.this);
        }
    };

    private void updateBalance(boolean forceRefresh){
        if(getView()==null)
            return;
        if(forceRefresh){
            new Thread(new Runnable() {
                @Override
                public void run() {
                    int nRet = BitsharesWalletWraper.getInstance().build_connect();
                    if (nRet == 0) {
                        BitsharesWalletWraper.getInstance().prepare_data_to_display(true);
                    }
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            updateBalance(false);
                        }
                    });
                }
            }).start();
            return;
        }

        BitshareData bitshareData = BitsharesWalletWraper.getInstance().getBitshareData();
        if(bitshareData==null)
            return;

        List<asset> assetList = bitshareData.listBalances;
        if(assetList!=null){
            balance = 0;
            Map<object_id<asset_object>, asset_object>  mapId2AssetObject = bitshareData.mapId2AssetObject;
            for(asset as: assetList){
                asset_object assetObject = mapId2AssetObject.get(as.asset_id);
                asset_object.asset_object_legible assetObjectLegible = assetObject.get_legible_asset_object(as.amount);
                double fResult = (double)assetObjectLegible.lDecimal / assetObjectLegible.scaled_precision + assetObjectLegible.lCount;

                if(isBuy()){
                    if(assetObjectLegible.symbol.equals(baseAsset)){
                        balance = fResult;
                        break;
                    }
                } else {
                    if(assetObjectLegible.symbol.equals(quoteAsset)){
                        balance = fResult;
                        break;
                    }
                }
            }
            onBalanceUpdated();
        }
    }

    private void onBalanceUpdated() {
        DecimalFormat decimalFormat = new DecimalFormat("#.####");
        balanceText.setText(decimalFormat.format(balance));
    }

    @Override
    public void onMarketStatUpdate(MarketStat.Stat stat) {
        if(getView()==null)
            return;
        if(stat.orderBook!=null){
            buyRecyclerViewAdapter.setList(stat.orderBook.bids.subList(0,maxOrderCount));
            sellRecyclerViewAdapter.setList(stat.orderBook.asks.subList(0,maxOrderCount));

            if(stat.orderBook.bids.size()>0){
                higgestBuyPrice = stat.orderBook.bids.get(0).price;
            } else {
                higgestBuyPrice = -1;
            }

            if(stat.orderBook.asks.size()>0){
                lowestSellPrice = stat.orderBook.asks.get(0).price;
            } else {
                lowestSellPrice = -1;
            }

            initPriceValue();
        }
    }

    public void initPriceValue() {
        DecimalFormat decimalFormat = new DecimalFormat("#.####");
        if (!isInitPriceValue) {
            isInitPriceValue = true;
            if (transactionType == TRANSACTION_BUY && lowestSellPrice != -1) {
                pEditText.setText(decimalFormat.format(lowestSellPrice));
            } else if (transactionType == TRANSACTION_SELL && higgestBuyPrice != -1) {
                pEditText.setText(decimalFormat.format(higgestBuyPrice));
            }
        }

        if (transactionType == TRANSACTION_BUY && lowestSellPrice != -1) {
            askText.setText(decimalFormat.format(lowestSellPrice));
        } else if (transactionType == TRANSACTION_SELL && higgestBuyPrice != -1) {
            askText.setText(decimalFormat.format(higgestBuyPrice));
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            transactionType = getArguments().getInt(TRANSACTION_TYPE);
        }
    }

    @Override
    public void onShow() {
        super.onShow();
        updateCurency();
        initFee();
        updateMaxOrderCount();
        orderStat.subscribe(baseAsset, quoteAsset, MarketStat.STAT_MARKET_ORDER_BOOK,
                MARKET_STAT_INTERVAL_MILLIS, this);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Broadcast.CURRENCY_UPDATED);
        LocalBroadcastManager.getInstance(getContext())
                .registerReceiver(currencyUpdateReceiver, intentFilter);
    }

    @Override
    public void onHide() {
        super.onHide();
        orderStat.unsubscribe(baseAsset, quoteAsset);
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(currencyUpdateReceiver);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_buy_sell, container, false);

        Button okButton = (Button) view.findViewById(R.id.okButton);
        okButton.setOnClickListener(this);
        if (isBuy()) {
            okButton.setText(getResources().getString(R.string.title_buy));
        } else {
            okButton.setText(getResources().getString(R.string.title_sell));
        }

        mProcessHud = KProgressHUD.create(getActivity())
                .setStyle(KProgressHUD.Style.SPIN_INDETERMINATE)
                .setLabel("Please Wait")
                .setCancellable(false)
                .setAnimationSpeed(2)
                .setDimAmount(0.5f);

        Button restButton = (Button) view.findViewById(R.id.restButton);
        restButton.setOnClickListener(this);

        qEditText = (EditText) view.findViewById(R.id.qEditText);
        pEditText = (EditText) view.findViewById(R.id.pEditText);
        pEditText.addTextChangedListener(this);
        qEditText.addTextChangedListener(this);

        tEditText = (EditText) view.findViewById(R.id.tEditText);
        fEditText = (EditText) view.findViewById(R.id.fEditText);
        pTextLastView = (TextView) view.findViewById(R.id.pTextLastView);
        qTextLastView = (TextView) view.findViewById(R.id.qTextLastView);
        tTextLastView = (TextView) view.findViewById(R.id.tTextLastView);

        balanceText = (TextView) view.findViewById(R.id.balanceText);
        askText = (TextView) view.findViewById(R.id.askText);

        balanceTextBase = (TextView) view.findViewById(R.id.balanceTextBase);
        askTextBase = (TextView) view.findViewById(R.id.askTextBase);
        askTextInfo = (TextView) view.findViewById(R.id.askTextInfo);

        buyRecyclerView = (RecyclerView) view.findViewById(R.id.buy_recycler);
        buyRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        buyRecyclerViewAdapter = new TransactionSellBuyRecyclerViewAdapter();
        buyRecyclerView.setAdapter(buyRecyclerViewAdapter);
        buyRecyclerView.setItemAnimator(null);

        sellRecyclerView = (RecyclerView) view.findViewById(R.id.sell_recycler);
        sellRecyclerViewAdapter = new TransactionSellBuyRecyclerViewAdapter();
        sellRecyclerView.setAdapter(sellRecyclerViewAdapter);
        sellRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        sellRecyclerView.setItemAnimator(null);

        return view;
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
//            throw new RuntimeException(context.toString()
//                    + " must implement OnFragmentInteractionListener");
        }


    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.okButton:
                ConfirmOrderDialog.ConfirmOrderData confirmOrderData = new ConfirmOrderDialog.ConfirmOrderData();
                if (transactionType == TRANSACTION_BUY) {
                    confirmOrderData.setOperationName(getResources().getString(R.string.title_buy));
                } else {
                    confirmOrderData.setOperationName(getResources().getString(R.string.title_sell));
                }
                confirmOrderData.setPrice(pEditText.getText().toString());
                confirmOrderData.setQuantity(qEditText.getText().toString());
                confirmOrderData.setTotal(tEditText.getText().toString());
                confirmOrderData.setFree(fEditText.getText().toString());
                confirmOrderData.setQuantityType(quoteAsset);
                confirmOrderData.setTotalType(baseAsset);

                Calendar calendar = Calendar.getInstance();
                calendar.set(Calendar.YEAR, calendar.get(Calendar.YEAR) + 5);
                calendar.getTime();
                SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
                confirmOrderData.setTimeExpiration(formatter.format(calendar.getTimeInMillis()));
                ConfirmOrderDialog confirmOrderDialog = new ConfirmOrderDialog(getActivity(), confirmOrderData);
                confirmOrderDialog.setListener(this);
                confirmOrderDialog.show();
                break;
            case R.id.restButton:
                resetInputInfo();
                break;
        }
    }

    public void resetInputInfo() {
        qEditText.setText("");
        pEditText.setText("");
        tEditText.setText("");
        fEditText.setText("");

        isInitPriceValue = false;
        initPriceValue();
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

    }

    @Override
    public void afterTextChanged(Editable s) {
        String qString = qEditText.getText().toString();
        String pString = pEditText.getText().toString();
        if (qString.equals("") || pString.equals("")) {
            tEditText.setText("");
            return;
        }

        double qValue;
        double pValue;

        try {
            qValue = Double.parseDouble(qString);
            pValue = Double.parseDouble(pString);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        DecimalFormat decimalFormat = new DecimalFormat("#.########");
        tEditText.setText(decimalFormat.format(qValue * pValue));

        if (isBuy()) {
            double fee = calculateBuyFee(quoteAssetObj, baseAssetObj, pValue, qValue);
            fEditText.setText(decimalFormat.format(fee));
        } else {
            double fee = calculateSellFee(quoteAssetObj, baseAssetObj, pValue, qValue);
            fEditText.setText(decimalFormat.format(fee));
        }
    }

    @Override
    public void onConfirm() {
        String qString = qEditText.getText().toString();
        String pString = pEditText.getText().toString();
        if (qString.equals("") || pString.equals("")) {
            return;
        }

        final double qValue;
        final double pValue;
        try {
            qValue = Double.parseDouble(qString);
            pValue = Double.parseDouble(pString);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        if (qValue <= 0 || pValue <= 0) {
            return;
        }

        if (!BitsharesWalletWraper.getInstance().is_locked()) {
            if (transactionType == TRANSACTION_BUY) {
                buy(pValue, qValue, buyTimeSec);
            } else if (transactionType == TRANSACTION_SELL) {
                sell(pValue, qValue, buyTimeSec);
            }
        } else {
            TransactionSellBuyPasswordDialog builder = new TransactionSellBuyPasswordDialog(getActivity());
            builder.setListener(new TransactionSellBuyPasswordDialog.OnDialogInterationListener() {
                @Override
                public void onConfirm(AlertDialog dialog, String passwordString) {
                    if (BitsharesWalletWraper.getInstance().unlock(passwordString) == 0) {
                        dialog.dismiss();
                        if (transactionType == TRANSACTION_BUY) {
                            buy(pValue, qValue, buyTimeSec);
                        } else if (transactionType == TRANSACTION_SELL) {
                            sell(pValue, qValue, buyTimeSec);
                        }
                    } else {
                        Toast.makeText(getContext(), getContext().getString(R.string.password_invalid), Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onReject(AlertDialog dialog) {
                    dialog.dismiss();
                }
            });
            builder.show();
        }
    }

    @Override
    public void onReject() {

    }

    private void initFee() {
        isAssetObjIsInit = false;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    baseAssetObj = BitsharesWalletWraper.getInstance().lookup_asset_symbols(baseAsset);
                    quoteAssetObj = BitsharesWalletWraper.getInstance().lookup_asset_symbols(quoteAsset);
                    globalPropertyObject = BitsharesWalletWraper.getInstance().get_global_properties();
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            isAssetObjIsInit = true;
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void buy(final double rate, final double amount, final int timeSec) {
        mProcessHud.show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    BitsharesWalletWraper.getInstance().buy(quoteAsset, baseAsset, rate, amount, timeSec);
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            updateBalance(true);
                            mProcessHud.dismiss();
                        }
                    });
                } catch (Exception e) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mProcessHud.dismiss();
                        }
                    });
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void sell(final double rate, final double amount, final int timeSec) {
        mProcessHud.show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    BitsharesWalletWraper.getInstance().sell(quoteAsset, baseAsset, rate, amount, timeSec);
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mProcessHud.dismiss();
                        }
                    });
                } catch (Exception e) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mProcessHud.dismiss();
                        }
                    });
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void updateMaxOrderCount(){
        int orderItemHeightDp = DensityUtil.dpToPx(getContext(), TransactionSellBuyRecyclerViewAdapter.ORDERITEMHEIGHTDP);
        int buyRecyclerHeight = buyRecyclerView.getHeight();

        maxOrderCount = buyRecyclerHeight / orderItemHeightDp;
        if (maxOrderCount <= 0) {
            maxOrderCount = 8;
        }
    }

    private void updateCurency(){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
//        if(isBuy()){
            quoteAsset = "BTS";
            baseAsset = prefs.getString("currency_setting", "USD");

        pTextLastView.setText(baseAsset + "/" + quoteAsset);
        qTextLastView.setText(quoteAsset);
        tTextLastView.setText(baseAsset);
        if (isBuy()) {
            balanceTextBase.setText(baseAsset);
            askTextBase.setText(baseAsset);
            askTextInfo.setText(getString(R.string.label_lowest_ask));
        } else {
            balanceTextBase.setText(quoteAsset);
            askTextBase.setText(quoteAsset);
            askTextInfo.setText(getString(R.string.label_highest_bid));
        }
//        } else {
//            baseAsset = "BTS";
//            quoteAsset = prefs.getString("currency_setting", "USD");
//        }
        updateBalance(false);
    }

    private boolean isBuy(){
        return transactionType == TRANSACTION_BUY;
    }

    private double calculateSellFee(asset_object symbolToSell, asset_object symbolToReceive, double rate,
                                    double amount) {
        if (!isAssetObjIsInit) {
            return 0;
        }
        asset a = BitsharesWalletWraper.getInstance().calculate_sell_fee(symbolToSell, symbolToReceive,
                rate, amount, globalPropertyObject);
        if (a.asset_id.equals(baseAssetObj.id)) {
            return utils.get_asset_amount(a.amount, baseAssetObj);
        } else {
            return utils.get_asset_amount(a.amount, quoteAssetObj);
        }
    }

    private double calculateBuyFee(asset_object symbolToReceive, asset_object symbolToSell, double rate,
                                   double amount) {
        if (!isAssetObjIsInit) {
            return 0;
        }
        asset a = BitsharesWalletWraper.getInstance().calculate_buy_fee(symbolToReceive, symbolToSell,
                rate, amount, globalPropertyObject);
        if (a.asset_id.equals(baseAssetObj.id)) {
            return utils.get_asset_amount(a.amount, baseAssetObj);
        } else {
            return utils.get_asset_amount(a.amount, quoteAssetObj);
        }
    }
}
