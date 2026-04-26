package com.nhom3.server.service;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import com.nhom3.shared.model.item.Item;
import com.nhom3.shared.model.user.Seller;

public class ItemService {
    private static final Logger log = LoggerFactory.getLogger(AuctionService.class);
    
    public void createItem(Seller seller, Item newItem) { 
        if (seller == null || newItem == null) {
            log.info("Seller hoặc Item không tồn tại.");
            return;
        }
        seller.getManagedItems().add(newItem);
    }
    public void removeItem(Seller seller, int itemId) {
        if (seller.getManagedItems() == null) {
            log.info("No items to remove.");
            return;
        }
        if (!seller.getManagedItems().removeIf(item -> item.getId() == itemId)) {
            log.info("Item with ID " + itemId + " not found.");
        }
    }
}
