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
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Draws the dart's item model and the rope back to the thrower's hand. The hand position follows
 * vanilla's fishing line ({@code FishingHookRenderer}).
 */
public class RopeDartRenderer extends EntityRenderer<RopeDart> {
	private static final float TEXTURE_TILT = -45.0f;
	private static final float DART_SCALE = 0.6f;
	private static final int SEGMENTS = 16;
	/** Cream rope, like the texture. */
	private static final int ROPE_COLOR = 0xFFD9C8A0;
	/** How far a slack rope sags in the middle. */
	private static final float SAG = 0.25f;

	private final ItemRenderer itemRenderer;

	public RopeDartRenderer(EntityRendererProvider.Context context) {
		super(context);
		this.itemRenderer = context.getItemRenderer();
	}

	@Override
	public void render(RopeDart entity, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light) {
		ItemStack stack = entity.getItem();
		if (!stack.isEmpty() && !entity.isTethered()) {
			poseStack.pushPose();
			poseStack.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partialTick, entity.yRotO, entity.getYRot()) - 90.0f));
			poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(partialTick, entity.xRotO, entity.getXRot())));
			poseStack.scale(DART_SCALE, DART_SCALE, DART_SCALE);
			poseStack.mulPose(Axis.ZP.rotationDegrees(TEXTURE_TILT));
			itemRenderer.renderStatic(stack, ItemDisplayContext.NONE, light, OverlayTexture.NO_OVERLAY,
					poseStack, buffers, entity.level(), entity.getId());
			poseStack.popPose();
		}

		if (entity.getOwner() instanceof Player owner) {
			Vec3 hand = handPos(owner, partialTick);
			Vec3 dart = entity.getPosition(partialTick);
			Vec3 rope = hand.subtract(dart);
			Vec3 normal = rope.lengthSqr() > 1.0e-6 ? rope.normalize() : new Vec3(0, 1, 0);
			float sag = entity.isAnchored() || entity.isTethered() ? 0 : SAG;
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
		super.render(entity, yaw, partialTick, poseStack, buffers, light);
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

	@Override
	public ResourceLocation getTextureLocation(RopeDart entity) {
		return InventoryMenu.BLOCK_ATLAS;
	}
}
