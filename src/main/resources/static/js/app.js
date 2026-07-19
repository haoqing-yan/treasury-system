const state = {
    csrf: null,
    user: null,
    dashboard: null,
    accounts: [],
    payments: [],
    plans: [],
    paymentStatus: ""
};

const pageMeta = {
    dashboard: ["资金驾驶舱", "资金运营总览"],
    accounts: ["银行账户", "账户全生命周期"],
    payments: ["付款管理", "支付流程与风险控制"],
    plans: ["资金计划", "现金流预测"],
    audits: ["审计日志", "资金操作追溯"]
};

const statusNames = {
    DRAFT: "草稿", PENDING: "待审批", APPROVED: "待支付", PAID: "已支付", REJECTED: "已驳回",
    ACTIVE: "正常", RESTRICTED: "受限", FROZEN: "冻结", CLOSED: "已销户"
};

const accountTypeNames = {
    BASIC: "基本账户", GENERAL: "一般账户", SPECIAL: "专用账户", SETTLEMENT: "结算账户", CASH_POOL: "资金池账户"
};

const actionNames = {
    CREATE: "新建", UPDATE: "更新", SYNC: "同步", SUBMIT: "提交审批", APPROVE: "审批通过",
    REJECT: "驳回", BANK_EXECUTE: "银行执行"
};

document.addEventListener("DOMContentLoaded", initialize);

async function initialize() {
    setTodayLabels();
    bindGlobalEvents();
    setDefaultPlanDates();
    try {
        const [csrf, user] = await Promise.all([
            rawFetch("/api/auth/csrf"),
            rawFetch("/api/auth/me")
        ]);
        state.csrf = csrf;
        state.user = user;
        applyUser();
        await Promise.all([loadDashboard(), loadAccounts(), loadPayments(), loadPlans()]);
    } catch (error) {
        if (error.status === 401) {
            location.href = "/login.html";
            return;
        }
        toast("初始化失败", error.message, true);
    }
}

async function rawFetch(path, options = {}) {
    const headers = { "Accept": "application/json", ...(options.headers || {}) };
    if (options.body && typeof options.body !== "string") {
        headers["Content-Type"] = "application/json";
        options.body = JSON.stringify(options.body);
    }
    if (options.method && options.method !== "GET" && state.csrf) {
        headers[state.csrf.headerName] = state.csrf.token;
    }
    const response = await fetch(path, { credentials: "same-origin", ...options, headers });
    if (response.status === 401) {
        const error = new Error("登录状态已失效");
        error.status = 401;
        throw error;
    }
    const text = await response.text();
    const data = text ? JSON.parse(text) : null;
    if (!response.ok) {
        const detail = data?.fields ? Object.values(data.fields)[0] : null;
        const error = new Error(detail || data?.message || "操作失败");
        error.status = response.status;
        throw error;
    }
    return data;
}

function bindGlobalEvents() {
    document.querySelectorAll(".nav-item").forEach(button => button.addEventListener("click", () => navigate(button.dataset.page)));
    document.querySelectorAll("[data-page-link]").forEach(button => button.addEventListener("click", () => navigate(button.dataset.pageLink)));
    document.querySelectorAll("[data-action='open-payment']").forEach(button => button.addEventListener("click", openPaymentModal));
    document.querySelectorAll("[data-action='open-account']").forEach(button => button.addEventListener("click", () => document.getElementById("accountModal").showModal()));
    document.querySelectorAll("[data-action='open-plan']").forEach(button => button.addEventListener("click", () => document.getElementById("planModal").showModal()));

    document.getElementById("userButton").addEventListener("click", event => {
        event.stopPropagation();
        const dropdown = document.getElementById("userDropdown");
        dropdown.classList.toggle("open");
        event.currentTarget.setAttribute("aria-expanded", dropdown.classList.contains("open"));
    });
    document.addEventListener("click", () => document.getElementById("userDropdown").classList.remove("open"));
    document.getElementById("logoutButton").addEventListener("click", logout);

    document.getElementById("menuButton").addEventListener("click", toggleMobileMenu);
    document.getElementById("mobileOverlay").addEventListener("click", toggleMobileMenu);

    document.getElementById("accountSearch").addEventListener("input", debounce(renderAccounts, 180));
    document.getElementById("accountStatusFilter").addEventListener("change", renderAccounts);
    document.getElementById("paymentSearch").addEventListener("input", debounce(renderPayments, 180));
    document.getElementById("paymentTabs").addEventListener("click", event => {
        const button = event.target.closest("button[data-status]");
        if (!button) return;
        state.paymentStatus = button.dataset.status;
        document.querySelectorAll("#paymentTabs button").forEach(item => item.classList.toggle("active", item === button));
        renderPayments();
    });

    document.getElementById("paymentsBody").addEventListener("click", handlePaymentAction);
    document.getElementById("paymentForm").addEventListener("submit", createPayment);
    document.getElementById("accountForm").addEventListener("submit", createAccount);
    document.getElementById("planForm").addEventListener("submit", createPlan);
    document.getElementById("planQuery").addEventListener("click", loadPlans);
    document.getElementById("refreshAudits").addEventListener("click", loadAudits);
}

