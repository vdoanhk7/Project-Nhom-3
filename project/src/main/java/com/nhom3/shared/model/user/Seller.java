package com.nhom3.shared.model.user;
import java.util.ArrayList;
import java.util.List;
import com.nhom3.shared.model.auction.Auction;
import com.nhom3.shared.model.item.Item;

public class Seller extends User {
    private List<Item> managedItems;
    private List<Auction> managedAuctions;
    public Seller(int id, UserInfo userInfo, UserContact userContact) {
        super(id, userInfo, userContact, Role.SELLER);
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

