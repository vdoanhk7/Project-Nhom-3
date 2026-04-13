package com.nhom3.shared.model.user;
import java.util.ArrayList;
import java.util.List;
import com.nhom3.shared.model.auction.Auction;
import com.nhom3.shared.model.item.Item;

public class Seller extends User {
    private List<Item> managedItems;
    private List<Auction> managedAuctions;
    public void setManagedItems(List<Item> managedItems) {
        this.managedItems = managedItems;
    }

    public void setManagedAuctions(List<Auction> managedAuctions) {
        this.managedAuctions = managedAuctions;
    }

    public Seller(int id, String userName, String password, String name, String email, String phoneNumber) {
        super(id, userName, password, name, email, phoneNumber);
        this.managedItems = new ArrayList<>();
        this.managedAuctions = new ArrayList<>();
    }

    public List<Item> getManagedItems() {
        return managedItems;
    }
    public List<Auction> getManagedAuctions() {
        return managedAuctions;
    }
}

