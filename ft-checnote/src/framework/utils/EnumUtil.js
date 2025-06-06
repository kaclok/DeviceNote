export class EnumUtil {
    static asEnum(table) {
        return Object.freeze(table);
    }
}

export function asEnum(table) {
    return DateTimeUtil.asEnum(table);
}
