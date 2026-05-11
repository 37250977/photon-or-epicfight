package net.zidou.photon_or_epicfight.mixin;

import com.lowdragmc.lowdraglib.gui.editor.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.gui.editor.runtime.AnnotationDetector;
import com.lowdragmc.lowdraglib.gui.editor.ui.view.FloatViewWidget;
import net.zidou.photon_or_epicfight.editor_config.PhotonEditorConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

@Mixin(targets = "com.lowdragmc.lowdraglib.gui.editor.ui.menu.ViewMenu", remap = false)
public class MixinViewMenu {

    @Redirect(method = "createMenu", at = @At(value = "FIELD", target = "Lcom/lowdragmc/lowdraglib/gui/editor/runtime/AnnotationDetector;REGISTER_FLOAT_VIEWS:Ljava/util/List;"), remap = false)
    private List<AnnotationDetector.Wrapper<LDLRegister, FloatViewWidget>> photon$filterViews() {
        if (PhotonEditorConfig.epicfightPreview) {
            return AnnotationDetector.REGISTER_FLOAT_VIEWS;
        }
        return AnnotationDetector.REGISTER_FLOAT_VIEWS.stream()
                .filter(w -> !"animation_control".equals(w.annotation().name()))
                .toList();
    }
}
