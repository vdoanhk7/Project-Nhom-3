package com.nhom3;

public abstract class Entity {
    // Moi thuc the deu phai co id xac dinh duy nhat
    protected String id;
    protected String type;

    public Entity(String id) {
        this.id = id;
    }

    public abstract String getId();
    public abstract String getType();
    public abstract void displayInfo();
}
