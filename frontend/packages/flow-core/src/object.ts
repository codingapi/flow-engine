export class ObjectUtils {

    public static isEmptyObject(obj: any) {
        return Object.keys(obj).length === 0 && obj.constructor === Object;
    }


    public static cleanObject(obj: any, options = {
        removeNull: true,
        removeUndefined: true,
        removeEmptyString: true,
        removeEmptyArray: true,
        removeEmptyObject: true,
    }) {
        const {
            removeNull,
            removeUndefined,
            removeEmptyString,
            removeEmptyArray,
            removeEmptyObject,
        } = options;

        let newObj: any = {};

        for (const [key, value] of Object.entries(obj)) {
            // 跳过 null
            if (removeNull && value === null) continue;

            // 跳过 undefined
            if (removeUndefined && value === undefined) continue;

            // 跳过空字符串
            if (removeEmptyString && value === '') continue;

            // 跳过空数组
            if (removeEmptyArray && Array.isArray(value) && value.length === 0) continue;

            // 跳过空对象
            if (removeEmptyObject &&
                value &&
                typeof value === 'object' &&
                !Array.isArray(value) &&
                Object.keys(value).length === 0) continue;

            // 递归处理嵌套对象
            if (value && typeof value === 'object' && !Array.isArray(value)) {
                const cleaned = ObjectUtils.cleanObject(value, options);
                // 只有当清理后的对象不为空时才保留
                if (Object.keys(cleaned).length > 0) {
                    newObj[key] = cleaned;
                }
            } else {
                newObj[key] = value;
            }
        }
        return newObj;
    }
}