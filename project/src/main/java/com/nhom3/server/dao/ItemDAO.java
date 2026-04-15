package com.nhom3.server.dao;

import com.nhom3.shared.model.item.Item;

public interface ItemDAO {
    boolean saveItem(Item item, int sellerId);
}
