package com.nhom3.server.dao;

import java.util.List;

import com.nhom3.shared.model.item.Item;

public interface ItemDAO {
    boolean saveItem(Item item, int sellerId);
    List<Item> getItemsBySellerId(int sellerId);
    String getItemImageBase64(int itemId);
    boolean deleteItem(int itemId, int sellerId);
    boolean updateItem(Item item);
}
