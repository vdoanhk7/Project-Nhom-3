package com.nhom3.shared.factory;

import com.nhom3.shared.model.item.Electronics;
import com.nhom3.shared.model.item.Item;

public class ElectronicsCreator implements ItemCreator {
    @Override
    public Item createItem(int id, String name, double startPrice) {
        return new Electronics(id, name, startPrice);
    }
    
}
