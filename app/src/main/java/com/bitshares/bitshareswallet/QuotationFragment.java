package com.bitshares.bitshareswallet;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bitshares.bitshareswallet.market.MarketStat;
import com.bitshares.bitshareswallet.wallet.Broadcast;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class QuotationFragment extends BaseFragment implements MarketStat.OnMarketStatUpdateListener {
    private static final String TAG = "QuotationFragment";
    private static final long MARKET_STAT_INTERVAL_MILLIS = TimeUnit.SECONDS.toMillis(30);
    private static final long TICKER_STAT_INTERVAL_MILLIS = TimeUnit.SECONDS.toMillis(2);

    private QuotationRecyclerViewAdapter quotationRecyclerViewAdapter;
    private List<QuotationItem> quotation = new ArrayList<>();

    private MarketStat marketStat;
    private MarketStat tickerStat;
    private String baseAsset;
    private String quoteAsset;
    private TextView titleValue;
    private TextView timeText;
    private TextView highsAndLowsText;
    private ImageView valueIcon;
    private ImageView valueIcon2;
    private View viewLayoutLatest;

    private BroadcastReceiver currencyUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            marketStat.unsubscribe(baseAsset, quoteAsset);
            tickerStat.unsubscribe(baseAsset, quoteAsset);

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
            baseAsset = prefs.getString("currency_setting", "USD");
            quoteAsset = "BTS";
            marketStat.subscribe(baseAsset, quoteAsset, MarketStat.STAT_MARKET_HISTORY,
                    MARKET_STAT_INTERVAL_MILLIS, QuotationFragment.this);
            tickerStat.subscribe(baseAsset, quoteAsset, MarketStat.STAT_MARKET_TICKER,
                    TICKER_STAT_INTERVAL_MILLIS, QuotationFragment.this);
        }
    };

    public QuotationFragment() {
        marketStat = new MarketStat();
        tickerStat = new MarketStat();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public static QuotationFragment newInstance() {
        QuotationFragment fragment = new QuotationFragment();
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_quotation, container, false);

        Context context = view.getContext();
        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.recyclerView);

        quotationRecyclerViewAdapter = new QuotationRecyclerViewAdapter(quotation);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(context);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setAdapter(quotationRecyclerViewAdapter);
        recyclerView.setItemAnimator(null);
        int spacingInPixels = 1;
        recyclerView.addItemDecoration(new SpacesItemDecoration(spacingInPixels));

        titleValue = (TextView) view.findViewById(R.id.textTotalBalance);
        timeText = (TextView) view.findViewById(R.id.timeText);
        highsAndLowsText = (TextView) view.findViewById(R.id.highsAndLowsText);
        valueIcon = (ImageView) view.findViewById(R.id.valueIcon);
        valueIcon2 = (ImageView) view.findViewById(R.id.valueIcon2);

        viewLayoutLatest = view.findViewById(R.id.layoutLatest);

        return view;
    }

    @Override
    public void onShow() {
        super.onShow();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        baseAsset = prefs.getString("currency_setting", "USD");
        quoteAsset = "BTS";
        marketStat.subscribe(baseAsset, quoteAsset, MarketStat.STAT_MARKET_HISTORY,
                MARKET_STAT_INTERVAL_MILLIS, this);
        tickerStat.subscribe(baseAsset, quoteAsset, MarketStat.STAT_MARKET_TICKER,
                TICKER_STAT_INTERVAL_MILLIS, this);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Broadcast.CURRENCY_UPDATED);
        LocalBroadcastManager.getInstance(getContext())
                .registerReceiver(currencyUpdateReceiver, intentFilter);
    }

    @Override
    public void onHide() {
        super.onHide();
        marketStat.unsubscribe(baseAsset, quoteAsset);
        tickerStat.unsubscribe(baseAsset, quoteAsset);
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(currencyUpdateReceiver);
    }

    @Override
    public void onMarketStatUpdate(MarketStat.Stat stat) {
        if(getView()==null)
            return;
        if (stat.prices != null && stat.prices.length > 0) {
            quotation.clear();
            for (int i = 0; i < stat.prices.length; i++) {
                MarketStat.HistoryPrice price = stat.prices[i];
                quotation.add(new QuotationItem(price.date.getTime(), price.high, price.low, price.volume));
            }
            quotationRecyclerViewAdapter.notifyDataSetChanged();
        }
        if (stat.ticker != null) {
            Log.d(TAG, String.format("ticker>>> base:%s, quote:%s, latest:%g, change:%s",
                    stat.ticker.base, stat.ticker.quote, stat.ticker.latest, stat.ticker.percent_change));
            valueIcon.setVisibility(View.VISIBLE);
            valueIcon2.setVisibility(View.VISIBLE);

            double change = 0.f;
            try {
                change = Double.parseDouble(stat.ticker.percent_change);
            } catch (Exception e) {
                e.printStackTrace();
            }
            DecimalFormat decimalFormat = new DecimalFormat("#.######");
            if (change > 0) {
                valueIcon.setVisibility(View.VISIBLE);
                valueIcon2.setVisibility(View.GONE);
                if (!MainActivity.rasingColorRevers) {
                    valueIcon.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.quotation_top_triangle4));
                    highsAndLowsText.setTextColor(ContextCompat.getColor(getContext(), R.color.quotation_top_green));
                    titleValue.setTextColor(ContextCompat.getColor(getContext(), R.color.quotation_top_green));
                } else {
                    valueIcon.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.quotation_top_triangle2));
                    highsAndLowsText.setTextColor(ContextCompat.getColor(getContext(), R.color.quotation_top_red));
                    titleValue.setTextColor(ContextCompat.getColor(getContext(), R.color.quotation_top_red));
                }
            } else if (change < 0) {
                valueIcon.setVisibility(View.VISIBLE);
                valueIcon2.setVisibility(View.GONE);
                if (!MainActivity.rasingColorRevers) {
                    valueIcon.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.quotation_top_triangle3));
                    highsAndLowsText.setTextColor(ContextCompat.getColor(getContext(), R.color.quotation_top_red));
                    titleValue.setTextColor(ContextCompat.getColor(getContext(), R.color.quotation_top_red));
                } else {
                    valueIcon.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.quotation_top_triangle));
                    highsAndLowsText.setTextColor(ContextCompat.getColor(getContext(), R.color.quotation_top_green));
                    titleValue.setTextColor(ContextCompat.getColor(getContext(), R.color.quotation_top_green));
                }
            } else {
                valueIcon.setVisibility(View.GONE);
                valueIcon2.setVisibility(View.VISIBLE);
            }

            DecimalFormat decimalFormat2 = new DecimalFormat("#.##");
            highsAndLowsText.setText(String.format("%s%%", decimalFormat2.format(change)));
            titleValue.setText(decimalFormat.format(stat.ticker.latest));
        }
        viewLayoutLatest.setVisibility(View.VISIBLE);

        if (stat.latestTradeDate != null) {
            SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
            timeText.setText(formatter.format(stat.latestTradeDate));
        }
    }

    public class SpacesItemDecoration extends RecyclerView.ItemDecoration {
        private int space;
        private Paint dividerPaint;

        public SpacesItemDecoration(int space) {
            dividerPaint = new Paint();
            dividerPaint.setColor(ContextCompat.getColor(getContext(), R.color.grey));
            this.space = space;
        }

        @Override
        public void getItemOffsets(Rect outRect, View view,
                                   RecyclerView parent, RecyclerView.State state) {
            outRect.left = space;
            outRect.right = space;
            outRect.bottom = space;

            if (parent.getChildAdapterPosition(view) == 0)
                outRect.top = space;
        }

        @Override
        public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
            int childCount = parent.getChildCount();
            int left = parent.getPaddingLeft();
            int right = parent.getWidth() - parent.getPaddingRight();

            for (int i = 0; i < childCount - 1; i++) {
                View view = parent.getChildAt(i);
                float top = view.getBottom();
                float bottom = view.getBottom() + space;
                c.drawRect(left, top, right, bottom, dividerPaint);
            }
        }
    }
}
