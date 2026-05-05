package com.nhom3.shared.factory;

import com.nhom3.shared.model.item.Item;

public interface ItemCreator {
    Item createItem(int id, String name, double startPrice);
}