package com.nhom3.shared.model.item;

public class Other extends Item {
    public Other(int id, String name, double startPrice) {
        super(id, name, startPrice, ItemType.OTHER);
    }

    @Override
    public String printInfo() {
        return formatItemInfo("Other");
    }
}
