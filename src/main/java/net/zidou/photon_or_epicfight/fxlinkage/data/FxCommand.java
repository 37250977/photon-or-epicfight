package net.zidou.photon_or_epicfight.fxlinkage.data;

import java.util.List;
import java.util.Map;

public class FxCommand {
    public String type;
    public String fx;
    public String sound;
    public String position;
    public Boolean follow;
    public String bone;
    public Object follow_rotation;
    public Integer duration;
    public Boolean inherit_color;
    public Boolean inherit_size;
    public Boolean inherit_rotation;
    public Map<String, Object> params;
    public List<FxCondition> conditions;
    public List<FxCommand> commands;
    public Double fx_damage;
    public Boolean bypass_iframe;
    public String value;
    public Double scale;
    public Float volume;
    public Float pitch;
    public String command;
    public Boolean allow_multi;

    public enum CommandType {
        SPAWN_FX, SPAWN_FX_BURST,
        PLAY_SOUND, DAMAGE,
        SET_PHASE, SET_COOLDOWN, INCREMENT_COUNTER, RESET_COUNTER,
        COMMAND;

        public static CommandType fromString(String s) {
            try {
                return valueOf(s.toUpperCase().replace(":", "_"));
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
    }

    public CommandType getCommandType() {
        return CommandType.fromString(type);
    }
}
