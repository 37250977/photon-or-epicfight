package net.zidou.photon_or_epicfight;

import yesman.epicfight.api.animation.Joint;
import yesman.epicfight.api.model.Armature;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Epic Fight Armature 关节查找工具。
 * <p>
 * 用反射从 Armature 子类的 Joint 类型公开字段中按名称查找 Joint 对象。
 * HumanoidArmature(BIPED) 的常见字段名：toolR, toolL, handR, handL, head, chest, rootJoint 等。
 */
public class ArmatureResolver {

    /**
     * 按名称从 Armature 中查找 Joint。
     * <p>
     * 查找顺序：
     * 1. 用字段名精确匹配（如 "toolR"）
     * 2. 尝试转换下划线命名为驼峰命名（如 "Tool_R" → "toolR"）
     * 3. 递归查找父类的字段
     *
     * @param armature Epic Fight 骨架对象
     * @param name     关节名称（如 "Tool_R", "toolR", "Head", "head"）
     * @return Joint 对象，未找到则返回 null
     */
    @Nullable
    public static Joint jointByName(Armature armature, String name) {
        if (armature == null || name == null || name.isEmpty()) return null;

        // 先尝试直接按字段名查找
        Joint joint = findField(armature, name);
        if (joint != null) return joint;

        // 尝试驼峰转换：Tool_R → toolR, Hand_L → handL
        String camelName = toCamelCase(name);
        if (!camelName.equals(name)) {
            joint = findField(armature, camelName);
            if (joint != null) return joint;
        }

        // 尝试首字母小写：Tool_R → tool_R
        String lowerFirst = Character.toLowerCase(name.charAt(0)) + name.substring(1);
        if (!lowerFirst.equals(name)) {
            joint = findField(armature, lowerFirst);
            if (joint != null) return joint;
        }

        // 尝试全小写无下划线：Tool_R → toolr
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

    /**
     * 将下划线命名转换为驼峰命名。
     * Tool_R → toolR
     * Head → head
     */
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

    /**
     * 获取骨架中所有关节的名称列表。
     * 通过反射扫描 Armature 子类中所有 Joint 类型的公开字段。
     */
    public static List<String> allJointNames(Armature armature) {
        List<String> names = new ArrayList<>();
        Class<?> clazz = armature.getClass();
        while (clazz != null && clazz != Object.class) {
            for (Field field : clazz.getDeclaredFields()) {
                if (Joint.class.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    try {
                        if (field.get(armature) != null) {
                            // 将驼峰或原始字段名转为用户友好的格式
                            String name = field.getName();
                            // toolR → Tool_R, head → Head
                            names.add(name);
                        }
                    } catch (IllegalAccessException ignored) { }
                }
            }
            clazz = clazz.getSuperclass();
        }
        return names;
    }
}