function applyUser() {
    const role = hasRole("ADMIN") ? "系统管理员" : hasRole("APPROVER") ? "资金审批" : "资金经办";
    const display = hasRole("ADMIN") ? "管理员" : hasRole("APPROVER") ? "审批专员" : "资金专员";
    document.getElementById("userDisplayName").textContent = display;
    document.getElementById("userRoleName").textContent = role;
    document.getElementById("dropdownUsername").textContent = state.user.username;
    document.getElementById("userAvatar").textContent = role.slice(0, 1);
    document.querySelectorAll(".admin-only").forEach(element => element.classList.toggle("hidden", !hasRole("ADMIN")));
    document.querySelectorAll(".operator-only").forEach(element => element.classList.toggle("hidden", !(hasRole("OPERATOR") || hasRole("ADMIN"))));
}

function hasRole(role) {
    return state.user?.roles?.includes(role);
}

function navigate(page) {
    if (page === "audits" && !hasRole("ADMIN")) return;
    document.querySelectorAll(".page").forEach(section => section.classList.toggle("active", section.id === `page-${page}`));
    document.querySelectorAll(".nav-item").forEach(button => button.classList.toggle("active", button.dataset.page === page));
    document.getElementById("pageTitle").textContent = pageMeta[page][0];
    document.getElementById("pageEyebrow").textContent = pageMeta[page][1];
    document.getElementById("sidebar").classList.remove("open");
    document.getElementById("mobileOverlay").classList.remove("open");
    if (page === "audits") loadAudits();
    window.scrollTo({ top: 0, behavior: "smooth" });
}

function toggleMobileMenu() {
    document.getElementById("sidebar").classList.toggle("open");
    document.getElementById("mobileOverlay").classList.toggle("open");
}

async function loadDashboard() {
    state.dashboard = await rawFetch("/api/dashboard");
    renderDashboard();
}

