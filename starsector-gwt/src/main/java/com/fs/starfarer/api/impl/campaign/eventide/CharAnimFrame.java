package com.fs.starfarer.api.impl.campaign.eventide;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.util.vector.Vector2f;

public class CharAnimFrame  {
	public float dur;
	public float tx, ty, tw, th;
	public float width, height;
	public Vector2f move = new Vector2f();
	
	public List<HitArea> hittableArea = new ArrayList<HitArea>();
	public List<HitArea> attackArea = new ArrayList<HitArea>();
	public List<HitArea> blockArea = new ArrayList<HitArea>();
	
	public List<String> soundIds = new ArrayList<String>();
	public boolean attackCanActuallyHit = true;
	
	public int hitDamage = 1;
	
	public void setHittable(float x, float w) {
		HitArea area = new HitArea();
		area.y = -height/2f;
		area.x = x;
		area.h = height;
		area.w = w;
		hittableArea.add(area);
	}
	
	public void setAttack(float x, float w) {
		HitArea area = new HitArea();
		area.y = -height/2f;
		area.x = x;
		area.h = height;
		area.w = w;
		attackArea.add(area);
	}
	
	public void setBlock(float x, float w) {
		HitArea area = new HitArea();
		area.y = -height/2f;
		area.x = x;
		area.h = height;
		area.w = w;
		blockArea.add(area);
	}

	
	public CharAnimFrame clone() {
		return null; // GWT hack
	}
	
	
}




