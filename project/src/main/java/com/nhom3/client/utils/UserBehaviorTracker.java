package com.nhom3.client.utils;

import com.nhom3.shared.model.auction.Auction;
import com.nhom3.shared.model.item.Item;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

public final class UserBehaviorTracker {
    private static final double CATEGORY_MAX_SCORE = 50.0;
    private static final double BUDGET_MAX_SCORE = 30.0;
    private static final double URGENCY_MAX_SCORE = 20.0;
    private static final double BUDGET_TOLERANCE_RATIO = 0.3;
    private static final int VIEW_CATEGORY_WEIGHT = 1;
    private static final int BID_CATEGORY_WEIGHT = 10;
    private static final UserBehaviorTracker INSTANCE = new UserBehaviorTracker();

    private final Map<String, Integer> categoryScores = new HashMap<>();
    private double totalInteractedValue = 0;
    private int interactionCount = 0;

    private UserBehaviorTracker() {
    }

    public static UserBehaviorTracker getInstance() {
        return INSTANCE;
    }

    public synchronized void recordView(Item item) {
        recordInteraction(item, VIEW_CATEGORY_WEIGHT);
    }

    public synchronized void recordBid(Item item) {
        recordInteraction(item, BID_CATEGORY_WEIGHT);
    }

    public synchronized double getAverageBudget() {
        return interactionCount == 0 ? 0 : totalInteractedValue / interactionCount;
    }

    public synchronized double calculateMatchScore(Auction auction) {
        return getCategoryScore(auction) + getBudgetScore(auction) + getUrgencyScore(auction);
    }

    public synchronized double getCategoryScore(Auction auction) {
        Item item = getItem(auction);
        if (item == null || item.getType() == null || categoryScores.isEmpty()) {
            return 0;
        }

        int totalScore = categoryScores.values().stream().mapToInt(Integer::intValue).sum();
        if (totalScore <= 0) {
            return 0;
        }

        int categoryScore = categoryScores.getOrDefault(item.getType().name(), 0);
        return CATEGORY_MAX_SCORE * categoryScore / totalScore;
    }

    public synchronized double getBudgetScore(Auction auction) {
        Item item = getItem(auction);
        double budget = getAverageBudget();
        if (item == null || budget <= 0) {
            return 0;
        }

        double price = item.getCurHighest();
        double minPreferred = budget * (1 - BUDGET_TOLERANCE_RATIO);
        double maxPreferred = budget * (1 + BUDGET_TOLERANCE_RATIO);
        if (price >= minPreferred && price <= maxPreferred) {
            return BUDGET_MAX_SCORE;
        }

        double distance = price < minPreferred ? minPreferred - price : price - maxPreferred;
        double decayBase = budget * BUDGET_TOLERANCE_RATIO;
        if (decayBase <= 0) {
            return 0;
        }
        double penalty = distance / decayBase * BUDGET_MAX_SCORE;
        return Math.max(0, BUDGET_MAX_SCORE - penalty);
    }

    public double getUrgencyScore(Auction auction) {
        if (auction == null || auction.getEndTime() == null) {
            return 0;
        }

        long hoursLeft = ChronoUnit.HOURS.between(LocalDateTime.now(), auction.getEndTime());
        if (hoursLeft < 0) {
            return 0;
        }
        if (hoursLeft < 24) {
            return URGENCY_MAX_SCORE;
        }
        if (hoursLeft < 48) {
            return 10;
        }
        return 0;
    }

    private void recordInteraction(Item item, int categoryWeight) {
        if (item == null) {
            return;
        }

        if (item.getType() != null) {
            categoryScores.merge(item.getType().name(), categoryWeight, Integer::sum);
        }
        totalInteractedValue += Math.max(0, item.getCurHighest());
        interactionCount++;
    }

    private Item getItem(Auction auction) {
        return auction == null ? null : auction.getItem();
    }
}
