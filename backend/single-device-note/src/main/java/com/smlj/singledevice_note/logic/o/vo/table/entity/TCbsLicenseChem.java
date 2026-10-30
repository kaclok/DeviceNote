package com.smlj.singledevice_note.logic.o.vo.table.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.Serializable;

/**
 * 许可的危险化学品（cbs.t_license_chem，PK=chem_id，原系统 /system/sysCompanyInvolveHazardChemicals）。
 * 储存量/年使用量为文本口径（沿用原系统）。
 */
@Component
@Data
@NoArgsConstructor
public class TCbsLicenseChem implements Serializable {

    private String chem_id;
    private String chem_name;
    /** 是否属于重点监管的危险化学品 */
    private Boolean is_key;
    private String storage;
    private String yearly;
    private String state;
    private String fire_class;
    private String catalog_no;
    private String cas_no;
    private String longitude;
    /** 危险性类别（按危化品目录口径） */
    private String hazard_class;
    /** 是否剧毒化学品 */
    private Boolean is_poison;
    /** 是否构成危险化学品重大危险源 */
    private Boolean is_major_hazard;
    /** 储存场所（罐区/仓库） */
    private String storage_place;
    /** 重大危险源临界量（吨，文本口径） */
    private String critical_qty;
    /** 实际存在量（吨，文本口径） */
    private String actual_qty;
    private String remark;
    private String creator;
    private Boolean open_status;
}
