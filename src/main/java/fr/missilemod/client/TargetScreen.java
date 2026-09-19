package fr.missilemod.client;

import fr.missilemod.entity.MissileEntity;
import fr.missilemod.entity.MissileState;
import fr.missilemod.network.LaunchMissilePacket;
import fr.missilemod.network.ModNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;

/**
 * Interface de ciblage. C'est un simple Screen (pas de conteneur/Menu) : il n'y a aucun inventaire
 * a synchroniser, seules les coordonnees partent au serveur via LaunchMissilePacket.
 */
public class TargetScreen extends Screen {

    private static final int PANEL_WIDTH = 230;
    private static final int PANEL_HEIGHT = 160;
    private static final int FIELD_WIDTH = 62;
    private static final int FIELD_GAP = 10;

    private final MissileEntity missile;
    private EditBox xField;
    private EditBox yField;
    private EditBox zField;
    private Component status = Component.empty();

    public TargetScreen(MissileEntity missile) {
        super(Component.translatable("gui.missilemod.title"));
        this.missile = missile;
    }

    private int left() {
        return (this.width - PANEL_WIDTH) / 2;
    }

    private int top() {
        return (this.height - PANEL_HEIGHT) / 2;
    }

    @Override
    protected void init() {
        int left = left();
        int top = top();
        int fieldsX = left + (PANEL_WIDTH - (3 * FIELD_WIDTH + 2 * FIELD_GAP)) / 2;
        int fieldsY = top + 44;

        this.xField = createField(fieldsX, fieldsY, "X");
        this.yField = createField(fieldsX + FIELD_WIDTH + FIELD_GAP, fieldsY, "Y");
        this.zField = createField(fieldsX + 2 * (FIELD_WIDTH + FIELD_GAP), fieldsY, "Z");

        addRenderableWidget(Button.builder(Component.translatable("gui.missilemod.fill_position"),
                        button -> fillWithPlayerPosition())
                .bounds(left + 12, top + 74, PANEL_WIDTH - 24, 20)
                .build());

        addRenderableWidget(Button.builder(Component.translatable("gui.missilemod.launch")
                                .withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
                        button -> launch())
                .bounds(left + 12, top + 102, PANEL_WIDTH - 24, 24)
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

    private void fillWithPlayerPosition() {
        if (this.minecraft == null || this.minecraft.player == null) {
            return;
        }
        BlockPos pos = this.minecraft.player.blockPosition();
        this.xField.setValue(Integer.toString(pos.getX()));
        this.yField.setValue(Integer.toString(pos.getY()));
        this.zField.setValue(Integer.toString(pos.getZ()));
        this.status = Component.empty();
    }

    private void launch() {
        Integer x = parse(this.xField);
        Integer y = parse(this.yField);
        Integer z = parse(this.zField);
        if (x == null || y == null || z == null) {
            this.status = Component.translatable("gui.missilemod.invalid_number");
            return;
        }
        ModNetwork.CHANNEL.sendToServer(new LaunchMissilePacket(this.missile.getId(), x, y, z));
        onClose();
    }

    @Nullable
    private static Integer parse(EditBox field) {
        try {
            return Integer.parseInt(field.getValue().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public void tick() {
        super.tick();
        this.xField.tick();
        this.yField.tick();
        this.zField.tick();
        Player player = this.minecraft != null ? this.minecraft.player : null;
        if (player == null
                || this.missile.isRemoved()
                || this.missile.getState() != MissileState.POSED
                || player.distanceToSqr(this.missile) > 64.0D) {
            onClose();
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        int left = left();
        int top = top();

        graphics.fill(left, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, 0xE0101418);
        graphics.renderOutline(left, top, PANEL_WIDTH, PANEL_HEIGHT, 0xFF8A9199);
        graphics.fill(left + 1, top + 1, left + PANEL_WIDTH - 1, top + 22, 0xFF2A3038);

        graphics.drawCenteredString(this.font, this.title, this.width / 2, top + 8, 0xFFFFFF);

        BlockPos missilePos = this.missile.blockPosition();
        graphics.drawCenteredString(this.font,
                Component.translatable("gui.missilemod.missile_position",
                        missilePos.getX(), missilePos.getY(), missilePos.getZ()),
                this.width / 2, top + 26, 0xA0A8B0);

        graphics.drawString(this.font, "X", this.xField.getX() + 2, this.xField.getY() - 10, 0xD0D0D0);
        graphics.drawString(this.font, "Y", this.yField.getX() + 2, this.yField.getY() - 10, 0xD0D0D0);
        graphics.drawString(this.font, "Z", this.zField.getX() + 2, this.zField.getY() - 10, 0xD0D0D0);

        super.render(graphics, mouseX, mouseY, partialTick);

        if (!this.status.getString().isEmpty()) {
            graphics.drawCenteredString(this.font, this.status, this.width / 2, top + PANEL_HEIGHT - 20, 0xFF5555);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
