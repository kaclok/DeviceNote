<template>
    <el-table
        :data="rows"
        v-loading="loading"
        element-loading-background="rgba(15,20,32,.6)"
        size="small"
        height="100%"
    >
        <el-table-column type="index" label="#" width="48" align="center"/>
        <el-table-column prop="code" label="代码" width="86" sortable/>
        <el-table-column prop="name" label="名称" width="106" show-overflow-tooltip/>

        <el-table-column label="现价" width="84" align="right">
            <template #default="{row}">{{ row.price?.toFixed?.(2) ?? '-' }}</template>
        </el-table-column>

        <el-table-column prop="change_pct" label="涨跌幅" width="92" align="right" sortable>
            <template #default="{row}">
                <span :class="trendClass(row.change_pct)">{{ fmtPct(row.change_pct) }}</span>
            </template>
        </el-table-column>

        <el-table-column prop="boards" label="连板" width="88" align="center" sortable>
            <template #default="{row}">
                <span class="board-tag"
                      :style="{borderColor: boardColor(row.boards), color: boardColor(row.boards)}">
                    {{ fmtBoards(row.boards) }}
                </span>
            </template>
        </el-table-column>

        <el-table-column prop="sector" label="所属板块" width="126" sortable show-overflow-tooltip/>

        <el-table-column label="首次涨停" width="94" align="center">
            <template #default="{row}">{{ row.first_limit_time || '-' }}</template>
        </el-table-column>

        <el-table-column prop="broken_times" label="炸板" width="70" align="center" sortable/>

        <el-table-column prop="turnover_rate" label="换手率" width="90" align="right" sortable>
            <template #default="{row}">{{ row.turnover_rate?.toFixed?.(2) ?? '-' }}%</template>
        </el-table-column>

        <el-table-column prop="amount" label="成交额" width="98" align="right" sortable>
            <template #default="{row}">{{ fmtMoney(row.amount) }}</template>
        </el-table-column>

        <el-table-column prop="main_inflow" label="主力净流入" width="108" align="right" sortable>
            <template #default="{row}">
                <span :class="trendClass(row.main_inflow)">{{ fmtMoneySigned(row.main_inflow) }}</span>
            </template>
        </el-table-column>

        <el-table-column label="涨停原因" min-width="240" show-overflow-tooltip>
            <template #default="{row}">
                <span v-if="row.reason">{{ themeOf(row.reason) }}</span>
                <span v-else class="tx-muted">-</span>
            </template>
        </el-table-column>
    </el-table>
</template>

<script setup lang="js">
import {boardColor, fmtBoards, fmtMoney, fmtMoneySigned, fmtPct, trendClass} from '../utils/FmtX.js'

defineProps({
    rows: {type: Array, default: () => []},
    loading: {type: Boolean, default: false},
})

/** 涨停原因形如「机器人|公司在人形机器人领域…」，表格只展示主题 */
function themeOf(reason) {
    return String(reason || '').split('|')[0] || ''
}
</script>
