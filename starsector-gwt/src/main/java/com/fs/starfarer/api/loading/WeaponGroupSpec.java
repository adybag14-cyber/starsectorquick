package com.fs.starfarer.api.loading;

import java.util.ArrayList;
import java.util.List;

public class WeaponGroupSpec  {

	private WeaponGroupType type = WeaponGroupType.LINKED;
	private boolean autofireOnByDefault = false;
	//private boolean isModuleGroup = false;
	
	private List<String> slots = new ArrayList<String>(); // list of slot ids

	
	public WeaponGroupSpec clone() {
		return null; // GWT hack
	}

	public WeaponGroupSpec() {
	}

	public WeaponGroupSpec(WeaponGroupType type) {
		this.type = type;
	}

	public WeaponGroupType getType() {
		return type;
	}

	public void setType(WeaponGroupType type) {
		this.type = type;
	}

	public List<String> getSlots() {
		return slots;
	}
	
	public void addSlot(String slotId) {
		slots.add(slotId);
	}
	
	public void removeSlot(String slotId) {
		slots.remove(slotId);
	}

	public boolean isAutofireOnByDefault() {
		return autofireOnByDefault;
	}

	public void setAutofireOnByDefault(boolean autofireOnByDefault) {
		this.autofireOnByDefault = autofireOnByDefault;
	}

//	public boolean isModuleGroup() {
//		return isModuleGroup;
//	}
//
//	public void setModuleGroup(boolean isModuleGroup) {
//		this.isModuleGroup = isModuleGroup;
//	}
	

}




