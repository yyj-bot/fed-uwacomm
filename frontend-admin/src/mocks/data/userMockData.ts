// 用户管理 API Mock数据
// 基于 user-api-reference.md 文档

/**
 * 生成长期有效的Mock JWT Token（用于开发环境）
 * 过期时间设置为30天后，避免频繁重新登录
 */
function generateMockToken(): string {
  const header = btoa(JSON.stringify({ "alg": "HS256", "typ": "JWT" }))
  const expiresIn = Math.floor(Date.now() / 1000) + (30 * 24 * 60 * 60) // 30天后过期
  const payload = btoa(JSON.stringify({ 
    "sub": "a1b2c3d4e5f678901234567890123456",
    "userId": "a1b2c3d4e5f678901234567890123456",
    "iat": Math.floor(Date.now() / 1000),
    "exp": expiresIn
  }))
  return `${header}.${payload}.mock_signature`
}

/**
 * 生成长期有效的Mock Refresh Token（用于开发环境）
 * 过期时间设置为90天后
 */
function generateMockRefreshToken(): string {
  const header = btoa(JSON.stringify({ "alg": "HS256", "typ": "JWT" }))
  const expiresIn = Math.floor(Date.now() / 1000) + (90 * 24 * 60 * 60) // 90天后过期
  const payload = btoa(JSON.stringify({ 
    "sub": "a1b2c3d4e5f678901234567890123456",
    "type": "refresh",
    "iat": Math.floor(Date.now() / 1000),
    "exp": expiresIn
  }))
  return `${header}.${payload}.mock_refresh_signature`
}

/**
 * 用户角色类型
 */
type UserRole = "ADMIN" | "RESEARCHER" | "OPERATOR" | "VIEWER";

/**
 * 用户状态类型
 */
type UserStatus = "ACTIVE" | "INACTIVE" | "LOCKED" | "DELETED";

/**
 * Mock 用户数据接口
 */
interface MockUser {
  userId: string;
  username: string;
  email: string;
  password: string; // 仅用于mock验证，实际API不返回
  role: UserRole;
  status: UserStatus;
  createdAt: string;
  updatedAt: string;
  lastLoginTime: string;
  lastLoginIp: string;
}

/**
 * Mock 用户数据
 * 注意：实际API中不会返回密码，密码仅用于登录验证
 */
export const mockUsers: MockUser[] = [
  {
    userId: "a1b2c3d4e5f678901234567890123456",
    username: "admin",
    email: "admin@feduwacomm.com",
    password: "ab123456", // 仅用于mock验证，实际API不返回
    role: "ADMIN",
    status: "ACTIVE",
    createdAt: "2024-01-01T00:00:00.000Z",
    updatedAt: "2024-01-01T00:00:00.000Z",
    lastLoginTime: "2024-01-01T10:00:00.000Z",
    lastLoginIp: "192.168.1.100"
  }
];

/**
 * 用户API Mock数据
 */
