package com.nhom3.shared.model.Item;

public class ArtCreator implements ItemCreator {
    @Override
    public Item createItem(String id, String name, String info, double startPrice) {
        return new Art(id, name, info, startPrice);
    }
}
