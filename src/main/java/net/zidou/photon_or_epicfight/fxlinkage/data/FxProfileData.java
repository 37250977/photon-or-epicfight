package net.zidou.photon_or_epicfight.fxlinkage.data;

import java.util.List;

public class FxProfileData {
    public String type;
    public String name;
    public String fx;
    public String position;
    public Boolean follow;
    public String bone;
    public Object follow_rotation;
    public Float scale;
    public Integer duration;
    public Boolean inherit_color;
    public List<FxCondition> conditions;
    public List<FxCommand> commands;
    public Boolean allow_multi;
}
