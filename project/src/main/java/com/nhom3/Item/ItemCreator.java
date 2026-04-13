package com.nhom3.Item;

public interface ItemCreator {
    Item createItem(String id, String name, String info, double startPrice);
}