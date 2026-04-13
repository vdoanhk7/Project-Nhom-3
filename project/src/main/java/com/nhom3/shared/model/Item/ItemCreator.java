package com.nhom3.shared.model.Item;

public interface ItemCreator {
    Item createItem(String id, String name, String info, double startPrice);
}