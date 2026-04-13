package com.nhom3.shared.model.Item;

public class Art extends Item{
	public Art(String id, String iName, String info, double startPrice){
		super("Art-" + id, iName, info, startPrice);
		type = "Art";
	}


}