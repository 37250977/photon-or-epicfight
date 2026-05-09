package net.zidou.photon_or_epicfight.editor;

import com.lowdragmc.lowdraglib.gui.editor.ColorPattern;
import com.lowdragmc.lowdraglib.gui.editor.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.gui.editor.ui.view.FloatViewWidget;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.ProgressTexture;
import com.lowdragmc.lowdraglib.gui.texture.ResourceBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.DraggableScrollableWidgetGroup;
import com.lowdragmc.lowdraglib.gui.widget.ProgressWidget;
import com.lowdragmc.lowdraglib.gui.widget.TextFieldWidget;
import com.lowdragmc.photon.gui.editor.FXEditor;
import com.lowdragmc.photon.gui.editor.ParticleScenePanel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.zidou.photon_or_epicfight.animation.ArmatureResolver;
import net.zidou.photon_or_epicfight.config.PhotonEditorConfig;
import net.zidou.photon_or_epicfight.store.PhotonPatchStore;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import yesman.epicfight.api.animation.AnimationManager;
import yesman.epicfight.api.animation.AnimationPlayer;
import yesman.epicfight.api.animation.Joint;
import yesman.epicfight.api.animation.Pose;
import yesman.epicfight.api.animation.types.DynamicAnimation;
import yesman.epicfight.api.client.animation.ClientAnimator;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.api.utils.math.Vec3f;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@LDLRegister(name = "animation_control", group = "editor.fx")
@OnlyIn(Dist.CLIENT)
public class AnimationControlView extends FloatViewWidget {

    private TextFieldWidget searchField;
    private DraggableScrollableWidgetGroup suggestionScroll;
    private String currentAnimationPath = "";
    private volatile boolean isPlaying = false;
    private final List<String> matchedAnimations = new ArrayList<>();
    private ButtonWidget mainBtn;
    private ProgressWidget progressBar;
    private ClientAnimator currentAnimator;
    private LivingEntityPatch<?> currentPatch;
    private DynamicAnimation currentAnimation;
    private float totalTime;

    private TextFieldWidget boneField;
    private DraggableScrollableWidgetGroup boneSuggestScroll;
    private String currentBoneName = "";
    private final List<String> matchedBones = new ArrayList<>();

    public AnimationControlView() {
        super(0, 0, 200, 215, false);
    }

    @Override
    public void initWidget() {
        super.initWidget();

        searchField = new TextFieldWidget(3, 3, 194, 14, null, null);
        searchField.setCurrentString(currentAnimationPath);
        searchField.setResourceLocationOnly();
        searchField.setTextResponder(this::onTextChanged);
        content.addWidget(searchField);

        suggestionScroll = new DraggableScrollableWidgetGroup(3, 18, 194, 80);
        suggestionScroll.setVisible(false);
        content.addWidget(suggestionScroll);

        mainBtn = new ButtonWidget(3, 100, 60, 14,
                new GuiTextureGroup(ResourceBorderTexture.BUTTON_COMMON, new TextTexture("§a播放").setWidth(60).setType(TextTexture.TextType.ROLL)),
                cd -> {
                    if (isPlaying) {
                        stopAnimation();
                    } else {
                        playAnimation();
                    }
                    updateMainButton();
                });
        content.addWidget(mainBtn);

        content.addWidget(new ButtonWidget(68, 100, 40, 14,
                new GuiTextureGroup(ResourceBorderTexture.BUTTON_COMMON, new TextTexture("§7清空").setWidth(40).setType(TextTexture.TextType.ROLL)),
                cd -> {
                    searchField.setCurrentString("");
                    currentAnimationPath = "";
                    suggestionScroll.setVisible(false);
                    suggestionScroll.clearAllWidgets();
                    stopAnimation();
                    updateMainButton();
                }));

        progressBar = new ProgressWidget(this::getProgress, 3, 118, 194, 10,
                new ProgressTexture(ColorPattern.T_GRAY.rectTexture().setRadius(5f), ColorPattern.GREEN.rectTexture().setRadius(5f))
                        .setFillDirection(ProgressTexture.FillDirection.LEFT_TO_RIGHT));
        progressBar.setClientSideWidget();
        content.addWidget(progressBar);

        boneField = new TextFieldWidget(3, 132, 194, 14, null, null);
        boneField.setCurrentString(currentBoneName);
        boneField.setTextResponder(this::onBoneTextChanged);
        content.addWidget(boneField);

        boneSuggestScroll = new DraggableScrollableWidgetGroup(3, 148, 194, 60);
        boneSuggestScroll.setVisible(false);
        content.addWidget(boneSuggestScroll);
    }

    private double getProgress() {
        if (!isPlaying || currentAnimator == null || totalTime <= 0) return 0;
        AnimationPlayer player = currentAnimator.baseLayer.animationPlayer;
        if (player == null) return 0;
        return Math.min(player.getElapsedTime() / totalTime, 1.0);
    }

