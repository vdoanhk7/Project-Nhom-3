package com.nhom3.Item;
import java.time.LocalDate;
import com.nhom3.Entity;

public abstract class Item extends Entity{
	protected String name;
	protected String info;
	protected double startPrice;
	protected double curHighest;
	protected LocalDate startAuction;
	protected LocalDate endAuction;
	
	public Item(String id, String name, String info, double startPrice, LocalDate startAuction, LocalDate endAuction){
		super(id);
		this.name = name;
		this.info = info;
		this.startPrice = startPrice;
		this.curHighest = startPrice;
		this.startAuction = startAuction;
		this.endAuction = endAuction;
	}

	abstract public void setName(String newName);
	abstract public String getName();
	
	abstract public void setInfo(String newInfo);
	abstract public String getInfo();
		
	abstract public void setStartPrice(double newStartPrice);
	abstract public double getStartPrice();
	
	abstract public double getCurHighest();
	abstract public void setCurHighest(double newCurHighest);

	abstract public void setStartAuction(LocalDate newStartAuction);
	abstract public LocalDate getStartAuction();

	abstract public void setEndAuction(LocalDate newEndAuction);
	abstract public LocalDate getEndAuction();
}