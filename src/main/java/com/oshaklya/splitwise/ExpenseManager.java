package com.oshaklya.splitwise;

import java.util.*;

public class ExpenseManager {
    private final Map<String, Double> balances; // positive = gets money, negative = owes money

    public ExpenseManager() {
        this.balances = new HashMap<>();
    }

    public void addExpense(Expense expense, SplitStrategy strategy) {
        Map<String, Double> splitAmounts = strategy.calculateSplitByUserId(
                expense.amount,
                new ArrayList<>(expense.credit_to),
                null
        );

        // Person who paid gets credited
        balances.put(expense.debited_from,
                balances.getOrDefault(expense.debited_from, 0.0) + expense.amount);

        // Each participant owes their share
        for (Map.Entry<String, Double> entry : splitAmounts.entrySet()) {
            String user = entry.getKey();
            double share = entry.getValue();
            balances.put(user, balances.getOrDefault(user, 0.0) - share);
        }
    }

    public Map<String, Double> getBalances() {
        return new HashMap<>(balances);
    }

    public List<Transaction> settle() {
        List<Transaction> transactions = new ArrayList<>();

        PriorityQueue<UserBalance> creditors = new PriorityQueue<>((a, b) -> Double.compare(b.amount, a.amount));
        PriorityQueue<UserBalance> debtors = new PriorityQueue<>((a, b) -> Double.compare(b.amount, a.amount));

        for (String user : balances.keySet()) {
            double amount = balances.get(user);
            if (amount < -0.01) {
                debtors.add(new UserBalance(user, Math.abs(amount)));
            } else if (amount > 0.01) {
                creditors.add(new UserBalance(user, amount));
            }
        }

        // The sum of balance of all users always results in 0
        // so if first heap is empty then
        // second heap will also have no elements.
        while(!debtors.isEmpty() || !creditors.isEmpty()){
            UserBalance credit = creditors.poll();
            UserBalance debt = debtors.poll();

            assert credit != null;
            assert debt != null;
            double settlement = Math.min(credit.amount, debt.amount);
            transactions.add(
                    new Transaction(debt.userId, credit.userId, settlement)
            );
            credit.amount-=settlement;
            debt.amount-=settlement;

            if(credit.amount>0.01){
                creditors.add(credit);
            }
            if(debt.amount>0.01){
                debtors.add(debt);
            }
        }
        return transactions;
    }

    public void printBalances() {
        System.out.println("\n=== Current Balances ===");
        for (Map.Entry<String, Double> entry : balances.entrySet()) {
            double amount = Math.round(entry.getValue() * 100.0) / 100.0;
            if (amount > 0.01) {
                System.out.printf("%s gets back: %.2f\n", entry.getKey(), amount);
            } else if (amount < -0.01) {
                System.out.printf("%s owes: %.2f\n", entry.getKey(), Math.abs(amount));
            } else {
                System.out.printf("%s is settled\n", entry.getKey());
            }
        }
    }

    public void printSettlements(List<Transaction> transactions) {
        System.out.println("\n=== Settlement Plan (Minimum Transactions: " + transactions.size() + ") ===");
        for (Transaction t : transactions) {
            System.out.printf("%s pays %s: %.2f\n", t.from, t.to, t.amount);
        }
    }
}
