package com.smlj.common.o;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ValuePasser<T> {
    protected T target;
    protected String fieldName1;
    protected String fieldName2 = null;
    protected String fieldName3 = null;

    protected String aimName1;
    protected String aimName2 = null;
    protected String aimName3 = null;
    protected String aimName4 = null;
    protected String aimName5 = null;
    protected String aimName6 = null;
    protected String aimName7 = null;
    protected String aimName8 = null;
    protected String aimName9 = null;

    public ValuePasser setFieldNames(String fieldName1) {
        this.fieldName1 = fieldName1;
        return this;
    }

    public ValuePasser setFieldNames(String fieldName1, String fieldName2) {
        var t = setFieldNames(fieldName1);
        t.fieldName2 = fieldName2;
        return this;
    }

    public ValuePasser setFieldNames(String fieldName1, String fieldName2, String fieldName3) {
        var t = setFieldNames(fieldName1, fieldName2);
        t.fieldName3 = fieldName3;
        return this;
    }

    public ValuePasser by1_1(String fieldName1, String aimName1, T from) {
        this.fieldName1 = fieldName1;
        this.aimName1 = aimName1;
        this.target = from;
        return this;
    }

    public ValuePasser by2_1(String fieldName1, String fieldName2, String aimName1, T from) {
        by1_1(fieldName1, aimName1, from);
        this.fieldName2 = fieldName2;
        this.target = from;
        return this;
    }

    public ValuePasser by3_1(String fieldName1, String fieldName2, String fieldName3, String aimName1, T from) {
        this.by2_1(fieldName1, fieldName2, aimName1, from);
        this.fieldName3 = fieldName3;
        this.target = from;
        return this;
    }

    public ValuePasser by1_2(String fieldName1, String aimName1, String aimName2, T from) {
        var t = by1_1(fieldName1, aimName1, from);
        t.aimName2 = aimName2;
        return this;
    }

    public ValuePasser by2_2(String fieldName1, String fieldName2, String aimName1, String aimName2, T from) {
        var t = this.by2_1(fieldName1, fieldName2, aimName1, from);
        t.aimName2 = aimName2;
        return this;
    }

    public ValuePasser by3_2(String fieldName1, String fieldName2, String fieldName3, String aimName1, String aimName2, T from) {
        var t = this.by3_1(fieldName1, fieldName2, fieldName3, aimName1, from);
        t.aimName2 = aimName2;
        return this;
    }

    public ValuePasser by1_3(String fieldName1, String aimName1, String aimName2, String aimName3, T from) {
        var t = by1_2(fieldName1, aimName1, aimName2, from);
        t.aimName3 = aimName3;
        return this;
    }

    public ValuePasser by2_3(String fieldName1, String fieldName2, String aimName1, String aimName2, String aimName3, T from) {
        var t = this.by2_2(fieldName1, fieldName2, aimName1, aimName2, from);
        t.aimName3 = aimName3;
        return this;
    }

    public ValuePasser by3_3(String fieldName1, String fieldName2, String fieldName3, String aimName1, String aimName2, String aimName3, T from) {
        var t = this.by3_2(fieldName1, fieldName2, fieldName3, aimName1, aimName2, from);
        t.aimName3 = aimName3;
        return this;
    }

    public ValuePasser by1_4(String fieldName1, String aimName1, String aimName2, String aimName3, String aimName4, T from) {
        var t = by1_3(fieldName1, aimName1, aimName2, aimName3, from);
        t.aimName4 = aimName4;
        return this;
    }

    public ValuePasser by2_4(String fieldName1, String fieldName2, String aimName1, String aimName2, String aimName3, String aimName4, T from) {
        var t = this.by2_3(fieldName1, fieldName2, aimName1, aimName2, aimName3, from);
        t.aimName4 = aimName4;
        return this;
    }

    public ValuePasser by3_4(String fieldName1, String fieldName2, String fieldName3, String aimName1, String aimName2, String aimName3, String aimName4, T from) {
        var t = this.by3_3(fieldName1, fieldName2, fieldName3, aimName1, aimName2, aimName3, from);
        t.aimName4 = aimName4;
        return this;
    }

    public ValuePasser by1_5(String fieldName1, String aimName1, String aimName2, String aimName3, String aimName4, String aimName5, T from) {
        var t = by1_4(fieldName1, aimName1, aimName2, aimName3, aimName4, from);
        t.aimName5 = aimName5;
        return this;
    }

    public ValuePasser by2_5(String fieldName1, String fieldName2, String aimName1, String aimName2, String aimName3, String aimName4, String aimName5, T from) {
        var t = this.by2_4(fieldName1, fieldName2, aimName1, aimName2, aimName3, aimName4, from);
        t.aimName5 = aimName5;
        return this;
    }

    public ValuePasser by3_5(String fieldName1, String fieldName2, String fieldName3, String aimName1, String aimName2, String aimName3, String aimName4, String aimName5, T from) {
        var t = this.by3_4(fieldName1, fieldName2, fieldName3, aimName1, aimName2, aimName3, aimName4, from);
        t.aimName5 = aimName5;
        return this;
    }

    public ValuePasser by1_6(String fieldName1, String aimName1, String aimName2, String aimName3, String aimName4, String aimName5, String aimName6, T from) {
        var t = by1_5(fieldName1, aimName1, aimName2, aimName3, aimName4, aimName5, from);
        t.aimName6 = aimName6;
        return this;
    }

    public ValuePasser by2_6(String fieldName1, String fieldName2, String aimName1, String aimName2, String aimName3, String aimName4, String aimName5, String aimName6, T from) {
        var t = this.by2_5(fieldName1, fieldName2, aimName1, aimName2, aimName3, aimName4, aimName5, from);
        t.aimName6 = aimName6;
        return this;
    }

    public ValuePasser by3_6(String fieldName1, String fieldName2, String fieldName3, String aimName1, String aimName2, String aimName3, String aimName4, String aimName5, String aimName6, T from) {
        var t = this.by3_5(fieldName1, fieldName2, fieldName3, aimName1, aimName2, aimName3, aimName4, aimName5, from);
        t.aimName6 = aimName6;
        return this;
    }

    public ValuePasser by1_7(String fieldName1, String aimName1, String aimName2, String aimName3, String aimName4, String aimName5, String aimName6, String aimName7, T from) {
        var t = by1_6(fieldName1, aimName1, aimName2, aimName3, aimName4, aimName5, aimName6, from);
        t.aimName7 = aimName7;
        return this;
    }

    public ValuePasser by2_7(String fieldName1, String fieldName2, String aimName1, String aimName2, String aimName3, String aimName4, String aimName5, String aimName6, String aimName7, T from) {
        var t = this.by2_6(fieldName1, fieldName2, aimName1, aimName2, aimName3, aimName4, aimName5, aimName6, from);
        t.aimName7 = aimName7;
        return this;
    }

    public ValuePasser by3_7(String fieldName1, String fieldName2, String fieldName3, String aimName1, String aimName2, String aimName3, String aimName4, String aimName5, String aimName6, String aimName7, T from) {
        var t = this.by3_6(fieldName1, fieldName2, fieldName3, aimName1, aimName2, aimName3, aimName4, aimName5, aimName6, from);
        t.aimName7 = aimName7;
        return this;
    }

    public ValuePasser by1_8(String fieldName1, String aimName1, String aimName2, String aimName3, String aimName4, String aimName5, String aimName6, String aimName7, String aimName8, T from) {
        var t = by1_7(fieldName1, aimName1, aimName2, aimName3, aimName4, aimName5, aimName6, aimName7, from);
        t.aimName8 = aimName8;
        return this;
    }

    public ValuePasser by2_8(String fieldName1, String fieldName2, String aimName1, String aimName2, String aimName3, String aimName4, String aimName5, String aimName6, String aimName7, String aimName8, T from) {
        var t = this.by2_7(fieldName1, fieldName2, aimName1, aimName2, aimName3, aimName4, aimName5, aimName6, aimName7, from);
        t.aimName8 = aimName8;
        return this;
    }

    public ValuePasser by3_8(String fieldName1, String fieldName2, String fieldName3, String aimName1, String aimName2, String aimName3, String aimName4, String aimName5, String aimName6, String aimName7, String aimName8, T from) {
        var t = this.by3_7(fieldName1, fieldName2, fieldName3, aimName1, aimName2, aimName3, aimName4, aimName5, aimName6, aimName7, from);
        t.aimName8 = aimName8;
        return this;
    }

    public ValuePasser by1_9(String fieldName1, String aimName1, String aimName2, String aimName3, String aimName4, String aimName5, String aimName6, String aimName7, String aimName8, String aimName9, T from) {
        var t = by1_8(fieldName1, aimName1, aimName2, aimName3, aimName4, aimName5, aimName6, aimName7, aimName8, from);
        t.aimName9 = aimName9;
        return this;
    }

    public ValuePasser by2_9(String fieldName1, String fieldName2, String aimName1, String aimName2, String aimName3, String aimName4, String aimName5, String aimName6, String aimName7, String aimName8, String aimName9, T from) {
        var t = this.by2_8(fieldName1, fieldName2, aimName1, aimName2, aimName3, aimName4, aimName5, aimName6, aimName7, aimName8, from);
        t.aimName9 = aimName9;
        return this;
    }

    public ValuePasser by3_9(String fieldName1, String fieldName2, String fieldName3, String aimName1, String aimName2, String aimName3, String aimName4, String aimName5, String aimName6, String aimName7, String aimName8, String aimName9, T from) {
        var t = this.by3_8(fieldName1, fieldName2, fieldName3, aimName1, aimName2, aimName3, aimName4, aimName5, aimName6, aimName7, aimName8, from);
        t.aimName9 = aimName9;
        return this;
    }
}