function renderDashboard() {
    const data = state.dashboard;
    const overview = data.overview;
    document.getElementById("dashboardUpdatedAt").textContent = formatDateTime(new Date().toISOString());
    document.getElementById("pendingNavBadge").textContent = overview.pendingPaymentCount;
    document.getElementById("alertDot").classList.toggle("hidden", data.alerts.length === 0);
    document.getElementById("metricGrid").innerHTML = `
        <article class="metric-card featured">
            <div class="metric-top"><span class="metric-label">集团资金余额</span><span class="metric-icon">¥</span></div>
            <div class="metric-value">${formatMoney(overview.totalBalance)} <small>万元</small></div>
            <div class="metric-foot"><span class="positive">● 实时</span><span>覆盖 ${overview.accountCount} 个银行账户</span></div>
        </article>
        <article class="metric-card">
            <div class="metric-top"><span class="metric-label">可用资金</span><span class="metric-icon">可</span></div>
            <div class="metric-value">${formatMoney(overview.availableBalance)} <small>万元</small></div>
            <div class="metric-foot"><span>受限及冻结资金已扣除</span></div>
        </article>
        <article class="metric-card">
            <div class="metric-top"><span class="metric-label">今日资金流出</span><span class="metric-icon">↗</span></div>
            <div class="metric-value">${formatMoney(overview.todayOutflow)} <small>万元</small></div>
            <div class="metric-foot"><span class="warning">${overview.pendingPaymentCount} 笔待审批</span></div>
        </article>
        <article class="metric-card">
            <div class="metric-top"><span class="metric-label">未来七日净流量</span><span class="metric-icon">趋</span></div>
            <div class="metric-value">${formatSignedMoney(overview.nextSevenDaysNet)} <small>万元</small></div>
            <div class="metric-foot"><span>${Number(overview.nextSevenDaysNet) >= 0 ? "预计净流入" : "关注资金缺口"}</span></div>
        </article>`;

    const maxFlow = Math.max(1, ...data.cashFlow.flatMap(day => [Number(day.inflow), Number(day.outflow)]));
    document.getElementById("cashflowChart").innerHTML = data.cashFlow.map(day => `
        <div class="flow-day">
            <div class="flow-bars">
                <i class="flow-bar inflow" style="height:${Math.max(3, Number(day.inflow) / maxFlow * 138)}px" data-value="流入 ${formatWan(day.inflow)}"></i>
                <i class="flow-bar outflow" style="height:${Math.max(3, Number(day.outflow) / maxFlow * 138)}px" data-value="流出 ${formatWan(day.outflow)}"></i>
            </div>
            <span class="flow-date">${shortDate(day.date)}</span>
        </div>`).join("");

    const bankTotal = data.balanceByBank.reduce((sum, item) => sum + Number(item.value), 0);
    document.getElementById("bankDistribution").innerHTML = `
        <div class="bank-total-row"><span>人民币账户合计</span><strong>¥ ${formatWan(bankTotal)}</strong></div>
        ${data.balanceByBank.map(item => `
            <div class="bank-row"><span class="bank-row-name">${escapeHtml(item.name)}</span><div class="bank-track"><i style="width:${bankTotal ? Number(item.value) / bankTotal * 100 : 0}%"></i></div><span class="bank-row-value">${formatWan(item.value)}</span></div>
        `).join("")}`;

    document.getElementById("recentPaymentsBody").innerHTML = data.recentPayments.slice(0, 5).map(payment => `
        <tr><td><span class="table-primary">${escapeHtml(payment.paymentNo)}</span><span class="table-secondary">${escapeHtml(payment.purpose)}</span></td><td>${escapeHtml(payment.payeeName)}</td><td class="amount">${currencySymbol(payment.currency)} ${number(payment.amount)}</td><td>${statusBadge(payment.status)}</td></tr>
    `).join("") || emptyRow(4, "暂无付款数据");

    document.getElementById("riskCount").textContent = data.alerts.length;
    document.getElementById("riskList").innerHTML = data.alerts.map(alert => `
        <div class="risk-item"><span class="risk-symbol ${alert.level}">${alert.level === "danger" ? "!" : alert.level === "warning" ? "△" : "i"}</span><div><strong>${escapeHtml(alert.title)}</strong><span>${escapeHtml(alert.description)}</span></div></div>
    `).join("") || `<div class="empty-state">当前没有需要处理的风险事项</div>`;
}

async function loadAccounts() {
    state.accounts = await rawFetch("/api/accounts");
    renderAccounts();
    renderAccountOptions();
}

