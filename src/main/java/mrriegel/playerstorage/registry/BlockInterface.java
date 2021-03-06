package mrriegel.playerstorage.registry;

import mrriegel.limelib.block.CommonBlockContainer;
import mrriegel.limelib.util.GlobalBlockPos;
import mrriegel.playerstorage.ExInventory;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class BlockInterface extends CommonBlockContainer<TileInterface> {

	public BlockInterface() {
		super(Material.IRON, "interface");
		setHardness(2.8f);
		setCreativeTab(CreativeTabs.MISC);
	}

	@Override
	protected Class<? extends TileInterface> getTile() {
		return TileInterface.class;
	}

	@Override
	public void onBlockPlacedBy(World worldIn, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
		super.onBlockPlacedBy(worldIn, pos, state, placer, stack);
		TileEntity t;
		if ((t = worldIn.getTileEntity(pos)) instanceof TileInterface && placer instanceof EntityPlayer) {
			((TileInterface) t).setPlayer((EntityPlayer) placer);
			((TileInterface) t).setOn(true);
			ExInventory.getInventory((EntityPlayer) placer).tiles.add(GlobalBlockPos.fromTile(t));
		}
	}

}
