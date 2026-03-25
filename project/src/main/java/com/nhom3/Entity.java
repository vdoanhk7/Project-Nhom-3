package com.nhom3;

public abstract class Entity {
    // Moi thuc the deu phai co id xac dinh duy nhat
    protected String id;

    public Entity(String id) {
        this.id = id;
    }

    public String getId(){
        return id;
    }
    public abstract void displayInfo();
}
