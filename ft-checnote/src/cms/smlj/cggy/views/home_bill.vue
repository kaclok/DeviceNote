<template>
    <div class="file-upload-container">
        <el-select
            v-model="curLevelId"
            placeholder="请选择物资:"
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

        <!-- 表单（移除原生提交，改用自定义事件） -->
        <el-form ref="uploadForm" enctype="multipart/form-data">
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

            <el-button
                type="success"
                @click="handleSubmit"
                :disabled="!canShow()"
            >
                提交上传
            </el-button>
        </el-form>
    </div>
</template>

<script setup>
import {onMounted, onUnmounted, ref} from 'vue';
import {ElMessage, ElLoading} from 'element-plus';
import {doGet, doPost} from "@/framework/services/net/Request.js"

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

onMounted(() => {

});

onUnmounted(() => {
    AC_upload.abort();
})

function onLevelClicked(levelId) {
    curLevelId.value = levelId

    handleRemove1()
    handleRemove2()
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

            ElMessage.error(`${prompt}`);
        } else {
            _reqList()
        }
    })
}

function _reqList() {

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