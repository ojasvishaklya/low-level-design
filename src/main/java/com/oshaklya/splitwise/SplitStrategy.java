package com.oshaklya.splitwise;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

interface SplitStrategy {
    Map<String, Double> calculateSplitByUserId(double amount, List<String> participants, Object splitDetails);
}

class EqualSplitStrategyImpl implements SplitStrategy {
    @Override
    public Map<String, Double> calculateSplitByUserId(double amount, List<String> participants, Object splitDetails) {
        Map<String, Double> map = new HashMap<>();
        double share = (amount / participants.size());
        // assuming we have unique list of participants;
        for (String s : participants) {
            map.put(s, share);
        }
        return map;
    }
}
