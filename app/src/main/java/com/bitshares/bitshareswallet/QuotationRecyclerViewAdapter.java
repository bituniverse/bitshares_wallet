package com.bitshares.bitshareswallet;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.List;

public class QuotationRecyclerViewAdapter extends RecyclerView.Adapter<QuotationRecyclerViewAdapter.ViewHolder> {

    private final List<QuotationItem> list;

    public QuotationRecyclerViewAdapter(List<QuotationItem> items) {
        list = items;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.view_quotation_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        QuotationItem quotationItem = holder.item = list.get(list.size() - position - 1);
        long time = quotationItem.getTime();
        Calendar calendar = Calendar.getInstance();

        calendar.setTimeInMillis(time);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        String timeString = hour < 10 ? "0" + hour : hour + "";
        timeString += ":";
        timeString += minute < 10 ? "0" + minute : minute + "";
        holder.timeText.setText(timeString);

        DecimalFormat decimalFormat = new DecimalFormat("#.####");
        holder.lowText.setText(decimalFormat.format(quotationItem.getLow()));
        holder.highText.setText(decimalFormat.format(quotationItem.getHigh()));
        holder.volText.setText(decimalFormat.format(quotationItem.getVol()));
        holder.view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            }
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final View view;
        public QuotationItem item;
        public TextView timeText;
        public TextView highText;
        public TextView lowText;
        public TextView volText;

        public ViewHolder(View view) {
            super(view);
            this.view = view;
            timeText = (TextView) view.findViewById(R.id.timeText);
            highText = (TextView) view.findViewById(R.id.highText);
            lowText = (TextView) view.findViewById(R.id.lowText);
            volText = (TextView) view.findViewById(R.id.volText);
        }
    }
}