    @Override
    public void updateScreen() {
        super.updateScreen();

        if (!PhotonEditorConfig.epicfightPreview) {
            if (isPlaying) {
                stopAnimation();
            }
            return;
        }

        applyBoneBinding();

        PhotonPatchStore.syncCloneToPlayer();

        if (!isPlaying || currentAnimator == null) return;
        if (currentPatch == null) return;

        AnimationPlayer player = currentAnimator.baseLayer.animationPlayer;
        if (player == null) return;

        if (player.isEnd()) {
            currentAnimator.setHardPause(false);
            currentAnimator.baseLayer.disableLayer();
            isPlaying = false;
            PhotonPatchStore.IS_PLAYING = false;
            resetClonePosition();
            updateMainButton();
            return;
        }

        player.tick(currentPatch);

        applyBoneBinding();
    }

    private void applyBoneBinding() {
        if (currentBoneName == null || currentBoneName.isEmpty()) {
            PhotonPatchStore.FX_RUNTIME = null;
            PhotonPatchStore.BONE_POSITION = null;
            PhotonPatchStore.BONE_ROTATION = null;
            return;
        }
        if (!(editor instanceof FXEditor fxEditor)) return;
        if (!(fxEditor.getTabPages().focus instanceof ParticleScenePanel panel)) return;
        if (panel.getRuntime() == null) return;

        PhotonPatchStore.FX_RUNTIME = panel.getRuntime();

        LivingEntityPatch<?> patch = getClonePatch();
        if (patch == null || patch.getArmature() == null) return;

        Joint joint = ArmatureResolver.jointByName(patch.getArmature(), currentBoneName);
        if (joint == null) return;

        LivingEntity living = (LivingEntity) patch.getOriginal();
        Vec3 pos = living.getPosition(1.0F);

        Pose rawPose;
        if (patch.getAnimator() instanceof ClientAnimator ca) {
            rawPose = ca.baseLayer.animationPlayer.getCurrentPose(patch, 1.0F);
        } else {
            rawPose = patch.getAnimator().getPose(1.0F);
        }

        OpenMatrix4f modelTf = OpenMatrix4f.createTranslation((float) pos.x, (float) pos.y, (float) pos.z)
                .mulBack(OpenMatrix4f.createRotatorDeg(180.0F, Vec3f.Y_AXIS).mulBack(patch.getModelMatrix(1.0F)));
        OpenMatrix4f boneTf = new OpenMatrix4f(patch.getArmature().getBindedTransformFor(rawPose, joint)).mulFront(modelTf);
        Vec3 bonePos = OpenMatrix4f.transform(boneTf, Vec3.ZERO);

        PhotonPatchStore.BONE_POSITION = new Vector3f((float) bonePos.x, (float) bonePos.y, (float) bonePos.z);

        OpenMatrix4f rotMatrix = boneTf.removeScale();
        rotMatrix.m30 = 0; rotMatrix.m31 = 0; rotMatrix.m32 = 0;
        org.joml.Matrix4f jomlMat = new org.joml.Matrix4f(
                rotMatrix.m00, rotMatrix.m01, rotMatrix.m02, 0,
                rotMatrix.m10, rotMatrix.m11, rotMatrix.m12, 0,
                rotMatrix.m20, rotMatrix.m21, rotMatrix.m22, 0,
                0, 0, 0, 1);
        PhotonPatchStore.BONE_ROTATION = new Quaternionf().setFromUnnormalized(jomlMat);
    }

    private void updateMainButton() {
        String text = isPlaying ? "§c停止" : "§a播放";
        mainBtn.setBackground(new GuiTextureGroup(ResourceBorderTexture.BUTTON_COMMON,
                new TextTexture(text).setWidth(60).setType(TextTexture.TextType.ROLL)));
    }

    private void onTextChanged(String newText) {
        currentAnimationPath = newText;
        updateSuggestions();
    }

    private void onBoneTextChanged(String newText) {
        currentBoneName = newText;
        updateBoneSuggestions();
    }

    private Set<ResourceLocation> getAnimationKeys() {
        return AnimationManager.getInstance().getAnimations(a -> true).keySet();
    }

    private void updateSuggestions() {
        String input = currentAnimationPath.toLowerCase().trim();
        matchedAnimations.clear();

        if (input.isEmpty()) {
            suggestionScroll.setVisible(false);
            suggestionScroll.clearAllWidgets();
            return;
        }

        Set<ResourceLocation> keys = getAnimationKeys();
        for (ResourceLocation key : keys) {
            if (key.toString().toLowerCase().contains(input)) {
                matchedAnimations.add(key.toString());
            }
        }

        suggestionScroll.clearAllWidgets();
        if (matchedAnimations.isEmpty()) {
            suggestionScroll.setVisible(false);
            return;
        }

        int itemHeight = 14;
        int gap = 2;
        int totalHeight = matchedAnimations.size() * (itemHeight + gap);

        for (int i = 0; i < matchedAnimations.size(); i++) {
            String animPath = matchedAnimations.get(i);
            final String selectedPath = animPath;

            IGuiTexture itemTexture = new GuiTextureGroup(
                    ColorPattern.T_GRAY.rectTexture(),
                    ColorPattern.GRAY.borderTexture(-1),
                    new TextTexture(animPath).setWidth(194).setType(TextTexture.TextType.NORMAL));

            var btn = new ButtonWidget(0, i * (itemHeight + gap), 194, itemHeight, itemTexture, cd -> {
                searchField.setCurrentString(selectedPath);
                currentAnimationPath = selectedPath;
                suggestionScroll.setVisible(false);
                suggestionScroll.clearAllWidgets();
            });
            suggestionScroll.addWidget(btn);
        }

        suggestionScroll.setSize(194, Math.min(totalHeight, 80));
        suggestionScroll.setVisible(true);
    }

