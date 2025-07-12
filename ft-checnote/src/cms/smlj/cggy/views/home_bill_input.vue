<template>
    <div class="file-upload-container">
        <!-- 表单（移除原生提交，改用自定义事件） -->
        <el-form v-loading="loadingUpload" enctype="multipart/form-data">
            <el-select
                v-model="curLevelId"
                placeholder="请选择物资类别:"
                style="width: 600px; margin-bottom: 35px"
                @change="onLevelClicked"
            >
                <el-option
                    v-for="item in options"
                    :key="item.value"
                    :label="item.label"
                    :value="item.value"
                />
            </el-select>

            <el-form-item>
                <el-upload
                    style="margin-left: 15px;margin-bottom:5px;width: 100%"
                    v-model:file-list="fileList1"
                    :auto-upload="false"
                    :limit="1"
                    :on-change="handleFileChange1"
                    :on-remove="handleRemove1"
                    accept=".xlsx"
                >
                    <el-button type="primary" style="width: 150px">选择物流仓储表</el-button>
                </el-upload>
            </el-form-item>

            <el-form-item>
                <el-upload
                    style="margin-left: 15px;margin-bottom:5px;width: 100%"
                    v-model:file-list="fileList2"
                    :auto-upload="false"
                    :limit="1"
                    :on-change="handleFileChange2"
                    :on-remove="handleRemove2"
                    accept=".xlsx"
                >
                    <el-button type="primary" style="width: 150px">选择实验室表</el-button>
                </el-upload>
            </el-form-item>

            <el-form-item>
                <el-upload
                    style="margin-left: 15px;margin-bottom:25px;width: 100%"
                    v-model:file-list="fileList3"
                    :auto-upload="false"
                    :limit="1"
                    :on-change="handleFileChange3"
                    :on-remove="handleRemove3"
                    accept=".xlsx"
                >
                    <el-button type="primary" style="width: 150px">选择扣灰表</el-button>
                </el-upload>
            </el-form-item>

            <div style="display: flex; justify-content: center;">
                <el-button
                    type="success"
                    @click="handleSubmit"
                    :disabled="!canShow()"
                >
                    分析表格
                </el-button>

                <!--				<el-button
                                    type="success"
                                    @click="onBtnSpClicked"
                                    :disabled="!(callback[curLevelId].gotResult)"
                                >
                                    审批表格
                                </el-button>-->
            </div>
        </el-form>
    </div>
</template>

<script setup>
import {onMounted, onUnmounted, ref} from 'vue';
import {doPost} from "@/framework/services/net/Request.js"
import {DateTimeUtil} from "@/framework/utils/DateTimeUtil.js";
import {ExcelService} from "@/framework/services/ExcelService.js";

const AC_upload = new AbortController();
const loadingUpload = ref(false);

// 文件状态
const file1 = ref(null);
const file2 = ref(null);
const file3 = ref(null);
const fileList1 = ref([]);
const fileList2 = ref([]);
const fileList3 = ref([]);

const curLevelId = ref(500000004)
const options = [
    {
        value: 500000004,
        label: '电石',
    },
    {
        value: 500000005,
        label: '煤',
    },
    {
        value: 200000775,
        label: '焦沫（精品）',
    },
]

onMounted(() => {

});

onUnmounted(() => {
    AC_upload.abort();
})

