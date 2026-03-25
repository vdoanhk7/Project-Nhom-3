package com.nhom3.Item;
import java.time.LocalDate;

abstract class Item extends Entity{
	protected String iName;
	protected String info;
	protected double startPrice;
	protected double curHighest;
	protected LocalDate startAuction;
	protected LocalDate endAuction;
	
	protected Item(String id, String name, String info, double startPrice, double curHighest, LocalDate start, LocalDate end){
		super(id);
		this.iName = name;
		this.info = info;
		this.startPrice = startPrice;
		this.curHighest = curHighest;
		this.start = start;
		this.end = end;
	}
	
	
}