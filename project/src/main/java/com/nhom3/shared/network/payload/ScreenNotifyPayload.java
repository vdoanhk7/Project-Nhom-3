package com.nhom3.shared.network.payload;

public class ScreenNotifyPayload {
    private double highestPrice;

    public ScreenNotifyPayload(double highestPrice) {
        this.highestPrice = highestPrice;
    }

    public double getHighestPrice() {
        return highestPrice;
    }

    public void setHighestPrice(double highestPrice) {
        this.highestPrice = highestPrice;
    }
}
