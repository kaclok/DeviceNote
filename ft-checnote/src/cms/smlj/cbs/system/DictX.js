import {computed, ref} from "vue"
import {SysX} from "./SysX.js"
import {Singleton} from "@/framework/services/Singleton.js"

/**
 * 字典下拉选项的唯一读取入口。
 *
 * 承包商类型 / 承包商处罚类型 / 项目类型 三类字典可在「基础设置」里自行增减，
 * 因此选项必须现查后端（cbs/dict/options），不允许写进 gd.json，也不许前端硬编码。
 *
 * 缓存：同一 dictType 只请求一次（模块级）；字典维护页增删改后调 invalidateDict 使其失效。
 */
const sys = Singleton.getInstance(SysX)
const cache = new Map()      // dictType -> ref([{id, name}])
const inflight = new Map()   // dictType -> AbortController

function load(type) {
    inflight.get(type)?.abort()
    const ac = new AbortController()
    inflight.set(type, ac)
    sys.biz('cbs/dict/options', {dictType: type}, ac.signal, () => {}, (r, data) => {
        inflight.delete(type)
        // NON_EMPTY 会把空集合整条吞掉，取不到按空数组处理（下拉为空，而不是报错）
        if (r) cache.get(type).value = data.data || []
    })
}

/** 某类字典的选项 [{id, name}]：首次调用触发请求，之后命中缓存 */
export function useDict(type) {
    let items = cache.get(type)
    if (!items) {
        items = ref([])
        cache.set(type, items)
        load(type)
    }
    return items
}

/** 某类字典的名称数组（业务字段存名称文本时用，与 useDict 共用同一份缓存） */
export function useDictNames(type) {
    const items = useDict(type)
    return computed(() => items.value.map(i => i.name))
}

/** 字典维护页增删改成功后调用：清缓存，下次 useDict 重新拉 */
export function invalidateDict(type) {
    inflight.get(type)?.abort()
    inflight.delete(type)
    cache.delete(type)
}
