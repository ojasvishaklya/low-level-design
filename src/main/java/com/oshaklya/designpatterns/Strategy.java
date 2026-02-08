package com.oshaklya.designpatterns;


interface PaymentStrategy {
    void makePayment(String amount);
}

class CashPaymentStrategy implements PaymentStrategy {
    @Override
    public void makePayment(String amount) {
        System.out.println("Payed with cash, amount: " + amount);
    }
}

class UPIPaymentStrategy implements PaymentStrategy {
    @Override
    public void makePayment(String amount) {
        System.out.println("Payed with UPI, amount: " + amount);
    }
}

class ShoppingCart {
    private PaymentStrategy paymentStrategy;

    public void setPaymentStrategy(PaymentStrategy strategy) {
        this.paymentStrategy = strategy;
    }

    public void checkout(String amount) {
        paymentStrategy.makePayment(amount);
    }
}

class Strategy {
    public static void main(String[] args) {
        ShoppingCart cart = new ShoppingCart();

        cart.setPaymentStrategy(new UPIPaymentStrategy());
        cart.checkout("100.00");

        cart.setPaymentStrategy(new CashPaymentStrategy());
        cart.checkout("50.00");
    }
}