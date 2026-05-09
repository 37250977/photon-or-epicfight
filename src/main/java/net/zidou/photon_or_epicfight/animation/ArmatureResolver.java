package net.zidou.photon_or_epicfight.animation;

import yesman.epicfight.api.animation.Joint;
import yesman.epicfight.api.model.Armature;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class ArmatureResolver {

    @Nullable
    public static Joint jointByName(Armature armature, String name) {
        if (armature == null || name == null || name.isEmpty()) return null;

        Joint joint = findField(armature, name);
        if (joint != null) return joint;

        String camelName = toCamelCase(name);
        if (!camelName.equals(name)) {
            joint = findField(armature, camelName);
            if (joint != null) return joint;
        }

        String lowerFirst = Character.toLowerCase(name.charAt(0)) + name.substring(1);
        if (!lowerFirst.equals(name)) {
            joint = findField(armature, lowerFirst);
            if (joint != null) return joint;
        }

        String flat = name.replace("_", "").toLowerCase();
        if (!flat.equals(name)) {
            joint = findField(armature, flat);
            if (joint != null) return joint;
        }

        return null;
    }

    @Nullable
    private static Joint findField(Armature armature, String fieldName) {
        Class<?> clazz = armature.getClass();
        while (clazz != null && clazz != Object.class) {
            try {
                Field field = clazz.getDeclaredField(fieldName);
                field.setAccessible(true);
                Object value = field.get(armature);
                if (value instanceof Joint joint) {
                    return joint;
                }
            } catch (NoSuchFieldException | IllegalAccessException ignored) {
            }
            clazz = clazz.getSuperclass();
        }
        return null;
    }

    private static String toCamelCase(String name) {
        StringBuilder sb = new StringBuilder();
        boolean nextUpper = false;
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c == '_') {
                nextUpper = true;
            } else if (nextUpper) {
                sb.append(Character.toUpperCase(c));
                nextUpper = false;
            } else {
                sb.append(i == 0 ? Character.toLowerCase(c) : c);
            }
        }
        return sb.toString();
    }

    public static List<String> allJointNames(Armature armature) {
        List<String> names = new ArrayList<>();
        Class<?> clazz = armature.getClass();
        while (clazz != null && clazz != Object.class) {
            for (var field : clazz.getDeclaredFields()) {
                if (Joint.class.isAssignableFrom(field.getType())) {
                    names.add(field.getName());
                }
            }
            clazz = clazz.getSuperclass();
        }
        return names;
    }
}
