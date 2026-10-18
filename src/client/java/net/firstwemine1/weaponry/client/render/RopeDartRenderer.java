package net.firstwemine1.weaponry.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.firstwemine1.weaponry.entity.RopeDart;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;

/**
 * Draws the dart's item model and the rope back to the thrower's hand. The hand position follows
 * vanilla's fishing line ({@code FishingHookRenderer}).
 */
public class RopeDartRenderer extends EntityRenderer<RopeDart, RopeDartRenderer.State> {
	private static final float TEXTURE_TILT = -45.0f;
	private static final float DART_SCALE = 0.6f;
	private static final int SEGMENTS = 16;
	/** Cream rope, like the texture. */
	private static final int ROPE_COLOR = 0xFFD9C8A0;
	/** How far a slack rope sags in the middle. */
	private static final float SAG = 0.25f;

	private final ItemModelResolver itemModelResolver;

	public RopeDartRenderer(EntityRendererProvider.Context context) {
		super(context);
		this.itemModelResolver = context.getItemModelResolver();
	}

	public static class State extends EntityRenderState {
		public final ItemStackRenderState item = new ItemStackRenderState();
		public float yRot;
		public float xRot;
		public boolean tethered;
		public boolean anchored;
		/** From the dart to the thrower's hand, or {@code null} without a thrower. */
		public Vec3 rope;
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(RopeDart entity, State state, float partialTick) {
		super.extractRenderState(entity, state, partialTick);
		itemModelResolver.updateForNonLiving(state.item, entity.getItem(), ItemDisplayContext.NONE, entity);
		state.yRot = entity.getYRot(partialTick);
		state.xRot = entity.getXRot(partialTick);
		state.tethered = entity.isTethered();
		state.anchored = entity.isAnchored();
		state.rope = entity.getOwner() instanceof Player owner
				? handPos(owner, partialTick).subtract(entity.getPosition(partialTick))
				: null;
	}

	@Override
	public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		if (!state.item.isEmpty() && !state.tethered) {
			poseStack.pushPose();
			poseStack.rotateDegrees(Axis.YP, state.yRot - 90.0f);
			poseStack.rotateDegrees(Axis.ZP, state.xRot);
			poseStack.scale(DART_SCALE, DART_SCALE, DART_SCALE);
			poseStack.rotateDegrees(Axis.ZP, TEXTURE_TILT);
			state.item.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
			poseStack.popPose();
		}

		if (state.rope != null) {
			Vec3 rope = state.rope;
			Vec3 normal = rope.lengthSqr() > 1.0e-6 ? rope.normalize() : new Vec3(0, 1, 0);
			float sag = state.anchored || state.tethered ? 0 : SAG;
			float width = Minecraft.getInstance().gameRenderer.gameRenderState().windowRenderState.appropriateLineWidth;
			// Line segments, one vertex pair each, like vanilla's fishing line.
			collector.submitCustomGeometry(poseStack, RenderTypes.lines(), (pose, consumer) -> {
				for (int i = 0; i < SEGMENTS; i++) {
					for (float t : new float[] {(float) i / SEGMENTS, (float) (i + 1) / SEGMENTS}) {
						float x = (float) rope.x * t;
						float y = (float) rope.y * t - sag * 4 * t * (1 - t);
						float z = (float) rope.z * t;
						consumer.addVertex(pose, x, y, z).setColor(ROPE_COLOR)
								.setNormal(pose, (float) normal.x, (float) normal.y, (float) normal.z).setLineWidth(width);
					}
				}
			});
		}
		super.submit(state, poseStack, collector, camera);
	}

	/** The hand holding the rope dart: in first person, near the bottom corner of the view. */
	private Vec3 handPos(Player player, float partialTick) {
		int side = player.getMainArm() == HumanoidArm.RIGHT ? 1 : -1;
		if (!RopeDart.isRopeDart(player.getMainHandItem())) {
			side = -side;
		}
		float swing = Mth.sin(Mth.sqrt(player.getSwingAnimation(partialTick)) * Mth.PI);
		if (entityRenderDispatcher.options.getCameraType().isFirstPerson() && player == Minecraft.getInstance().player) {
			float fov = entityRenderDispatcher.options.fov().get().intValue();
			double fovScale = 960.0 / fov;
			Vec3 offset = entityRenderDispatcher.camera.getNearPlane(fov).getPointOnPlane(side * 0.525f, -0.1f)
					.scale(fovScale).yRot(swing * 0.5f).xRot(-swing * 0.7f);
			return player.getEyePosition(partialTick).add(offset);
		}
		float bodyYaw = Mth.lerp(partialTick, player.yBodyRotO, player.yBodyRot) * Mth.DEG_TO_RAD;
		double sin = Mth.sin(bodyYaw);
		double cos = Mth.cos(bodyYaw);
		float scale = player.getScale();
		double sideways = side * 0.35 * scale;
		double forward = 0.8 * scale;
		float crouch = player.isCrouching() ? -0.1875f : 0.0f;
		return player.getEyePosition(partialTick).add(-cos * sideways - sin * forward, crouch - 0.45 * scale, -sin * sideways + cos * forward);
	}
}
