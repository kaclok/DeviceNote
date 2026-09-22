import {axiosInst as axiosR} from "@/framework/services/net/AxiosInst.js"

/**
 * 合同台账 - 后端接口层
 *
 * 说明：后端接口尚未开发时，SysX 会走 MockX 本地兜底；
 * 后端就绪后，只需保持下面 URL 与后端路由一致即可无缝切换。
 *
 * 注意：axios.post(url, data, config)。
 * - 需要 query 参数(@RequestParam)的接口：第二参传 null，第三参用 {params, signal}，
 *   这样 pageNum/pageSize/筛选条件才会作为查询串发到后端，被 @RequestParam 读到。
 *   之前写成 post(url, {params, signal}) 会把 params 当成 JSON body 发出，后端 @RequestParam 读不到，分页参数永远丢失。
 * - 需要 body(@RequestBody)的接口：第二参传 paras，第三参用 {signal}。
 */
export class ApiX {
    /* ---------------- 合同台账 CRUD ---------------- */
    static getContractList(paras, signal) {
        return axiosR.post("cghtz/contract/list", null, {
            params: paras, signal: signal,
        })
    }

    // post的参数如果想变现为query的形式，可以被后端的RequestParam捕获则不传递data
    // 如果表现为json body,，则传递data
    static getContract(paras, signal) {
        return axiosR.post("cghtz/contract/get", null, {
            params: paras, signal: signal,
        })
    }

    static createContract(paras, signal) {
        return axiosR.post("cghtz/contract/create", paras, {
            signal: signal,
        })
    }

    static updateContract(paras, signal) {
        return axiosR.post("cghtz/contract/update", paras, {
            signal: signal,
        })
    }

    static deleteContract(paras, signal) {
        return axiosR.post("cghtz/contract/delete", null, {
            params: paras, signal: signal,
        })
    }

    /* ---------------- Excel 导入 / 导出 ---------------- */
    // 入参刻意分成两半：body = 合同行数组，params = {tpl_id}。
    // tpl_id 是「整批属于哪套模版」的声明，后端要拿它在写库前先比对部门绑定 ——
    // 塞进每一行会让人误以为逐行可以不同，而部门与模版是一对一的，逐行声明没有意义。
    static importContractExcel(rows, params, signal) {
        return axiosR.post("cghtz/contract/import", rows, {
            params: params, signal: signal,
        })
    }

    /* ---------------- 账号与权限 ---------------- */
    static getAccountList(paras, signal) {
        return axiosR.post("cghtz/account/list", null, {
            params: paras, signal: signal,
        })
    }

    static saveAccount(paras, signal) {
        return axiosR.post("cghtz/account/save", null, {
            params: paras, signal: signal,
        })
    }

    static resetPassword(paras, signal) {
        return axiosR.post("cghtz/account/resetPwd", null, {
            params: paras, signal: signal,
        })
    }

    static toggleAccountStatus(paras, signal) {
        return axiosR.post("cghtz/account/toggle", null, {
            params: paras, signal: signal,
        })
    }

    /* ---------------- 角色与权限字典 ---------------- */
    static getRoleList(paras, signal) {
        return axiosR.post("cghtz/role/list", null, {
            params: paras, signal: signal,
        })
    }

    static getPermDefs(paras, signal) {
        return axiosR.post("cghtz/perm/list", null, {
            params: paras, signal: signal,
        })
    }

    /* ---------------- 组织架构（部门字典，只读） ---------------- */
    // 数据源是 train.t_org（后端 @DS("train") 已切换），返回 dept_code/dept_name/dept_all_name/parent_dept_code
    static getDeptList(paras, signal) {
        return axiosR.post("cghtz/dept/list", null, {
            params: paras, signal: signal,
        })
    }

    /* ---------------- 合同模版 / 部门-模版映射 ---------------- */
    // 模版全量（id/name/tb_name/col_order/dept_codes）：台账页与导入页的「合同模版」下拉数据源。
    // dept_codes = 持有该模版（含继承）的部门编码，台账页用它把组织树收窄到相关部门。
    static getTemplateList(paras, signal) {
        return axiosR.post("cghtz/template/list", null, {
            params: paras, signal: signal,
        })
    }

    // 保存模版的 Excel 列顺序（模板表头预览里拖拽后的结果）。
    // col_order 是逗号分隔的字段名；传空串 = 清除覆盖、回到 gd.json 的登记顺序。
    static saveTplColOrder(paras, signal) {
        return axiosR.post("cghtz/template/colOrder", null, {
            params: paras, signal: signal,
        })
    }

    // 部门 → 模版 绑定全量（后端已按操作者的数据范围收窄，前端无需再过滤）
    static getDeptTplList(paras, signal) {
        return axiosR.post("cghtz/deptTpl/list", null, {
            params: paras, signal: signal,
        })
    }

    // 设置某部门的合同模版：一个部门一条记录（dept_code 主键），重复设置即覆盖
    static saveDeptTpl(paras, signal) {
        return axiosR.post("cghtz/deptTpl/save", null, {
            params: paras, signal: signal,
        })
    }

    // 解除某部门的合同模版绑定
    static deleteDeptTpl(paras, signal) {
        return axiosR.post("cghtz/deptTpl/delete", null, {
            params: paras, signal: signal,
        })
    }

    // 单个部门「实际生效」的合同模版（自身未绑定则沿组织树取最近的已绑定祖先）。
    // 批量导入页用它在上传前核对绑定关系；刻意不复用 deptTpl/list ——
    // 那个接口要 perm:assign，而导入页的用户持有的是 contract:import，两者不一定重合。
    static getDeptTplEffective(paras, signal) {
        return axiosR.post("cghtz/deptTpl/effective", null, {
            params: paras, signal: signal,
        })
    }
}
