<template>
  <el-card>
    <template #header>
      <div class="hd">
        <b>题库管理</b>
        <span>
          <el-button size="small" type="primary" @click="openDialog()">新增题目</el-button>
          <el-button size="small" @click="onImport">Excel 导入</el-button>
          <input ref="fileRef" type="file" accept=".xlsx,.xls" style="display:none" @change="onFile" />
        </span>
      </div>
    </template>

    <el-table :data="list" border size="small">
      <el-table-column prop="orderNo" label="排序" width="70" />
      <el-table-column label="题干" min-width="220">
        <template #default="{ row }">
          <div class="title-cell">{{ row.title }}</div>
          <div class="opts">
            <span v-for="(o, i) in parseOptions(row.options)" :key="i" class="opt">
              {{ LETTERS[i] }}. {{ o }}
              <el-tag v-if="isCorrect(row.answer, i)" size="small" type="success">✓</el-tag>
            </span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="类型" width="90">
        <template #default="{ row }">
          <el-tag size="small">{{ row.type === 'MULTIPLE' ? '多选' : '单选' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="130">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDialog(row)">编辑</el-button>
          <el-button link type="danger" @click="del(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="visible" :title="form.id ? '编辑题目' : '新增题目'" width="560px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="题型">
          <el-radio-group v-model="form.type">
            <el-radio value="SINGLE">单选</el-radio>
            <el-radio value="MULTIPLE">多选</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="题干">
          <el-input v-model="form.title" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="选项">
          <div class="opt-edit">
            <div v-for="(o, i) in form.optionInputs" :key="i" class="opt-row">
              <span class="opt-label">{{ LETTERS[i] }}</span>
              <el-input v-model="form.optionInputs[i]" :placeholder="`选项${LETTERS[i]}`" />
            </div>
          </div>
        </el-form-item>
        <el-form-item :label="form.type === 'MULTIPLE' ? '正确答案' : '正确答案'">
          <el-checkbox-group v-model="form.correct" v-if="form.type === 'MULTIPLE'">
            <el-checkbox v-for="(o, i) in form.optionInputs" :key="i" :value="i">{{ LETTERS[i] }}</el-checkbox>
          </el-checkbox-group>
          <el-radio-group v-model="form.correctSingle" v-else>
            <el-radio v-for="(o, i) in form.optionInputs" :key="i" :value="i">{{ LETTERS[i] }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.orderNo" :min="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listQuestionsAdmin, saveQuestion, updateQuestion, deleteQuestion, importQuestions } from '../../api'

const LETTERS = ['A', 'B', 'C', 'D', 'E', 'F']
const list = ref([])
const visible = ref(false)
const fileRef = ref(null)

const emptyForm = () => ({
  id: '', title: '', type: 'SINGLE', orderNo: 0,
  optionInputs: ['', '', '', ''],
  correct: [], correctSingle: 0
})
const form = reactive(emptyForm())

const parseOptions = (json) => {
  try { return JSON.parse(json || '[]') } catch (e) { return [] }
}
const parseAnswer = (json) => {
  try { return JSON.parse(json || '[]') } catch (e) { return [] }
}
const isCorrect = (json, idx) => parseAnswer(json).includes(idx)

async function load() {
  list.value = await listQuestionsAdmin()
}

function openDialog(row) {
  const f = emptyForm()
  if (row) {
    f.id = row.id
    f.title = row.title
    f.type = row.type || 'SINGLE'
    f.orderNo = row.orderNo || 0
    const opts = parseOptions(row.options)
    f.optionInputs = opts.length ? opts.concat(Array(Math.max(0, 4 - opts.length)).fill('')) : ['', '', '', '']
    const ans = parseAnswer(row.answer)
    f.correct = ans
    f.correctSingle = ans.length ? ans[0] : 0
  }
  Object.assign(form, f)
  visible.value = true
}

async function save() {
  if (!form.title.trim()) return ElMessage.warning('请输入题干')
  const options = form.optionInputs.map((o) => (o || '').trim()).filter((o) => o)
  if (options.length < 2) return ElMessage.warning('至少填写 2 个选项')
  let answerIdx
  if (form.type === 'MULTIPLE') {
    answerIdx = [...form.correct].sort((a, b) => a - b)
    if (!answerIdx.length) return ElMessage.warning('请选择正确答案')
  } else {
    answerIdx = [form.correctSingle]
  }
  const payload = {
    title: form.title,
    type: form.type,
    options: JSON.stringify(options),
    answer: JSON.stringify(answerIdx),
    orderNo: form.orderNo
  }
  if (form.id) {
    await updateQuestion(form.id, payload)
  } else {
    await saveQuestion(payload)
  }
  visible.value = false
  await load()
  ElMessage.success('已保存')
}

async function del(row) {
  await ElMessageBox.confirm('确认删除该题目？', '提示', { type: 'warning' })
  await deleteQuestion(row.id)
  await load()
}

function onImport() {
  fileRef.value.click()
}
async function onFile(e) {
  const file = e.target.files[0]
  if (!file) return
  try {
    const res = await importQuestions(file)
    ElMessage.success(`导入完成：成功 ${res.success}，失败 ${res.fail}`)
    await load()
  } catch (err) {
    ElMessage.error('导入失败：' + (err.message || err))
  } finally {
    e.target.value = ''
  }
}

onMounted(load)
</script>

<style scoped>
.hd {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.title-cell {
  font-weight: 600;
  margin-bottom: 4px;
}
.opts {
  display: flex;
  flex-wrap: wrap;
  gap: 4px 16px;
  color: #555;
  font-size: 13px;
}
.opt-edit .opt-row {
  display: flex;
  align-items: center;
  margin-bottom: 6px;
}
.opt-label {
  width: 24px;
  font-weight: 600;
}
</style>
