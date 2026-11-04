package net.firstwemine1.weaponry.crossbow;

import java.util.List;

/** What the mod needs to know about either crossbow: its registry id, name and tooltip lines. */
public interface WeaponCrossbow {
	String id();

	String displayName();

	/** English tooltip lines, each starting with the ability's name; datagen turns them into lang keys. */
	List<String> description();
}
