/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package foodvendor;

import java.io.Serializable;

public class Food implements Serializable {

    private String name, transCode;
    private int qty, price, total;

    //GET
    public String getName() {
        return name;
    }

    //return quantity

    public int getPrice() {
        return price;
    }

    //SET
    public void setName(String name) {
        this.name = name;
    }

    public void setPrice(int price) {
        this.price = price;
    }

}
