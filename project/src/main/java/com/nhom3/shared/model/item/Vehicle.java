package com.nhom3.shared.model.item;

public class Vehicle extends Item{
    // Constructor
    public Vehicle(int id, String name, double startPrice){
        super(id, name, startPrice, ItemType.VEHICLE);
    }

    @Override
    public String printInfo() {
        return formatItemInfo("Vehicle");
    }
}
