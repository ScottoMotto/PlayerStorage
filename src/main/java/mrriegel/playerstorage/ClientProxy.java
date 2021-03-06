package mrriegel.playerstorage;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.List;

import org.lwjgl.input.Keyboard;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import mrriegel.limelib.LimeLib;
import mrriegel.limelib.helper.ColorHelper;
import mrriegel.limelib.helper.NBTHelper;
import mrriegel.limelib.network.OpenGuiMessage;
import mrriegel.limelib.network.PacketHandler;
import mrriegel.playerstorage.Enums.MessageAction;
import mrriegel.playerstorage.gui.GuiExI;
import mrriegel.playerstorage.registry.Registry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.client.event.ClientChatEvent;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.oredict.OreDictionary;

@EventBusSubscriber(modid = PlayerStorage.MODID, value = { Side.CLIENT })
public class ClientProxy extends CommonProxy {

	public static final KeyBinding GUI = new KeyBinding("keybinding.playerstorage.gui", KeyConflictContext.IN_GAME, Keyboard.KEY_I, PlayerStorage.MODID);
	public static final KeyBinding INVERTPICKUP = new KeyBinding("keybinding.playerstorage.invert", KeyConflictContext.IN_GAME, Keyboard.KEY_LCONTROL, PlayerStorage.MODID);
	public static final KeyBinding OPENLIMIT = new KeyBinding("keybinding.playerstorage.limit", KeyConflictContext.GUI, Keyboard.KEY_L, PlayerStorage.MODID);
	public static final KeyBinding HIGHLIGHT = new KeyBinding("keybinding.playerstorage.highlight", KeyConflictContext.GUI, Keyboard.KEY_H, PlayerStorage.MODID);
	public static Int2IntOpenHashMap colorMap = new Int2IntOpenHashMap(4);

	@Override
	public void preInit(FMLPreInitializationEvent event) {
		super.preInit(event);
		Registry.initClient();
	}

	@Override
	public void init(FMLInitializationEvent event) {
		super.init(event);
		ClientRegistry.registerKeyBinding(GUI);
		ClientRegistry.registerKeyBinding(INVERTPICKUP);
		ClientRegistry.registerKeyBinding(OPENLIMIT);
		ClientRegistry.registerKeyBinding(HIGHLIGHT);
		Minecraft.getMinecraft().getItemColors().registerItemColorHandler((stack, tint) -> {
			/** @author mezz */
			if (tint != 0)
				return 0xffffff;
			Minecraft mc = Minecraft.getMinecraft();
			if (colorMap.containsKey(stack.getItemDamage()))
				return colorMap.get(stack.getItemDamage());
			String ore = ConfigHandler.appleList.get(stack.getItemDamage());
			List<ItemStack> ores = OreDictionary.getOres(ore);
			if (ores.isEmpty())
				return 0xffffff;
			TextureAtlasSprite tas = mc.getRenderItem().getItemModelMesher().getItemModel(ores.get(0)).getParticleTexture();
			if (tas == mc.getTextureMapBlocks().getMissingSprite() || tas.getIconHeight() <= 0 || tas.getIconWidth() <= 0 || tas.getFrameCount() <= 0)
				return 0xffffff;
			BufferedImage img = new BufferedImage(tas.getIconWidth(), tas.getIconHeight() * tas.getFrameCount(), BufferedImage.TYPE_4BYTE_ABGR);
			for (int i = 0; i < tas.getFrameCount(); i++) {
				int[][] frameTextureData = tas.getFrameTextureData(i);
				int[] largestMipMapTextureData = frameTextureData[0];
				img.setRGB(0, i * tas.getIconHeight(), tas.getIconWidth(), tas.getIconHeight(), largestMipMapTextureData, 0, tas.getIconWidth());
			}
			int red = 0, green = 0, blue = 0, count = 0;
			for (int x = 0; x < img.getWidth(); x++)
				for (int y = 0; y < img.getHeight(); y++) {
					int rgb = img.getRGB(x, y);
					if (ColorHelper.getAlpha(rgb) == 255) {
						red += ColorHelper.getRed(rgb);
						green += ColorHelper.getGreen(rgb);
						blue += ColorHelper.getBlue(rgb);
						count++;
					}
				}
			int c = new Color((red / count), (green / count), (blue / count)).getRGB();
			c = ColorHelper.brighter(c, .15);
			colorMap.put(stack.getItemDamage(), c);
			return c;
		}, Registry.apple);
	}

