package foodvendor;

import java.io.Serializable;

public class Order implements Serializable {

    private Food food;
    private String name, transCode;
    private int qty, price, total;

    //GET
    public Food getFood() {
        return food;
    }

    public int getQty() {
        return qty;
    }

    public int getTotal() {
        return total;
    }

    public String getTransCode() {
        return transCode;
    }

    //SET
    public void setFood(Food food) {
        this.food = food;
    }

    public void setQty(int qty) {
        this.qty = qty;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public void setTransCode(String transCode) {
        this.transCode = transCode;
    }

}
