<template>
    <div class="file-upload-container">
        <!-- 表单（移除原生提交，改用自定义事件） -->
        <el-form enctype="multipart/form-data">
            <el-select
                v-model="curLevelId"
                placeholder="请选择物资类别:"
                style="width: 240px; margin-bottom: 15px"
                @change="onLevelClicked"
            >
                <el-option
                    v-for="item in options"
                    :key="item.value"
                    :label="item.label"
                    :value="item.value"
                />
            </el-select>

            <el-form-item label="选择物流仓储表：">
                <el-upload
                    v-model:file-list="fileList1"
                    :auto-upload="false"
                    :limit="1"
                    :on-change="handleFileChange1"
                    :on-remove="handleRemove1"
                    accept=".xlsx"
                >
                    <el-button type="primary">点击选择文件</el-button>
                </el-upload>
            </el-form-item>

            <el-form-item label="选择实验室表：">
                <el-upload
                    v-model:file-list="fileList2"
                    :auto-upload="false"
                    :limit="1"
                    :on-change="handleFileChange2"
                    :on-remove="handleRemove2"
                    accept=".xlsx"
                >
                    <el-button type="primary">点击选择文件</el-button>
                </el-upload>
            </el-form-item>

            <div style="display: flex; justify-content: center;">
                <el-button
                    type="success"
                    @click="handleSubmit"
                    :disabled="!canShow()"
                >
                    提交上传
                </el-button>
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
let loadingUpload = ref(false);

// 文件状态
const file1 = ref(null);
const file2 = ref(null);
const fileList1 = ref([]);
const fileList2 = ref([]);

const curLevelId = ref("500000004")
const options = [
    {
        value: "500000004",
        label: '电石',
    },
    {
        value: '500000005',
        label: '煤',
    },
    {
        value: '200000775',
        label: '焦沫（精品）',
    },
]

const router = useRouter();

onMounted(() => {

});

onUnmounted(() => {
    AC_upload.abort();
})

function onLevelClicked(levelId) {
    if (levelId !== curLevelId.value) {
        curLevelId.value = levelId

        handleRemove1()
        handleRemove2()
    }
}

// 文件选择回调
function handleFileChange1(uploadFile) {
    file1.value = uploadFile.raw;
}

function handleFileChange2(uploadFile) {
    file2.value = uploadFile.raw;
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

function canShow() {
    return (file1.value !== null) && (file2.value !== null);
}

// 提交上传
async function handleSubmit() {
    if (!canShow()) {
        ElMessage.error('请选择两个文件！');
        return;
    }

    const formData = new FormData()
    formData.append("goods_id", curLevelId.value)
    formData.append('file_wlcc', file1.value);
    formData.append('file_library', file2.value);
    let now = DateTimeUtil.nowMSTimestamp();
    formData.append('timestamp', now);

    await doPost("x/cggy/submitExcel", formData, AC_upload.signal, () => {
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

            // window.alert(`${prompt}`);
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
                }
                if (one.xy_dt !== null) {
                    objArr[i].xy_dt = new Date(one.xy_dt);
                }
                if (one.modify_dt !== null) {
                    objArr[i].modify_dt = new Date(one.modify_dt);
                }
                one.is_filtered = !one.is_filtered;
            }

            const startRow = 1
            ExcelService.ExportJsonToExcel(objArr, colNames, 'export_js', startRow, (wb, ws) => {
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
        }
    })
}

</script>

<style scoped>
.file-upload-container {
    max-width: 600px;
    margin: 20px auto;
    padding: 20px;
    border: 1px solid #ebeef5;
    border-radius: 4px;
}
</style>