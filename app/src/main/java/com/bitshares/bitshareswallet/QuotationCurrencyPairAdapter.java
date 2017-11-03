package com.bitshares.bitshareswallet;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bitshares.bitshareswallet.market.MarketTicker;
import com.bitshares.bitshareswallet.room.BitsharesMarketTicker;
import com.bitshares.bitshareswallet.wallet.graphene.chain.utils;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Created by lorne on 22/09/2017.
 */

public class QuotationCurrencyPairAdapter extends RecyclerView.Adapter<QuotationCurrencyPairAdapter.ViewHolder> {
    public interface OnItemClickListner {
        void onItemClick(View view, int position);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private View mView;
        private View mViewSelected;
        private ImageView mCurrencyIconView;
        private TextView mViewCurrencyPair;
        private TextView mViewPrice;
        private TextView mView24h;

        public ViewHolder(View itemView) {
            super(itemView);
            mView = itemView;
            mViewSelected = mView.findViewById(R.id.viewSelected);
            mCurrencyIconView = (ImageView) mView.findViewById(R.id.imageViewCurrency);
            mViewCurrencyPair = (TextView) mView.findViewById(R.id.textViewCurrencyPair);
            mViewPrice = (TextView) mView.findViewById(R.id.textViewPrice);
            mView24h = (TextView) mView.findViewById(R.id.textView24h);
        }
    }

    private String[] marrOptions;
    private String[] marrValues;
    private Context mContext;
    private OnItemClickListner monItemClickListner;
    private Map<String, Integer> mapSymbol2Id = new HashMap<>();
    private List<BitsharesMarketTicker> bitsharesMarketTickerList;
    private Set<String> currecnyPairSet = new HashSet<>();
    private int selected = 0;

    public QuotationCurrencyPairAdapter(Context context) {
        mContext = context;
        marrOptions = context.getResources().getStringArray(R.array.quotation_currency_pair_options);
        marrValues = context.getResources().getStringArray(R.array.quotation_currency_pair_values);
        currecnyPairSet.addAll(Arrays.asList(marrValues));

        mapSymbol2Id.put("BTS", R.mipmap.bts);
        mapSymbol2Id.put("BTC", R.mipmap.btc);
        mapSymbol2Id.put("ETH", R.mipmap.eth);
        mapSymbol2Id.put("HERO", R.mipmap.hero);
        mapSymbol2Id.put("OBITS", R.mipmap.obits);
        mapSymbol2Id.put("SMOKE", R.mipmap.smok);
        mapSymbol2Id.put("USDT", R.mipmap.usdt);
        mapSymbol2Id.put("OCT", R.mipmap.oct);
        mapSymbol2Id.put("YOYOW", R.mipmap.yoyow);
        mapSymbol2Id.put("DASH", R.mipmap.dash);

    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.recyclerview_item_currency_pair, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        BitsharesMarketTicker bitsharesMarketTicker = bitsharesMarketTickerList.get(position);

        if (selected == position) {
            holder.mViewSelected.setVisibility(View.VISIBLE);
        } else {
            holder.mViewSelected.setVisibility(View.INVISIBLE);
        }

        MarketTicker marketTicker = bitsharesMarketTicker.marketTicker;
        String currencyPair = utils.getAssetSymbolDisply(marketTicker.quote) + " : " +
                utils.getAssetSymbolDisply(marketTicker.base);

        holder.mViewCurrencyPair.setText(currencyPair);
        Integer nId = mapSymbol2Id.get(utils.getAssetSymbolDisply(marketTicker.quote));
        if (nId == null) {
            nId = R.mipmap.bts;
        }
        holder.mCurrencyIconView.setImageResource(nId);


        DecimalFormat decimalFormat = new DecimalFormat("#.####");
        holder.mViewPrice.setText(decimalFormat.format(marketTicker.latest));

        double percent_change = 0.f;
        try {
            percent_change = Double.parseDouble(marketTicker.percent_change);
        } catch (Exception e) {
            e.printStackTrace();
        }

        String strPercentChange;
        if (percent_change >= 0) {
            if (!MainActivity.rasingColorRevers) {
                holder.mView24h.setBackgroundResource(R.drawable.percent_change_background_green);
            } else {
                holder.mView24h.setBackgroundResource(R.drawable.percent_change_background_red);
            }
            strPercentChange = String.format(
                    Locale.ENGLISH,
                    "+%.2f%%",
                    percent_change
            );
        } else {
            if (!MainActivity.rasingColorRevers) {
                holder.mView24h.setBackgroundResource(R.drawable.percent_change_background_red);
            } else {
                holder.mView24h.setBackgroundResource(R.drawable.percent_change_background_green);
            }
            strPercentChange = String.format(
                    Locale.ENGLISH,
                    "%.2f%%",
                    percent_change
            );
        }
        holder.mView24h.setText(strPercentChange);


        holder.mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (monItemClickListner != null) {
                    selected = position;
                    monItemClickListner.onItemClick(holder.mView, position);
                    notifyDataSetChanged();
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        if (bitsharesMarketTickerList == null) {
            return 0;
        } else {
            return bitsharesMarketTickerList.size();
        }
    }

    public void setOnItemClickListenr(OnItemClickListner onItemClickListenr) {
        monItemClickListner = onItemClickListenr;
    }

    public void notifyDataUpdated(List<BitsharesMarketTicker> marketTickerList) {
        bitsharesMarketTickerList = new ArrayList<>();
        for (BitsharesMarketTicker bitsharesMarketTicker : marketTickerList) {
            if (currecnyPairSet.contains(bitsharesMarketTicker.marketTicker.quote + ":" + bitsharesMarketTicker.marketTicker.base)) {
                bitsharesMarketTickerList.add(bitsharesMarketTicker);
            }
        }

        Collections.sort(
                bitsharesMarketTickerList,
                (o1, o2) -> o1.marketTicker.quote.compareTo(o2.marketTicker.quote)
        );

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        String strAssetPairConfig = prefs.getString("quotation_currency_pair", "BTS:USD");
        for (int i = 0; i < bitsharesMarketTickerList.size(); ++i) {
            MarketTicker marketTicker = bitsharesMarketTickerList.get(i).marketTicker;
            String currencyPair = utils.getAssetSymbolDisply(marketTicker.quote) + ":" +
                    utils.getAssetSymbolDisply(marketTicker.base);
            if (strAssetPairConfig.compareTo(currencyPair) == 0) {
                selected = i;
                break;
            }
        }

        notifyDataSetChanged();
    }

    public BitsharesMarketTicker getSelectedMarketTicker() {
        if (selected >= bitsharesMarketTickerList.size()) {
            return null;
        }

        return bitsharesMarketTickerList.get(selected);
    }
}
