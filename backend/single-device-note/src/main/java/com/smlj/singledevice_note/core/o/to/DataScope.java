package com.smlj.singledevice_note.core.o.to;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 数据范围档位（cght.t_user.data_scope）。
 *
 * <p>范围的唯一来源是账号行自己的 {@code data_scope}（NOT NULL，新建必填），角色侧不再有任何
 * 范围字段，所以每个常量的注释只跟 t_user 对表。
 *
 * <p>⚠️ 编号本身就是「包含序」，而且是<strong>严格</strong>包含序：
 * {归属部门子树} ⊂ {所属公司子树} ⊂ {全集}。
 * 「本部门」档自带下级，所以不存在单独的「本部门及下级」档 —— 同一个 dept_code 下两者都展开成
 * {@code subtreeOf(dept_code)}，结果完全一致，多留一档只会让人以为有得选。
 * 一级真正包含一级之后，"该配哪一档"只剩一个问题：这个人管到部门、公司，还是整个集团。
 *
 * <p>比较一律走 {@link #atLeast}，不要比对 {@code code}、更不要用 {@code ordinal()}：
 * 新增档位时只要插在包含序的正确位置上，判定就自动跟着走。
 *
 * <p>前端 gd.json 的 dataScope 字典（1/2/3/4 + 文案）与本枚举编号一一对应，改编号必须两边一起改。
 */
@Getter
@AllArgsConstructor
public enum DataScope {
    /**
     * 哨兵：<strong>不是</strong>合法档位。
     * <p>两个用途：① 库里 data_scope 为空 / 未知编号时的统一表示（见 {@link #of}），按最严处理；
     * ② {@code @RequirePermission#minScope} 的默认值，表示「本接口不额外要求范围」。
     */
    NONE(0, "未设置"),

    SELF(1, "本人"),
    /** 含全部下级部门 / 分厂 / 中心（BFS 展开，不写死层数） */
    DEPT(2, "本部门"),
    /** 起点向上定位到的公司节点，取其整棵子树 */
    COMPANY(3, "本公司"),
    /** 不限制 */
    ALL(4, "全集团");

    // 自定义状态码（与库中 data_scope 的取值一致）
    private final int code;
    // 自定义描述
    private final String label;

    /**
     * 按库值解析。null / 未知编号一律返回 {@link #NONE}，<strong>绝不返回 null</strong> ——
     * 调用处无需判空，也堵住"忘了判空就当默认档放行"这类越权写法。
     */
    public static DataScope of(Integer code) {
        if (code != null) {
            for (DataScope s : values()) {
                if (s != NONE && s.code == code) {
                    return s;
                }
            }
        }
        return NONE;
    }

    /**
     * 按接口入参文本解析（语义同 {@link #of(Integer)}）。空串 / 非数字 / 未知编号一律 NONE，
     * 调用处用 {@link #isValid()} 判定，从而"不是数字"与"是数字但不在合法集合内"共用一套处理。
     */
    public static DataScope of(String text) {
        if (text == null) {
            return NONE;
        }
        try {
            return of(Integer.valueOf(text.trim()));
        } catch (NumberFormatException e) {
            return NONE;
        }
    }

    /** 是否为合法档位（{@link #NONE} 不是）。写入侧的入参校验、展开前的合法性判定都用它。 */
    public boolean isValid() {
        return this != NONE;
    }

    /** 本档位是否「不小于」min（编号即包含序）。NONE 不满足任何真实档位 → fail-closed。 */
    public boolean atLeast(DataScope min) {
        return min != null && this.code >= min.code;
    }
}
