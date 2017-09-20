package com.bitshares.bitshareswallet;

public class QuotationItem {
    private double high;
    private double low;
    private double vol;
    private long time;

    public QuotationItem(long time, double high, double low, double vol) {
        this.high = high;
        this.low = low;
        this.vol = vol;
        this.time = time;
    }

    public QuotationItem() {

    }

    public double getHigh() {
        return high;
    }

    public void setHigh(double high) {
        this.high = high;
    }

    public double getLow() {
        return low;
    }

    public void setLow(double low) {
        this.low = low;
    }

    public double getVol() {
        return vol;
    }

    public void setVol(double vol) {
        this.vol = vol;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }
}
