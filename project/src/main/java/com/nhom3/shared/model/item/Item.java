package com.nhom3.shared.model.item;

import com.nhom3.shared.model.Entity;

public abstract class Item extends Entity{
	protected String name;
	protected double startPrice;
	protected double curHighest;
	protected String type;

	public Item(int id, String name, double startPrice, String type) {
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
	public String getType() {
		return type;
	}

}