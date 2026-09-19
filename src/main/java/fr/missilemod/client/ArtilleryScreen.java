package fr.missilemod.client;

import fr.missilemod.block.ArtilleryBlockEntity;
import fr.missilemod.block.ArtilleryType;
import fr.missilemod.entity.ShellType;
import fr.missilemod.item.ShellItem;
import fr.missilemod.network.FireArtilleryPacket;
import fr.missilemod.network.ModNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Interface de tir de l'artillerie : coordonnees de la cible (meme systeme que le missile) et choix de l'obus
 * a charger parmi ceux du bon calibre. L'obus est pris dans l'inventaire au moment du tir.
 */
public class ArtilleryScreen extends Screen {

    private static final int PANEL_WIDTH = 240;
    private static final int PANEL_HEIGHT = 178;
    private static final int FIELD_WIDTH = 62;
    private static final int FIELD_GAP = 10;

    private final BlockPos gunPos;
    private final List<ShellType> shells = new ArrayList<>();
    private int selected;
    private EditBox xField;
    private EditBox yField;
    private EditBox zField;
    private Button shellButton;
    private Component status = Component.empty();

    public ArtilleryScreen(BlockPos gunPos) {
        super(Component.translatable("gui.missilemod.artillery"));
        this.gunPos = gunPos;
    }

    private ArtilleryBlockEntity gun() {
        return this.minecraft != null && this.minecraft.level != null
                && this.minecraft.level.getBlockEntity(this.gunPos) instanceof ArtilleryBlockEntity gun ? gun : null;
    }

    private int left() {
        return (this.width - PANEL_WIDTH) / 2;
    }

    private int top() {
        return (this.height - PANEL_HEIGHT) / 2;
    }

    @Override
    protected void init() {
        ArtilleryBlockEntity gun = gun();
        this.shells.clear();
        if (gun != null) {
            for (ShellType type : ShellType.values()) {
                if (gun.getArtilleryType().accepts(type)) {
                    this.shells.add(type);
                }
            }
        }
        // Presélectionne le premier obus disponible dans l'inventaire.
        for (int i = 0; i < this.shells.size(); i++) {
            if (countInInventory(this.shells.get(i)) > 0) {
                this.selected = i;
                break;
            }
        }

        int left = left();
        int top = top();
        int fieldsX = left + (PANEL_WIDTH - (3 * FIELD_WIDTH + 2 * FIELD_GAP)) / 2;
        int fieldsY = top + 44;
        this.xField = createField(fieldsX, fieldsY, "X");
        this.yField = createField(fieldsX + FIELD_WIDTH + FIELD_GAP, fieldsY, "Y");
        this.zField = createField(fieldsX + 2 * (FIELD_WIDTH + FIELD_GAP), fieldsY, "Z");

        this.shellButton = addRenderableWidget(Button.builder(shellLabel(), button -> {
                    if (!this.shells.isEmpty()) {
                        this.selected = (this.selected + 1) % this.shells.size();
                        button.setMessage(shellLabel());
                    }
                })
                .bounds(left + 12, top + 90, PANEL_WIDTH - 24, 20)
                .build());

        addRenderableWidget(Button.builder(Component.translatable("gui.missilemod.fire")
                                .withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
                        button -> fire())
                .bounds(left + 12, top + 118, PANEL_WIDTH - 24, 24)
                .build());

        setInitialFocus(this.xField);
    }

    private EditBox createField(int x, int y, String axis) {
        EditBox field = new EditBox(this.font, x, y, FIELD_WIDTH, 20, Component.literal(axis));
        field.setMaxLength(9);
        field.setFilter(text -> text.isEmpty() || text.equals("-") || text.matches("-?\\d{1,8}"));
        field.setHint(Component.literal(axis).withStyle(ChatFormatting.DARK_GRAY));
        return addRenderableWidget(field);
    }

    private Component shellLabel() {
        if (this.shells.isEmpty()) {
            return Component.translatable("gui.missilemod.no_compatible_shell");
        }
        ShellType type = this.shells.get(this.selected);
        return Component.translatable("gui.missilemod.shell_choice",
                Component.translatable("item.missilemod." + type.id), countInInventory(type));
    }

    private int countInInventory(ShellType type) {
        if (this.minecraft == null || this.minecraft.player == null) {
            return 0;
        }
        Inventory inventory = this.minecraft.player.getInventory();
        int count = 0;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.getItem() instanceof ShellItem item && item.getShellType() == type) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private void fire() {
        ArtilleryBlockEntity gun = gun();
        if (gun == null) {
            onClose();
            return;
        }
        if (this.shells.isEmpty()) {
            this.status = Component.translatable("gui.missilemod.no_compatible_shell").withStyle(ChatFormatting.RED);
            return;
        }
        if (!gun.isReady()) {
            this.status = Component.translatable("message.missilemod.gun_busy").withStyle(ChatFormatting.RED);
            return;
        }
        try {
            int x = Integer.parseInt(this.xField.getValue().trim());
            int y = Integer.parseInt(this.yField.getValue().trim());
            int z = Integer.parseInt(this.zField.getValue().trim());
            ModNetwork.CHANNEL.sendToServer(new FireArtilleryPacket(this.gunPos, x, y, z, this.shells.get(this.selected)));
            onClose();
        } catch (NumberFormatException e) {
            this.status = Component.translatable("gui.missilemod.invalid_number").withStyle(ChatFormatting.RED);
        }
    }

    @Override
    public void tick() {
        super.tick();
        ArtilleryBlockEntity gun = gun();
        if (gun == null || this.minecraft == null || this.minecraft.player == null
                || this.minecraft.player.distanceToSqr(this.gunPos.getCenter()) > 64.0D) {
            onClose();
            return;
        }
        this.xField.tick();
        this.yField.tick();
        this.zField.tick();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        int left = left();
        int top = top();
        graphics.fill(left, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, 0xE0101418);
        graphics.renderOutline(left, top, PANEL_WIDTH, PANEL_HEIGHT, 0xFF5A6B4A);

        ArtilleryBlockEntity gun = gun();
        ArtilleryType type = gun != null ? gun.getArtilleryType() : ArtilleryType.MORTAR;
        Component title = Component.translatable("block.missilemod." + type.id);
        graphics.drawCenteredString(this.font, title, this.width / 2, top + 10, 0xFFD9E3C0);
        if (gun != null) {
            graphics.drawCenteredString(this.font, Component.translatable("gui.missilemod.range",
                    gun.minRange(), gun.maxRange()), this.width / 2, top + 24, 0xFF9AA58A);
        }
        graphics.drawString(this.font, Component.translatable("gui.missilemod.ammo"), left + 12, top + 78, 0xFFB0B8A0);

        super.render(graphics, mouseX, mouseY, partialTick);

        Component state = gun == null ? Component.empty()
                : Component.translatable("gui.missilemod.phase." + gun.getPhase().name().toLowerCase());
        graphics.drawCenteredString(this.font, this.status.getString().isEmpty() ? state : this.status,
                this.width / 2, top + 152, 0xFFE0E0E0);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 257 || keyCode == 335) { // Entree
            fire();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
