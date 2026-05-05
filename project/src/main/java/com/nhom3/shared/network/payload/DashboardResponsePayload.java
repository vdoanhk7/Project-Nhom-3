package com.nhom3.shared.network.payload;
import java.util.List;

public class DashboardResponsePayload {

    public static class TopBidderDTO {
        public int rank;
        public String name;
        public double spent;
        public TopBidderDTO(int rank, String name, double spent) { this.rank = rank; this.name = name; this.spent = spent; }
    }

    public static class TopItemDTO {
        public int rank;
        public String name;
        public double price;
        public TopItemDTO(int rank, String name, double price) { this.rank = rank; this.name = name; this.price = price; }
    }

    private int totalUsers;
    private int activeItems;
    private double totalRevenue;
    private List<TopBidderDTO> topBidders;
    private List<TopItemDTO> topItems;

    public DashboardResponsePayload(int totalUsers, int activeItems, double totalRevenue, List<TopBidderDTO> topBidders, List<TopItemDTO> topItems) {
        this.totalUsers = totalUsers;
        this.activeItems = activeItems;
        this.totalRevenue = totalRevenue;
        this.topBidders = topBidders;
        this.topItems = topItems;
    }

    public int getTotalUsers() { return totalUsers; }
    public int getActiveItems() { return activeItems; }
    public double getTotalRevenue() { return totalRevenue; }
    public List<TopBidderDTO> getTopBidders() { return topBidders; }
    public List<TopItemDTO> getTopItems() { return topItems; }
}