package com.nhom3.shared.factory;

import com.nhom3.shared.model.Item.Item;
import com.nhom3.shared.model.Item.Vehicle;

public class VehicleCreator implements ItemCreator {
    @Override
    public Item createItem(int id, String name, String info, double startPrice) {
        return new Vehicle(id, name, info, startPrice);
    }
}