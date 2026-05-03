package com.oshaklya.splitwise;

import java.util.*;

enum Currency {
    RUPEE,
    DOLLAR,
    EURO
}

class Expense {
    double amount;
    Set<String> credit_to;
    String debited_from;
    Currency currency;

    public Expense(List<String> credit_to, int amount, String debited_from) {
        this.credit_to = new HashSet<>(credit_to);
        this.amount = amount;
        this.debited_from = debited_from;
        this.currency = Currency.RUPEE;
    }
}

class Transaction {
    String from;
    String to;
    double amount;

    public Transaction(String from, String to, double amount) {
        this.from = from;
        this.to = to;
        this.amount = amount;
    }
}

public class Main {
    public static void main(String[] args) {
        ExpenseManager manager = new ExpenseManager();
        SplitStrategy equalSplit = new EqualSplitStrategyImpl();

        // Example scenario: 4 friends on a trip
        // Alice pays for dinner: 1200
        Expense dinner = new Expense(
            Arrays.asList("Alice", "Bob", "Charlie", "David"),
            1200,
            "Alice"
        );
        manager.addExpense(dinner, equalSplit);

        // Bob pays for hotel: 2000
        Expense hotel = new Expense(
            Arrays.asList("Alice", "Bob", "Charlie", "David"),
            2000,
            "Bob"
        );
        manager.addExpense(hotel, equalSplit);

        // Charlie pays for transport: 800
        Expense transport = new Expense(
            Arrays.asList("Alice", "Bob", "Charlie", "David"),
            800,
            "Charlie"
        );
        manager.addExpense(transport, equalSplit);

        // David pays for breakfast: 400
        Expense breakfast = new Expense(
            Arrays.asList("Alice", "Bob", "Charlie"),
            400,
            "David"
        );
        manager.addExpense(breakfast, equalSplit);

        // Show current balances
        manager.printBalances();

        // Calculate and show minimum settlements
        List<Transaction> settlements = manager.settle();
        manager.printSettlements(settlements);
    }
}
