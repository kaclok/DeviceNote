<template>
    <div class="stat-card" :class="`tone-${tone}`">
        <div class="stat-title">{{ title }}</div>
        <div class="stat-value">
            {{ value }}
            <span v-if="unit" class="stat-unit">{{ unit }}</span>
        </div>
        <div v-if="sub" class="stat-sub">{{ sub }}</div>
    </div>
</template>

<script setup lang="js">
defineProps({
    title: {type: String, required: true},
    value: {type: [String, Number], default: '-'},
    unit: {type: String, default: ''},
    sub: {type: String, default: ''},
    // up=红 down=绿 primary=蓝 flat=灰
    tone: {type: String, default: 'primary'},
})
</script>

<style scoped>
.stat-card {
    flex: 1 1 0;
    min-width: 132px;
    padding: 12px 14px;
    border-radius: 10px;
    background: var(--bg-panel);
    border: 1px solid var(--bg-border);
    border-top: 2px solid var(--tone, var(--accent));
}

.stat-title {
    font-size: 12px;
    color: var(--tx-muted);
    margin-bottom: 6px;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
}

.stat-value {
    font-size: 22px;
    font-weight: 700;
    line-height: 1.15;
    color: var(--tone, var(--tx-main));
    font-variant-numeric: tabular-nums;
}

.stat-unit {
    font-size: 12px;
    font-weight: 400;
    margin-left: 2px;
    color: var(--tx-muted);
}

.stat-sub {
    margin-top: 5px;
    font-size: 11px;
    color: var(--tx-muted);
}

.tone-up {
    --tone: var(--up);
}

.tone-down {
    --tone: var(--down);
}

.tone-primary {
    --tone: var(--accent);
}

.tone-flat {
    --tone: var(--tx-muted);
}
</style>
