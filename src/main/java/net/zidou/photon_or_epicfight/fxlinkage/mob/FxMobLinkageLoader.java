package net.zidou.photon_or_epicfight.fxlinkage.mob;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class FxMobLinkageLoader extends SimpleJsonResourceReloadListener {
    private static final Logger LOGGER = LoggerFactory.getLogger("FxMobLinkage");
    private static final Gson GSON = new GsonBuilder().setLenient().create();
    private static final String FOLDER = "fx_linkage_mob";

    private static final Set<String> EVENT_KEYS = Set.of(
            "on_hit", "on_kill", "on_blocked", "on_first_hit",
            "on_stun", "on_knockdown", "on_guard", "on_parry", "on_dodge",
            "events"
    );

    private static volatile Map<ResourceLocation, RuntimeMobLinkage> linkages = Collections.emptyMap();
    private static volatile List<RuntimeMobLinkage> sortedLinkages = Collections.emptyList();

    public FxMobLinkageLoader() {
        super(GSON, FOLDER);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> objects,
                         ResourceManager manager, ProfilerFiller profiler) {
        Map<ResourceLocation, RuntimeMobLinkage> result = new HashMap<>();

        for (Map.Entry<ResourceLocation, JsonElement> entry : objects.entrySet()) {
            ResourceLocation id = entry.getKey();
            try {
                JsonElement element = entry.getValue();
                if (element instanceof JsonObject obj) {
                    for (String key : EVENT_KEYS) {
                        if (obj.has(key) && obj.get(key).isJsonObject()) {
                            JsonArray arr = new JsonArray();
                            arr.add(obj.get(key));
                            obj.add(key, arr);
                        }
                    }
                }
                FxMobLinkageData data = GSON.fromJson(element, FxMobLinkageData.class);
                if (data == null) continue;
                if (!"epicfight_fx:mob_linkage".equals(data.type)) continue;
                result.put(id, new RuntimeMobLinkage(data, id));
                LOGGER.debug("Loaded mob FX linkage: {}", id);
            } catch (Exception e) {
                LOGGER.error("Failed to load mob FX linkage {}: {}", id, e.getMessage());
            }
        }

        linkages = result;
        sortedLinkages = result.values().stream()
                .sorted(Comparator.comparingInt(l -> -l.priority))
                .collect(Collectors.toList());

        LOGGER.info("Loaded {} mob FX linkages", result.size());
    }

    public static List<RuntimeMobLinkage> getAllLinkages() {
        return sortedLinkages;
    }

    public static List<RuntimeMobLinkage> getLinkagesForEvent(String event) {
        return sortedLinkages.stream()
                .filter(l -> l.eventEffects.containsKey(event))
                .collect(Collectors.toList());
    }

    public static RuntimeMobLinkage getLinkage(ResourceLocation id) {
        return linkages.get(id);
    }
}
