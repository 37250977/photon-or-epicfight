package net.zidou.photon_or_epicfight.fxlinkage.data;

import java.util.List;

public class FxEffect {
    public String trigger;
    public String fx;
    public String profile;
    public String position;
    public Boolean follow;
    public String bone;
    public Object follow_rotation;
    public Integer duration;
    public Boolean inherit_color;
    public Float scale;
    public List<FxCommand> commands;
    public List<FxCondition> conditions;
    public String priority;
    public Boolean allow_multi;
}
