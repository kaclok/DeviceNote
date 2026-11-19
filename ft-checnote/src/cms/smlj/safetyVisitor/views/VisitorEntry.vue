<template>
  <div class="visitor-wrap">
    <el-card class="visitor-card">
      <template #header>
        <div class="title">{{ appName }}</div>
      </template>

      <el-steps :active="active" align-center finish-status="success" class="steps">
        <el-step title="选择被访" />
        <el-step title="观看告知" />
        <el-step title="签到登记" />
        <el-step title="安全考试" />
        <el-step title="完成" />
      </el-steps>

      <!-- 步骤1：选择部门/人员 -->
      <div v-if="active === 0" class="step-body">
        <el-form label-width="100px">
          <el-form-item label="被访问部门">
            <el-select v-model="hostId" placeholder="请选择部门" filterable @change="onHostChange" style="width:100%">
              <el-option v-for="h in hosts" :key="h.id" :label="h.name" :value="h.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="被访问人">
            <el-select v-model="staffId" placeholder="请选择被访问人" filterable style="width:100%">
              <el-option v-for="s in staffList" :key="s.id" :label="s.name + (s.position ? '（' + s.position + '）' : '')" :value="s.id" />
            </el-select>
          </el-form-item>
        </el-form>
        <div class="actions">
          <el-button type="primary" :disabled="!hostId || !staffId" @click="active = 1">下一步</el-button>
        </div>
      </div>

      <!-- 步骤2：观看视频 -->
      <div v-else-if="active === 1" class="step-body">
        <div v-if="videoUrl" class="video-box">
          <video :src="videoUrl" controls style="width:100%" />
        </div>
        <el-alert v-else title="管理员尚未配置安全告知视频，可直接点击下一步" type="warning" :closable="false" />
        <p class="tip">请认真观看《访客安全告知》，观看完毕后点击下方按钮进入登记。</p>
        <div class="actions">
          <el-button @click="active = 0">上一步</el-button>
          <el-button type="primary" @click="onWatched">我已观看，下一步</el-button>
        </div>
      </div>

      <!-- 步骤3：签到表单 -->
      <div v-else-if="active === 2" class="step-body">
        <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
          <el-form-item label="姓名" prop="name">
            <el-input v-model="form.name" placeholder="请输入姓名" />
          </el-form-item>
          <el-form-item label="手机号" prop="phone">
            <el-input v-model="form.phone" placeholder="请输入手机号" />
          </el-form-item>
          <el-form-item label="身份证" prop="idCard">
            <el-input v-model="form.idCard" placeholder="请输入身份证号" />
          </el-form-item>
          <el-form-item label="来访事由" prop="reason">
            <el-input v-model="form.reason" type="textarea" :rows="2" placeholder="请输入来访事由" />
          </el-form-item>
          <el-form-item label="车牌号" prop="plate">
            <el-input v-model="form.plate" placeholder="如无请留空" />
          </el-form-item>
        </el-form>
        <div class="actions">
          <el-button @click="active = 1">上一步</el-button>
          <el-button type="primary" @click="submitSignin">提交签到</el-button>
        </div>
      </div>

      <!-- 步骤4：考试 -->
      <div v-else-if="active === 3" class="step-body">
        <el-alert v-if="!questions.length" title="题库为空，请联系管理员配置题目" type="error" :closable="false" />
        <div v-for="(q, qi) in questions" :key="q.id" class="question">
          <div class="q-title">{{ qi + 1 }}. {{ q.title }}</div>
          <el-radio-group v-model="answers[q.id]">
            <el-radio v-for="(opt, oi) in q.options" :key="oi" :value="oi" border class="q-opt">
              {{ LETTERS[oi] }}. {{ opt }}
            </el-radio>
          </el-radio-group>
        </div>
        <div class="actions">
          <el-button @click="active = 2">上一步</el-button>
          <el-button type="primary" :disabled="!allAnswered" @click="handleSubmitExam">交卷</el-button>
        </div>
      </div>

      <!-- 步骤5：完成 -->
      <div v-else class="step-body result">
        <el-result
          :icon="examResult && examResult.passed ? 'success' : 'warning'"
          :title="examResult && examResult.passed ? '考试通过' : '未通过'"
          :sub-title="examResult ? `本次得分 ${examResult.score} 分（及格线 ${examResult.passLine} 分）` : ''"
        >
          <template #extra>
            <div class="code-box">访客编号：<b>{{ visitorCode }}</b></div>
            <p class="notice">请凭本成绩到前台 / 门卫处<b>人工领取定位卡</b>。</p>
            <el-button type="primary" @click="restart">再登记一位访客</el-button>
          </template>
        </el-result>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getHosts, getStaff, getPublicSettings, createVisitor, getQuestions, submitExam } from '../api'

