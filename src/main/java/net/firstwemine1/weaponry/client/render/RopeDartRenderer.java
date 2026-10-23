package net.firstwemine1.weaponry.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.firstwemine1.weaponry.entity.RopeDart;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
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
	public void render(State state, PoseStack poseStack, MultiBufferSource buffers, int light) {
		if (!state.item.isEmpty() && !state.tethered) {
			poseStack.pushPose();
			poseStack.mulPose(Axis.YP.rotationDegrees(state.yRot - 90.0f));
			poseStack.mulPose(Axis.ZP.rotationDegrees(state.xRot));
			poseStack.scale(DART_SCALE, DART_SCALE, DART_SCALE);
			poseStack.mulPose(Axis.ZP.rotationDegrees(TEXTURE_TILT));
			state.item.render(poseStack, buffers, light, OverlayTexture.NO_OVERLAY);
			poseStack.popPose();
		}

		if (state.rope != null) {
			Vec3 rope = state.rope;
			Vec3 normal = rope.lengthSqr() > 1.0e-6 ? rope.normalize() : new Vec3(0, 1, 0);
			float sag = state.anchored || state.tethered ? 0 : SAG;
			VertexConsumer consumer = buffers.getBuffer(RenderType.lineStrip());
			PoseStack.Pose pose = poseStack.last();
			for (int i = 0; i <= SEGMENTS; i++) {
				float t = (float) i / SEGMENTS;
				float x = (float) rope.x * t;
				float y = (float) rope.y * t - sag * 4 * t * (1 - t);
				float z = (float) rope.z * t;
				consumer.addVertex(pose, x, y, z).setColor(ROPE_COLOR).setNormal(pose, (float) normal.x, (float) normal.y, (float) normal.z);
			}
		}
		super.render(state, poseStack, buffers, light);
	}

	/** The hand holding the rope dart: in first person, near the bottom corner of the view. */
	private Vec3 handPos(Player player, float partialTick) {
		int side = player.getMainArm() == HumanoidArm.RIGHT ? 1 : -1;
		if (!RopeDart.isRopeDart(player.getMainHandItem())) {
			side = -side;
		}
		float swing = Mth.sin(Mth.sqrt(player.getAttackAnim(partialTick)) * Mth.PI);
		if (entityRenderDispatcher.options.getCameraType().isFirstPerson() && player == Minecraft.getInstance().player) {
			double fovScale = 960.0 / entityRenderDispatcher.options.fov().get().intValue();
			Vec3 offset = entityRenderDispatcher.camera.getNearPlane().getPointOnPlane(side * 0.525f, -0.1f)
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
