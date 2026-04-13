package com.nhom3.shared.model.Item;

public class Electronics extends Item{
	public Electronics(String id, String iName, String info, double startPrice){
		super("Electronics-" + id, iName, info, startPrice);
		type = "Electronics";
	}


}