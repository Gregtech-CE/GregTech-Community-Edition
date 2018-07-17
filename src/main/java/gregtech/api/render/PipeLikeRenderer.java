package gregtech.api.render;

import codechicken.lib.render.BlockRenderer;
import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.block.ICCBlockRenderer;
import codechicken.lib.render.item.IItemRenderer;
import codechicken.lib.render.particle.IModelParticleProvider;
import codechicken.lib.render.pipeline.ColourMultiplier;
import codechicken.lib.render.pipeline.IVertexOperation;
import codechicken.lib.texture.TextureUtils;
import codechicken.lib.vec.Cuboid6;
import codechicken.lib.vec.Translation;
import codechicken.lib.vec.Vector3;
import codechicken.lib.vec.uv.IconTransformation;
import gregtech.api.pipelike.*;
import gregtech.api.unification.material.MaterialIconSet;
import gregtech.api.unification.material.type.Material;
import gregtech.api.util.GTUtility;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.model.IModelState;
import net.minecraftforge.common.model.TRSRTransformation;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nonnull;
import javax.vecmath.Matrix4f;
import java.util.Collections;
import java.util.Set;

import static gregtech.api.render.MetaTileEntityRenderer.BLOCK_TRANSFORMS;

@SuppressWarnings({"unchecked"})
public abstract class PipeLikeRenderer<Q extends Enum<Q> & IBaseProperty & IStringSerializable> implements ICCBlockRenderer, IItemRenderer, IModelParticleProvider {

    public static int MASK_FORMAL_CONNECTION = 1;
    public static int MASK_RENDER_SIDE = 1 << 6;

    private PipeLikeObjectFactory<Q, ?, ?> factory;

    protected PipeLikeRenderer(PipeLikeObjectFactory<Q, ?, ?> factory) {
        this.factory = factory;
    }

    private static final int ITEM_RENDER_MASK = (MASK_RENDER_SIDE | MASK_FORMAL_CONNECTION) << EnumFacing.SOUTH.getIndex()
        | (MASK_RENDER_SIDE | MASK_FORMAL_CONNECTION) << EnumFacing.NORTH.getIndex();

    @Override
    public void renderItem(ItemStack stack, ItemCameraTransforms.TransformType transformType) {
        GlStateManager.enableBlend();
        CCRenderState renderState = CCRenderState.instance();
        GlStateManager.enableBlend();
        renderState.reset();
        renderState.startDrawing(GL11.GL_QUADS, DefaultVertexFormats.ITEM);
        BlockPipeLike<Q, ?, ?> block = (BlockPipeLike<Q, ?, ?>) ((ItemBlock) stack.getItem()).getBlock();
        Q baseProperty = block.getBaseProperties(stack);
        Material material = block.material;
        renderBlock(material, baseProperty, factory.getDefaultColor(), renderState, new IVertexOperation[0], ITEM_RENDER_MASK);
        renderState.draw();
        GlStateManager.disableBlend();
    }

    @Override
    public boolean renderBlock(IBlockAccess world, BlockPos pos, IBlockState state, BufferBuilder buffer) {
        CCRenderState renderState = CCRenderState.instance();
        renderState.reset();
        renderState.bind(buffer);
        IVertexOperation[] pipeline = {new Translation(pos)};
        renderState.setBrightness(world, pos);

        BlockPipeLike<Q, ?, ?> block = (BlockPipeLike<Q, ?, ?>) state.getBlock();
        ITilePipeLike<Q, ?> tile = factory.getTile(world, pos);
        if(tile == null) return false;
        int paintingColor = tile.getColor();
        int connectedSidesMask = factory.getRenderMask(tile, world, pos);

        Q baseProperty = state.getValue(block.getBaseProperty());
        Material material = block.material;

        renderBlock(material, baseProperty, paintingColor, renderState, pipeline, connectedSidesMask);
        return true;
    }

    @Override
    public void renderBrightness(IBlockState state, float brightness) {
        renderItem(new ItemStack(state.getBlock(), 1, state.getBlock().getMetaFromState(state)), ItemCameraTransforms.TransformType.FIXED);
    }

    @Override
    public void handleRenderBlockDamage(IBlockAccess world, BlockPos pos, IBlockState state, TextureAtlasSprite sprite, BufferBuilder buffer) {
        CCRenderState renderState = CCRenderState.instance();
        renderState.reset();
        renderState.bind(buffer);
        renderState.setPipeline(new Vector3(new Vec3d(pos)).translation(), new IconTransformation(sprite));
        ITilePipeLike<Q, ?> tile = factory.getTile(world, pos);
        if(tile == null) return;
        float thickness = tile.getBaseProperty().getThickness();
        int connectedSidesMask = factory.getRenderMask(tile, world, pos);
        Cuboid6 baseBox = PipeLikeObjectFactory.getSideBox(null, thickness);
        BlockRenderer.renderCuboid(renderState, baseBox, 0);
        for(EnumFacing renderSide : EnumFacing.VALUES) {
            if((connectedSidesMask & (MASK_FORMAL_CONNECTION << renderSide.getIndex())) > 0) {
                Cuboid6 sideBox = PipeLikeObjectFactory.getSideBox(renderSide, thickness);
                BlockRenderer.renderCuboid(renderState, sideBox, 0);
            }
        }
    }

    @Override
    public void registerTextures(TextureMap map) {
    }

