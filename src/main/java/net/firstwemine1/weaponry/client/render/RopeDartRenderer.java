package net.firstwemine1.weaponry.client.render;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3f;
import net.firstwemine1.weaponry.entity.RopeDart;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.HandSide;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.container.PlayerContainer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.vector.Vector3d;

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
	private static final float DEG_TO_RAD = (float) (Math.PI / 180.0);

	private final ItemRenderer itemRenderer;

	public RopeDartRenderer(EntityRendererManager dispatcher) {
		super(dispatcher);
		this.itemRenderer = Minecraft.getInstance().getItemRenderer();
	}

	@Override
	public void render(RopeDart entity, float yaw, float partialTick, MatrixStack poseStack, IRenderTypeBuffer buffers, int light) {
		ItemStack stack = entity.getItem();
		if (!stack.isEmpty() && !entity.isTethered()) {
			poseStack.pushPose();
			poseStack.mulPose(Vector3f.YP.rotationDegrees(MathHelper.lerp(partialTick, entity.yRotO, entity.yRot) - 90.0f));
			poseStack.mulPose(Vector3f.ZP.rotationDegrees(MathHelper.lerp(partialTick, entity.xRotO, entity.xRot)));
			poseStack.scale(DART_SCALE, DART_SCALE, DART_SCALE);
			poseStack.mulPose(Vector3f.ZP.rotationDegrees(TEXTURE_TILT));
			itemRenderer.renderStatic(stack, ItemCameraTransforms.TransformType.NONE, light, OverlayTexture.NO_OVERLAY,
					poseStack, buffers);
			poseStack.popPose();
		}

		if (entity.getOwner() instanceof PlayerEntity) {
			PlayerEntity owner = (PlayerEntity) entity.getOwner();
			Vector3d hand = handPos(owner, partialTick);
			Vector3d dart = new Vector3d(MathHelper.lerp(partialTick, entity.xo, entity.getX()),
					MathHelper.lerp(partialTick, entity.yo, entity.getY()),
					MathHelper.lerp(partialTick, entity.zo, entity.getZ()));
			Vector3d rope = hand.subtract(dart);
			float sag = entity.isAnchored() || entity.isTethered() ? 0 : SAG;
			// No line strip in this version: separate segments, one vertex pair each, like vanilla's fishing line.
			IVertexBuilder consumer = buffers.getBuffer(RenderType.lines());
			Matrix4f pose = poseStack.last().pose();
			for (int i = 0; i < SEGMENTS; i++) {
				ropeVertex(consumer, pose, rope, sag, (float) i / SEGMENTS);
				ropeVertex(consumer, pose, rope, sag, (float) (i + 1) / SEGMENTS);
			}
		}
		super.render(entity, yaw, partialTick, poseStack, buffers, light);
	}

	private static void ropeVertex(IVertexBuilder consumer, Matrix4f pose, Vector3d rope, float sag, float t) {
		float x = (float) rope.x * t;
		float y = (float) rope.y * t - sag * 4 * t * (1 - t);
		float z = (float) rope.z * t;
		consumer.vertex(pose, x, y, z)
				.color(ROPE_COLOR >> 16 & 0xFF, ROPE_COLOR >> 8 & 0xFF, ROPE_COLOR & 0xFF, ROPE_COLOR >>> 24)
				.endVertex();
	}

	/**
	 * The hand holding the rope dart: in first person, near the bottom corner of the view. This
	 * version's camera has no near plane, so the first-person offset is vanilla 1.16.5's own.
	 */
	private Vector3d handPos(PlayerEntity player, float partialTick) {
		int side = player.getMainArm() == HandSide.RIGHT ? 1 : -1;
		if (!RopeDart.isRopeDart(player.getMainHandItem())) {
			side = -side;
		}
		float swing = MathHelper.sin(MathHelper.sqrt(player.getAttackAnim(partialTick)) * (float) Math.PI);
		Vector3d feet = new Vector3d(MathHelper.lerp(partialTick, player.xo, player.getX()),
				MathHelper.lerp(partialTick, player.yo, player.getY()),
				MathHelper.lerp(partialTick, player.zo, player.getZ()));
		if ((entityRenderDispatcher.options == null || entityRenderDispatcher.options.getCameraType().isFirstPerson())
				&& player == Minecraft.getInstance().player) {
			double fovScale = entityRenderDispatcher.options.fov / 100.0;
			Vector3d offset = new Vector3d(side * -0.36 * fovScale, -0.045 * fovScale, 0.4)
					.xRot(-MathHelper.lerp(partialTick, player.xRotO, player.xRot) * DEG_TO_RAD)
					.yRot(-MathHelper.lerp(partialTick, player.yRotO, player.yRot) * DEG_TO_RAD)
					.yRot(swing * 0.5f)
					.xRot(-swing * 0.7f);
			return feet.add(offset).add(0, player.getEyeHeight(), 0);
		}
		float bodyYaw = MathHelper.lerp(partialTick, player.yBodyRotO, player.yBodyRot) * DEG_TO_RAD;
		double sin = MathHelper.sin(bodyYaw);
		double cos = MathHelper.cos(bodyYaw);
		double sideways = side * 0.35;
		float crouch = player.isCrouching() ? -0.1875f : 0.0f;
		return feet.add(-cos * sideways - sin * 0.8, player.getEyeHeight() - 0.45 + crouch, -sin * sideways + cos * 0.8);
	}

	@Override
	public ResourceLocation getTextureLocation(RopeDart entity) {
		return PlayerContainer.BLOCK_ATLAS;
	}
}
