package com.nhom3;

public abstract class Item extends Entity{
	private String iName;
	private String info;
	private double startPrice;
	private double curHighest;
	private LocalDate startAuction;
	private LocalDate endAuction;
	
	protected Item(String id, String name, String info, double startPrice, double curHighest, LocalDate start, LocalDate end){
		super(id);
		this.iName = name;
		this.info = info;
		this.startPrice = startPrice;
		this.curHighest = curHighest;
		this.start = start;
		this.end = end;
	}
	
	protected void setName(){
		
	}
	protected void setName(){

	}
	protected void setName(){

	}
	protected void setName(){

	}
	protected void setName(){

	}
	protected void setName(){

	}
	protected void setName(){

	}

}