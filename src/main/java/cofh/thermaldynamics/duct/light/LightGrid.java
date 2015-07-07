package cofh.thermaldynamics.duct.light;

import cofh.core.CoFHProps;
import cofh.core.network.PacketCoFHBase;
import cofh.core.network.PacketHandler;
import cofh.lib.util.position.ChunkCoord;
import cofh.thermaldynamics.core.WorldGridList;
import cofh.thermaldynamics.multiblock.IMultiBlock;
import cofh.thermaldynamics.multiblock.MultiBlockGrid;
import com.google.common.collect.Iterables;
import cpw.mods.fml.common.FMLCommonHandler;

import java.util.HashSet;
import java.util.List;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.World;

public class LightGrid extends MultiBlockGrid {

	public HashSet<ChunkCoord> chunks;
	public boolean upToDate = false;

	public LightGrid(WorldGridList worldGrid) {

		super(worldGrid);
	}

	public LightGrid(World worldObj) {

		super(worldObj);
	}

	@Override
	public boolean canAddBlock(IMultiBlock aBlock) {

		return aBlock instanceof TileLightDuct;
	}

	@Override
	public void onMajorGridChange() {

		super.onMajorGridChange();
		upToDate = false;
		chunks = null;
	}

	@Override
	public void onMinorGridChange() {

		super.onMinorGridChange();
		upToDate = false;
	}

	@Override
	public void tickGrid() {

		super.tickGrid();

		if (upToDate && worldGrid.worldObj.getTotalWorldTime() % 160 != 0) {
            if (nextRedstoneLevel != -128)
                upToDate = false;
            return;
		}

		upToDate = nextRedstoneLevel == -128;

		boolean shouldBeLit;

        if(nextRedstoneLevel != -128){
            shouldBeLit = nextRedstoneLevel > 0;
        } else
            shouldBeLit = redstoneLevel > 0;


//        shouldBeLit = false;

        if (!shouldBeLit)
            for (Object object : Iterables.concat(nodeSet, idleSet)) {
                TileLightDuct lamp = (TileLightDuct) object;
                if (lamp.lit) {
                    shouldBeLit = true;
                    break;
                }
            }

		if (lit != shouldBeLit) {
			setLight(shouldBeLit);
		}
	}

	boolean lit = false;

	public void setLight(boolean lit) {

		this.lit = lit;

		if (chunks == null) {
			buildMap();
		}

		PacketCoFHBase packet = new PacketLight(lit, this);
		int dimension = worldGrid.worldObj.provider.dimensionId;
		for (EntityPlayerMP player : (List<EntityPlayerMP>) FMLCommonHandler.instance().getMinecraftServerInstance().getConfigurationManager().playerEntityList) {
			if (dimension == player.dimension) {
				for (ChunkCoord chunk : chunks) {
					double d4 = chunk.chunkX - player.posX;
					double d6 = chunk.chunkZ - player.posZ;

					if (d4 * d4 + d6 * d6 < CoFHProps.NETWORK_UPDATE_RANGE * CoFHProps.NETWORK_UPDATE_RANGE) {
						PacketHandler.sendTo(packet, player);
						break;
					}
				}
			}
		}

		for (Object block : Iterables.concat(nodeSet, idleSet)) {
			((TileLightDuct) block).checkLight();
		}
	}

	public void buildMap() {

		chunks = new HashSet<ChunkCoord>();
		for (IMultiBlock iMultiBlock : Iterables.concat(nodeSet, idleSet)) {
			buildMapEntry(iMultiBlock);
		}
	}

	private void buildMapEntry(IMultiBlock iMultiBlock) {

		chunks.add(new ChunkCoord(iMultiBlock.x() + 8, iMultiBlock.z() + 8));
	}

}
