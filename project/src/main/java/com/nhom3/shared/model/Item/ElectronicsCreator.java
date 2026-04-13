package com.nhom3.Item;

public class ElectronicsCreator implements ItemCreator {
    @Override
    public Item createItem(String id, String name, String info, double startPrice) {
        return new Electronics(id, name, info, startPrice);
    }
    
}
