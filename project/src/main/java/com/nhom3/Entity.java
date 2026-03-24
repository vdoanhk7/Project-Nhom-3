package com.nhom3;

public abstract class Entity {
    // Moi thuc the deu phai co id xac dinh duy nhat
    protected int id;

    public Entity(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}
