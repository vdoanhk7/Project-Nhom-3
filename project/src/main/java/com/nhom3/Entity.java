package com.nhom3;

public abstract class Entity {
    // Moi thuc the deu phai co id xac dinh duy nhat
    protected int id;
    protected String type;

    public Entity(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
