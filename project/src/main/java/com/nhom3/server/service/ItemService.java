package com.nhom3.server.service;

import com.nhom3.shared.model.item.Item;
import com.nhom3.shared.model.user.Seller;

public class ItemService {
    
    public void createItem(Seller seller, Item newItem) { // Thêm item mới vào danh sách quản 
        if (seller == null || newItem == null) {
            System.out.println("Seller hoặc Item không tồn tại.");
            return;
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