    @Override
    public IModelState getTransforms() {
        return TRSRTransformation.identity();
    }

    @Override
    public Pair<? extends IBakedModel, Matrix4f> handlePerspective(ItemCameraTransforms.TransformType cameraTransformType) {
        if(BLOCK_TRANSFORMS.containsKey(cameraTransformType)) {
            return Pair.of(this, BLOCK_TRANSFORMS.get(cameraTransformType).getMatrix());
        }
        return Pair.of(this, null);
    }

    @Override
    public TextureAtlasSprite getParticleTexture() {
        return TextureUtils.getMissingSprite();
    }

    @Override
    public boolean isBuiltInRenderer() {
        return true;
    }

    @Override
    public boolean isAmbientOcclusion() {
        return true;
    }

    @Override
    public boolean isGui3d() {
        return true;
    }

    @Override
    public Set<TextureAtlasSprite> getHitEffects(@Nonnull RayTraceResult traceResult, IBlockState state, IBlockAccess world, BlockPos pos) {
        return getDestroyEffects(state, world, pos);
    }

    @Override
    public Set<TextureAtlasSprite> getDestroyEffects(IBlockState state, IBlockAccess world, BlockPos pos) {
        BlockPipeLike<Q, ?, ?> block = (BlockPipeLike<Q, ?, ?>) state.getBlock();
        return Collections.singleton(state.getValue(block.getBaseProperty()).isColorable() ? getDullTexture() : getBaseTexture(block.material.materialIconSet));
    }

    public void renderBlock(Material material, Q baseProperty, int tileColor, CCRenderState state, IVertexOperation[] pipeline, int renderMask) {
        MaterialIconSet iconSet = material.materialIconSet;
        int materialColor = GTUtility.convertRGBtoOpaqueRGBA_CL(material.materialRGB);
        float thickness = baseProperty.getThickness();

        IVertexOperation[] bases = ArrayUtils.addAll(pipeline, new IconTransformation(getBaseTexture(iconSet)), new ColourMultiplier(materialColor));
        IVertexOperation[] overlays = bases;
        IVertexOperation[] dullSides = bases;

        if(baseProperty.isColorable()) {
            int insulationColor = GTUtility.convertRGBtoOpaqueRGBA_CL(tileColor);
            ColourMultiplier multiplier = new ColourMultiplier(insulationColor);
            dullSides = ArrayUtils.addAll(pipeline, new IconTransformation(getDullTexture()), multiplier);
            overlays = ArrayUtils.addAll(pipeline, new IconTransformation(getOverlayTexture(baseProperty)), multiplier);
        }

        Cuboid6 cuboid6 = PipeLikeObjectFactory.getSideBox(null, thickness);
        for(EnumFacing renderedSide : EnumFacing.VALUES) {
            if((renderMask & MASK_FORMAL_CONNECTION << renderedSide.getIndex()) == 0) {
                int oppositeIndex = renderedSide.getOpposite().getIndex();
                if((renderMask & MASK_FORMAL_CONNECTION << oppositeIndex) > 0 && (renderMask & ~(MASK_FORMAL_CONNECTION << oppositeIndex)) == 0) {
                    //if there is something on opposite side, render overlay + base
                    renderSide(state, bases, renderedSide, cuboid6);
                    renderSide(state, overlays, renderedSide, cuboid6);
                } else {
                    renderSide(state, dullSides, renderedSide, cuboid6);
                }
            }
        }

        for (EnumFacing side : EnumFacing.VALUES) renderSideBox(renderMask, state, dullSides, bases, overlays, side, thickness);
    }

    private static void renderSideBox(int renderMask, CCRenderState renderState, IVertexOperation[] pipeline, IVertexOperation[] bases, IVertexOperation[] overlays, EnumFacing side, float thickness) {
        if((renderMask & MASK_FORMAL_CONNECTION << side.getIndex()) > 0) {
            boolean renderFrontSide = (renderMask & MASK_RENDER_SIDE << side.getIndex()) > 0;
            Cuboid6 cuboid6 = PipeLikeObjectFactory.getSideBox(side, thickness);
            for(EnumFacing renderedSide : EnumFacing.VALUES) {
                if(renderedSide == side) {
                    if(renderFrontSide) {
                        renderSide(renderState, bases, renderedSide, cuboid6);
                        renderSide(renderState, overlays, renderedSide, cuboid6);
                    }
                } else if(renderedSide != side.getOpposite()) {
                    renderSide(renderState, pipeline, renderedSide, cuboid6);
                }
            }
        }
    }

    private static ThreadLocal<BlockRenderer.BlockFace> blockFaces = ThreadLocal.withInitial(BlockRenderer.BlockFace::new);
    private static void renderSide(CCRenderState renderState, IVertexOperation[] pipeline, EnumFacing side, Cuboid6 cuboid6) {
        BlockRenderer.BlockFace blockFace = blockFaces.get();
        blockFace.loadCuboidFace(cuboid6, side.getIndex());
        renderState.setPipeline(blockFace, 0, blockFace.verts.length, pipeline);
        renderState.render();
    }

    protected abstract TextureAtlasSprite getBaseTexture(MaterialIconSet materialIconSet);
    protected abstract TextureAtlasSprite getDullTexture();
    protected abstract TextureAtlasSprite getOverlayTexture(Q baseProperty);

    public abstract EnumBlockRenderType getRenderType();

}
