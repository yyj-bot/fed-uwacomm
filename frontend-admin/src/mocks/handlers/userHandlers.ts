/**
 * 用户管理 API 处理器
 * 基于 user-api-reference.md 文档
 */

import { http, HttpResponse } from 'msw'
import { userApiMock, adminUserApiMock, mockUsers } from '../data/userMockData'

export const userHandlers = [
  // ==================== 认证接口 (/api/user) ====================

  // 3.1 用户注册 - POST /api/user/register
  http.post('http://localhost:5173/api/user/register', async ({ request }) => {
    const body = await request.json() as any

    // 参数验证
    if (!body.username || !body.email || !body.password || !body.confirmPassword) {
      return HttpResponse.json({
        code: 400,
        message: "参数验证失败",
        data: {
          field: "required",
          error: "必填字段不能为空"
        }
      }, { status: 400 })
    }

    // 密码确认验证
    if (body.password !== body.confirmPassword) {
      return HttpResponse.json({
        code: 400,
        message: "参数验证失败",
        data: {
          field: "confirmPassword",
          error: "两次输入的密码不一致"
        }
      }, { status: 400 })
    }

    // 邮箱格式验证
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
    if (!emailRegex.test(body.email)) {
      return HttpResponse.json(userApiMock.register.error400, { status: 400 })
    }

    // 检查用户名是否已存在
    const existingUser = mockUsers.find(
      u => u.username === body.username || u.email === body.email
    )
    if (existingUser) {
      return HttpResponse.json(userApiMock.register.error409, { status: 409 })
    }

    return HttpResponse.json(userApiMock.register.success)
  }),

  // 3.2 用户登录 - POST /api/user/login
  http.post('http://localhost:5173/api/user/login', async ({ request }) => {
    const body = await request.json() as any

    // 参数验证
    if (!body.loginIdentifier || !body.password) {
      return HttpResponse.json({
        code: 400,
        message: "参数验证失败",
        data: {
          field: "required",
          error: "登录标识符和密码不能为空"
        }
      }, { status: 400 })
    }

    // 查找用户
    const user = mockUsers.find(
      u => (u.username === body.loginIdentifier || u.email === body.loginIdentifier)
    )

    // 用户不存在或密码错误
    if (!user || user.password !== body.password) {
      return HttpResponse.json(userApiMock.login.error401, { status: 401 })
    }

    // 检查用户状态
    if (user.status === 'LOCKED') {
      return HttpResponse.json(userApiMock.login.error423, { status: 423 })
    }

    if (user.status === 'INACTIVE' || user.status === 'DELETED') {
      return HttpResponse.json({
        code: 403,
        message: "账号已被禁用",
        data: null
      }, { status: 403 })
    }

    // 登录成功
    const loginData = {
      ...userApiMock.login.success,
      data: {
        ...userApiMock.login.success.data,
        user: {
          userId: user.userId,
          username: user.username,
          email: user.email,
          role: user.role,
          status: user.status,
          lastLoginTime: new Date().toISOString(),
          lastLoginIp: "192.168.1.100"
        }
      }
    }

    return HttpResponse.json(loginData)
  }),

  // 3.3 刷新Token - POST /api/user/refresh
  http.post('http://localhost:5173/api/user/refresh', async ({ request }) => {
    const authHeader = request.headers.get('Authorization')

    // 检查Authorization头
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      return HttpResponse.json({
        code: 401,
        message: "Token缺失",
        data: null
      }, { status: 401 })
    }

    const token = authHeader.substring(7)

    // 简单验证：检查是否是有效的refresh token
    if (token !== userApiMock.login.success.data.refreshToken) {
      return HttpResponse.json(userApiMock.refresh.error401, { status: 401 })
    }

    return HttpResponse.json(userApiMock.refresh.success)
  }),

  // 3.4 用户登出 - POST /api/user/logout
  http.post('http://localhost:5173/api/user/logout', async ({ request }) => {
    const authHeader = request.headers.get('Authorization')

    // 检查Authorization头
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      return HttpResponse.json({
        code: 401,
        message: "Token缺失",
        data: null
      }, { status: 401 })
    }

    return HttpResponse.json(userApiMock.logout.success)
  }),

  // ==================== 用户管理接口 (/api/user) ====================

  // 4.1 获取当前用户信息 - GET /api/user/profile
  http.get('http://localhost:5173/api/user/profile', ({ request }) => {
    const authHeader = request.headers.get('Authorization')

    // 检查Authorization头
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      return HttpResponse.json(userApiMock.profile.error401, { status: 401 })
    }

    return HttpResponse.json(userApiMock.profile.success)
  }),

  // 4.2 更新用户信息 - PUT /api/user/profile
  http.put('http://localhost:5173/api/user/profile', async ({ request }) => {
    const authHeader = request.headers.get('Authorization')

    // 检查Authorization头
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      return HttpResponse.json({
        code: 401,
        message: "Token缺失",
        data: null
      }, { status: 401 })
    }

    const body = await request.json() as any

    // 如果提供了邮箱，验证格式
    if (body.email) {
      const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
      if (!emailRegex.test(body.email)) {
        return HttpResponse.json(userApiMock.updateProfile.error400, { status: 400 })
      }

      // 检查邮箱是否已被使用（排除当前用户）
      const existingUser = mockUsers.find(
        u => u.email === body.email && u.userId !== userApiMock.profile.success.data.userId
      )
      if (existingUser) {
        return HttpResponse.json(userApiMock.updateProfile.error409, { status: 409 })
      }
    }

    // 更新成功
    const updatedData = {
      ...userApiMock.updateProfile.success,
      data: {
        ...userApiMock.updateProfile.success.data,
        username: body.username || userApiMock.updateProfile.success.data.username,
        email: body.email || userApiMock.updateProfile.success.data.email
      }
    }

    return HttpResponse.json(updatedData)
  }),

  // 4.3 修改密码 - PUT /api/user/password
  http.put('http://localhost:5173/api/user/password', async ({ request }) => {
    const authHeader = request.headers.get('Authorization')

    // 检查Authorization头
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      return HttpResponse.json({
        code: 401,
        message: "Token缺失",
        data: null
      }, { status: 401 })
    }

    const body = await request.json() as any

    // 参数验证
    if (!body.oldPassword || !body.newPassword || !body.confirmPassword) {
      return HttpResponse.json({
        code: 400,
        message: "参数验证失败",
        data: {
          field: "required",
          error: "所有密码字段不能为空"
        }
      }, { status: 400 })
    }

    // 新密码确认验证
    if (body.newPassword !== body.confirmPassword) {
      return HttpResponse.json({
        code: 400,
        message: "参数验证失败",
        data: {
          field: "confirmPassword",
          error: "两次输入的新密码不一致"
        }
      }, { status: 400 })
    }

    // 密码长度验证
    if (body.newPassword.length < 6 || body.newPassword.length > 20) {
      return HttpResponse.json(userApiMock.updatePassword.error400, { status: 400 })
    }

    // 验证旧密码（这里用admin的密码进行验证）
    const currentUser = mockUsers[0] // admin用户
    if (body.oldPassword !== currentUser.password) {
      return HttpResponse.json(userApiMock.updatePassword.error401, { status: 401 })
    }

    return HttpResponse.json(userApiMock.updatePassword.success)
  }),

  // ==================== 管理员用户管理接口 (/api/admin/user) ====================

  // 管理员用户列表 - GET /api/admin/user/list
  http.get('http://localhost:5173/api/admin/user/list', ({ request }) => {
    const authHeader = request.headers.get('Authorization')

    // 检查Authorization头
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      return HttpResponse.json({
        code: 401,
        message: "Token缺失",
        data: null
      }, { status: 401 })
    }

    // 获取查询参数
    const url = new URL(request.url)
    const page = parseInt(url.searchParams.get('page') || '1')
    const size = parseInt(url.searchParams.get('size') || '10')

    // 返回用户列表
    const responseData = {
      ...adminUserApiMock.list.success,
      data: {
        ...adminUserApiMock.list.success.data,
        page,
        size
      }
    }

    return HttpResponse.json(responseData)
  }),

  // 创建用户 - POST /api/admin/user/create
  http.post('http://localhost:5173/api/admin/user/create', async ({ request }) => {
    const authHeader = request.headers.get('Authorization')

    // 检查Authorization头
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      return HttpResponse.json({
        code: 401,
        message: "Token缺失",
        data: null
      }, { status: 401 })
    }

    const body = await request.json() as any

    // 参数验证
    if (!body.username || !body.email || !body.password) {
      return HttpResponse.json({
        code: 400,
        message: "参数验证失败",
        data: {
          field: "required",
          error: "必填字段不能为空"
        }
      }, { status: 400 })
    }

    return HttpResponse.json(adminUserApiMock.create.success)
  }),

  // 更新用户 - PUT /api/admin/user/:userId
  http.put('http://localhost:5173/api/admin/user/:userId', async ({ request, params }) => {
    const authHeader = request.headers.get('Authorization')

    // 检查Authorization头
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      return HttpResponse.json({
        code: 401,
        message: "Token缺失",
        data: null
      }, { status: 401 })
    }

    const { userId } = params

    // 检查用户是否存在
    const user = mockUsers.find(u => u.userId === userId)
    if (!user) {
      return HttpResponse.json({
        code: 404,
        message: "用户不存在",
        data: null
      }, { status: 404 })
    }

    return HttpResponse.json(adminUserApiMock.update.success)
  }),

  // 删除用户 - DELETE /api/admin/user/:userId
  http.delete('http://localhost:5173/api/admin/user/:userId', ({ request, params }) => {
    const authHeader = request.headers.get('Authorization')

    // 检查Authorization头
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      return HttpResponse.json({
        code: 401,
        message: "Token缺失",
        data: null
      }, { status: 401 })
    }

    const { userId } = params

    // 检查用户是否存在
    const user = mockUsers.find(u => u.userId === userId)
    if (!user) {
      return HttpResponse.json({
        code: 404,
        message: "用户不存在",
        data: null
      }, { status: 404 })
    }

    return HttpResponse.json(adminUserApiMock.delete.success)
  })
];

