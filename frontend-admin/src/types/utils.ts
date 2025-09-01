/**
 * 通用工具类型定义
 * 提供项目中通用的TypeScript工具类型
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

/**
 * 深度只读类型
 * 将对象的所有属性（包括嵌套对象）设置为只读
 */
export type DeepReadonly<T> = {
  readonly [P in keyof T]: T[P] extends object ? DeepReadonly<T[P]> : T[P]
}

/**
 * 部分字段可选类型
 * 将指定字段设为可选，其他字段保持原有类型
 */
export type PartialBy<T, K extends keyof T> = Omit<T, K> & Partial<Pick<T, K>>

/**
 * 部分字段必需类型
 * 将指定字段设为必需，其他字段保持原有类型
 */
export type RequiredBy<T, K extends keyof T> = Omit<T, K> & Required<Pick<T, K>>

/**
 * 选择字段类型
 * 从对象类型中选择指定字段
 */
export type PickBy<T, K extends keyof T> = Pick<T, K>

/**
 * 排除字段类型
 * 从对象类型中排除指定字段
 */
export type OmitBy<T, K extends keyof T> = Omit<T, K>

/**
 * 可为空类型
 * 允许值为null或undefined
 */
export type Nullable<T> = T | null | undefined

/**
 * 非空类型
 * 确保值不为null或undefined
 */
export type NonNullable<T> = T extends null | undefined ? never : T

/**
 * 可选的键类型
 * 获取对象中可选属性的键
 */
export type OptionalKeys<T> = {
  [K in keyof T]-?: {} extends Pick<T, K> ? K : never
}[keyof T]

/**
 * 必需的键类型
 * 获取对象中必需属性的键
 */
export type RequiredKeys<T> = {
  [K in keyof T]-?: {} extends Pick<T, K> ? never : K
}[keyof T]