	@Override
	public void postInit(FMLPostInitializationEvent event) {
		super.postInit(event);
	}

	@SubscribeEvent
	public static void key(InputEvent.KeyInputEvent event) {
		if (!Minecraft.getMinecraft().inGameHasFocus)
			return;
		if (GUI.isPressed() && Minecraft.getMinecraft().player.hasCapability(ExInventory.EXINVENTORY, null)) {
			PacketHandler.sendToServer(new OpenGuiMessage(PlayerStorage.MODID, 0, null));
		}
		if (Keyboard.getEventKey() == INVERTPICKUP.getKeyCode()) {
			NBTTagCompound nbt = new NBTTagCompound();
			MessageAction.INVERTPICKUP.set(nbt);
			NBTHelper.set(nbt, "inverted", Keyboard.getEventKeyState());
			PacketHandler.sendToServer(new Message2Server(nbt));
		}
	}

	@SubscribeEvent
	public static void renderText(RenderGameOverlayEvent event) {
		if (event.getType() == ElementType.TEXT && INVERTPICKUP.isKeyDown()) {
			String text = "Auto Pickup is inverted.";
			int color = Color.HSBtoRGB(0f, 0f, (float) ((Math.sin((Minecraft.getMinecraft().player.ticksExisted + event.getPartialTicks()) / 10) / 2) + .5f));
			GlStateManager.pushMatrix();
			GlStateManager.translate(event.getResolution().getScaledWidth() / 2, event.getResolution().getScaledHeight() - 68 - (2 + Minecraft.getMinecraft().fontRenderer.FONT_HEIGHT), 0.0F);
			GlStateManager.enableBlend();
			GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
			Minecraft.getMinecraft().fontRenderer.drawString(TextFormatting.UNDERLINE + text, -Minecraft.getMinecraft().fontRenderer.getStringWidth(text) / 2, -4, color);
			GlStateManager.disableBlend();
			GlStateManager.popMatrix();
		}
	}

	public static final String TEAMCODE = "²³¼′°°!^’â^^Ô`¸'";

	@SubscribeEvent
	public static void chat(ClientChatEvent event) {
		if (event.getMessage().contains(TEAMCODE)) {
			String sender = event.getMessage().replace(TEAMCODE, "");
			if (sender != null) {
				NBTTagCompound nbt = new NBTTagCompound();
				MessageAction.TEAMACCEPT.set(nbt);
				NBTHelper.set(nbt, "player1", Minecraft.getMinecraft().player.getName());
				NBTHelper.set(nbt, "player2", sender);
				PacketHandler.sendToServer(new Message2Server(nbt));
			}
			event.setMessage("");

		}
	}

	@SubscribeEvent
	public static void gui(GuiOpenEvent event) {
		if (event.getGui() instanceof GuiInventory && !(Minecraft.getMinecraft().currentScreen instanceof GuiExI) && ExInventory.getInventory(LimeLib.proxy.getClientPlayer()).defaultGUI) {
			event.setCanceled(true);
			PacketHandler.sendToServer(new OpenGuiMessage(PlayerStorage.MODID, 0, null));
		}
	}

	public static TextureAtlasSprite sprite;

	@SubscribeEvent
	public static void onTextureStitch(TextureStitchEvent event) {
		sprite = event.getMap().registerSprite(new ResourceLocation(PlayerStorage.MODID, "items/bin"));
	}

}
