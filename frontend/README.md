# 司库云控独立前端

Vue 3 + TypeScript + Vite 的前后端分离管理台，默认通过开发代理连接 `http://localhost:8080` 的 Java 后端。

## 页面结构

- `/login`：三岗位演示登录。
- `/dashboard`：资金驾驶舱与风险待办。
- `/accounts`：银行账户台账与新增账户。
- `/payments`：付款申请、审批、驳回和模拟银行执行。
- `/accounts`：银行、支付宝和微信支付资金账户。
- `/reconciliations`：银行与支付平台流水、自动/手工匹配与异常登记。
- `/exceptions`：业务异常与系统异常分类统计、筛选及闭环处置。
- `/plans`：资金计划和区间汇总。
- `/audits`：管理员审计日志。

## 本地启动

```bash
pnpm install
pnpm dev
```

前端访问地址：<http://localhost:5173>

如后端不在 `8080` 端口，复制 `.env.example` 为 `.env.local` 并修改 `VITE_API_TARGET`。

## 构建

```bash
pnpm build
```

生产环境应让网关把 `/api` 和 `/logout` 转发给 Java 后端，并把其他路径回退到前端 `index.html`。
