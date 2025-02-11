import {createI18n} from 'vue-i18n'

export const ELocaleType = {
    nil: 'nil',
    zh_CN: "zh_CN",
    en_US: 'en_US',
}

export class LocaleService {
    static _curLocale = ELocaleType.nil;
    static _curLocaleContent = null;

    static i18n = null;

    static _SetHTMLLocale(localeType) {
        document.querySelector('html')?.setAttribute('lang', localeType)
    }

    static _LocaleOption(localeContent) {
        return {
            legacy: false, // 组合式api
            locale: LocaleService._curLocale,
            fallbackLocale: LocaleService._curLocale,
            messages: {
                [LocaleService._curLocale]: localeContent.default ?? {},
            },
            sync: true,
            silentTranslationWarn: true,
            missingWarn: false,
            silentFallbackWarn: true
        }
    }

    static async Switch(app, targetLocale = ELocaleType.zh_CN, force = false) {
        if (!force && targetLocale === this._curLocale) {
            return;
        }

        console.assert(targetLocale in ELocaleType, "Locale config not valid.");
        this._curLocale = targetLocale;
        console.log("current locale is: ", targetLocale);

        LocaleService._SetHTMLLocale(targetLocale);
        LocaleService._curLocaleContent = await import(`@/locales/${targetLocale}.json`);
        let options = LocaleService._LocaleOption(LocaleService._curLocaleContent);
        LocaleService.i18n = createI18n(options);

        app.use(LocaleService.i18n);
    }

    static GetByKey(key, ...args) {
        if (!key || !LocaleService.i18n) {
            return null;
        }
        return LocaleService.i18n.global.t(key, ...args);
    }
}

// 外部使用必须是t, 否则i18n ally不能正常使用，但是语言获取功能是正常的
export function t(key, ...args) {
    return LocaleService.GetByKey(key, ...args);
}

export function Switch(app, targetLocale = ELocaleType.zh_CN, force = false) {
    return LocaleService.Switch(app, targetLocale = ELocaleType.zh_CN, force = false);
}
