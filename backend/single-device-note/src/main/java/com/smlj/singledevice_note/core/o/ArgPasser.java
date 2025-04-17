package com.smlj.common.o;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ArgPasser {
    protected String fieldName1;
    protected Object fieldValue1;

    protected String fieldName2 = null;
    protected Object fieldValue2 = null;
    protected String fieldName3 = null;
    protected Object fieldValue3 = null;

    protected String aimName1;
    protected Object aimValue1;
    protected String aimName2 = null;
    protected Object aimValue2 = null;
    protected String aimName3 = null;
    protected Object aimValue3 = null;
    protected String aimName4 = null;
    protected Object aimValue4 = null;
    protected String aimName5 = null;
    protected Object aimValue5 = null;
    protected String aimName6 = null;
    protected Object aimValue6 = null;
    protected String aimName7 = null;
    protected Object aimValue7 = null;
    protected String aimName8 = null;
    protected Object aimValue8 = null;
    protected String aimName9 = null;
    protected Object aimValue9 = null;

    public ArgPasser setFieldNames(String fieldName1) {
        this.fieldName1 = fieldName1;
        return this;
    }

    public ArgPasser setFieldNames(String fieldName1, String fieldName2) {
        var t = setFieldNames(fieldName1);
        t.fieldName2 = fieldName2;
        return this;
    }

    public ArgPasser setFieldNames(String fieldName1, String fieldName2, String fieldName3) {
        var t = setFieldNames(fieldName1, fieldName2);
        t.fieldName3 = fieldName3;
        return this;
    }

    public ArgPasser setFields(String fieldName1, Object fieldValue1) {
        this.fieldName1 = fieldName1;
        this.fieldValue1 = fieldValue1;
        return this;
    }

    public ArgPasser setFields(String fieldName1, Object fieldValue1, String fieldName2, Object fieldValue2) {
        var t = this.setFields(fieldName1, fieldValue1);
        t.fieldName2 = fieldName2;
        t.fieldValue2 = fieldValue2;
        return this;
    }

    public ArgPasser setFields(String fieldName1, Object fieldValue1, String fieldName2, Object fieldValue2, String fieldName3, Object fieldValue3) {
        var t = this.setFields(fieldName1, fieldValue1, fieldName2, fieldValue2);
        t.fieldName3 = fieldName3;
        t.fieldValue3 = fieldValue3;
        return this;
    }

    public ArgPasser by1_1(String fieldName1, Object fieldValue1, String aimName1, Object aimValue1) {
        this.fieldName1 = fieldName1;
        this.fieldValue1 = fieldValue1;
        this.aimName1 = aimName1;
        this.aimValue1 = aimValue1;
        return this;
    }

    public ArgPasser by2_1(String fieldName1, Object fieldValue1, String fieldName2, Object fieldValue2, String aimName1, Object aimValue1) {
        by1_1(fieldName1, fieldValue1, aimName1, aimValue1);
        this.fieldName2 = fieldName2;
        this.fieldValue2 = fieldValue2;
        return this;
    }

    public ArgPasser by3_1(String fieldName1, Object fieldValue1, String fieldName2, Object fieldValue2, String fieldName3, Object fieldValue3, String aimName1, Object aimValue1) {
        this.by2_1(fieldName1, fieldValue1, fieldName2, fieldValue2, aimName1, aimValue1);
        this.fieldName3 = fieldName3;
        this.fieldValue3 = fieldValue3;
        return this;
    }

    public ArgPasser by1_2(String fieldName1, Object fieldValue1, String aimName1, Object aimValue1, String aimName2, Object aimValue2) {
        var t = by1_1(fieldName1, fieldValue1, aimName1, aimValue1);
        t.aimName2 = aimName2;
        t.aimValue2 = aimValue2;
        return this;
    }

    public ArgPasser by2_2(String fieldName1, Object fieldValue1, String fieldName2, Object fieldValue2, String aimName1, Object aimValue1, String aimName2, Object aimValue2) {
        var t = this.by2_1(fieldName1, fieldValue1, fieldName2, fieldValue2, aimName1, aimValue1);
        t.aimName2 = aimName2;
        t.aimValue2 = aimValue2;
        return this;
    }

    public ArgPasser by3_2(String fieldName1, Object fieldValue1, String fieldName2, Object fieldValue2, String fieldName3, Object fieldValue3, String aimName1, Object aimValue1, String aimName2, Object aimValue2) {
        var t = this.by3_1(fieldName1, fieldValue1, fieldName2, fieldValue2, fieldName3, fieldValue3, aimName1, aimValue1);
        t.aimName2 = aimName2;
        t.aimValue2 = aimValue2;
        return this;
    }

    public ArgPasser by1_3(String fieldName1, Object fieldValue1, String aimName1, Object aimValue1, String aimName2, Object aimValue2, String aimName3, Object aimValue3) {
        var t = by1_2(fieldName1, fieldValue1, aimName1, aimValue1, aimName2, aimValue2);
        t.aimName3 = aimName3;
        t.aimValue3 = aimValue3;
        return this;
    }

    public ArgPasser by2_3(String fieldName1, Object fieldValue1, String fieldName2, Object fieldValue2, String aimName1, Object aimValue1, String aimName2, Object aimValue2, String aimName3, Object aimValue3) {
        var t = this.by2_2(fieldName1, fieldValue1, fieldName2, fieldValue2, aimName1, aimValue1, aimName2, aimValue2);
        t.aimName3 = aimName3;
        t.aimValue3 = aimValue3;
        return this;
    }

    public ArgPasser by3_3(String fieldName1, Object fieldValue1, String fieldName2, Object fieldValue2, String fieldName3, Object fieldValue3, String aimName1, Object aimValue1, String aimName2, Object aimValue2, String aimName3, Object aimValue3) {
        var t = this.by3_2(fieldName1, fieldValue1, fieldName2, fieldValue2, fieldName3, fieldValue3, aimName1, aimValue1, aimName2, aimValue2);
        t.aimName3 = aimName3;
        t.aimValue3 = aimValue3;
        return this;
    }

    public ArgPasser by1_4(String fieldName1, Object fieldValue1, String aimName1, Object aimValue1, String aimName2, Object aimValue2, String aimName3, Object aimValue3, String aimName4, Object aimValue4) {
        var t = by1_3(fieldName1, fieldValue1, aimName1, aimValue1, aimName2, aimValue2, aimName3, aimValue3);
        t.aimName4 = aimName4;
        t.aimValue4 = aimValue4;
        return this;
    }

    public ArgPasser by2_4(String fieldName1, Object fieldValue1, String fieldName2, Object fieldValue2, String aimName1, Object aimValue1, String aimName2, Object aimValue2, String aimName3, Object aimValue3, String aimName4, Object aimValue4) {
        var t = this.by2_3(fieldName1, fieldValue1, fieldName2, fieldValue2, aimName1, aimValue1, aimName2, aimValue2, aimName3, aimValue3);
        t.aimName4 = aimName4;
        t.aimValue4 = aimValue4;
        return this;
    }

    public ArgPasser by3_4(String fieldName1, Object fieldValue1, String fieldName2, Object fieldValue2, String fieldName3, Object fieldValue3, String aimName1, Object aimValue1, String aimName2, Object aimValue2, String aimName3, Object aimValue3, String aimName4, Object aimValue4) {
        var t = this.by3_3(fieldName1, fieldValue1, fieldName2, fieldValue2, fieldName3, fieldValue3, aimName1, aimValue1, aimName2, aimValue2, aimName3, aimValue3);
        t.aimName4 = aimName4;
        t.aimValue4 = aimValue4;
        return this;
    }

    public ArgPasser by1_5(String fieldName1, Object fieldValue1, String aimName1, Object aimValue1, String aimName2, Object aimValue2, String aimName3, Object aimValue3, String aimName4, Object aimValue4, String aimName5, Object aimValue5) {
        var t = by1_4(fieldName1, fieldValue1, aimName1, aimValue1, aimName2, aimValue2, aimName3, aimValue3, aimName4, aimValue4);
        t.aimName5 = aimName5;
        t.aimValue5 = aimValue5;
        return this;
    }

    public ArgPasser by2_5(String fieldName1, Object fieldValue1, String fieldName2, Object fieldValue2, String aimName1, Object aimValue1, String aimName2, Object aimValue2, String aimName3, Object aimValue3, String aimName4, Object aimValue4, String aimName5, Object aimValue5) {
        var t = this.by2_4(fieldName1, fieldValue1, fieldName2, fieldValue2, aimName1, aimValue1, aimName2, aimValue2, aimName3, aimValue3, aimName4, aimValue4);
        t.aimName5 = aimName5;
        t.aimValue5 = aimValue5;
        return this;
    }

    public ArgPasser by3_5(String fieldName1, Object fieldValue1, String fieldName2, Object fieldValue2, String fieldName3, Object fieldValue3, String aimName1, Object aimValue1, String aimName2, Object aimValue2, String aimName3, Object aimValue3, String aimName4, Object aimValue4, String aimName5, Object aimValue5) {
        var t = this.by3_4(fieldName1, fieldValue1, fieldName2, fieldValue2, fieldName3, fieldValue3, aimName1, aimValue1, aimName2, aimValue2, aimName3, aimValue3, aimName4, aimValue4);
        t.aimName5 = aimName5;
        t.aimValue5 = aimValue5;
        return this;
    }

    public ArgPasser by1_6(String fieldName1, Object fieldValue1, String aimName1, Object aimValue1, String aimName2, Object aimValue2, String aimName3, Object aimValue3, String aimName4, Object aimValue4, String aimName5, Object aimValue5, String aimName6, Object aimValue6) {
        var t = by1_5(fieldName1, fieldValue1, aimName1, aimValue1, aimName2, aimValue2, aimName3, aimValue3, aimName4, aimValue4, aimName5, aimValue5);
        t.aimName6 = aimName6;
        t.aimValue6 = aimValue6;
        return this;
    }

    public ArgPasser by2_6(String fieldName1, Object fieldValue1, String fieldName2, Object fieldValue2, String aimName1, Object aimValue1, String aimName2, Object aimValue2, String aimName3, Object aimValue3, String aimName4, Object aimValue4, String aimName5, Object aimValue5, String aimName6, Object aimValue6) {
        var t = this.by2_5(fieldName1, fieldValue1, fieldName2, fieldValue2, aimName1, aimValue1, aimName2, aimValue2, aimName3, aimValue3, aimName4, aimValue4, aimName5, aimValue5);
        t.aimName6 = aimName6;
        t.aimValue6 = aimValue6;
        return this;
    }

    public ArgPasser by3_6(String fieldName1, Object fieldValue1, String fieldName2, Object fieldValue2, String fieldName3, Object fieldValue3, String aimName1, Object aimValue1, String aimName2, Object aimValue2, String aimName3, Object aimValue3, String aimName4, Object aimValue4, String aimName5, Object aimValue5, String aimName6, Object aimValue6) {
        var t = this.by3_5(fieldName1, fieldValue1, fieldName2, fieldValue2, fieldName3, fieldValue3, aimName1, aimValue1, aimName2, aimValue2, aimName3, aimValue3, aimName4, aimValue4, aimName5, aimValue5);
        t.aimName6 = aimName6;
        t.aimValue6 = aimValue6;
        return this;
    }

    public ArgPasser by1_7(String fieldName1, Object fieldValue1, String aimName1, Object aimValue1, String aimName2, Object aimValue2, String aimName3, Object aimValue3, String aimName4, Object aimValue4, String aimName5, Object aimValue5, String aimName6, Object aimValue6, String aimName7, Object aimValue7) {
        var t = by1_6(fieldName1, fieldValue1, aimName1, aimValue1, aimName2, aimValue2, aimName3, aimValue3, aimName4, aimValue4, aimName5, aimValue5, aimName6, aimValue6);
        t.aimName7 = aimName7;
        t.aimValue7 = aimValue7;
        return this;
    }

    public ArgPasser by2_7(String fieldName1, Object fieldValue1, String fieldName2, Object fieldValue2, String aimName1, Object aimValue1, String aimName2, Object aimValue2, String aimName3, Object aimValue3, String aimName4, Object aimValue4, String aimName5, Object aimValue5, String aimName6, Object aimValue6, String aimName7, Object aimValue7) {
        var t = this.by2_6(fieldName1, fieldValue1, fieldName2, fieldValue2, aimName1, aimValue1, aimName2, aimValue2, aimName3, aimValue3, aimName4, aimValue4, aimName5, aimValue5, aimName6, aimValue6);
        t.aimName7 = aimName7;
        t.aimValue7 = aimValue7;
        return this;
    }

    public ArgPasser by3_7(String fieldName1, Object fieldValue1, String fieldName2, Object fieldValue2, String fieldName3, Object fieldValue3, String aimName1, Object aimValue1, String aimName2, Object aimValue2, String aimName3, Object aimValue3, String aimName4, Object aimValue4, String aimName5, Object aimValue5, String aimName6, Object aimValue6, String aimName7, Object aimValue7) {
        var t = this.by3_6(fieldName1, fieldValue1, fieldName2, fieldValue2, fieldName3, fieldValue3, aimName1, aimValue1, aimName2, aimValue2, aimName3, aimValue3, aimName4, aimValue4, aimName5, aimValue5, aimName6, aimValue6);
        t.aimName7 = aimName7;
        t.aimValue7 = aimValue7;
        return this;
    }

    public ArgPasser by1_8(String fieldName1, Object fieldValue1, String aimName1, Object aimValue1, String aimName2, Object aimValue2, String aimName3, Object aimValue3, String aimName4, Object aimValue4, String aimName5, Object aimValue5, String aimName6, Object aimValue6, String aimName7, Object aimValue7, String aimName8, Object aimValue8) {
        var t = by1_7(fieldName1, fieldValue1, aimName1, aimValue1, aimName2, aimValue2, aimName3, aimValue3, aimName4, aimValue4, aimName5, aimValue5, aimName6, aimValue6, aimName7, aimValue7);
        t.aimName8 = aimName8;
        t.aimValue8 = aimValue8;
        return this;
    }

    public ArgPasser by2_8(String fieldName1, Object fieldValue1, String fieldName2, Object fieldValue2, String aimName1, Object aimValue1, String aimName2, Object aimValue2, String aimName3, Object aimValue3, String aimName4, Object aimValue4, String aimName5, Object aimValue5, String aimName6, Object aimValue6, String aimName7, Object aimValue7, String aimName8, Object aimValue8) {
        var t = this.by2_7(fieldName1, fieldValue1, fieldName2, fieldValue2, aimName1, aimValue1, aimName2, aimValue2, aimName3, aimValue3, aimName4, aimValue4, aimName5, aimValue5, aimName6, aimValue6, aimName7, aimValue7);
        t.aimName8 = aimName8;
        t.aimValue8 = aimValue8;
        return this;
    }

    public ArgPasser by3_8(String fieldName1, Object fieldValue1, String fieldName2, Object fieldValue2, String fieldName3, Object fieldValue3, String aimName1, Object aimValue1, String aimName2, Object aimValue2, String aimName3, Object aimValue3, String aimName4, Object aimValue4, String aimName5, Object aimValue5, String aimName6, Object aimValue6, String aimName7, Object aimValue7, String aimName8, Object aimValue8) {
        var t = this.by3_7(fieldName1, fieldValue1, fieldName2, fieldValue2, fieldName3, fieldValue3, aimName1, aimValue1, aimName2, aimValue2, aimName3, aimValue3, aimName4, aimValue4, aimName5, aimValue5, aimName6, aimValue6, aimName7, aimValue7);
        t.aimName8 = aimName8;
        t.aimValue8 = aimValue8;
        return this;
    }

    public ArgPasser by1_9(String fieldName1, Object fieldValue1, String aimName1, Object aimValue1, String aimName2, Object aimValue2, String aimName3, Object aimValue3, String aimName4, Object aimValue4, String aimName5, Object aimValue5, String aimName6, Object aimValue6, String aimName7, Object aimValue7, String aimName8, Object aimValue8, String aimName9, Object aimValue9) {
        var t = by1_8(fieldName1, fieldValue1, aimName1, aimValue1, aimName2, aimValue2, aimName3, aimValue3, aimName4, aimValue4, aimName5, aimValue5, aimName6, aimValue6, aimName7, aimValue7, aimName8, aimValue8);
        t.aimName9 = aimName9;
        t.aimValue9 = aimValue9;
        return this;
    }

    public ArgPasser by2_9(String fieldName1, Object fieldValue1, String fieldName2, Object fieldValue2, String aimName1, Object aimValue1, String aimName2, Object aimValue2, String aimName3, Object aimValue3, String aimName4, Object aimValue4, String aimName5, Object aimValue5, String aimName6, Object aimValue6, String aimName7, Object aimValue7, String aimName8, Object aimValue8, String aimName9, Object aimValue9) {
        var t = this.by2_8(fieldName1, fieldValue1, fieldName2, fieldValue2, aimName1, aimValue1, aimName2, aimValue2, aimName3, aimValue3, aimName4, aimValue4, aimName5, aimValue5, aimName6, aimValue6, aimName7, aimValue7, aimName8, aimValue8);
        t.aimName9 = aimName9;
        t.aimValue9 = aimValue9;
        return this;
    }

    public ArgPasser by3_9(String fieldName1, Object fieldValue1, String fieldName2, Object fieldValue2, String fieldName3, Object fieldValue3, String aimName1, Object aimValue1, String aimName2, Object aimValue2, String aimName3, Object aimValue3, String aimName4, Object aimValue4, String aimName5, Object aimValue5, String aimName6, Object aimValue6, String aimName7, Object aimValue7, String aimName8, Object aimValue8, String aimName9, Object aimValue9) {
        var t = this.by3_8(fieldName1, fieldValue1, fieldName2, fieldValue2, fieldName3, fieldValue3, aimName1, aimValue1, aimName2, aimValue2, aimName3, aimValue3, aimName4, aimValue4, aimName5, aimValue5, aimName6, aimValue6, aimName7, aimValue7, aimName8, aimValue8);
        t.aimName9 = aimName9;
        t.aimValue9 = aimValue9;
        return this;
    }
}