function renderAccounts() {
    const keyword = document.getElementById("accountSearch").value.trim().toLowerCase();
    const status = document.getElementById("accountStatusFilter").value;
    const filtered = state.accounts.filter(account => {
        const searchable = `${account.organizationName} ${account.bankName} ${account.accountName} ${account.accountNoMasked}`.toLowerCase();
        return (!keyword || searchable.includes(keyword)) && (!status || account.status === status);
    });
    const cnyBalance = state.accounts.filter(account => account.currency === "CNY" && account.status !== "CLOSED").reduce((sum, item) => sum + Number(item.balance), 0);
    document.getElementById("accountSummary").innerHTML = `
        <div class="summary-item"><span>账户总数</span><strong>${state.accounts.length}</strong></div>
        <div class="summary-item"><span>正常运行</span><strong>${state.accounts.filter(item => item.status === "ACTIVE").length}</strong></div>
        <div class="summary-item"><span>人民币余额</span><strong>¥ ${formatWan(cnyBalance)}</strong></div>
        <div class="summary-item"><span>余额预警</span><strong>${state.accounts.filter(item => item.lowBalance).length}</strong></div>`;
    document.getElementById("accountsBody").innerHTML = filtered.map(account => `
        <tr>
            <td><div class="account-name"><span class="bank-logo">${escapeHtml(account.bankCode.slice(0, 3))}</span><div><span class="table-primary">${escapeHtml(account.accountName)}</span><span class="table-secondary">${escapeHtml(account.organizationName)}</span></div></div></td>
            <td>${escapeHtml(account.bankName)}</td>
            <td><span class="table-primary">${escapeHtml(account.accountNoMasked)}</span><span class="table-secondary">${account.currency}</span></td>
            <td><span class="amount">${currencySymbol(account.currency)} ${number(account.availableBalance)}</span>${account.lowBalance ? '<span class="low-balance-note">低于预警线</span>' : ''}</td>
            <td>${accountTypeNames[account.accountType] || account.accountType}</td>
            <td>${statusBadge(account.status)}</td>
            <td>${formatDateTime(account.lastSyncTime)}</td>
        </tr>`).join("") || emptyRow(7, "没有符合条件的银行账户");
}

function renderAccountOptions() {
    const options = state.accounts.filter(account => account.status === "ACTIVE").map(account => `
        <option value="${account.id}">${escapeHtml(account.accountName)} · ${escapeHtml(account.accountNoMasked)} · 可用 ${currencySymbol(account.currency)} ${number(account.availableBalance)}</option>`).join("");
    document.getElementById("payerAccountSelect").innerHTML = options || '<option value="">暂无可用付款账户</option>';
}

async function loadPayments() {
    state.payments = await rawFetch("/api/payments");
    renderPayments();
    renderPaymentCounts();
}

function renderPaymentCounts() {
    const count = status => state.payments.filter(item => item.status === status).length;
    document.getElementById("countAll").textContent = state.payments.length;
    document.getElementById("countDraft").textContent = count("DRAFT");
    document.getElementById("countPending").textContent = count("PENDING");
    document.getElementById("countApproved").textContent = count("APPROVED");
    document.getElementById("countPaid").textContent = count("PAID");
    document.getElementById("countRejected").textContent = count("REJECTED");
    document.getElementById("pendingNavBadge").textContent = count("PENDING");
}

function renderPayments() {
    const keyword = document.getElementById("paymentSearch").value.trim().toLowerCase();
    const filtered = state.payments.filter(payment => {
        const searchable = `${payment.paymentNo} ${payment.payeeName} ${payment.purpose}`.toLowerCase();
        return (!keyword || searchable.includes(keyword)) && (!state.paymentStatus || payment.status === state.paymentStatus);
    });
    document.getElementById("paymentsBody").innerHTML = filtered.map(payment => `
        <tr>
            <td><span class="table-primary">${escapeHtml(payment.paymentNo)} ${payment.riskFlag ? '<em class="risk-flag">风险提示</em>' : ''}</span><span class="table-secondary" title="${escapeHtml(payment.purpose)}">${escapeHtml(payment.purpose)}</span></td>
            <td><span class="table-primary">${escapeHtml(payment.payerAccountName)}</span><span class="table-secondary">${escapeHtml(payment.organizationName)}</span></td>
            <td><span class="table-primary">${escapeHtml(payment.payeeName)}</span><span class="table-secondary">${escapeHtml(payment.payeeAccountNoMasked)}</span></td>
            <td class="amount">${currencySymbol(payment.currency)} ${number(payment.amount)}</td>
            <td>${escapeHtml(payment.applicant)}</td>
            <td>${statusBadge(payment.status)}${payment.rejectReason ? `<span class="table-secondary" title="${escapeHtml(payment.rejectReason)}">${escapeHtml(payment.rejectReason)}</span>` : ""}</td>
            <td><div class="row-actions">${paymentActions(payment)}</div></td>
        </tr>`).join("") || emptyRow(7, "没有符合条件的付款单");
}

