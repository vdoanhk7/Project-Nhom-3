package com.nhom3.sever.service;

import java.util.ArrayList;

import com.nhom3.shared.model.item.Item;
import com.nhom3.shared.model.user.Seller;

public class ItemService {
    
    public void createItem(Seller seller, Item newItem) { // Thêm item mới vào danh sách quản lý
        if (seller.getManagedItems() == null) {
            seller.setManagedItems(new ArrayList<>());
        }
        seller.getManagedItems().add(newItem);
    }
    public void removeItem(Seller seller, int itemId) {
        if (seller.getManagedItems() == null) {
            System.out.println("No items to remove.");
            return;
        }
        if (!seller.getManagedItems().removeIf(item -> item.getId() == itemId)) {
            System.out.println("Item with ID " + itemId + " not found.");
        }
    }
}
