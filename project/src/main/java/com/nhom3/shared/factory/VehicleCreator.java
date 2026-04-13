package com.nhom3.shared.model.Item;

public class VehicleCreator implements ItemCreator {
    @Override
    public Item createItem(String id, String name, String info, double startPrice) {
        return new Vehicle(id, name, info, startPrice);
    }
}