function paymentActions(payment) {
    const buttons = [];
    if (payment.status === "DRAFT" && (hasRole("ADMIN") || (hasRole("OPERATOR") && payment.applicant === state.user.username))) {
        buttons.push(`<button class="row-button primary" data-payment-action="submit" data-id="${payment.id}">提交</button>`);
    }
    if (payment.status === "PENDING" && (hasRole("APPROVER") || hasRole("ADMIN"))) {
        buttons.push(`<button class="row-button primary" data-payment-action="approve" data-id="${payment.id}">通过</button>`);
        buttons.push(`<button class="row-button danger" data-payment-action="reject" data-id="${payment.id}">驳回</button>`);
    }
    if (payment.status === "APPROVED" && hasRole("ADMIN")) {
        buttons.push(`<button class="row-button primary" data-payment-action="execute" data-id="${payment.id}">发送银行</button>`);
    }
    return buttons.join("") || '<span class="table-secondary">—</span>';
}

async function handlePaymentAction(event) {
    const button = event.target.closest("button[data-payment-action]");
    if (!button) return;
    const action = button.dataset.paymentAction;
    const id = button.dataset.id;
    const config = {
        submit: ["提交付款审批", "提交后将进入审批岗位待办，确认继续？", false],
        approve: ["审批通过付款", "通过后该付款将进入银行执行队列，确认继续？", false],
        reject: ["驳回付款申请", "请填写驳回原因，内容将记录在付款单和审计日志中。", true],
        execute: ["模拟发送银行", "系统将模拟银行支付成功并实时扣减付款账户余额。", false]
    }[action];
    const result = await askConfirm(config[0], config[1], config[2]);
    if (result === false || (config[2] && !result)) return;
    try {
        const options = { method: "POST" };
        if (action === "reject") options.body = { reason: result };
        await rawFetch(`/api/payments/${id}/${action}`, options);
        toast("操作成功", action === "execute" ? "银行已返回支付成功状态。" : "付款单状态已更新。");
        await Promise.all([loadPayments(), loadAccounts(), loadDashboard()]);
    } catch (error) {
        toast("操作失败", error.message, true);
    }
}

function openPaymentModal() {
    renderAccountOptions();
    document.getElementById("paymentModal").showModal();
}

async function createPayment(event) {
    event.preventDefault();
    if (!event.currentTarget.reportValidity()) return;
    const form = new FormData(event.currentTarget);
    const account = state.accounts.find(item => String(item.id) === form.get("payerAccountId"));
    if (!account) {
        toast("无法创建付款", "请选择可用付款账户。", true);
        return;
    }
    try {
        const payment = await rawFetch("/api/payments", {
            method: "POST",
            body: {
                payerAccountId: Number(form.get("payerAccountId")),
                payeeName: form.get("payeeName").trim(),
                payeeBankName: form.get("payeeBankName").trim(),
                payeeAccountNo: form.get("payeeAccountNo").trim(),
                amount: Number(form.get("amount")),
                currency: account.currency,
                purpose: form.get("purpose").trim()
            }
        });
        document.getElementById("paymentModal").close();
        event.currentTarget.reset();
        toast("付款草稿已保存", payment.riskFlag ? payment.riskMessage : `单号：${payment.paymentNo}`);
        await Promise.all([loadPayments(), loadDashboard()]);
        navigate("payments");
    } catch (error) {
        toast("保存失败", error.message, true);
    }
}

