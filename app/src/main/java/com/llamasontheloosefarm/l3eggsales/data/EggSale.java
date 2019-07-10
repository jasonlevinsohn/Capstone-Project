package com.llamasontheloosefarm.l3eggsales.data;


import com.google.firebase.Timestamp;

import java.math.BigDecimal;
import java.util.Date;

public class EggSale implements Comparable<EggSale>{

    public EggSale() {}

    public EggSale(String customer, Timestamp saleDate, int numberOfDozen, int pricePerDozen, double totalPrice) {
        this.customer = customer;
        this.saleDate = saleDate;
        this.numberOfDozen = numberOfDozen;
        this.pricePerDozen = pricePerDozen;
        this.totalPrice = totalPrice;
    }

    public EggSale(int id, String customer, Timestamp saleDate, int numberOfDozen, int pricePerDozen, double totalPrice) {
        this.id = id;
        this.customer = customer;
        this.saleDate = saleDate;
        this.numberOfDozen = numberOfDozen;
        this.pricePerDozen = pricePerDozen;
        this.totalPrice = totalPrice;
    }

    private int id;
    private String customer;
    private Timestamp saleDate;
    private int numberOfDozen;
    private int pricePerDozen;
    private double totalPrice;


    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCustomer() {
        return customer;
    }

    public void setCustomer(String customer) {
        this.customer = customer;
    }

    public Timestamp getSaleDate() {
        return saleDate;
    }

    public void setSaleDate(Timestamp saleDate) {
        this.saleDate = saleDate;
    }

    public int getNumberOfDozen() {
        return numberOfDozen;
    }

    public void setNumberOfDozen(int numberOfDozen) {
        this.numberOfDozen = numberOfDozen;
    }

    public int getPricePerDozen() {
        return pricePerDozen;
    }

    public void setPricePerDozen(int pricePerDozen) {
        this.pricePerDozen = pricePerDozen;
    }

    public double getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(double totalPrice) {
        this.totalPrice = totalPrice;
    }

    @Override
    public int compareTo(EggSale sale) {
        return getSaleDate().compareTo(sale.getSaleDate());
    }

}