export const userApiMock = {
  // 3.2 用户登录接口 - POST /api/user/login
  login: {
    success: {
      code: 200,
      message: "登录成功",
      data: {
        token: generateMockToken(),
        refreshToken: generateMockRefreshToken(),
        expiresIn: 30 * 24 * 60 * 60, // 30天（秒）
        user: {
          userId: "a1b2c3d4e5f678901234567890123456",
          username: "admin",
          email: "admin@feduwacomm.com",
          role: "ADMIN",
          status: "ACTIVE",
          lastLoginTime: new Date().toISOString(),
          lastLoginIp: "192.168.1.100"
        }
      }
    },
    error401: {
      code: 401,
      message: "账号或密码错误",
      data: {
        loginAttempts: 1,
        lockedUntil: null
      }
    },
    error423: {
      code: 423,
      message: "账号已锁定",
      data: {
        loginAttempts: 5,
        lockedUntil: new Date(Date.now() + 3600000).toISOString() // 锁定1小时
      }
    }
  },

  // 3.1 用户注册接口 - POST /api/user/register
  register: {
    success: {
      code: 200,
      message: "注册成功",
      data: {
        userId: "new-user-id-123456",
        username: "newuser",
        email: "newuser@example.com",
        role: "VIEWER",
        status: "ACTIVE",
        createdAt: new Date().toISOString()
      }
    },
    error400: {
      code: 400,
      message: "参数验证失败",
      data: {
        field: "email",
        error: "邮箱格式不正确"
      }
    },
    error409: {
      code: 409,
      message: "用户已存在",
      data: {
        field: "username",
        error: "用户名已存在"
      }
    }
  },

  // 3.3 刷新Token接口 - POST /api/user/refresh
  refresh: {
    success: {
      code: 200,
      message: "Token刷新成功",
      data: {
        token: generateMockToken(),
        refreshToken: generateMockRefreshToken(),
        expiresIn: 30 * 24 * 60 * 60 // 30天（秒）
      }
    },
    error401: {
      code: 401,
      message: "刷新Token已过期",
      data: null
    }
  },

  // 3.4 用户登出接口 - POST /api/user/logout
  logout: {
    success: {
      code: 200,
      message: "登出成功",
      data: null
    }
  },

  // 4.1 获取当前用户信息 - GET /api/user/profile
  profile: {
    success: {
      code: 200,
      message: "获取成功",
      data: {
        userId: "a1b2c3d4e5f678901234567890123456",
        username: "admin",
        email: "admin@feduwacomm.com",
        role: "ADMIN",
        status: "ACTIVE",
        lastLoginTime: "2024-01-01T10:00:00.000Z",
        lastLoginIp: "192.168.1.100",
        createdAt: "2024-01-01T00:00:00.000Z",
        updatedAt: "2024-01-01T00:00:00.000Z"
      }
    },
    error401: {
      code: 401,
      message: "Token无效或已过期",
      data: null
    }
  },

  // 4.2 更新用户信息 - PUT /api/user/profile
  updateProfile: {
    success: {
      code: 200,
      message: "更新成功",
      data: {
        userId: "a1b2c3d4e5f678901234567890123456",
        username: "admin",
        email: "admin@feduwacomm.com",
        role: "ADMIN",
        status: "ACTIVE",
        updatedAt: new Date().toISOString()
      }
    },
    error400: {
      code: 400,
      message: "参数验证失败",
      data: {
        field: "email",
        error: "邮箱格式不正确"
      }
    },
    error409: {
      code: 409,
      message: "邮箱已存在",
      data: {
        field: "email",
        error: "该邮箱已被其他用户使用"
      }
    }
  },

  // 4.3 修改密码 - PUT /api/user/password
  updatePassword: {
    success: {
      code: 200,
      message: "密码修改成功",
      data: null
    },
    error400: {
      code: 400,
      message: "参数验证失败",
      data: {
        field: "newPassword",
        error: "密码长度必须为6-20字符"
      }
    },
    error401: {
      code: 401,
      message: "旧密码错误",
      data: null
    }
  }
};

/**
 * 管理员用户管理 API Mock数据
 */
export const adminUserApiMock = {
  // 管理员用户列表 - GET /api/admin/user/list
  list: {
    success: {
      code: 200,
      message: "获取成功",
      data: {
        total: 1,
        page: 1,
        size: 10,
        pages: 1,
        items: [
          {
            userId: "a1b2c3d4e5f678901234567890123456",
            username: "admin",
            email: "admin@feduwacomm.com",
            role: "ADMIN",
            status: "ACTIVE",
            createdAt: "2024-01-01T00:00:00.000Z",
            updatedAt: "2024-01-01T00:00:00.000Z",
            lastLoginTime: "2024-01-01T10:00:00.000Z",
            lastLoginIp: "192.168.1.100"
          }
        ]
      }
    }
  },

  // 创建用户 - POST /api/admin/user/create
  create: {
    success: {
      code: 200,
      message: "用户创建成功",
      data: {
        userId: "new-user-id",
        username: "newuser",
        email: "newuser@example.com",
        role: "VIEWER",
        status: "ACTIVE",
        createdAt: new Date().toISOString()
      }
    }
  },

  // 更新用户 - PUT /api/admin/user/{userId}
  update: {
    success: {
      code: 200,
      message: "用户更新成功",
      data: {
        userId: "a1b2c3d4e5f678901234567890123456",
        username: "admin",
        email: "admin@feduwacomm.com",
        role: "ADMIN",
        status: "ACTIVE",
        updatedAt: new Date().toISOString()
      }
    }
  },

  // 删除用户 - DELETE /api/admin/user/{userId}
  delete: {
    success: {
      code: 200,
      message: "用户删除成功",
      data: {
        userId: "a1b2c3d4e5f678901234567890123456",
        deletedAt: new Date().toISOString()
      }
    }
  }
};

/**
 * 请求参数示例
 */
export const userApiRequestExamples = {
  // 用户注册请求参数
  register: {
    username: "testuser",
    email: "test@example.com",
    password: "password123",
    confirmPassword: "password123"
  },

  // 用户登录请求参数
  login: {
    loginIdentifier: "admin", // 用户名或邮箱
    password: "ab123456"
  },

  // 更新用户信息请求参数
  updateProfile: {
    username: "newusername",
    email: "newemail@example.com"
  },

  // 修改密码请求参数
  updatePassword: {
    oldPassword: "ab123456",
    newPassword: "newpassword123",
    confirmPassword: "newpassword123"
  }
};

