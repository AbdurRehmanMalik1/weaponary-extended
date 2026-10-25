package net.firstwemine1.weaponry.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;
import net.firstwemine1.weaponry.Weaponry;
import net.firstwemine1.weaponry.entity.RopeDart;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Draws the dart on its own ({@link #HEAD_MODEL}, the icon without its rope coil) and the rope back
 * to the thrower's hand. The hand position follows vanilla's fishing line ({@code FishingHookRenderer}).
 */
public class RopeDartRenderer extends EntityRenderer<RopeDart> {
	/** {@code models/item/rope_dart_head.json}: not an item, loaded by {@code WeaponryClient}. */
	public static final ModelResourceLocation HEAD_MODEL = new ModelResourceLocation(Weaponry.id("rope_dart_head"), "inventory");
	private static final float TEXTURE_TILT = -45.0f;
	private static final float DART_SCALE = 0.6f;
	private static final int SEGMENTS = 16;
	/** Cream rope, like the texture. */
	private static final int ROPE_COLOR = 0xFFD9C8A0;
	/** How far a slack rope sags in the middle. */
	private static final float SAG = 0.25f;
	private static final float DEG_TO_RAD = (float) (Math.PI / 180.0);

	private final ItemRenderer itemRenderer;

	public RopeDartRenderer(EntityRenderDispatcher dispatcher) {
		super(dispatcher);
		this.itemRenderer = Minecraft.getInstance().getItemRenderer();
	}

	@Override
	public void render(RopeDart entity, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light) {
		ItemStack stack = entity.getItem();
		if (!stack.isEmpty() && !entity.isTethered()) {
			poseStack.pushPose();
			poseStack.mulPose(Vector3f.YP.rotationDegrees(Mth.lerp(partialTick, entity.yRotO, entity.yRot) - 90.0f));
			poseStack.mulPose(Vector3f.ZP.rotationDegrees(Mth.lerp(partialTick, entity.xRotO, entity.xRot)));
			poseStack.scale(DART_SCALE, DART_SCALE, DART_SCALE);
			poseStack.mulPose(Vector3f.ZP.rotationDegrees(TEXTURE_TILT));
			// The rope dart's stack still picks the render type and the enchantment glint.
			BakedModel head = Minecraft.getInstance().getModelManager().getModel(HEAD_MODEL);
			itemRenderer.render(stack, ItemTransforms.TransformType.NONE, false, poseStack, buffers, light,
					OverlayTexture.NO_OVERLAY, head);
			poseStack.popPose();
		}

		if (entity.getOwner() instanceof Player) {
			Player owner = (Player) entity.getOwner();
			Vec3 hand = handPos(owner, partialTick);
			Vec3 dart = new Vec3(Mth.lerp(partialTick, entity.xo, entity.getX()),
					Mth.lerp(partialTick, entity.yo, entity.getY()),
					Mth.lerp(partialTick, entity.zo, entity.getZ()));
			Vec3 rope = hand.subtract(dart);
			float sag = entity.isAnchored() || entity.isTethered() ? 0 : SAG;
			// No line strip in this version: separate segments, one vertex pair each, like vanilla's fishing line.
			VertexConsumer consumer = buffers.getBuffer(RenderType.lines());
			Matrix4f pose = poseStack.last().pose();
			for (int i = 0; i < SEGMENTS; i++) {
				ropeVertex(consumer, pose, rope, sag, (float) i / SEGMENTS);
				ropeVertex(consumer, pose, rope, sag, (float) (i + 1) / SEGMENTS);
			}
		}
		super.render(entity, yaw, partialTick, poseStack, buffers, light);
	}

	private static void ropeVertex(VertexConsumer consumer, Matrix4f pose, Vec3 rope, float sag, float t) {
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
	private Vec3 handPos(Player player, float partialTick) {
		int side = player.getMainArm() == HumanoidArm.RIGHT ? 1 : -1;
		if (!RopeDart.isRopeDart(player.getMainHandItem())) {
			side = -side;
		}
		float swing = Mth.sin(Mth.sqrt(player.getAttackAnim(partialTick)) * (float) Math.PI);
		Vec3 feet = new Vec3(Mth.lerp(partialTick, player.xo, player.getX()),
				Mth.lerp(partialTick, player.yo, player.getY()),
				Mth.lerp(partialTick, player.zo, player.getZ()));
		if ((entityRenderDispatcher.options == null || entityRenderDispatcher.options.getCameraType().isFirstPerson())
				&& player == Minecraft.getInstance().player) {
			double fovScale = entityRenderDispatcher.options.fov / 100.0;
			Vec3 offset = new Vec3(side * -0.36 * fovScale, -0.045 * fovScale, 0.4)
					.xRot(-Mth.lerp(partialTick, player.xRotO, player.xRot) * DEG_TO_RAD)
					.yRot(-Mth.lerp(partialTick, player.yRotO, player.yRot) * DEG_TO_RAD)
					.yRot(swing * 0.5f)
					.xRot(-swing * 0.7f);
			return feet.add(offset).add(0, player.getEyeHeight(), 0);
		}
		float bodyYaw = Mth.lerp(partialTick, player.yBodyRotO, player.yBodyRot) * DEG_TO_RAD;
		double sin = Mth.sin(bodyYaw);
		double cos = Mth.cos(bodyYaw);
		double sideways = side * 0.35;
		float crouch = player.isCrouching() ? -0.1875f : 0.0f;
		return feet.add(-cos * sideways - sin * 0.8, player.getEyeHeight() - 0.45 + crouch, -sin * sideways + cos * 0.8);
	}

	@Override
	public ResourceLocation getTextureLocation(RopeDart entity) {
		return InventoryMenu.BLOCK_ATLAS;
	}
}
