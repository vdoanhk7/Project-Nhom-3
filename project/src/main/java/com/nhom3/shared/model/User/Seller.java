package com.nhom3.shared.model.User;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;

import com.nhom3.shared.model.Auction.Auction;
import com.nhom3.shared.model.Item.Item;

public class Seller extends User {
    private List<Item> managedItems;
    private List<Auction> managedAuctions;

    public Seller(int id, String userName, String password, String name, String email, String phoneNumber) {
        super(id, userName, password, name, email, phoneNumber);
        this.managedItems = new ArrayList<>();
    }
    // Các phương thức quản lý item
    public void createItem(Item newItem) { // Thêm item mới vào danh sách quản lý
        if (managedItems == null) {
            managedItems = new ArrayList<>();
        }
        managedItems.add(newItem);
    }
    public void removeItem(int itemId) {
        if (managedItems == null) {
            System.out.println("No items to remove.");
            return;
        }
        if (!managedItems.removeIf(item -> item.getId() == itemId)) {
            System.out.println("Item with ID " + itemId + " not found.");
        }
    }

    public void createAuction(Item item, int id, LocalDateTime startTime, LocalDateTime endTime) {
        if (managedAuctions == null) {
            managedAuctions = new ArrayList<>();
        }
        Auction newAuction = new Auction(id, item, startTime, endTime);
        managedAuctions.add(newAuction);
    }
    public void removeAuction(int auctionId) {
        if (managedAuctions == null) {
            System.out.println("No auctions to remove.");
            return;
        }
        if (!managedAuctions.removeIf(auction -> auction.getId() == auctionId)) {
            System.out.println("Auction with ID " + auctionId + " not found.");
        }
    }

    public void runAuction(int auctionId) {
        if (managedAuctions == null) {
            System.out.println("No auctions to run.");
            return;
        }
        for (Auction auction : managedAuctions) {
            if (auction.getId() == auctionId) {
                auction.runAuction();
                return;
            }
        }
        System.out.println("Auction with ID " + auctionId + " not found.");
    }

    public List<Item> getManagedItems() {
        return managedItems;
    }
    public List<Auction> getManagedAuctions() {
        return managedAuctions;
    }
}

