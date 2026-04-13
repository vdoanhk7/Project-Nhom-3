package com.nhom3.shared.factory;

import com.nhom3.shared.model.Item.Item;

public interface ItemCreator {
    Item createItem(int id, String name, String info, double startPrice);
}