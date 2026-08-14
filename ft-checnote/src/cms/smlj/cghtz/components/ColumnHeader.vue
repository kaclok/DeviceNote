<script setup lang="js">
/**
 * 可排序 + 可搜索 的表头组件
 * - 点击表头（列名/箭头）触发排序，排序状态由父组件维护（箭头状态通过 order 传入）
 * - 点击 🔍 弹出本列搜索框，confirm/clear 事件回传父组件，实现"作用于当前列"的筛选
 */
const props = defineProps({
    label: {type: String, required: true},
    sortable: {type: Boolean, default: false},
    // 当前排序方向：'ascending' | 'descending' | ''
    order: {type: String, default: ''},
    // 当前列已生效的筛选值（用于高亮 🔍）
    value: {type: [String, Number], default: ''},
})

const emit = defineEmits(['sort', 'confirm', 'clear'])

const searchVal = ref(props.value ?? '')
watch(() => props.value, v => {
    searchVal.value = v ?? ''
})

function onSortClick() {
    if (!props.sortable) return
    emit('sort')
}

function onConfirm() {
    emit('confirm', String(searchVal.value).trim())
}

function onClear() {
    searchVal.value = ''
    emit('clear')
}
</script>

<template>
    <div class="col-header" :class="{sortable}" @click="onSortClick">
        <span class="col-title">{{ label }}</span>

        <span v-if="sortable" class="sort-arrows">
            <span class="arrow up" :class="{active: order === 'ascending'}">▲</span>
            <span class="arrow down" :class="{active: order === 'descending'}">▼</span>
        </span>

        <el-popover placement="bottom" :width="240" trigger="click" :show-after="0" popper-class="col-search-pop">
            <template #reference>
                <span class="search-icon" :class="{active: !!value}" @click.stop title="搜索本列" @click.prevent>🔍</span>
            </template>
            <div class="search-panel">
                <el-input v-model="searchVal" :placeholder="'按' + label + '搜索'" size="small" clearable
                          @keyup.enter="onConfirm" @clear="onClear"/>
                <div class="search-actions">
                    <el-button size="small" @click="onClear">重置</el-button>
                    <el-button size="small" type="primary" @click="onConfirm">确定</el-button>
                </div>
                <div v-if="value" class="search-active">
                    <span>当前筛选：{{ value }}</span>
                    <el-button link type="danger" size="small" @click="onClear">清除</el-button>
                </div>
            </div>
        </el-popover>
    </div>
</template>

<style scoped>
.col-header {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    user-select: none;

    &.sortable {
        cursor: pointer;
    }

    .col-title {
        white-space: nowrap;
        font-weight: 600;
        color: #475569;
    }

    .sort-arrows {
        display: inline-flex;
        flex-direction: column;
        line-height: 1;
        gap: 1px;

        .arrow {
            font-size: 8px;
            color: #cbd5e1;
            transition: color .15s;

            &.active {
                color: #2563eb;
            }
        }
    }

    .search-icon {
        font-size: 13px;
        cursor: pointer;
        color: #94a3b8;
        border-radius: 4px;
        padding: 1px 3px;
        transition: all .15s;
        line-height: 1.2;

        &:hover {
            background: #eff6ff;
            color: #2563eb;
        }

        &.active {
            color: #2563eb;
            background: #eff6ff;
        }
    }
}
</style>

<style lang="scss">
/* 弹层内容（teleport 到 body，不能用 scoped） */
.col-search-pop {
    .search-panel {
        .search-actions {
            display: flex;
            justify-content: flex-end;
            gap: 8px;
            margin-top: 10px;
        }

        .search-active {
            display: flex;
            align-items: center;
            justify-content: space-between;
            margin-top: 10px;
            padding-top: 8px;
            border-top: 1px dashed #e2e8f0;
            font-size: 12px;
            color: #2563eb;
        }
    }
}
</style>
