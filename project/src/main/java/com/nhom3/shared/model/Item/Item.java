package com.nhom3.Item;
import com.nhom3.Entity;

public abstract class Item extends Entity{
	protected String name;
	protected String info;
	protected double startPrice;
	protected double curHighest;
	protected String type;

	public Item(String id, String name, String info, double startPrice){
		super(id);
		this.name = name;
		this.info = info;
		this.startPrice = startPrice;
		// curHighest starting price is startPrice
		this.curHighest = startPrice;

	}

	public void setName(String name){
		this.name = name;
	}
	public String getName(){
		return name;
	}
	public void setInfo(String info){
		this.info = info;
	}
	public String getInfo(){
		return info;
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
	
	@Override
	public void displayInfo() {
		System.out.println("Item Info:");
		System.out.println("Name: " + name);
		System.out.println("Info: " + info);
		System.out.println("Start Price: " + startPrice);
		System.out.println("Current Highest: " + curHighest);
		System.out.println("Type: " + type);
	}

	

}