async function createAccount(event) {
    event.preventDefault();
    if (!event.currentTarget.reportValidity()) return;
    const form = Object.fromEntries(new FormData(event.currentTarget));
    try {
        await rawFetch("/api/accounts", {
            method: "POST",
            body: {
                ...form,
                balance: Number(form.balance),
                availableBalance: Number(form.availableBalance),
                lowBalanceThreshold: Number(form.lowBalanceThreshold)
            }
        });
        document.getElementById("accountModal").close();
        event.currentTarget.reset();
        toast("账户新增成功", "账户台账与审计日志已同步更新。");
        await Promise.all([loadAccounts(), loadDashboard()]);
    } catch (error) {
        toast("保存失败", error.message, true);
    }
}

async function loadPlans() {
    const from = document.getElementById("planFrom").value;
    const to = document.getElementById("planTo").value;
    state.plans = await rawFetch(`/api/cash-plans?from=${encodeURIComponent(from)}&to=${encodeURIComponent(to)}`);
    renderPlans();
}

function renderPlans() {
    const inflow = state.plans.filter(item => item.type === "INFLOW").reduce((sum, item) => sum + Number(item.amount), 0);
    const outflow = state.plans.filter(item => item.type === "OUTFLOW").reduce((sum, item) => sum + Number(item.amount), 0);
    document.getElementById("planOverview").innerHTML = `
        <div class="plan-stat in"><span>计划流入</span><strong>¥ ${formatWan(inflow)}</strong><small>${state.plans.filter(item => item.type === "INFLOW").length} 笔计划</small></div>
        <div class="plan-stat out"><span>计划流出</span><strong>¥ ${formatWan(outflow)}</strong><small>${state.plans.filter(item => item.type === "OUTFLOW").length} 笔计划</small></div>
        <div class="plan-stat"><span>计划净流量</span><strong>${inflow - outflow >= 0 ? "+" : "-"} ¥ ${formatWan(Math.abs(inflow - outflow))}</strong><small>${inflow - outflow >= 0 ? "预计资金净流入" : "需关注资金缺口"}</small></div>`;
    document.getElementById("plansBody").innerHTML = state.plans.map(plan => `
        <tr><td><span class="table-primary">${longDate(plan.planDate)}</span></td><td><span class="direction ${plan.type === "INFLOW" ? "in" : "out"}">${plan.type === "INFLOW" ? "资金流入" : "资金流出"}</span></td><td>${escapeHtml(plan.category)}</td><td>${escapeHtml(plan.organizationName)}</td><td class="amount">${plan.type === "INFLOW" ? "+" : "-"} ¥ ${number(plan.amount)}</td><td>${escapeHtml(plan.description || "—")}</td><td>${escapeHtml(plan.createdBy)}</td></tr>
    `).join("") || emptyRow(7, "当前日期范围内没有资金计划");
}

async function createPlan(event) {
    event.preventDefault();
    if (!event.currentTarget.reportValidity()) return;
    const form = Object.fromEntries(new FormData(event.currentTarget));
    try {
        await rawFetch("/api/cash-plans", { method: "POST", body: { ...form, amount: Number(form.amount) } });
        document.getElementById("planModal").close();
        event.currentTarget.reset();
        setDefaultPlanDates();
        toast("资金计划已保存", "七日资金预测将自动更新。");
        await Promise.all([loadPlans(), loadDashboard()]);
    } catch (error) {
        toast("保存失败", error.message, true);
    }
}

