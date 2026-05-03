package com.oshaklya.splitwise;

public class UserBalance {
    String userId;
    double amount;

    public UserBalance(String userId, double amount) {
        this.userId = userId;
        this.amount = amount;
    }

    public String getUserId() {
        return userId;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }
}
