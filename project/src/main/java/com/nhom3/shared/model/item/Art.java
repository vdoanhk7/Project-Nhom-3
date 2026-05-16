package com.nhom3.shared.model.item;

public class Art extends Item{
    public Art(int id, String name, double startPrice){
        super(id, name, startPrice, ItemType.ART);
    }

    @Override
    public String printInfo() {
        return formatItemInfo("Art");
    }
}
