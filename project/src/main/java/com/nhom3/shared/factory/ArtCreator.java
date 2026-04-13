package com.nhom3.shared.factory;

import com.nhom3.shared.model.item.Art;
import com.nhom3.shared.model.item.Item;

public class ArtCreator implements ItemCreator {
    @Override
    public Item createItem(int id, String name, double startPrice) {
        return new Art(id, name, startPrice);
    }
}
