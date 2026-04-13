package com.nhom3.shared.model.Item;

public abstract class Item {
	protected String name;
	protected String info;
	protected double startPrice;
	protected double curHighest;
	protected String type;
	protected String id;

	public Item(String id, String name, String info, double startPrice){
		this.id = id;
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
	public void setType(String type) {
		this.type = type;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}

}