package com.nhom3.Item;

public class Other extends Item{
	public Other(String id, String iName, String info, double startPrice){
		super("Other-" + id, iName, info, startPrice);
		type = "Other";
	}


}