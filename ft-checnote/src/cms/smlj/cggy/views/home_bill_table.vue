<template>
	<div class="file-upload-container">
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
					<el-button type="primary" style="width: 150px">选择结算表</el-button>
				</el-upload>
			</el-form-item>

			<div style="display: flex; justify-content: center;">
				<el-button
					type="success"
					@click="handleSubmit"
					:disabled="!canShow()"
				>
					审批表格
				</el-button>
			</div>
		</el-form>
	</div>
</template>

<script setup>
import {onMounted, onUnmounted, ref} from "vue";
import {DateTimeUtil} from "@/framework/utils/DateTimeUtil.js";

const AC_upload = new AbortController();
let loadingUpload = ref(false);

const fileList1 = ref([]);
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

const callback = {
	[500000004]: {
		workflowId: 2593,
		menuidforportal: 'custom_1694429295929_10',
		tabTitle: '6YeR5rOw5YyW5a2m5rWB56iL5Y%20R6LW3'
	},
	[500000005]: {
		workflowId: 2594,
		menuidforportal: 'custom_1694429295929_10',
		tabTitle: '6YeR5rOw5YyW5a2m5rWB56iL5Y%20R6LW3'
	},
	[200000775]: {
		workflowId: 2595,
		menuidforportal: 'custom_1694429295929_10',
		tabTitle: '6YeR5rOw5YyW5a2m5rWB56iL5Y%20R6LW3'
	}
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
	let workflowId = callback[curLevelId.value].workflowId
	let menuidforportal = callback[curLevelId.value].menuidforportal
	let tabTitle = callback[curLevelId.value].tabTitle
	const mills = DateTimeUtil.nowMSTimestamp()
	const url = 'http://oa.sxigc.com/spa/workflow/static4form/index.html?_rdm=' + mills + '#/main/workflow/req?iscreate=1&workflowid=' + workflowId + '&isagent=0&beagenter=0&f_weaver_belongto_userid=&f_weaver_belongto_usertype=0&menuidforportal=' + menuidforportal + '&tabTitle=' + tabTitle + '&preloadkey=' + mills + '&timestamp=' + mills/* + '&_key=22itfp'*/;

	window.open(url, '_blank');
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

.file-upload-container {
	max-width: 600px;
	margin: 20px auto;
	padding: 20px;
	border: 20px solid #ebeef5;
	border-radius: 40px;
	height: 220px;
}
</style>