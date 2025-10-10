/**
 * Mock 数据工具函数
 * 
 * @file mocks/utils.ts
 * @description 提供 mock 数据生成的通用工具函数
 */

/**
 * 生成随机时间戳
 * @param daysAgo 多少天前 (正数表示过去，负数表示未来)
 * @param hoursOffset 小时偏移量 (正数表示未来，负数表示过去)
 * @returns ISO 8601 格式的时间戳字符串
 */
export const generateTimestamp = (daysAgo: number = 0, hoursOffset: number = 0): string => {
  const date = new Date()
  date.setDate(date.getDate() - daysAgo)
  date.setHours(date.getHours() + hoursOffset)
  return date.toISOString()
}

/**
 * 生成随机百分比（0-100）
 * @param min 最小值
 * @param max 最大值
 * @param decimals 小数位数
 * @returns 随机百分比
 */
export const generateRandomPercent = (min: number = 0, max: number = 100, decimals: number = 2): number => {
  const value = Math.random() * (max - min) + min
  return Number(value.toFixed(decimals))
}

/**
 * 生成32位十六进制ID
 * @returns 32位十六进制字符串
 */
export const generateHexId = (length: number = 32): string => {
  return Array.from({ length }, () =>
    Math.floor(Math.random() * 16).toString(16)
  ).join('')
}

/**
 * 从数组中随机选择一个元素
 * @param array 源数组
 * @returns 随机选择的元素
 */
export const randomChoice = <T>(array: T[]): T => {
  return array[Math.floor(Math.random() * array.length)]
}

/**
 * 从数组中随机选择多个元素
 * @param array 源数组
 * @param count 选择数量
 * @returns 随机选择的元素数组
 */
export const randomChoices = <T>(array: T[], count: number): T[] => {
  const shuffled = [...array].sort(() => 0.5 - Math.random())
  return shuffled.slice(0, count)
}

