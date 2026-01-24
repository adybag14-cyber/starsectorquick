package com.fs.starfarer.api.impl.campaign.eventide;

public class HitArea  {
	public float x, y, w, h;
	
	public HitArea clone() {
		return null; // GWT hack
	}
	
	public HitArea getAdjustedForAction(AnimAction action) {
		HitArea copy = new HitArea();
		float sign = action.actor.facing;
		
		copy.x = action.actor.loc.x + x * action.anim.scale * sign;
		copy.y = action.actor.loc.y + y * action.anim.scale;
		copy.h = h * action.anim.scale;
		if (sign > 0) {
			copy.w = w * action.anim.scale;
		} else {
			copy.x = action.actor.loc.x + x * action.anim.scale * sign - w * action.anim.scale;
			copy.w = w * action.anim.scale;
		}
			
		
		return copy;
		
	}
	
	public boolean intersects(HitArea other) {
		if (x > other.x + other.w) return false;
		if (x + w < other.x) return false;
		if (y > other.y + other.h) return false;
		if (y + h < other.y) return false;
		return true;
	}
}




