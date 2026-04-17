package net.minecraft.client.renderer.entity;

import com.google.common.collect.Sets;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.SheetedDecalTextureGenerator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexMultiConsumer;
import com.mojang.math.MatrixUtil;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.item.ItemColors;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.ItemModelShaper;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HalfTransparentBlock;
import net.minecraft.world.level.block.StainedGlassPaneBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ItemRenderer implements ResourceManagerReloadListener {
   public static final ResourceLocation f_273897_ = new ResourceLocation("textures/misc/enchanted_glint_entity.png");
   public static final ResourceLocation f_273833_ = new ResourceLocation("textures/misc/enchanted_glint_item.png");
   private static final Set<Item> f_115094_ = Sets.newHashSet(Items.f_41852_);
   public static final int f_174221_ = 8;
   public static final int f_174222_ = 8;
   public static final int f_174218_ = 200;
   public static final float f_174219_ = 0.5F;
   public static final float f_174220_ = 0.75F;
   public static final float f_256734_ = 0.0078125F;
   private static final ModelResourceLocation f_244324_ = ModelResourceLocation.m_245263_("trident", "inventory");
   public static final ModelResourceLocation f_244055_ = ModelResourceLocation.m_245263_("trident_in_hand", "inventory");
   private static final ModelResourceLocation f_244537_ = ModelResourceLocation.m_245263_("spyglass", "inventory");
   public static final ModelResourceLocation f_243706_ = ModelResourceLocation.m_245263_("spyglass_in_hand", "inventory");
   private final Minecraft f_265848_;
   private final ItemModelShaper f_115095_;
   private final TextureManager f_115096_;
   private final ItemColors f_115097_;
   private final BlockEntityWithoutLevelRenderer f_174223_;

   public ItemRenderer(Minecraft p_266926_, TextureManager p_266774_, ModelManager p_266850_, ItemColors p_267016_, BlockEntityWithoutLevelRenderer p_267049_) {
      this.f_265848_ = p_266926_;
      this.f_115096_ = p_266774_;
      this.f_115095_ = new ItemModelShaper(p_266850_);
      this.f_174223_ = p_267049_;

      for(Item item : BuiltInRegistries.f_257033_) {
         if (!f_115094_.contains(item)) {
            this.f_115095_.m_109396_(item, new ModelResourceLocation(BuiltInRegistries.f_257033_.m_7981_(item), "inventory"));
         }
      }

      this.f_115097_ = p_267016_;
   }

   public ItemModelShaper m_115103_() {
      return this.f_115095_;
   }

   public void m_115189_(BakedModel p_115190_, ItemStack p_115191_, int p_115192_, int p_115193_, PoseStack p_115194_, VertexConsumer p_115195_) {
      RandomSource randomsource = RandomSource.m_216327_();
      long i = 42L;

      for(Direction direction : Direction.values()) {
         randomsource.m_188584_(42L);
         this.m_115162_(p_115194_, p_115195_, p_115190_.m_213637_((BlockState)null, direction, randomsource), p_115191_, p_115192_, p_115193_);
      }

      randomsource.m_188584_(42L);
      this.m_115162_(p_115194_, p_115195_, p_115190_.m_213637_((BlockState)null, (Direction)null, randomsource), p_115191_, p_115192_, p_115193_);
   }

   public void m_115143_(ItemStack p_115144_, ItemDisplayContext p_270188_, boolean p_115146_, PoseStack p_115147_, MultiBufferSource p_115148_, int p_115149_, int p_115150_, BakedModel p_115151_) {
      if (!p_115144_.m_41619_()) {
         p_115147_.m_85836_();
         boolean flag = p_270188_ == ItemDisplayContext.GUI || p_270188_ == ItemDisplayContext.GROUND || p_270188_ == ItemDisplayContext.FIXED;
         if (flag) {
            if (p_115144_.m_150930_(Items.f_42713_)) {
               p_115151_ = this.f_115095_.m_109393_().m_119422_(f_244324_);
            } else if (p_115144_.m_150930_(Items.f_151059_)) {
               p_115151_ = this.f_115095_.m_109393_().m_119422_(f_244537_);
            }
         }

         p_115151_.m_7442_().m_269404_(p_270188_).m_111763_(p_115146_, p_115147_);
         p_115147_.m_252880_(-0.5F, -0.5F, -0.5F);
         if (!p_115151_.m_7521_() && (!p_115144_.m_150930_(Items.f_42713_) || flag)) {
            boolean flag1;
            if (p_270188_ != ItemDisplayContext.GUI && !p_270188_.m_269069_() && p_115144_.m_41720_() instanceof BlockItem) {
               Block block = ((BlockItem)p_115144_.m_41720_()).m_40614_();
               flag1 = !(block instanceof HalfTransparentBlock) && !(block instanceof StainedGlassPaneBlock);
            } else {
               flag1 = true;
            }

            RenderType rendertype = ItemBlockRenderTypes.m_109279_(p_115144_, flag1);
            VertexConsumer vertexconsumer;
            if (m_285827_(p_115144_) && p_115144_.m_41790_()) {
               p_115147_.m_85836_();
               PoseStack.Pose posestack$pose = p_115147_.m_85850_();
               if (p_270188_ == ItemDisplayContext.GUI) {
                  MatrixUtil.m_253023_(posestack$pose.m_252922_(), 0.5F);
               } else if (p_270188_.m_269069_()) {
                  MatrixUtil.m_253023_(posestack$pose.m_252922_(), 0.75F);
               }

               if (flag1) {
                  vertexconsumer = m_115207_(p_115148_, rendertype, posestack$pose);
               } else {
                  vertexconsumer = m_115180_(p_115148_, rendertype, posestack$pose);
               }

               p_115147_.m_85849_();
            } else if (flag1) {
               vertexconsumer = m_115222_(p_115148_, rendertype, true, p_115144_.m_41790_());
            } else {
               vertexconsumer = m_115211_(p_115148_, rendertype, true, p_115144_.m_41790_());
            }

            this.m_115189_(p_115151_, p_115144_, p_115149_, p_115150_, p_115147_, vertexconsumer);
         } else {
            this.f_174223_.m_108829_(p_115144_, p_270188_, p_115147_, p_115148_, p_115149_, p_115150_);
         }

         p_115147_.m_85849_();
      }
   }

   private static boolean m_285827_(ItemStack p_286353_) {
      return p_286353_.m_204117_(ItemTags.f_215866_) || p_286353_.m_150930_(Items.f_42524_);
   }

   public static VertexConsumer m_115184_(MultiBufferSource p_115185_, RenderType p_115186_, boolean p_115187_, boolean p_115188_) {
      return p_115188_ ? VertexMultiConsumer.m_86168_(p_115185_.m_6299_(p_115187_ ? RenderType.m_110481_() : RenderType.m_110484_()), p_115185_.m_6299_(p_115186_)) : p_115185_.m_6299_(p_115186_);
   }

   public static VertexConsumer m_115180_(MultiBufferSource p_115181_, RenderType p_115182_, PoseStack.Pose p_115183_) {
      return VertexMultiConsumer.m_86168_(new SheetedDecalTextureGenerator(p_115181_.m_6299_(RenderType.m_110490_()), p_115183_.m_252922_(), p_115183_.m_252943_(), 0.0078125F), p_115181_.m_6299_(p_115182_));
   }

   public static VertexConsumer m_115207_(MultiBufferSource p_115208_, RenderType p_115209_, PoseStack.Pose p_115210_) {
      return VertexMultiConsumer.m_86168_(new SheetedDecalTextureGenerator(p_115208_.m_6299_(RenderType.m_110493_()), p_115210_.m_252922_(), p_115210_.m_252943_(), 0.0078125F), p_115208_.m_6299_(p_115209_));
   }

   public static VertexConsumer m_115211_(MultiBufferSource p_115212_, RenderType p_115213_, boolean p_115214_, boolean p_115215_) {
      if (p_115215_) {
         return Minecraft.m_91085_() && p_115213_ == Sheets.m_110791_() ? VertexMultiConsumer.m_86168_(p_115212_.m_6299_(RenderType.m_110487_()), p_115212_.m_6299_(p_115213_)) : VertexMultiConsumer.m_86168_(p_115212_.m_6299_(p_115214_ ? RenderType.m_110490_() : RenderType.m_110496_()), p_115212_.m_6299_(p_115213_));
      } else {
         return p_115212_.m_6299_(p_115213_);
      }
   }

   public static VertexConsumer m_115222_(MultiBufferSource p_115223_, RenderType p_115224_, boolean p_115225_, boolean p_115226_) {
      return p_115226_ ? VertexMultiConsumer.m_86168_(p_115223_.m_6299_(p_115225_ ? RenderType.m_110493_() : RenderType.m_110499_()), p_115223_.m_6299_(p_115224_)) : p_115223_.m_6299_(p_115224_);
   }

   public void m_115162_(PoseStack p_115163_, VertexConsumer p_115164_, List<BakedQuad> p_115165_, ItemStack p_115166_, int p_115167_, int p_115168_) {
      boolean flag = !p_115166_.m_41619_();
      PoseStack.Pose posestack$pose = p_115163_.m_85850_();

      for(BakedQuad bakedquad : p_115165_) {
         int i = -1;
         if (flag && bakedquad.m_111304_()) {
            i = this.f_115097_.m_92676_(p_115166_, bakedquad.m_111305_());
         }

         float f = (float)(i >> 16 & 255) / 255.0F;
         float f1 = (float)(i >> 8 & 255) / 255.0F;
         float f2 = (float)(i & 255) / 255.0F;
         p_115164_.m_85987_(posestack$pose, bakedquad, f, f1, f2, p_115167_, p_115168_);
      }

   }

   public BakedModel m_174264_(ItemStack p_174265_, @Nullable Level p_174266_, @Nullable LivingEntity p_174267_, int p_174268_) {
      BakedModel bakedmodel;
      if (p_174265_.m_150930_(Items.f_42713_)) {
         bakedmodel = this.f_115095_.m_109393_().m_119422_(f_244055_);
      } else if (p_174265_.m_150930_(Items.f_151059_)) {
         bakedmodel = this.f_115095_.m_109393_().m_119422_(f_243706_);
      } else {
         bakedmodel = this.f_115095_.m_109406_(p_174265_);
      }

      ClientLevel clientlevel = p_174266_ instanceof ClientLevel ? (ClientLevel)p_174266_ : null;
      BakedModel bakedmodel1 = bakedmodel.m_7343_().m_173464_(bakedmodel, p_174265_, clientlevel, p_174267_, p_174268_);
      return bakedmodel1 == null ? this.f_115095_.m_109393_().m_119409_() : bakedmodel1;
   }

   public void m_269128_(ItemStack p_270761_, ItemDisplayContext p_270648_, int p_270410_, int p_270894_, PoseStack p_270430_, MultiBufferSource p_270457_, @Nullable Level p_270149_, int p_270509_) {
      this.m_269491_((LivingEntity)null, p_270761_, p_270648_, false, p_270430_, p_270457_, p_270149_, p_270410_, p_270894_, p_270509_);
   }

   public void m_269491_(@Nullable LivingEntity p_270101_, ItemStack p_270637_, ItemDisplayContext p_270437_, boolean p_270434_, PoseStack p_270230_, MultiBufferSource p_270411_, @Nullable Level p_270641_, int p_270595_, int p_270927_, int p_270845_) {
      if (!p_270637_.m_41619_()) {
         BakedModel bakedmodel = this.m_174264_(p_270637_, p_270641_, p_270101_, p_270845_);
         this.m_115143_(p_270637_, p_270437_, p_270434_, p_270230_, p_270411_, p_270595_, p_270927_, bakedmodel);
      }
   }

   public void m_6213_(ResourceManager p_115105_) {
      this.f_115095_.m_109403_();
   }
}
