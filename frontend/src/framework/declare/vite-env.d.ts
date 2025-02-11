/// <reference types="vite/client" />
// 让IDE可以智能提示自定义的环境变量： https://cn.vitejs.dev/guide/env-and-mode#env-files
// https://blog.csdn.net/perfect2011/article/details/129930819?ydreferer=aHR0cHM6Ly9jbi5iaW5nLmNvbS8%3D

interface ImportMetaEnv {
    readonly VITE_BASE_API: string
    readonly VITE_APP_TITLE: string
    readonly VITE_OUT_DIR: string
    readonly VITE_LOCALE: string

    readonly VITE_MINIO_URL: string
    readonly VITE_ACCESS_KEY: string
    readonly VITE_SECRET_KEY: string
    readonly VITE_SECURE: boolean
    readonly VITE_BUCKET_NAME: string
    // 更多环境变量...
}

interface ImportMeta {
    readonly env: ImportMetaEnv
}