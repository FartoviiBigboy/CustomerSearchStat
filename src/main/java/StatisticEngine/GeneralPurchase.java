package StatisticEngine;

public class GeneralPurchase {
    private final String goodsName;
    private final double sum;

    public GeneralPurchase(String goodsName, double sum) {
        this.goodsName = goodsName;
        this.sum = sum;
    }

    public String getGoodsName() {
        return goodsName;
    }

    public double getSum() {
        return sum;
    }

}
