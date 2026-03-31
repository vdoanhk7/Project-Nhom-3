package com.nhom3.Item;

import java.time.LocalDate;

public class Art extends Item{
	// Constructor
	public Art(String id, String name, String info, double startPrice, LocalDate start, LocalDate end){
		super(id, name, info, startPrice, start, end);
	}

	public void setName(){
		this.name = name;
	}
	public String getName(){
		return name;	
	}
	
	public void setInfo(){
		this.info = info;
	}
	public String getInfo(){
		return info;
	}

	public void setStartPrice(){
		this.startPrice = startPrice;
	}
	public double getStartPrice(){
		return startPrice;
	}
	
	public double getCurHighest(){
		return curHighest;
	}
	public void setCurHighest(){
		this.curHighest = curHighest;
	}

	public void setStartAuction(){
		this.startAuction = startAuction;
	}
	public LocalDate getStartAuction(){
		return startAuction;
	}

	public void setEndAuction(){
		this.endAuction = endAuction;
	}
	public LocalDate getEndAuction(){
		return endAuction;
	}

	@Override
	public void displayInfo() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'displayInfo'");
	}
}