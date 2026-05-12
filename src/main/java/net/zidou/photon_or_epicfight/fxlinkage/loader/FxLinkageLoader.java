package net.zidou.photon_or_epicfight.fxlinkage.loader;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.zidou.photon_or_epicfight.fxlinkage.data.FxLinkageData;
import net.zidou.photon_or_epicfight.fxlinkage.data.RuntimeLinkage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class FxLinkageLoader extends SimpleJsonResourceReloadListener {
    private static final Logger LOGGER = LoggerFactory.getLogger("FxLinkage");
    private static final Gson GSON = new GsonBuilder().setLenient().create();
    private static final String FOLDER = "fx_linkage";

    private static final Set<String> EVENT_KEYS = Set.of(
            "on_skill_start", "on_skill_end", "on_hit", "on_guard", "on_combo",
            "on_charged", "on_dodge", "on_parry", "on_kill",
            "on_blocked", "on_first_hit", "on_phase_change", "on_airborne",
            "on_stun", "on_knockdown", "on_execution", "events"
    );

    private static volatile Map<ResourceLocation, RuntimeLinkage> linkages = Collections.emptyMap();
    private static volatile List<RuntimeLinkage> sortedLinkages = Collections.emptyList();

    public FxLinkageLoader() {
        super(GSON, FOLDER);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> objects,
                         ResourceManager manager, ProfilerFiller profiler) {
        Map<ResourceLocation, RuntimeLinkage> result = new HashMap<>();

        for (Map.Entry<ResourceLocation, JsonElement> entry : objects.entrySet()) {
            ResourceLocation id = entry.getKey();
            try {
                JsonElement element = entry.getValue();
                // 将所有 on_xxx / events 的单对象自动包装成数组，统一支持 {} 和 [] 两种写法
                if (element instanceof JsonObject obj) {
                    for (String key : EVENT_KEYS) {
                        if (obj.has(key) && obj.get(key).isJsonObject()) {
                            JsonArray arr = new JsonArray();
                            arr.add(obj.get(key));
                            obj.add(key, arr);
                        }
                    }
                }
                FxLinkageData data = GSON.fromJson(element, FxLinkageData.class);
                if (data == null) continue;
                if (!"epicfight_fx:linkage".equals(data.type)) continue;
                result.put(id, new RuntimeLinkage(data, id));
                LOGGER.debug("Loaded FX linkage: {}", id);
            } catch (Exception e) {
                LOGGER.error("Failed to load FX linkage {}: {}", id, e.getMessage());
            }
        }

        linkages = result;
        sortedLinkages = result.values().stream()
                .sorted(Comparator.comparingInt(l -> -l.priority))
                .collect(Collectors.toList());

        LOGGER.info("Loaded {} FX linkages", result.size());
    }

    public static List<RuntimeLinkage> getAllLinkages() {
        return sortedLinkages;
    }

    public static List<RuntimeLinkage> getLinkagesForEvent(String event) {
        return sortedLinkages.stream()
                .filter(l -> l.eventEffects.containsKey(event))
                .collect(Collectors.toList());
    }

    public static RuntimeLinkage getLinkage(ResourceLocation id) {
        return linkages.get(id);
    }
}
