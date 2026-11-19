<template>
  <el-card>
    <template #header><b>系统设置</b></template>
    <el-form :model="form" label-width="120px" style="max-width:520px">
      <el-form-item label="系统名称">
        <el-input v-model="form.appName" placeholder="前台显示的标题" />
      </el-form-item>
      <el-form-item label="访客入口地址">
        <el-input v-model="form.entryUrl" placeholder="如 https://your-domain/ （用于生成二维码）" />
      </el-form-item>
      <el-form-item label="安全告知视频">
        <el-input v-model="form.videoUrl" placeholder="视频文件 URL（mp4 等）" />
      </el-form-item>
      <el-form-item label="考试及格线">
        <el-input-number v-model="form.passLine" :min="0" :max="100" />
        <span class="suffix">分</span>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="saving" @click="onSave">保存</el-button>
      </el-form-item>
    </el-form>

    <el-divider>访客入口二维码</el-divider>
    <div v-if="form.entryUrl" class="qr-area">
      <img :src="qrDataUrl" alt="二维码" class="qr-img" />
      <div class="qr-tip">访客扫码进入登记流程（地址：{{ form.entryUrl }}）</div>
    </div>
    <el-empty v-else description="请先填写「访客入口地址」以生成二维码" />
  </el-card>
</template>

<script setup>
import { ref, reactive, watch, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import QRCode from 'qrcode'
import { getSettings, saveSettings } from '../../api'

const form = reactive({ appName: '', entryUrl: '', videoUrl: '', passLine: 80 })
const saving = ref(false)
const qrDataUrl = ref('')

async function load() {
  try {
    const s = await getSettings()
    form.appName = s.appName || ''
    form.entryUrl = s.entryUrl || ''
    form.videoUrl = s.videoUrl || ''
    form.passLine = Number(s.passLine) || 80
  } catch (e) {
    ElMessage.error('加载设置失败')
  }
}

async function renderQr() {
  if (!form.entryUrl) {
    qrDataUrl.value = ''
    return
  }
  try {
    qrDataUrl.value = await QRCode.toDataURL(form.entryUrl, { width: 200 })
  } catch (e) {
    qrDataUrl.value = ''
  }
}

watch(() => form.entryUrl, renderQr)

async function onSave() {
  saving.value = true
  try {
    await saveSettings({
      appName: form.appName,
      entryUrl: form.entryUrl,
      videoUrl: form.videoUrl,
      passLine: String(form.passLine)
    })
    ElMessage.success('保存成功')
    await renderQr()
  } catch (e) {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  load().then(renderQr)
})
</script>

<style scoped>
.suffix {
  margin-left: 8px;
  color: #888;
}
.qr-area {
  text-align: center;
}
.qr-img {
  width: 200px;
  height: 200px;
  border: 1px solid #eee;
}
.qr-tip {
  margin-top: 8px;
  color: #666;
  font-size: 13px;
}
</style>
