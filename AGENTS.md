# Repository Guidelines
## 项目级约束

使用简体中文回复

除非有我的明确要求，否则不要在代码中留下 TODO以及未完成的业务逻辑

如果你有任何疑问，都应该先向我发出询问，不要擅自做决定。

所有数据库修改都保存在 docs/shared/database/mysql/init/init_mysql.sql 中

## 后端测试约束
通用测试命令，使用指定版本的java运行单元测试，需要跟据实际的测试用例进行修改：
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 PATH=$JAVA_HOME/bin:$PATH mvn -pl feduwacomm-server -am
    -Dtest=CompleteFederatedLearningFlowTestV151 -Dsurefire.failIfNoSpecifiedTests=false test

## Project Structure & Module Organization
FedUWAComm is a monorepo with four active modules. `python-vm/` houses the machine learning and BELLHOP adapters under `src/feduwacomm/` with automation scripts in `scripts/`. `backend-springboot/` is a tri-module Maven project (`feduwacomm-common`, `-pojo`, `-server`) with code in `feduwacomm-server/src/main/java` and config in `src/main/resources`. `frontend-admin/` delivers the Vite/React dashboard; UI primitives sit in `src/components`, data hooks in `src/services`, and state in `src/store`. MATLAB experiments remain isolated in `ofdm-underwater/`, and cross-cutting documentation lives under `docs/`.

## Build, Test, and Development Commands
Python VM: `pip install -r requirements.txt`, then `python scripts/complete_workflow.py` for the default pipeline. Backend: from `backend-springboot/`, run `mvn -pl feduwacomm-server spring-boot:run` for local API work, `mvn clean package` for artifacts, and `mvn test` to execute the suite. Frontend: within `frontend-admin/`, run `npm install`, `npm run dev`, `npm run lint`, and `npm run test:coverage` for CI parity.

## Coding Style & Naming Conventions
Use PEP 8 in Python with four-space indents, expressive module names (for example `acoustic`, `ml`), and type hints on public APIs; keep datasets and notebooks outside `src/`. Java code follows Spring conventions: PascalCase classes, camelCase beans, Lombok for boilerplate, REST controllers grouped by resource packages. TypeScript favors strict types, PascalCase components, kebab-case asset files, and ESLint autofix via `npm run lint -- --fix` when needed.

## Testing Guidelines
Focus on quick unit checks before longer simulations. Place Python tests in a `python-vm/tests/` package using `pytest`, mocking external IO where possible. Backend tests live in `backend-springboot/feduwacomm-server/src/test/java`; mirror the main package tree and isolate database calls. Frontend specs belong in `frontend-admin/test` or alongside components as `.test.tsx`; keep coverage from `npm run test:coverage` steady.

## Commit & Pull Request Guidelines
History mixes Chinese and English but leans on `type(scope): subject` (for example `docs(model): ...`). Keep commit subjects under 72 characters, use imperative verbs, and avoid touching multiple modules unless necessary. Pull requests should link an issue, spell out cross-module impacts, attach screenshots or console output for UI/UI-state changes, and list manual steps such as migrations or env updates. Tag reviewers who own the areas you edited.

## Security & Configuration Tips
Keep secrets in local `.env` files (`python-vm/.env`, backend `application-*.yml`) and never commit them. Large reference datasets like `bellhop_features_extracted.csv` should be reused rather than duplicated. When exposing services externally, confirm TLS material and JWT keys match the values shared via the secure ops channel.