function _req_500000004() {
    if (curLevelId.value)
        if (!canShow()) {
            window.alert('请选择3个文件！');
            return;
        }

    const formData = new FormData()
    formData.append("goods_id", curLevelId.value)
    formData.append('file_wlcc', file1.value);
    formData.append('file_library', file2.value);
    formData.append('file_khl', file3.value);
    let now = DateTimeUtil.nowMSTimestamp();
    formData.append('timestamp', now);

    doPost("x/cggy/submitExcel", formData, AC_upload.signal, () => {
        loadingUpload.value = true;
    }, (r, data) => {
        loadingUpload.value = false;

        let prompt = "更新成功"
        if (!r) {
            if (data.message) {
                prompt = "更新失败:" + data.message
            } else {
                prompt = "更新失败"
            }

            window.alert(`${prompt}`);
        } else {
            // router.push({name: "home_bill_table", params: {goodsId: curLevelId.value, timestamp: now}});
            const colNames = [
                '日期',
                '供货单位',
                '批号',
                '车号',
                '结算数量(吨)',
                '扣灰量(吨)',
                '净重(吨)',
                '发气量(升/千克)',
                '基准单价(元/吨)',
                '考核单价(元/吨)',
                '结算总价',
                '毛重时间',
                '化验室-流水号',
                '化验室-下样时间',
                '化验室-备注',
                '化验室-样品等级',
                '附加-更新时间',
                '附加-是否物流仓储数据行合理',
                '附加-是否有匹配的化验室数据行',
                '附加-是否结算过',
            ];

            let objArr = data.data
            for (let i = 0; i < objArr.length; i++) {
                let one = objArr[i];
                if (one.date !== null) {
                    objArr[i].date = new Date(one.date);
                    objArr[i].date = objArr[i].date.toLocaleString('zh-CN', {
                        timeZone: 'Asia/Shanghai', // 上海时区 = 北京时区
                        hour12: false // 24小时制
                    });
                }
                if (one.xy_dt !== null) {
                    objArr[i].xy_dt = new Date(one.xy_dt);
                    objArr[i].xy_dt = objArr[i].xy_dt.toLocaleString('zh-CN', {
                        timeZone: 'Asia/Shanghai', // 上海时区 = 北京时区
                        hour12: false // 24小时制
                    });
                }
                if (one.modify_dt !== null) {
                    objArr[i].modify_dt = new Date(one.modify_dt);
                    objArr[i].modify_dt = objArr[i].modify_dt.toLocaleString('zh-CN', {
                        timeZone: 'Asia/Shanghai', // 上海时区 = 北京时区
                        hour12: false // 24小时制
                    });
                }
                if (one.gross_dt !== null) {
                    objArr[i].gross_dt = new Date(one.gross_dt);
                    objArr[i].gross_dt = objArr[i].gross_dt.toLocaleString('zh-CN', {
                        timeZone: 'Asia/Shanghai', // 上海时区 = 北京时区
                        hour12: false // 24小时制
                    });
                }
                one.is_filtered = !one.is_filtered;

                // 小数点保持两位
                /*one.ash_weight = one.ash_weight.toFixed(2)
                one.weight = one.weight.toFixed(2)
                one.final_weight = one.final_weight.toFixed(2)
                one.base_price = one.base_price.toFixed(2)
                one.price = one.price.toFixed(2)
                one.total_price = one.total_price.toFixed(2)*/
            }

            const startRow = 1
            ExcelService.ExportJsonToExcel(objArr, colNames, 'export_ds_js', startRow, (wb, ws) => {
                for (let j = 0; j < objArr.length; j++) {
                    let row = j + startRow + 1
                    ws[`E${row}`] = {
                        t: 'n',
                        f: `=G${row}-F${row}`
                    }

                    ws[`J${row}`] = {
                        t: 'n',
                        f: `=IFS(H${row}<240,I${row}-20-80-480-24*(240-H${row}),H${row}<270,I${row}-20-80-16*(270-H${row}),H${row}<280,I${row}-20-8*(280-H${row}),H${row}<285,I${row}-4*(285-H${row}),H${row}<=295,I${row},H${row}<=300,I${row}+8*(H${row}-295),H${row}<=310,I${row}+40+16*(H${row}-300),H${row}<=320,I${row}+40+160+24*(H${row}-310),320<H${row},I${row}+40+160+240+32*(H${row}-320))`
                    }

                    ws[`K${row}`] = {t: 'n', f: `=E${row}*J${row}`}
                }
            }, false);

            callback[curLevelId.value].gotResult = true
        }
    })
}

function _req_500000005() {
    console.log("_req_500000005")
}

function _req_200000775() {
    console.log("_req_200000775")
}

const callback = {
    [500000004]: {fn: _req_500000004, gotResult: false},
    [500000005]: {fn: _req_500000005, gotResult: false},
    [200000775]: {fn: _req_200000775, gotResult: false}
}

// 组件的callback触发时机 晚于 ref变量的更新
// 所以逻辑为:if(curLevelId.value !== levelId) 这种逻辑肯定是false
function onLevelClicked(levelId) {
    curLevelId.value = levelId

    callback[curLevelId.value].gotResult = false;

    handleRemove1()
    handleRemove2()
    handleRemove3()
}

// 文件选择回调
function handleFileChange1(uploadFile) {
    file1.value = uploadFile.raw;
}

function handleFileChange2(uploadFile) {
    file2.value = uploadFile.raw;
}

function handleFileChange3(uploadFile) {
    file3.value = uploadFile.raw;
}

// 新增：文件移除处理
function handleRemove1() {
    fileList1.value = [];
    file1.value = null
}

function handleRemove2() {
    fileList2.value = [];
    file2.value = null
}

function handleRemove3() {
    fileList3.value = [];
    file3.value = null
}

function canShow() {
    return (file1.value !== null) && (file2.value !== null) && (file3.value !== null);
}

// 提交上传
function handleSubmit() {
    callback[curLevelId.value].fn?.()
}

function onBtnSpClicked() {

}

</script>

<style scoped>
.file-upload-container {
    max-width: 600px;
    margin: 20px auto;
    padding: 20px;
    border: 20px solid #ebeef5;
    border-radius: 40px;
    height: 350px;
}
</style>