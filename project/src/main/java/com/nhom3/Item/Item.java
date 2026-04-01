package com.nhom3.Item;
import java.time.LocalDate;
import com.nhom3.Entity;

abstract class Item extends Entity{
	protected String iName;
	protected String info;
	protected double startPrice;
	protected double curHighest;
	protected LocalDate startAuction;
	protected LocalDate endAuction;
	
	//Constructor Item 
	public Item(String id, String iName, String info, double startPrice, LocalDate startAuction, LocalDate endAuction){
		super(id);
		this.iName = iName;
		this.info = info;
		this.startPrice = startPrice;
		// curHighest starting price is startPrice
		this.curHighest = startPrice;
		this.startAuction = startAuction;
		this.endAuction = endAuction;
	}

	// Setter getter name 
	public void setName(String iName){
		this.iName = iName;
	}
	public String getName(){
		return iName;
	}
	
	// Setter getter info	
	public void setInfo(String info){
		this.info = info;
	}
	public String getInfo(){
		return info;
	}
		
	// Setter getter startPrice
	public void setStartPrice(double startPrice){
		this.startPrice = startPrice;
	}
	public double getStartPrice(){
		return startPrice;
	}
	
	// Setter getter curHighest
	public void setCurHighest(double curHighest){
		this.curHighest = curHighest;
	}
	public double getCurHighest(){
		return curHighest;
	}

	// Setter getter startAuction
	public void setStartAuction(LocalDate startAuction){
		this.startAuction = startAuction;
	}
	public LocalDate getStartAuction(){
		return startAuction;
	}

	// Setter getter endAuction
	public void setEndAuction(LocalDate endAuction){
		this.endAuction = endAuction;
	}
	public LocalDate getEndAuction(){
		return endAuction;
	}
}