    private void updateBoneSuggestions() {
        String input = currentBoneName.toLowerCase().trim();
        matchedBones.clear();

        if (input.isEmpty()) {
            boneSuggestScroll.setVisible(false);
            boneSuggestScroll.clearAllWidgets();
            return;
        }

        LivingEntityPatch<?> patch = getClonePatch();
        if (patch == null || patch.getArmature() == null) {
            boneSuggestScroll.setVisible(false);
            boneSuggestScroll.clearAllWidgets();
            return;
        }

        List<String> allJoints = ArmatureResolver.allJointNames(patch.getArmature());
        for (String jointName : allJoints) {
            if (jointName.toLowerCase().contains(input)) {
                matchedBones.add(jointName);
            }
        }

        boneSuggestScroll.clearAllWidgets();
        if (matchedBones.isEmpty()) {
            boneSuggestScroll.setVisible(false);
            return;
        }

        int itemHeight = 14;
        int gap = 2;
        int totalHeight = matchedBones.size() * (itemHeight + gap);

        for (int i = 0; i < matchedBones.size(); i++) {
            String boneName = matchedBones.get(i);
            final String selectedBone = boneName;

            IGuiTexture itemTexture = new GuiTextureGroup(
                    ColorPattern.T_GRAY.rectTexture(),
                    ColorPattern.GRAY.borderTexture(-1),
                    new TextTexture(boneName).setWidth(194).setType(TextTexture.TextType.NORMAL));

            var btn = new ButtonWidget(0, i * (itemHeight + gap), 194, itemHeight, itemTexture, cd -> {
                boneField.setCurrentString(selectedBone);
                currentBoneName = selectedBone;
                boneSuggestScroll.setVisible(false);
                boneSuggestScroll.clearAllWidgets();
            });
            boneSuggestScroll.addWidget(btn);
        }

        boneSuggestScroll.setSize(194, Math.min(totalHeight, 60));
        boneSuggestScroll.setVisible(true);
    }

    private LivingEntityPatch<?> getClonePatch() {
        int cloneId = PhotonPatchStore.CLONE_ID;
        if (cloneId < 0) return null;
        var entry = PhotonPatchStore.PATCHES.get(cloneId);
        return entry instanceof LivingEntityPatch<?> livingPatch ? livingPatch : null;
    }

    private void playAnimation() {
        suggestionScroll.setVisible(false);
        suggestionScroll.clearAllWidgets();
        if (currentAnimationPath == null || currentAnimationPath.isEmpty()) return;

        try {
            currentPatch = getClonePatch();
            if (currentPatch == null) return;

            ResourceLocation rl = ResourceLocation.parse(currentAnimationPath);
            var accessor = AnimationManager.byKey(rl);
            if (accessor == null || accessor.isEmpty()) return;

            currentAnimator = (ClientAnimator) currentPatch.getAnimator();

            currentAnimator.setHardPause(true);
            currentAnimator.baseLayer.playAnimationInstantly(accessor, currentAnimator.getEntityPatch());

            currentAnimation = accessor.get();
            totalTime = currentAnimation == null ? 0 : currentAnimation.getTotalTime();
            if (totalTime <= 0) return;

            currentAnimator.baseLayer.animationPlayer.setElapsedTime(0, 0);

            isPlaying = true;
            PhotonPatchStore.IS_PLAYING = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void stopAnimation() {
        isPlaying = false;
        PhotonPatchStore.IS_PLAYING = false;

        if (currentAnimator != null) {
            currentAnimator.setHardPause(false);
            currentAnimator.baseLayer.disableLayer();
        }

        resetClonePosition();
    }

    private void resetClonePosition() {
        try {
            int cloneId = PhotonPatchStore.CLONE_ID;
            if (cloneId < 0) return;
            var entry = PhotonPatchStore.PATCHES.get(cloneId);
            if (entry != null && entry.getOriginal() != null) {
                var entity = entry.getOriginal();
                entity.setPos(0.5, 1.0, 0.5);
                entity.setDeltaMovement(0, 0, 0);
                entity.setYRot(0);
                entity.setXRot(0);
                if (entity instanceof LivingEntity living) {
                    living.yBodyRot = 0;
                    living.yBodyRotO = 0;
                    living.yHeadRot = 0;
                    living.yHeadRotO = 0;
                }
            }
        } catch (Exception ignored) {}
    }
}
