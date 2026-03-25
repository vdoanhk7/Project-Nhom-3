package com.nhom3.Item;
import java.time.LocalDate;

abstract class Item extends Entity{
	protected String iName;
	protected String info;
	protected double startPrice;
	protected double curHighest;
	protected LocalDate startAuction;
	protected LocalDate endAuction;
	
	//Constructor Item 
	protected Item(String id, String name, String info, double startPrice, LocalDate startAuction, LocalDate endAuction){
		super(id);
		this.iName = name;
		this.info = info;
		this.startPrice = startPrice;
		// curHighest starting is startPrice
		this.curHighest = startPrice;
		this.startAuction = startAuction;
		this.endAuction = endAuction;
	}

	// Setter getter name 
	abstract protected void setName();
	abstract protected void getName();
	
	// Setter getter info	
	abstract protected void setInfo();
	abstract protected void getInfo();
		
	// Setter getter startPrice
	abstract protected void setStartPrice();
	abstract protected void getStartPrice();
	
	// Setter getter curHighest
	abstract protected void getCurHighest();
	abstract protected void setCurHighest();

	// Setter getter startAuction
	abstract protected void setStartAuction();
	abstract protected void getStartAuction();

	// Setter getter startAuction
	abstract protected void setEndAuction();
	abstract protected void getEndAuction();
}