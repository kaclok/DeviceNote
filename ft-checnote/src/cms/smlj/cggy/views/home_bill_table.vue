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

            <el-form-item label="选择结算表：">
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
import {onMounted, onUnmounted, ref} from "vue";

const AC_upload = new AbortController();
let loadingUpload = ref(false);
const fileList1 = ref([]);

// 文件状态
const file1 = ref(null);

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

function _req_500000004() {
    console.log("_req_500000004")
}

function _req_500000005() {
    console.log("_req_500000005")
}

function _req_200000775() {
    console.log("_req_200000775")
}

const callback = {
    [500000004]: _req_500000004,
    [500000005]: _req_500000005,
    [200000775]: _req_200000775,
}

onMounted(() => {

});

onUnmounted(() => {
    AC_upload.abort();
})

// 组件的callback触发时机 晚于 ref变量的更新
// 所以逻辑为:if(curLevelId.value !== levelId) 这种逻辑肯定是false
function onLevelClicked(levelId) {
    curLevelId.value = levelId

    handleRemove1()
}

// 文件选择回调
function handleFileChange1(uploadFile) {
    file1.value = uploadFile.raw;
}

// 新增：文件移除处理
function handleRemove1() {
    fileList1.value = [];
    file1.value = null
}

function canShow() {
    return (file1.value !== null);
}

// 提交上传
function handleSubmit() {
    callback[curLevelId.value]()
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