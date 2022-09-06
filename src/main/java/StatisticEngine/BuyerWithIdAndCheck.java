package StatisticEngine;

import java.util.ArrayList;

public class BuyerWithIdAndCheck {

    private final int buyerId;
    private final String fullName;
    private final double totalPurchasePrice;

    private ArrayList<GeneralPurchase> purchases;

    public BuyerWithIdAndCheck(int buyerId, String fullName, double totalPurchasePrice) {
        this.buyerId = buyerId;
        this.fullName = fullName;
        this.totalPurchasePrice = totalPurchasePrice;
        purchases = null;
    }

    public void setTotalPurchasePrice(ArrayList<GeneralPurchase> purchases) {
        this.purchases = purchases;
    }

    public int getBuyerId() {
        return buyerId;
    }

    public String getFullName() {
        return fullName;
    }

    public double getTotalPurchasePrice() {
        return totalPurchasePrice;
    }

    public ArrayList<GeneralPurchase> getPurchases() {
        return purchases;
    }

}
