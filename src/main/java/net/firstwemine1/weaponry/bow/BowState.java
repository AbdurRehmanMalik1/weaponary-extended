package net.firstwemine1.weaponry.bow;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;

/**
 * Per-shooter bow state that outlives one draw: when they last shot (for Quick Nock) and the head
 * start of the current draw. Client and server keep their own copies (different entity objects);
 * both run {@code use} and {@code releaseUsing}, so they agree. Synchronized because in single
 * player the client and server threads share this class.
 */
public final class BowState {
	private static final Map<Entity, Long> LAST_SHOT = Collections.synchronizedMap(new WeakHashMap<>());
	private static final Map<Entity, Integer> HEAD_START = Collections.synchronizedMap(new WeakHashMap<>());

	private BowState() {
	}

	public static void shot(LivingEntity shooter) {
		LAST_SHOT.put(shooter, shooter.level.getGameTime());
	}

	public static long ticksSinceShot(LivingEntity shooter) {
		Long last = LAST_SHOT.get(shooter);
		return last == null ? Long.MAX_VALUE : shooter.level.getGameTime() - last;
	}

	public static void startDraw(LivingEntity shooter, int headStart) {
		HEAD_START.put(shooter, headStart);
	}

	public static int headStart(LivingEntity shooter) {
		return HEAD_START.getOrDefault(shooter, 0);
	}
}
