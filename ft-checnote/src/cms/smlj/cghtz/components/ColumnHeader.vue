<script setup lang="js">
/**
 * 可排序表头组件
 * - 点击表头（列名/箭头）触发排序，排序状态由父组件维护（箭头状态通过 order 传入）
 */
const props = defineProps({
    label: {type: String, required: true},
    sortable: {type: Boolean, default: false},
    // 当前排序方向：'ascending' | 'descending' | ''
    order: {type: String, default: ''},
})

const emit = defineEmits(['sort'])

function onSortClick() {
    if (!props.sortable) return
    emit('sort')
}
</script>

<template>
    <div class="col-header" :class="{sortable}" @click="onSortClick">
        <span class="col-title">{{ label }}</span>

        <span v-if="sortable" class="sort-arrows">
            <span class="arrow up" :class="{active: order === 'ascending'}">▲</span>
            <span class="arrow down" :class="{active: order === 'descending'}">▼</span>
        </span>
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
}
</style>
