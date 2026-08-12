export class MapUtil {
    static Map2KVArray(map, kName = 'key', vName = 'value') {
        return Object.entries(map).map(([key, value]) => ({
            [kName]: key,    // 使用 [] 动态设置属性名
            [vName]: value   // 使用 [] 动态设置属性名
        }))
    }
}
