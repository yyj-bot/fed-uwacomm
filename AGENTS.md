# Repository Guidelines

## 项目约束
使用简体中文进行回复

所有对后端数据库的修改，应该都存放在 docs/shared/database/mysql/init/init_mysql.sql中，直接对该文件进行修改

项目管理员账号：admin 管理员密码：ab123456

modified文档应该放置于docs/shared/api/xxxxx/modified/ removed文档应该存放于docs/shared/api/xxxxx/removed/ 命名参考已经存在的文档 ，版本号使用我要求的 vx.x
### 测试运行命令
可以跟据实际测试进行实时修改
  JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 \
  PATH="$JAVA_HOME/bin:$PATH" \
  mvn -pl feduwacomm-server -am \
      -Dtest=CompleteFederatedLearningFlowTestV151 \
      -Dsurefire.failIfNoSpecifiedTests=false \
      test \
      > backend-springboot/feduwacomm-server/
  CompleteFederatedLearningFlowTestV151.log 2>&1

## 项目结构与模块组织
- `backend-springboot/`：包含三模块 Maven 工程。业务代码位于 `feduwacomm-server/src/main/java`，配置文件位于 `src/main/resources`，单元测试对应该模块下的 `src/test/java`。
- `python-vm/`：集中所有联邦学习流程脚本与 BELLHOP 适配器，源码在 `src/feduwacomm/`，自动化脚本位于 `scripts/`。
- `frontend-admin/`：Vite/React 管理端，核心组件在 `src/components`，数据访问在 `src/services`，状态管理在 `src/store`。
- `docs/`：跨模块文档、API 变更记录与数据库脚本（例如 `docs/shared/database/mysql/init/init_mysql.sql`）。

## 构建、测试与开发命令
```bash
# 后端本地启动
cd backend-springboot && mvn -pl feduwacomm-server spring-boot:run
# 后端指定用例
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 PATH=$JAVA_HOME/bin:$PATH mvn -pl feduwacomm-server -am \
  -Dtest=CompleteFederatedLearningFlowTestV151 -Dsurefire.failIfNoSpecifiedTests=false test
# 前端开发与检查
cd frontend-admin && npm install && npm run dev
npm run lint && npm run test:coverage
# Python 工作流
cd python-vm && pip install -r requirements.txt && python scripts/complete_workflow.py
```

## 编码风格与命名约定
- Java 采用 Spring 慣例：类名 PascalCase，Bean 方法 camelCase，使用 Lombok 减少样板代码。
- Python 必须遵循 PEP 8，四空格缩进，公开 API 需类型注解。
- TypeScript 组件用 PascalCase，资源文件保持 kebab-case；使用 `npm run lint -- --fix` 自动格式化。

## 测试指引
- Java 测试位于 `backend-springboot/feduwacomm-server/src/test/java`，使用 JUnit 与 Spring Test。命名遵循 `*Test`/`*IT`。
- 前端依赖 Vitest 与 Testing Library，放在 `test/` 或组件旁的 `*.test.tsx`。
- Python 使用 `pytest`，测试集中在 `python-vm/tests/`，优先 mock 外部 IO。
- 提交前需确保关键流程（任务创建、模型分发）拥有快速回归用例。

## 提交与 PR 准则
- Commit 建议遵循 `type(scope): subject`，不超 72 个字符，如 `feat(server): 支持 v1.5 模型分发`。
- PR 描述需链接关联任务，概述更改范围，并在涉及 UI 时附截图或终端输出。
- 说明手动步骤（数据库迁移、环境变量）及新增/调整的测试命令，便于 reviewer 快速验证。

## 安全与配置提示
- 敏感配置存放于 `.env` 或 `application-*.yml`，避免提交凭据。
- 重用 `bellhop_features_extracted.csv` 等大体量数据集，避免仓库膨胀。
- 对外暴露服务前确认 TLS 证书与 JWT 密钥来自安全渠道，保持与后端配置一致。
