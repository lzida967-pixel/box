declare module '*.vue' {
  import type { DefineComponent } from 'vue'
  const component: DefineComponent<{}, {}, any>
  export default component
}

declare module 'vue' {
  export * from '@vue/runtime-core'
}

declare module 'vue-router' {
  export * from '@vue/router'
}

declare module 'pinia' {
  export * from '@pinia/core'
}

declare module 'element-plus' {
  const ElementPlus: any
  export default ElementPlus
}

declare module '@element-plus/icons-vue' {
  export const Check: any
  export const Clock: any
  export const VideoCamera: any
  export const Phone: any
  export const MoreFilled: any
  export const Picture: any
  export const Paperclip: any
  export const FaceSmiling: any
  export const Promotion: any
  export const Search: any
}