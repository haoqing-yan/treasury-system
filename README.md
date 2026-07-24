# 司库云控平台 V0.1

这是一个可直接运行的企业司库系统首版。Java 后端采用 Spring Boot、Spring Security、Spring Data JPA、MySQL 和 Flyway；独立前端位于 `frontend`，采用 Vue 3、TypeScript、Vite 和 Pinia。后端仍保留一套内置管理台作为本地兜底。

## 已实现功能

- 资金驾驶舱：集团余额、可用资金、今日流出、七日净流量、银行分布、风险待办。
- 资金账户：统一管理银行、支付宝和微信支付账户的台账、余额、状态、用途、余额预警和账号脱敏。
- 付款管理：草稿、提交、审批、驳回、银行/支付宝/微信支付模拟执行及余额扣减。
- 批量付款：将已审批付款统一组批并预约执行，通过有界线程池并发处理，同一付款账户自动串行，并提供批次明细和结果汇总。
- 渠道对账：银行与支付平台流水台账、付款自动匹配、手工匹配和异常流水登记。
- 异常中心：分为业务异常和系统异常。付款风险、对账差异、账户预警归入业务异常；接口、定时任务和运行故障归入系统异常。支持认领、解决、重新打开及审计追踪。
- 资金计划：未来资金流入流出计划及区间汇总。
- 权限控制：基于 Spring Security 6 的 RBAC，用户与角色存储在 MySQL，业务接口按原子权限校验，支持资金经办、资金审批、系统管理员三类岗位。
- 风险控制：岗位分离、状态机校验、余额校验、重复付款提示、CSRF 防护。
- 审计追踪：记录关键业务动作、操作人、对象、时间和来源地址。
- 演示数据：首次启动时自动生成集团账户、付款和资金计划数据。

## 启动方式

需要 Java 17、Maven 3.8 或更高版本以及 MySQL 8 或更高版本。默认连接 `root@127.0.0.1:3306`，首次启动会创建 `treasury` 数据库并由 Flyway 建表。密码通过环境变量传入，不要写入配置文件：

```bash
export DB_PASSWORD='你的 MySQL 密码'
mvn spring-boot:run
```

如需覆盖连接信息，可设置 `DB_URL`、`DB_USERNAME` 和 `DB_PASSWORD`。也可以复制 `.env.example` 为 `.env`，执行 `set -a; source .env; set +a` 加载，但不要提交真实 `.env` 文件。

临时继续使用原 H2 文件库：

```bash
SPRING_PROFILES_ACTIVE=h2 mvn spring-boot:run
```

启动后访问：<http://localhost:8080>

独立前端另开一个终端启动：

```bash
cd frontend
pnpm install
pnpm dev
```

独立前端访问：<http://localhost:5173>

## 演示账号

| 岗位 | 账号 | 密码 | 能力 |
|---|---|---|---|
| 资金经办 | `operator` | `operator123` | 新建付款、提交审批、维护资金计划 |
| 资金审批 | `approver` | `approver123` | 审批或驳回付款 |
| 系统管理员 | `admin` | `admin123` | 全部功能、银行执行、账户维护、审计日志 |

### 权限模型

后端以权限而不是角色名保护业务方法，前端根据登录接口返回的同一份权限清单控制菜单和按钮。当前角色映射如下：

| 角色 | 权限 |
|---|---|
| `OPERATOR` | `payment:create`、`payment:submit`、`cash-plan:create` |
| `APPROVER` | `payment:approve`、`reconciliation:handle`、`exception:handle` |
| `ADMIN` | 全部权限（另含 `account:manage`、`payment:execute`、`payment:batch`、`audit:read`） |

新增业务动作时，先在 `SystemPermission` 定义原子权限，再将其分配给 `SystemRole`，最后用 `@PreAuthorize("hasAuthority('...')")` 保护服务方法。

## 测试

```bash
mvn test
```

集成测试覆盖驾驶舱、付款申请、岗位复核、模拟支付、批次组建、多线程批次执行、余额扣减、越权拦截和参数校验。

## 数据库

- 默认数据库：MySQL 8+，库名 `treasury`，字符集 `utf8mb4`；本机 MySQL 9.3 已通过连接和业务接口验证。
- 表结构：`bank_accounts`、`payment_orders`、`payment_batches`、`payment_batch_items`、`cash_plans`、`bank_transactions`、`exception_cases`、`audit_logs`、`app_users`、`app_user_roles`；资金账户使用 `channel` 区分 `BANK`、`ALIPAY`和 `WECHAT`。
- 版本管理：Flyway 脚本位于 `src/main/resources/db/migration`，禁止在生产环境使用 Hibernate 自动改表。
- H2 仅作为备用开发模式和自动化测试数据库。

## 数据与扩展说明

- MySQL 首次启动会写入演示数据；清空业务表后重启可重新初始化。
- 当前银行、支付宝和微信支付均为用于验证业务闭环的模拟执行；正式环境需分别对接银企直联、支付宝开放平台和微信支付 API v3，并完成证书与回调验签。
- 生产部署前应将演示账号替换为数据库用户体系、LDAP、OIDC 或企业统一身份认证。
- 银行账号目前在接口层脱敏；生产版本还需要增加数据库字段加密、密钥管理、电子签名和银行回单验签。
- 融资债务、票据、资金池、收款认领和 ERP 凭证接口可在现有领域模块上继续扩展。
