package com.nhom3.shared.model.item;

import com.nhom3.shared.model.Entity;

public abstract class Item extends Entity{
	protected String name;
	protected double startPrice;
	protected double curHighest;
	protected ItemType type;
	protected String imageBase64;

	public Item(int id, String name, double startPrice, ItemType type) {
		super(id);
		this.name = name;
		this.startPrice = startPrice;
		this.type = type;
		// curHighest starting price is startPrice
		this.curHighest = startPrice;

	}

	public void setName(String name){
		this.name = name;
	}
	public String getName(){
		return name;
	}
	public void setStartPrice(double startPrice){
		this.startPrice = startPrice;
	}
	public double getStartPrice(){
		return startPrice;
	}
	public void setCurHighest(double curHighest){
		this.curHighest = curHighest;
	}
	public double getCurHighest(){
		return curHighest;
	}
	public ItemType getType() {
		return type;
	}
	public void setImageBase64(String imageBase64) {
		this.imageBase64 = imageBase64;
	}
	public String getImageBase64() {
		return imageBase64;
	}

}