package com.nhom3.shared.model.Item;

public class Art extends Item{
	public Art(int id, String name, String info, double startPrice){
		super(id, name, info, startPrice);
		type = "Art";
	}


}