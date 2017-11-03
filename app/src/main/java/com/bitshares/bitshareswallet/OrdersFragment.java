package com.bitshares.bitshareswallet;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.bitshares.bitshareswallet.market.MarketStat;
import com.bitshares.bitshareswallet.market.OpenOrder;
import com.bitshares.bitshareswallet.wallet.BitsharesWalletWraper;
import com.bitshares.bitshareswallet.wallet.Broadcast;
import com.bitshares.bitshareswallet.wallet.exception.NetworkStatusException;
import com.bitshares.bitshareswallet.wallet.graphene.chain.utils;
import com.kaopiz.kprogresshud.KProgressHUD;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;


public class OrdersFragment extends BaseFragment
        implements MarketStat.OnMarketStatUpdateListener {
    private static final long MARKET_STAT_INTERVAL_MILLIS = TimeUnit.SECONDS.toMillis(10);

    private RecyclerView listOrders;
    private OrderListAdapter adapterOrders;
    private TextView txtNone;

    private MarketStat marketStat;
    private String baseAsset;
    private String quoteAsset;
    private KProgressHUD mProcessHud;
    private Handler handler = new Handler();

    private BroadcastReceiver currencyUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            marketStat.unsubscribe(baseAsset, quoteAsset);
            updateCurrency();
            marketStat.subscribe(baseAsset, quoteAsset, MarketStat.STAT_MARKET_OPEN_ORDER,
                    MARKET_STAT_INTERVAL_MILLIS, OrdersFragment.this);
        }
    };

    private void refresh(){
        marketStat.updateImmediately(baseAsset, quoteAsset);
    }

    public OrdersFragment() {
        marketStat = new MarketStat();
    }

    public static OrdersFragment newInstance() {
        OrdersFragment fragment = new OrdersFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_orders, container, false);

        adapterOrders = new OrderListAdapter();

        listOrders = (RecyclerView) view.findViewById(R.id.fo_list);
        listOrders.setAdapter(adapterOrders);
        listOrders.setLayoutManager(new LinearLayoutManager(getContext()));
        listOrders.setItemAnimator(null);

        txtNone = (TextView)view.findViewById(R.id.fo_txt_none);

        mProcessHud = KProgressHUD.create(getActivity())
                .setStyle(KProgressHUD.Style.SPIN_INDETERMINATE)
                .setLabel("Please Wait")
                .setCancellable(false)
                .setAnimationSpeed(2)
                .setDimAmount(0.5f);

        return view;
    }

    @Override
    public void onShow() {
        super.onShow();
        updateCurrency();
        marketStat.subscribe(baseAsset, quoteAsset, MarketStat.STAT_MARKET_OPEN_ORDER,
                MARKET_STAT_INTERVAL_MILLIS, this);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Broadcast.CURRENCY_UPDATED);
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(currencyUpdateReceiver, intentFilter);
    }

    @Override
    public void onHide() {
        super.onHide();
        marketStat.unsubscribe(baseAsset, quoteAsset);
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(currencyUpdateReceiver);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onMarketStatUpdate(MarketStat.Stat stat) {
        if (stat.openOrders != null) {
            List<OpenOrder> orderList = stat.openOrders;
            if(orderList.size()==0){
                txtNone.setVisibility(View.VISIBLE);
            } else {
                txtNone.setVisibility(View.GONE);
            }
            adapterOrders.setListOrders(orderList);
        }
    }

    private void updateCurrency() {
        Context ctx = getContext();
        if (ctx == null) {
            return;
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        String strAssetPair = prefs.getString("quotation_currency_pair", "BTS:USD");
        String strAsset[] = strAssetPair.split(":");
        baseAsset = strAsset[1];
        quoteAsset = strAsset[0];
    }

    class OrderListAdapter extends RecyclerView.Adapter<OrderViewHolder>{
        List<OpenOrder> listOrders;
        @Override
        public OrderViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(
                    parent.getContext()).inflate(R.layout.view_my_order_item, parent, false);
            return new OrderViewHolder(view);
        }

        public void setListOrders(List<OpenOrder> list){
            listOrders = list;
            notifyDataSetChanged();
        }

        @Override
        public void onBindViewHolder(OrderViewHolder holder, int position) {
            holder.update(listOrders.get(position));
        }

        @Override
        public int getItemCount() {
            return listOrders!=null ? listOrders.size():0;
        }
    }

    class OrderViewHolder extends RecyclerView.ViewHolder{
        TextView txtExpiration;
        TextView txtCacel;
        TextView txtOperation;
        TextView txtPrice;
        TextView txtSrcCoin;
        TextView txtSrcCoinName;
        TextView txtTargetCoin;
        TextView txtTargetCoinName;

        OpenOrder order;

        public OrderViewHolder(View itemView) {
            super(itemView);

            txtExpiration = (TextView)itemView.findViewById(R.id.voi_txt_expiration);
            txtCacel = (TextView)itemView.findViewById(R.id.voi_txt_cacel);
            txtOperation = (TextView)itemView.findViewById(R.id.voi_txt_operation);
            txtPrice = (TextView)itemView.findViewById(R.id.voi_txt_price);
            txtSrcCoin = (TextView)itemView.findViewById(R.id.voi_txt_src_coin);
            txtSrcCoinName = (TextView)itemView.findViewById(R.id.voi_txt_src_coin_name);
            txtTargetCoin = (TextView)itemView.findViewById(R.id.voi_txt_target_coin);
            txtTargetCoinName = (TextView)itemView.findViewById(R.id.voi_txt_target_coin_name);

            txtCacel.setOnClickListener(onCancelClickListener);
        }

        public void update(OpenOrder order){
            this.order = order;
            if (order.limitOrder.sell_price.quote.asset_id.equals(order.quote.id)) {
                txtOperation.setText(R.string.label_buy);
                double buyAmount = utils.get_asset_amount(order.limitOrder.sell_price.quote.amount, order.quote);
                txtTargetCoin.setText(String.format("%.6f",buyAmount));

                double sellAmount = utils.get_asset_amount(order.limitOrder.sell_price.base.amount, order.base);
                txtSrcCoin.setText(String.format("%.6f",sellAmount));
            } else {
                txtOperation.setText(R.string.label_sell);
                double buyAmount = utils.get_asset_amount(order.limitOrder.sell_price.quote.amount, order.base);
                txtSrcCoin.setText(String.format("%.6f",buyAmount));

                double sellAmount = utils.get_asset_amount(order.limitOrder.sell_price.base.amount, order.quote);
                txtTargetCoin.setText(String.format("%.6f",sellAmount));
            }

            txtPrice.setText(String.format(Locale.ENGLISH,
                    "%.6f %s/%s",
                    order.price,
                    utils.getAssetSymbolDisply(order.base.symbol),
                    utils.getAssetSymbolDisply(order.quote.symbol)
            ));
            //txtSrcCoinName.setText(order.base.symbol);
            //txtTargetCoinName.setText(order.quote.symbol);

            SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
            txtExpiration.setText(formatter.format(order.limitOrder.expiration));
        }

        private View.OnClickListener onCancelClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (BitsharesWalletWraper.getInstance().is_locked()) {
                    showPasswordDialog();
                } else {
                    showConfirmDialog();
                }

            }
        };

        private void showConfirmDialog(){
            CancelOrderDialog dialog = new CancelOrderDialog(getActivity(), order);
            dialog.setListener(new CancelOrderDialog.OnDialogInterationListener() {
                @Override
                public void onConfirm() {
                    new AsyncTask<Integer, Integer, Boolean>(){
                        @Override
                        protected void onPreExecute() {
                            super.onPreExecute();
                            Log.i("OrderFragment","1111111111");
                            mProcessHud.show();
                            Log.i("OrderFragment","2222222222");
                        }

                        @Override
                        protected void onPostExecute(Boolean success) {
                            super.onPostExecute(success);
                            mProcessHud.dismiss();
                            if(success){
                                refresh();
                            } else {
                                Toast.makeText(getContext(), getContext().getString(R.string.import_activity_connect_failed), Toast.LENGTH_LONG).show();
                            }
                        }

                        @Override
                        protected Boolean doInBackground(Integer... params) {
                            try {
                                BitsharesWalletWraper.getInstance().cancel_order(order.limitOrder.id);
                                return true;
                            } catch (NetworkStatusException e){
                               return false;
                            }
                        }
                    }.execute();
                }

                @Override
                public void onReject() {

                }
            });
            dialog.show();
        }

        private void showPasswordDialog(){
            TransactionSellBuyPasswordDialog builder = new TransactionSellBuyPasswordDialog(getActivity());
            builder.setListener(new TransactionSellBuyPasswordDialog.OnDialogInterationListener() {
                @Override
                public void onConfirm(AlertDialog dialog, String passwordString) {
                    if (BitsharesWalletWraper.getInstance().unlock(passwordString) == 0) {
                        dialog.dismiss();
                        showConfirmDialog();
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
}
