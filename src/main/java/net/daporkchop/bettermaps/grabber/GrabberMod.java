package net.daporkchop.bettermaps.grabber;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.block.Block;
import net.minecraft.block.BlockBed;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;

@Mod(
        modid = GrabberMod.MOD_ID,
        name = GrabberMod.MOD_NAME,
        version = GrabberMod.VERSION
)
public class GrabberMod {

    public static final String MOD_ID = "bettermaps.grabber";
    public static final String MOD_NAME = "bma_colorGrabber";
    public static final String VERSION = "1.0-SNAPSHOT";

    /**
     * This is the instance of your mod as created by Forge. It will never be null.
     */
    @Mod.Instance(MOD_ID)
    public static GrabberMod INSTANCE;

    public Logger logger;

    /**
     * This is the first initialization event. Register tile entities here.
     * The registry events below will have fired prior to entry to this method.
     */
    @Mod.EventHandler
    public void preinit(FMLPreInitializationEvent event) {
        logger = event.getModLog();
    }

    /**
     * This is the second initialization event. Register custom recipes
     */
    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {

    }

    /**
     * This is the final initialization event. Register actions from other mods here
     */
    @Mod.EventHandler
    public void postinit(FMLPostInitializationEvent event) throws IOException {
        logger.info("Generating color config...");

        Field blockMapColor;
        try {
            blockMapColor = Block.class.getDeclaredField("blockMapColor");
        } catch (NoSuchFieldException e) {
            try {
                blockMapColor = Block.class.getDeclaredField("field_181083_K");
            } catch (NoSuchFieldException e1) {
                logger.fatal("Unable to locate blockMapColor field, will not continue!");
                return;
            }
        }
        blockMapColor.setAccessible(true);

        JsonObject object = new JsonObject();

        {
            JsonObject colorList = new JsonObject();
            for (MapColor color : MapColor.COLORS) {
                if (color == null) {
                    continue;
                }
                colorList.add(String.valueOf(color.colorIndex), serializeMapColor(color, color.colorIndex));
            }
            object.add("colors", colorList);
        }

        {
            JsonArray array = new JsonArray();
            for (Block block : Block.REGISTRY) {
                NonNullList<ItemStack> list = NonNullList.create();
                block.getSubBlocks(null, list);
                for (ItemStack stack : list) {
                    JsonObject toAdd = new JsonObject();
                    Block block1 = Block.getBlockFromItem(stack.getItem());
                    if (block1 instanceof BlockBed) {
                        //causes a nullpointer because it's retardedly coded
                        continue;
                    }
                    IBlockState state = block1.getStateFromMeta(stack.getMetadata());
                    toAdd.addProperty("registryName", block1.getRegistryName().toString());
                    toAdd.addProperty("id", Block.REGISTRY.getIDForObject(block1));
                    toAdd.addProperty("meta", stack.getMetadata());
                    toAdd.addProperty("color", block1.isOpaqueCube(state) ? String.valueOf(block1.getMapColor(state, null, BlockPos.ORIGIN).colorIndex) : "0");
                    toAdd.addProperty("localizedName", stack.getItem().getItemStackDisplayName(stack));
                    array.add(toAdd);
                }
            }
            object.add("blocks", array);
        }
        object.addProperty("mcVersion", MinecraftForge.MC_VERSION);

        File file = new File(".", "bmaColors.json");
        if (file.exists()) {
            file.delete();
        }
        file.createNewFile();
        IOUtils.write(new GsonBuilder().setPrettyPrinting().create().toJson(object), new FileOutputStream(file));
        logger.info("Done! Saved to " + file.getAbsoluteFile().getAbsolutePath());
    }

    public JsonObject serializeMapColor(MapColor color, int index) {
        Color color1 = new Color(color.colorValue);
        JsonObject object = new JsonObject();
        object.addProperty("red", color1.getRed());
        object.addProperty("green", color1.getGreen());
        object.addProperty("blue", color1.getBlue());
        object.addProperty("alpha", index == 0 ? 0 : color1.getAlpha());
        return object;
    }
}