async function loadAudits() {
    if (!hasRole("ADMIN")) return;
    try {
        const audits = await rawFetch("/api/audits");
        document.getElementById("auditsBody").innerHTML = audits.map(audit => `
            <tr><td>${formatDateTime(audit.operatedAt)}</td><td><span class="table-primary">${escapeHtml(audit.username)}</span></td><td><span class="action-tag">${actionNames[audit.action] || escapeHtml(audit.action)}</span></td><td>${escapeHtml(audit.resourceType)}${audit.resourceId ? ` #${escapeHtml(audit.resourceId)}` : ""}</td><td>${escapeHtml(audit.detail)}</td><td>${escapeHtml(audit.ipAddress)}</td></tr>
        `).join("") || emptyRow(6, "暂无审计日志");
    } catch (error) {
        toast("日志加载失败", error.message, true);
    }
}

async function logout() {
    try {
        await rawFetch("/logout", { method: "POST" });
    } finally {
        location.href = "/login.html?logout";
    }
}

function askConfirm(title, message, reasonRequired) {
    const dialog = document.getElementById("confirmModal");
    document.getElementById("confirmTitle").textContent = title;
    document.getElementById("confirmMessage").textContent = message;
    document.getElementById("confirmReasonField").classList.toggle("hidden", !reasonRequired);
    document.getElementById("confirmReason").value = "";
    document.getElementById("confirmIcon").textContent = reasonRequired ? "!" : "✓";
    dialog.returnValue = "cancel";
    dialog.showModal();
    return new Promise(resolve => {
        dialog.addEventListener("close", () => {
            if (dialog.returnValue !== "default") return resolve(false);
            resolve(reasonRequired ? document.getElementById("confirmReason").value.trim() : true);
        }, { once: true });
    });
}

function toast(title, message, error = false) {
    const item = document.createElement("div");
    item.className = `toast${error ? " error" : ""}`;
    item.innerHTML = `<span>${error ? "!" : "✓"}</span><div><strong>${escapeHtml(title)}</strong><small>${escapeHtml(message)}</small></div>`;
    document.getElementById("toastStack").appendChild(item);
    setTimeout(() => item.remove(), 4200);
}

function statusBadge(status) {
    return `<span class="status status-${status}">${statusNames[status] || escapeHtml(status)}</span>`;
}

function emptyRow(columns, message) {
    return `<tr><td colspan="${columns}"><div class="empty-state">${escapeHtml(message)}</div></td></tr>`;
}

function setTodayLabels() {
    const now = new Date();
    document.getElementById("todayLabel").textContent = new Intl.DateTimeFormat("zh-CN", { year: "numeric", month: "long", day: "numeric", weekday: "short" }).format(now);
}

function setDefaultPlanDates() {
    const from = new Date();
    const to = new Date();
    to.setDate(to.getDate() + 30);
    document.getElementById("planFrom").value = toDateInput(from);
    document.getElementById("planTo").value = toDateInput(to);
    document.getElementById("newPlanDate").value = toDateInput(from);
}

function formatMoney(value) { return (Number(value) / 10000).toLocaleString("zh-CN", { minimumFractionDigits: 2, maximumFractionDigits: 2 }); }
function formatSignedMoney(value) { const numberValue = Number(value); return `${numberValue >= 0 ? "+" : "-"}${formatMoney(Math.abs(numberValue))}`; }
function formatWan(value) { return `${(Number(value) / 10000).toLocaleString("zh-CN", { maximumFractionDigits: 1 })} 万`; }
function number(value) { return Number(value).toLocaleString("zh-CN", { minimumFractionDigits: 2, maximumFractionDigits: 2 }); }
function currencySymbol(currency) { return currency === "CNY" ? "¥" : currency === "USD" ? "$" : currency === "EUR" ? "€" : currency; }
function shortDate(value) { const date = new Date(`${value}T00:00:00`); return `${date.getMonth() + 1}/${date.getDate()}`; }
function longDate(value) { return new Intl.DateTimeFormat("zh-CN", { month: "long", day: "numeric", weekday: "short" }).format(new Date(`${value}T00:00:00`)); }
function formatDateTime(value) { if (!value) return "—"; return new Intl.DateTimeFormat("zh-CN", { month: "2-digit", day: "2-digit", hour: "2-digit", minute: "2-digit", hour12: false }).format(new Date(value)); }
function toDateInput(date) { const local = new Date(date.getTime() - date.getTimezoneOffset() * 60000); return local.toISOString().slice(0, 10); }
function escapeHtml(value) { return String(value ?? "").replace(/[&<>'"]/g, char => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", "'": "&#39;", '"': "&quot;" })[char]); }
function debounce(fn, wait) { let timer; return (...args) => { clearTimeout(timer); timer = setTimeout(() => fn(...args), wait); }; }
