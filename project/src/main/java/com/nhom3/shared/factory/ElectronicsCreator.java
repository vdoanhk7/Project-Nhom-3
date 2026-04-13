package com.nhom3.shared.factory;

import com.nhom3.shared.model.Item.Electronics;
import com.nhom3.shared.model.Item.Item;

public class ElectronicsCreator implements ItemCreator {
    @Override
    public Item createItem(int id, String name, String info, double startPrice) {
        return new Electronics(id, name, info, startPrice);
    }
    
}
