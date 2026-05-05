package com.nhom3.shared.factory;

import com.nhom3.shared.model.item.Item;
import com.nhom3.shared.model.item.Vehicle;

public class VehicleCreator implements ItemCreator {
    @Override
    public Item createItem(int id, String name, double startPrice) {
        return new Vehicle(id, name, startPrice);
    }
}