/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_APP_ENV: string
  readonly VITE_API_BASE_URL: string
  readonly VITE_BACKEND_URL: string
  readonly VITE_ENABLE_API_LOGS: string
  // 更多环境变量...
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}


