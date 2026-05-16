package com.nhom3.shared.model.item;

import com.nhom3.shared.factory.ArtCreator;
import com.nhom3.shared.factory.ElectronicsCreator;
import com.nhom3.shared.factory.ItemCreator;
import com.nhom3.shared.factory.OtherCreator;
import com.nhom3.shared.factory.VehicleCreator;

public enum ItemType {
    ART(new ArtCreator()),
    ELECTRONICS(new ElectronicsCreator()),
    VEHICLE(new VehicleCreator()),
    OTHER(new OtherCreator());

    private final ItemCreator creator;

    ItemType(ItemCreator creator) {
        this.creator = creator;
    }

    public Item createItem(int id, String name, double startPrice) {
        return creator.createItem(id, name, startPrice);
    }
}
