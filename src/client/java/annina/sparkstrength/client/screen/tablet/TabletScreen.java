package annina.sparkstrength.client.screen.tablet;

import annina.sparkstrength.network.tablet.ApproveSuspectRemovalC2SPacket;
import annina.sparkstrength.network.tablet.CallTabletMeetingC2SPacket;
import annina.sparkstrength.network.tablet.CastTabletVoteC2SPacket;
import annina.sparkstrength.network.tablet.ConfirmTabletVoteC2SPacket;
import annina.sparkstrength.network.tablet.RequestTabletSnapshotC2SPacket;
import annina.sparkstrength.network.tablet.SendTabletChatC2SPacket;
import annina.sparkstrength.network.tablet.TabletSnapshot;
import annina.sparkstrength.tablet.TabletLayout;
import annina.sparkstrength.tablet.TabletUiSession;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;
import java.util.UUID;

public final class TabletScreen extends Screen {
    private static final DateTimeFormatter CLOCK_FORMAT = DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM);
    private static final int ROW_HEIGHT = 24;
    private static final int COLOR_SHELL = 0xF20A0D10;
    private static final int COLOR_SHELL_SHADOW = 0x8A000000;
    private static final int COLOR_METAL_EDGE = 0xFFA87A43;
    private static final int COLOR_METAL_DARK = 0xFF4B3928;
    private static final int COLOR_STATUS = 0xFF151D24;
    private static final int COLOR_NAVIGATION = 0xFF10161C;
    private static final int COLOR_BODY = 0xF20C1217;
    private static final int COLOR_FOOTER = 0xFF111920;
    private static final int COLOR_DIVIDER = 0xFF2A3944;
    private static final int COLOR_SIGNAL = 0xFF3AA8E8;
    private static final int COLOR_SIGNAL_PALE = 0xFF9EDFFF;
    private static final int COLOR_SIGNAL_SOFT = 0x553087B8;
    private static final int COLOR_TEXT = 0xFFEAF2F6;
    private static final int COLOR_MUTED = 0xFF94A1A9;
    private static final int COLOR_ROW = 0xE6162027;
    private static final int COLOR_ROW_ALT = 0xD719252D;
    private static final int COLOR_GREEN = 0xFF46D878;
    private static final int COLOR_ORANGE = 0xFFFF8C00;
    private static final int COLOR_ORANGE_PALE = 0xFFFFC062;

    private final TabletUiSession session = new TabletUiSession(TabletClientState.chatDraft());
    private TabletLayout layout;
    private TextFieldWidget chatField;
    private int snapshotRequestTicks;
    private boolean requestedInitialSnapshot;

    public TabletScreen() {
        super(Text.translatable("screen.sparkstrength.tablet.title"));
    }

    @Override
    protected void init() {
        super.init();
        layout = TabletLayout.forViewport(width, height);
        chatField = null;

        if (!requestedInitialSnapshot) {
            requestedInitialSnapshot = true;
            requestSnapshot();
        }

        initCloseButton();
        initNavigation();
        if (session.section() == TabletUiSession.Section.CHAT) {
            initChat();
        } else if (session.section() == TabletUiSession.Section.MEETING) {
            initMeeting();
        } else if (session.section() == TabletUiSession.Section.SUSPECTS) {
            initSuspects();
        }
    }

    public void refresh() {
        if (client == null) {
            return;
        }
        rememberChatDraft();
        chatField = null;
        clearChildren();
        init();
    }

    public void handleSnapshotUpdate() {
        TabletSnapshot.Meeting meeting = TabletClientState.snapshot().meeting();
        session.applyMeetingSnapshot(
                meeting.active(),
                meeting.targets().size(),
                visibleRows()
        );
        session.applySuspectSnapshot(
                TabletClientState.snapshot().suspects().size(),
                visibleRows()
        );
        if (session.section() == TabletUiSession.Section.MEETING
                || session.section() == TabletUiSession.Section.SUSPECTS) {
            refresh();
        }
    }

    @Override
    public void tick() {
        super.tick();
        snapshotRequestTicks++;
        if (snapshotRequestTicks >= 20) {
            snapshotRequestTicks = 0;
            requestSnapshot();
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (session.section() == TabletUiSession.Section.CHAT
                && (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER)) {
            sendChat();
            return true;
        }
        boolean handled = super.keyPressed(keyCode, scanCode, modifiers);
        if (session.section() == TabletUiSession.Section.CHAT
                && chatField != null
                && chatField.isFocused()
                && isMovementKey(keyCode, scanCode)) {
            return true;
        }
        return handled;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        TabletSnapshot snapshot = TabletClientState.snapshot();
        if (session.section() == TabletUiSession.Section.MEETING
                && snapshot.meeting().active()
                && layout.list().contains(mouseX, mouseY)) {
            int previousFirstRow = session.meetingFirstRow();
            session.scrollMeeting(
                    verticalAmount,
                    snapshot.meeting().targets().size(),
                    layout.visibleRows()
            );
            if (session.meetingFirstRow() != previousFirstRow) {
                refresh();
            }
            return true;
        }
        if (session.section() == TabletUiSession.Section.SUSPECTS
                && layout.list().contains(mouseX, mouseY)) {
            int previousFirstRow = session.suspectFirstRow();
            session.scrollSuspects(
                    verticalAmount,
                    snapshot.suspects().size(),
                    layout.visibleRows()
            );
            if (session.suspectFirstRow() != previousFirstRow) {
                refresh();
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, 0xB8000000);
        TextRenderer renderer = MinecraftClient.getInstance().textRenderer;
        drawTabletFrame(context, renderer);

        if (session.section() == TabletUiSession.Section.CONNECTIONS) {
            renderConnections(context, renderer);
        } else if (session.section() == TabletUiSession.Section.CHAT) {
            renderChat(context, renderer);
        } else if (session.section() == TabletUiSession.Section.MEETING) {
            renderMeeting(context, renderer);
        } else if (session.section() == TabletUiSession.Section.SUSPECTS) {
            renderSuspects(context, renderer);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        // Keep vanilla blur behind the tablet, not over custom content.
        // 将原版背景模糊层留在平板后方，避免盖住自绘内容。
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private void initCloseButton() {
        TabletLayout.Rect close = layout.closeButton();
        addDrawableChild(ButtonWidget.builder(Text.literal("×"), button -> close())
                .dimensions(close.x(), close.y(), close.width(), close.height())
                .build());
    }

    private void initNavigation() {
        TabletUiSession.Section[] sections = TabletUiSession.Section.values();
        TextRenderer renderer = MinecraftClient.getInstance().textRenderer;
        for (int index = 0; index < sections.length; index++) {
            TabletUiSession.Section section = sections[index];
            TabletLayout.Rect tab = layout.tabs().get(index);
            Text label = Text.translatable(section.translationKey());
            Text visibleLabel = Text.literal(trim(renderer, label.getString(), Math.max(0, tab.width() - 10)));
            ButtonWidget button = ButtonWidget.builder(visibleLabel, ignored -> {
                        rememberChatDraft();
                        session.select(section);
                        refresh();
                    })
                    .dimensions(tab.x(), tab.y(), tab.width(), tab.height())
                    .build();
            button.active = section != session.section();
            addDrawableChild(button);
        }
    }

    private void initChat() {
        TabletLayout.Rect footer = layout.footer();
        int fieldX = footer.x() + 10;
        int inputY = footer.y() + Math.max(0, (footer.height() - 20) / 2);
        int availableWidth = Math.max(0, footer.width() - 20);
        int sendWidth = Math.min(62, Math.max(44, availableWidth / 4));
        int fieldWidth = Math.max(1, availableWidth - sendWidth - 6);
        TextFieldWidget field = new TextFieldWidget(
                MinecraftClient.getInstance().textRenderer,
                fieldX,
                inputY,
                fieldWidth,
                20,
                Text.translatable("screen.sparkstrength.tablet.chat.placeholder")
        );
        field.setMaxLength(120);
        field.setText(session.draft());
        field.setPlaceholder(Text.translatable("screen.sparkstrength.tablet.chat.placeholder"));
        field.setChangedListener(value -> {
            session.updateDraft(value);
            TabletClientState.setChatDraft(value);
        });
        chatField = field;
        addDrawableChild(field);
        setInitialFocus(field);

        Text sendLabel = Text.translatable("screen.sparkstrength.tablet.chat.send");
        addDrawableChild(ButtonWidget.builder(
                        Text.literal(trim(MinecraftClient.getInstance().textRenderer, sendLabel.getString(), sendWidth - 8)),
                        button -> sendChat())
                .dimensions(fieldX + fieldWidth + 6, inputY, sendWidth, 20)
                .build());
    }

    private void initMeeting() {
        TabletSnapshot snapshot = TabletClientState.snapshot();
        TabletLayout.Rect footer = layout.footer();
        int controlX = footer.x() + 10;
        int controlY = footer.y() + Math.max(0, (footer.height() - 20) / 2);
        session.applyMeetingSnapshot(
                snapshot.meeting().active(),
                snapshot.meeting().targets().size(),
                layout.visibleRows()
        );

        if (!snapshot.meeting().active()) {
            ButtonWidget call = ButtonWidget.builder(
                            Text.translatable("screen.sparkstrength.tablet.meeting.call"),
                            button -> ClientPlayNetworking.send(new CallTabletMeetingC2SPacket()))
                    .dimensions(controlX, controlY, Math.min(130, Math.max(1, footer.width() - 20)), 20)
                    .build();
            call.active = snapshot.localMeetingParticipant()
                    && snapshot.cooldownSeconds() <= 0
                    && snapshot.localMeetingCallsRemaining() > 0;
            addDrawableChild(call);
            return;
        }

        ButtonWidget abstain = ButtonWidget.builder(
                        Text.translatable("screen.sparkstrength.tablet.meeting.abstain"),
                        button -> ClientPlayNetworking.send(new CastTabletVoteC2SPacket(null)))
                .dimensions(controlX, controlY, 74, 20)
                .build();
        abstain.active = snapshot.localMeetingParticipant() && !snapshot.meeting().localConfirmed();
        addDrawableChild(abstain);
        ButtonWidget confirm = ButtonWidget.builder(Text.translatable(
                        snapshot.meeting().localConfirmed()
                                ? "screen.sparkstrength.tablet.meeting.locked"
                                : "screen.sparkstrength.tablet.meeting.confirm"
                ), button -> ClientPlayNetworking.send(new ConfirmTabletVoteC2SPacket()))
                .dimensions(controlX + 82, controlY, 74, 20)
                .build();
        confirm.active = snapshot.localMeetingParticipant() && !snapshot.meeting().localConfirmed();
        addDrawableChild(confirm);

        var targets = snapshot.meeting().targets();
        int lastRow = Math.min(session.meetingFirstRow() + layout.visibleRows(), targets.size());
        int rowY = layout.list().y();
        int voteButtonX = layout.list().right() - 68;
        for (int index = session.meetingFirstRow(); index < lastRow; index++) {
            TabletSnapshot.VoteTarget target = targets.get(index);
            ButtonWidget vote = ButtonWidget.builder(
                            Text.translatable("screen.sparkstrength.tablet.meeting.vote"),
                            button -> ClientPlayNetworking.send(new CastTabletVoteC2SPacket(target.uuid())))
                    .dimensions(voteButtonX, rowY + 2, 64, 18)
                    .build();
            vote.active = snapshot.localMeetingParticipant()
                    && target.selectable()
                    && !snapshot.meeting().localConfirmed();
            addDrawableChild(vote);
            rowY += ROW_HEIGHT;
        }
    }

    private void initSuspects() {
        TabletSnapshot snapshot = TabletClientState.snapshot();
        int rowY = layout.list().y();
        int buttonX = layout.list().right() - 82;
        int lastRow = Math.min(session.suspectFirstRow() + layout.visibleRows(), snapshot.suspects().size());
        TextRenderer renderer = MinecraftClient.getInstance().textRenderer;
        for (int index = session.suspectFirstRow(); index < lastRow; index++) {
            TabletSnapshot.SuspectRow suspect = snapshot.suspects().get(index);
            Text label = Text.translatable(suspect.localApproved()
                    ? "screen.sparkstrength.tablet.suspects.cancel"
                    : "screen.sparkstrength.tablet.suspects.approve");
            ButtonWidget button = ButtonWidget.builder(
                            Text.literal(trim(renderer, label.getString(), 70)),
                            ignored -> ClientPlayNetworking.send(new ApproveSuspectRemovalC2SPacket(
                                    suspect.uuid(),
                                    !suspect.localApproved()
                            )))
                    .dimensions(buttonX, rowY + 2, 78, 18)
                    .build();
            button.active = snapshot.localMeetingParticipant();
            addDrawableChild(button);
            rowY += ROW_HEIGHT;
        }
    }

    private void renderConnections(DrawContext context, TextRenderer renderer) {
        TabletSnapshot snapshot = TabletClientState.snapshot();
        drawSectionHeader(context, renderer, String.valueOf(snapshot.connections().size()));
        if (snapshot.connections().isEmpty()) {
            drawEmptyState(context, renderer, Text.translatable("screen.sparkstrength.tablet.connections.empty"));
            return;
        }

        TabletLayout.Rect list = layout.list();
        context.enableScissor(list.x(), list.y(), list.right(), list.bottom());
        int rows = Math.min(layout.visibleRows(), snapshot.connections().size());
        int y = list.y();
        for (int index = 0; index < rows; index++) {
            TabletSnapshot.PlayerRow row = snapshot.connections().get(index);
            drawPlayerRow(
                    context,
                    renderer,
                    row.uuid(),
                    row.name(),
                    row.inGame() ? COLOR_GREEN : 0xFF6F787D,
                    list.x(),
                    y,
                    list.width(),
                    92
            );
            drawPill(
                    context,
                    renderer,
                    Text.translatable(row.inGame()
                            ? "screen.sparkstrength.tablet.status.ingame"
                            : "screen.sparkstrength.tablet.status.outside"),
                    list.right() - 6,
                    y + 4,
                    row.inGame() ? COLOR_GREEN : COLOR_MUTED,
                    row.inGame() ? 0x2736D36B : 0x24777777
            );
            y += ROW_HEIGHT;
        }
        context.disableScissor();
    }

    private void renderChat(DrawContext context, TextRenderer renderer) {
        var messages = TabletClientState.snapshot().chat();
        drawSectionHeader(context, renderer, String.valueOf(messages.size()));
        if (messages.isEmpty()) {
            drawEmptyState(context, renderer, Text.translatable("screen.sparkstrength.tablet.chat.empty"));
            return;
        }

        TabletLayout.Rect list = layout.list();
        context.enableScissor(list.x(), list.y(), list.right(), list.bottom());
        int rows = Math.min(layout.visibleRows(), messages.size());
        int start = messages.size() - rows;
        int y = list.y();
        for (int index = start; index < messages.size(); index++) {
            TabletSnapshot.ChatRow row = messages.get(index);
            context.fill(list.x(), y, list.right(), y + ROW_HEIGHT - 3, index % 2 == 0 ? COLOR_ROW : COLOR_ROW_ALT);
            context.fill(list.x(), y, list.x() + 3, y + ROW_HEIGHT - 3, COLOR_SIGNAL);
            String sender = trim(renderer, row.senderName(), Math.min(78, Math.max(0, list.width() / 3)));
            int senderWidth = Math.min(82, renderer.getWidth(sender) + 8);
            context.drawText(renderer, sender, list.x() + 9, y + 7, COLOR_SIGNAL_PALE, false);
            context.drawText(
                    renderer,
                    trim(renderer, row.message(), Math.max(0, list.width() - senderWidth - 20)),
                    list.x() + 9 + senderWidth,
                    y + 7,
                    COLOR_TEXT,
                    false
            );
            y += ROW_HEIGHT;
        }
        context.disableScissor();
    }

    private void renderMeeting(DrawContext context, TextRenderer renderer) {
        TabletSnapshot snapshot = TabletClientState.snapshot();
        String detail = snapshot.meeting().active()
                ? Text.translatable("screen.sparkstrength.tablet.meeting.timer", snapshot.meeting().remainingSeconds()).getString()
                : Text.translatable("screen.sparkstrength.tablet.meeting.chances", snapshot.localMeetingCallsRemaining()).getString();
        drawSectionHeader(context, renderer, detail);
        if (!snapshot.meeting().active()) {
            String key = snapshot.cooldownSeconds() > 0
                    ? "screen.sparkstrength.tablet.meeting.cooldown"
                    : "screen.sparkstrength.tablet.meeting.disabled";
            drawInfoLine(context, renderer, Text.translatable(key, snapshot.cooldownSeconds()), COLOR_MUTED);
            return;
        }

        var targets = snapshot.meeting().targets();
        TabletLayout.Rect list = layout.list();
        context.enableScissor(list.x(), list.y(), list.right(), list.bottom());
        int lastRow = Math.min(session.meetingFirstRow() + layout.visibleRows(), targets.size());
        int y = list.y();
        int voteButtonX = list.right() - 68;
        int votesX = voteButtonX - 28;
        for (int index = session.meetingFirstRow(); index < lastRow; index++) {
            TabletSnapshot.VoteTarget target = targets.get(index);
            boolean selected = target.uuid().equals(snapshot.meeting().localVoteTarget());
            drawPlayerRow(
                    context,
                    renderer,
                    target.uuid(),
                    target.name(),
                    target.selectable() ? COLOR_SIGNAL : 0xFF626C72,
                    list.x(),
                    y,
                    list.width(),
                    118
            );
            if (selected) {
                context.drawBorder(list.x(), y + 2, list.width(), ROW_HEIGHT - 3, COLOR_SIGNAL);
                context.fill(list.x(), y + 2, list.x() + 4, y + ROW_HEIGHT - 1, COLOR_SIGNAL);
            }
            context.drawText(renderer, String.valueOf(target.votes()), votesX, y + 7, COLOR_TEXT, false);
            y += ROW_HEIGHT;
        }
        context.disableScissor();
    }

    private void renderSuspects(DrawContext context, TextRenderer renderer) {
        TabletSnapshot snapshot = TabletClientState.snapshot();
        drawSectionHeader(context, renderer, String.valueOf(snapshot.suspects().size()));
        if (snapshot.suspects().isEmpty()) {
            drawEmptyState(context, renderer, Text.translatable("screen.sparkstrength.tablet.suspects.none"));
            return;
        }

        TabletLayout.Rect list = layout.list();
        context.enableScissor(list.x(), list.y(), list.right(), list.bottom());
        int lastRow = Math.min(session.suspectFirstRow() + layout.visibleRows(), snapshot.suspects().size());
        int y = list.y();
        int buttonX = list.right() - 82;
        for (int index = session.suspectFirstRow(); index < lastRow; index++) {
            TabletSnapshot.SuspectRow suspect = snapshot.suspects().get(index);
            drawPlayerRow(context, renderer, suspect.uuid(), suspect.name(), COLOR_ORANGE, list.x(), y, list.width(), 158);
            Text votes = Text.translatable(
                    "screen.sparkstrength.tablet.suspects.votes",
                    suspect.approvals(),
                    suspect.requiredApprovals()
            );
            String visibleVotes = trim(renderer, votes.getString(), 70);
            context.drawText(
                    renderer,
                    visibleVotes,
                    buttonX - 8 - renderer.getWidth(visibleVotes),
                    y + 7,
                    COLOR_ORANGE_PALE,
                    false
            );
            y += ROW_HEIGHT;
        }
        context.disableScissor();
    }

    private void drawPlayerRow(
            DrawContext context,
            TextRenderer renderer,
            UUID uuid,
            String name,
            int borderColor,
            int x,
            int y,
            int width,
            int reservedRightWidth
    ) {
        context.fill(x, y, x + width, y + ROW_HEIGHT - 3, COLOR_ROW);
        context.fill(x, y + 2, x + 4, y + ROW_HEIGHT - 1, borderColor);
        context.fill(x + 8, y + 3, x + 28, y + 23, borderColor);
        context.fill(x + 10, y + 5, x + 26, y + 21, 0xFF05080B);
        TabletPlayerRow.drawAvatar(context, uuid, name, x + 10, y + 5);
        context.drawText(
                renderer,
                trim(renderer, name, Math.max(0, width - reservedRightWidth - 42)),
                x + 36,
                y + 7,
                COLOR_TEXT,
                false
        );
    }

    private void drawTabletFrame(DrawContext context, TextRenderer renderer) {
        TabletLayout.Rect panel = layout.panel();
        TabletLayout.Rect status = layout.statusBar();
        TabletLayout.Rect navigation = layout.navigation();
        TabletLayout.Rect body = layout.body();
        TabletLayout.Rect footer = layout.footer();

        context.fill(panel.x() - 4, panel.y() - 4, panel.right() + 4, panel.bottom() + 4, COLOR_SHELL_SHADOW);
        context.fill(panel.x(), panel.y(), panel.right(), panel.bottom(), COLOR_SHELL);
        context.fill(status.x() + 3, status.y() + 3, status.right() - 3, status.bottom(), COLOR_STATUS);
        context.fill(navigation.x() + 3, navigation.y(), navigation.right() - 3, navigation.bottom(), COLOR_NAVIGATION);
        context.fill(body.x(), body.y(), body.right(), body.bottom(), COLOR_BODY);
        context.fill(footer.x(), footer.y(), footer.right(), footer.bottom() - 3, COLOR_FOOTER);
        context.fill(footer.x(), footer.y(), footer.right(), footer.y() + 1, COLOR_DIVIDER);

        if (layout.mode() == TabletLayout.Mode.WIDE) {
            context.fill(navigation.right() - 1, navigation.y(), navigation.right(), navigation.bottom(), COLOR_DIVIDER);
        } else {
            context.fill(navigation.x(), navigation.bottom() - 1, navigation.right(), navigation.bottom(), COLOR_DIVIDER);
        }

        drawStatus(context, renderer);
        drawSignalSpine(context);
        context.drawBorder(panel.x(), panel.y(), panel.width(), panel.height(), COLOR_METAL_EDGE);
        context.drawBorder(panel.x() + 2, panel.y() + 2, Math.max(0, panel.width() - 4), Math.max(0, panel.height() - 4), COLOR_METAL_DARK);
    }

    private void drawStatus(DrawContext context, TextRenderer renderer) {
        TabletLayout.Rect status = layout.statusBar();
        TabletLayout.Rect close = layout.closeButton();
        int glyphX = status.x() + 12;
        int glyphBottom = status.y() + 22;
        context.fill(glyphX, glyphBottom - 4, glyphX + 2, glyphBottom, COLOR_SIGNAL);
        context.fill(glyphX + 4, glyphBottom - 8, glyphX + 6, glyphBottom, COLOR_SIGNAL);
        context.fill(glyphX + 8, glyphBottom - 12, glyphX + 10, glyphBottom, COLOR_SIGNAL_PALE);

        String clock = LocalTime.now().format(CLOCK_FORMAT);
        int clockX = close.x() - 8 - renderer.getWidth(clock);
        context.drawText(renderer, clock, clockX, status.y() + 13, COLOR_SIGNAL_PALE, false);

        String titleValue = Text.translatable("screen.sparkstrength.tablet.title")
                .getString()
                .toUpperCase(Locale.ROOT);
        int titleX = glyphX + 18;
        context.drawText(
                renderer,
                trim(renderer, titleValue, Math.max(0, clockX - titleX - 8)),
                titleX,
                status.y() + 13,
                COLOR_TEXT,
                false
        );
    }

    private void drawSignalSpine(DrawContext context) {
        int selectedIndex = session.section().ordinal();
        TabletLayout.Rect selectedTab = layout.tabs().get(selectedIndex);
        TabletLayout.Rect body = layout.body();
        TabletLayout.Rect footer = layout.footer();
        context.fill(
                selectedTab.x() - 2,
                selectedTab.y() - 2,
                selectedTab.right() + 2,
                selectedTab.bottom() + 2,
                COLOR_SIGNAL_SOFT
        );

        if (layout.mode() == TabletLayout.Mode.WIDE) {
            int centerY = selectedTab.y() + selectedTab.height() / 2;
            context.fill(selectedTab.x() - 4, selectedTab.y() - 2, selectedTab.x() - 1, selectedTab.bottom() + 2, COLOR_SIGNAL);
            context.fill(selectedTab.right(), centerY - 1, body.x() + 5, centerY + 1, COLOR_SIGNAL);
        } else {
            int centerX = selectedTab.x() + selectedTab.width() / 2;
            context.fill(selectedTab.x(), selectedTab.bottom(), selectedTab.right(), selectedTab.bottom() + 2, COLOR_SIGNAL);
            context.fill(centerX - 1, selectedTab.bottom(), centerX + 1, layout.navigation().bottom(), COLOR_SIGNAL);
            fillHorizontal(context, centerX, body.x() + 5, layout.navigation().bottom() - 2, layout.navigation().bottom(), COLOR_SIGNAL);
        }
        context.fill(body.x() + 3, body.y(), body.x() + 6, footer.y(), COLOR_SIGNAL);
    }

    private void drawSectionHeader(DrawContext context, TextRenderer renderer, String detail) {
        TabletLayout.Rect body = layout.body();
        Text title = Text.translatable(session.section().translationKey());
        int left = body.x() + 12;
        int right = body.right() - 12;
        String visibleDetail = trim(renderer, detail, Math.max(0, body.width() / 3));
        int detailWidth = renderer.getWidth(visibleDetail);
        String visibleTitle = trim(renderer, title.getString(), Math.max(0, right - left - detailWidth - 12));
        context.drawText(renderer, visibleTitle, left, body.y() + 10, COLOR_TEXT, false);
        if (!visibleDetail.isBlank()) {
            context.drawText(renderer, visibleDetail, right - detailWidth, body.y() + 10, COLOR_ORANGE_PALE, false);
        }
        context.fill(left, body.y() + 25, right, body.y() + 26, COLOR_DIVIDER);
        context.fill(left, body.y() + 25, Math.min(right, left + 24), body.y() + 26, COLOR_SIGNAL);
    }

    private void drawEmptyState(DrawContext context, TextRenderer renderer, Text text) {
        TabletLayout.Rect list = layout.list();
        int boxHeight = Math.min(42, list.height());
        context.fill(list.x(), list.y(), list.right(), list.y() + boxHeight, 0xA1121A20);
        context.drawBorder(list.x(), list.y(), list.width(), boxHeight, COLOR_DIVIDER);
        String value = trim(renderer, text.getString(), Math.max(0, list.width() - 24));
        context.drawText(renderer, value, list.x() + 12, list.y() + Math.max(4, (boxHeight - 8) / 2), COLOR_MUTED, false);
    }

    private void drawInfoLine(DrawContext context, TextRenderer renderer, Text text, int color) {
        TabletLayout.Rect list = layout.list();
        int boxHeight = Math.min(30, list.height());
        context.fill(list.x(), list.y(), list.right(), list.y() + boxHeight, 0xA1121A20);
        context.drawText(
                renderer,
                trim(renderer, text.getString(), Math.max(0, list.width() - 24)),
                list.x() + 12,
                list.y() + Math.max(4, (boxHeight - 8) / 2),
                color,
                false
        );
    }

    private void drawPill(
            DrawContext context,
            TextRenderer renderer,
            Text text,
            int right,
            int y,
            int color,
            int backgroundColor
    ) {
        String value = trim(renderer, text.getString(), 70);
        int width = Math.min(78, renderer.getWidth(value) + 10);
        context.fill(right - width, y, right, y + 14, backgroundColor);
        context.drawBorder(right - width, y, width, 14, color);
        context.drawText(renderer, value, right - width + 5, y + 3, color, false);
    }

    private void fillHorizontal(DrawContext context, int firstX, int secondX, int y, int bottom, int color) {
        context.fill(Math.min(firstX, secondX), y, Math.max(firstX, secondX), bottom, color);
    }

    private int visibleRows() {
        return layout == null ? 0 : layout.visibleRows();
    }

    private String trim(TextRenderer renderer, String value, int width) {
        if (renderer.getWidth(value) <= width) {
            return value;
        }
        String ellipsis = "…";
        return renderer.trimToWidth(value, Math.max(0, width - renderer.getWidth(ellipsis))) + ellipsis;
    }

    private void sendChat() {
        rememberChatDraft();
        session.submitDraft().ifPresent(message ->
                ClientPlayNetworking.send(new SendTabletChatC2SPacket(message)));
        TabletClientState.setChatDraft(session.draft());
        if (chatField != null) {
            chatField.setText(session.draft());
        }
        refresh();
    }

    private void rememberChatDraft() {
        if (chatField != null) {
            session.updateDraft(chatField.getText());
            TabletClientState.setChatDraft(session.draft());
        }
    }

    private boolean isMovementKey(int keyCode, int scanCode) {
        if (client == null) {
            return false;
        }
        return client.options.forwardKey.matchesKey(keyCode, scanCode)
                || client.options.backKey.matchesKey(keyCode, scanCode)
                || client.options.leftKey.matchesKey(keyCode, scanCode)
                || client.options.rightKey.matchesKey(keyCode, scanCode)
                || client.options.jumpKey.matchesKey(keyCode, scanCode)
                || client.options.sneakKey.matchesKey(keyCode, scanCode)
                || client.options.sprintKey.matchesKey(keyCode, scanCode);
    }

    private void requestSnapshot() {
        ClientPlayNetworking.send(new RequestTabletSnapshotC2SPacket());
    }
}