const LETTERS = ['A', 'B', 'C', 'D', 'E', 'F']

const active = ref(0)
const appName = ref('访客安全准入系统')
const videoUrl = ref('')

const hosts = ref([])
const staffList = ref([])
const hostId = ref('')
const staffId = ref('')

const questions = ref([])
const answers = reactive({})
const examResult = ref(null)
const visitorId = ref(null)
const visitorCode = ref('')

const formRef = ref(null)
const form = reactive({
  name: '',
  phone: '',
  idCard: '',
  reason: '',
  plate: ''
})
const rules = {
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1\d{10}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  idCard: [
    { required: true, message: '请输入身份证', trigger: 'blur' },
    { pattern: /(^\d{15}$)|(^\d{17}(\d|X|x)$)/, message: '身份证格式不正确', trigger: 'blur' }
  ]
}

const allAnswered = computed(() => questions.value.length > 0 && questions.value.every((q) => answers[q.id] !== undefined && answers[q.id] !== null))

onMounted(async () => {
  try {
    const s = await getPublicSettings()
    appName.value = (s && s.appName) || '访客安全准入系统'
    videoUrl.value = (s && s.videoUrl) || ''
  } catch (e) { /* 公开接口失败忽略 */ }
  try {
    hosts.value = await getHosts()
  } catch (e) {
    hosts.value = []
  }
})

async function onHostChange() {
  staffId.value = ''
  if (!hostId.value) {
    staffList.value = []
    return
  }
  staffList.value = await getStaff(hostId.value)
}

function onWatched() {
  active.value = 2
}

async function submitSignin() {
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    try {
      const res = await createVisitor({
        name: form.name,
        phone: form.phone,
        idCard: form.idCard,
        reason: form.reason,
        plate: form.plate,
        hostId: hostId.value,
        staffId: staffId.value
      })
      visitorId.value = res.id
      visitorCode.value = res.code
      // 载入考题
      questions.value = await getQuestions()
      Object.keys(answers).forEach((k) => delete answers[k])
      active.value = 3
    } catch (e) {
      ElMessage.error('签到失败，请重试')
    }
  })
}

async function handleSubmitExam() {
  if (!visitorId.value) return
  try {
    const res = await submitExam(visitorId.value, answers)
    examResult.value = res
    active.value = 4
  } catch (e) {
    ElMessage.error('提交失败，请重试')
  }
}

function restart() {
  active.value = 0
  hostId.value = ''
  staffId.value = ''
  staffList.value = []
  Object.keys(answers).forEach((k) => delete answers[k])
  examResult.value = null
  visitorId.value = null
  visitorCode.value = ''
  form.name = ''
  form.phone = ''
  form.idCard = ''
  form.reason = ''
  form.plate = ''
  questions.value = []
}
</script>

<style scoped>
.visitor-wrap {
  min-height: 100vh;
  background: linear-gradient(135deg, #e3f2fd 0%, #f5f7fa 100%);
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding: 24px 12px;
}
.visitor-card {
  width: 100%;
  max-width: 760px;
}
.title {
  font-size: 20px;
  font-weight: 700;
  text-align: center;
}
.steps {
  margin-bottom: 24px;
}
.step-body {
  min-height: 220px;
}
.tip {
  color: #888;
  font-size: 13px;
  margin: 12px 0;
}
.actions {
  text-align: center;
  margin-top: 20px;
}
.video-box {
  background: #000;
  border-radius: 6px;
  overflow: hidden;
}
.question {
  margin-bottom: 18px;
}
.q-title {
  font-weight: 600;
  margin-bottom: 8px;
}
.q-opt {
  display: block;
  margin: 6px 0 !important;
}
.result .code-box {
  font-size: 16px;
  margin-bottom: 8px;
}
.result .notice {
  color: #e6a23c;
  font-weight: 600;
}
</style>
