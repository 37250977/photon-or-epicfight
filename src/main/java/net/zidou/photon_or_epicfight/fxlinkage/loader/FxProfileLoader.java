package net.zidou.photon_or_epicfight.fxlinkage.loader;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.zidou.photon_or_epicfight.fxlinkage.data.FxProfileData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class FxProfileLoader extends SimpleJsonResourceReloadListener {
    private static final Logger LOGGER = LoggerFactory.getLogger("FxProfile");
    private static final Gson GSON = new GsonBuilder().setLenient().create();
    private static final String FOLDER = "fx_profiles";

    private static volatile Map<String, FxProfileData> profiles = Collections.emptyMap();

    public FxProfileLoader() {
        super(GSON, FOLDER);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> objects,
                         ResourceManager manager, ProfilerFiller profiler) {
        Map<String, FxProfileData> result = new HashMap<>();
        for (Map.Entry<ResourceLocation, JsonElement> entry : objects.entrySet()) {
            try {
                FxProfileData data = GSON.fromJson(entry.getValue(), FxProfileData.class);
                if (data != null && data.name != null) {
                    result.put(data.name, data);
                }
            } catch (Exception e) {
                LOGGER.error("Failed to load FX profile {}: {}", entry.getKey(), e.getMessage());
            }
        }
        profiles = result;
        LOGGER.info("Loaded {} FX profiles", result.size());
    }

    public static FxProfileData getProfile(String name) {
        return profiles.get(name);
    }
}
