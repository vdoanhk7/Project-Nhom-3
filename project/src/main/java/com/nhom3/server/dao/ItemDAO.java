package com.nhom3.server.dao;

import java.util.List;

import com.nhom3.shared.model.item.Item;

public interface ItemDAO {
    boolean saveItem(Item item, int sellerId);
    List<Item> getItemsBySellerId(int sellerId);
    boolean deleteItem(int itemId);
    boolean updateItem(Item item);
}
