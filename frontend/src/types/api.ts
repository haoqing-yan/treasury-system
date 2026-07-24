export type Role = 'ADMIN' | 'OPERATOR' | 'APPROVER'
export type Permission =
  | 'account:manage'
  | 'payment:create'
  | 'payment:submit'
  | 'payment:approve'
  | 'payment:execute'
  | 'payment:batch'
  | 'reconciliation:handle'
  | 'exception:handle'
  | 'cash-plan:create'
  | 'audit:read'
export type PaymentStatus = 'DRAFT' | 'PENDING' | 'APPROVED' | 'QUEUED' | 'PROCESSING' | 'PAID' | 'FAILED' | 'REJECTED'
export type PaymentBatchStatus = 'READY' | 'PROCESSING' | 'COMPLETED' | 'PARTIAL_FAILED' | 'FAILED'
export type PaymentBatchItemStatus = 'READY' | 'PROCESSING' | 'SUCCESS' | 'FAILED'
export type AccountStatus = 'ACTIVE' | 'RESTRICTED' | 'FROZEN' | 'CLOSED'
export type AccountChannel = 'BANK' | 'ALIPAY' | 'WECHAT'
export type AccountType = 'BASIC' | 'GENERAL' | 'SPECIAL' | 'SETTLEMENT' | 'CASH_POOL' | 'PAYMENT_PLATFORM'
export type CashPlanType = 'INFLOW' | 'OUTFLOW'
export type TransactionDirection = 'INFLOW' | 'OUTFLOW'
export type ReconciliationStatus = 'UNMATCHED' | 'MATCHED' | 'EXCEPTION'
export type ExceptionCategory = 'BUSINESS' | 'SYSTEM'
export type ExceptionCaseType = 'PAYMENT_RISK' | 'RECONCILIATION' | 'ACCOUNT_BALANCE' | 'SYSTEM_INTEGRATION' | 'SYSTEM_JOB' | 'SYSTEM_RUNTIME'
export type ExceptionSeverity = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL'
export type ExceptionStatus = 'OPEN' | 'PROCESSING' | 'RESOLVED'

export interface CurrentUser {
  username: string
  roles: Role[]
  permissions: Permission[]
}

export interface CsrfToken {
  headerName: string
  parameterName: string
  token: string
}

export interface BankAccount {
  id: number
  channel: AccountChannel
  organizationName: string
  bankName: string
  bankCode: string
  accountName: string
  accountNoMasked: string
  currency: string
  balance: number
  availableBalance: number
  lowBalanceThreshold: number
  accountType: AccountType
  status: AccountStatus
  lowBalance: boolean
  lastSyncTime: string
}

export interface Payment {
  id: number
  paymentNo: string
  payerAccountId: number
  payerChannel: AccountChannel
  payerAccountName: string
  payerAccountNoMasked: string
  organizationName: string
  payeeName: string
  payeeBankName: string
  payeeAccountNoMasked: string
  amount: number
  currency: string
  purpose: string
  status: PaymentStatus
  applicant: string
  approver?: string
  rejectReason?: string
  riskFlag: boolean
  riskMessage?: string
  createdAt: string
  submittedAt?: string
  approvedAt?: string
  paidAt?: string
}

export interface PaymentBatchItem {
  id: number
  paymentId: number
  paymentNo: string
  payerAccountName: string
  payeeName: string
  amount: number
  currency: string
  status: PaymentBatchItemStatus
  requestId: string
  failureReason?: string
}

export interface PaymentBatch {
  id: number
  batchNo: string
  scheduledAt: string
  status: PaymentBatchStatus
  totalAmount: number
  totalCount: number
  successCount: number
  failedCount: number
  createdBy: string
  createdAt: string
  startedAt?: string
  completedAt?: string
  items: PaymentBatchItem[]
}

export interface CashPlan {
  id: number
  planDate: string
  type: CashPlanType
  category: string
  amount: number
  organizationName: string
  description?: string
  createdBy: string
  createdAt: string
}

export interface AuditLog {
  id: number
  username: string
  action: string
  resourceType: string
  resourceId?: string
  detail: string
  ipAddress: string
  operatedAt: string
}

export interface BankTransaction {
  id: number
  transactionNo: string
  bankAccountId: number
  channel: AccountChannel
  bankAccountName: string
  bankAccountNoMasked: string
  transactionTime: string
  direction: TransactionDirection
  counterpartyName: string
  counterpartyAccountNoMasked: string
  amount: number
  currency: string
  balanceAfter: number
  purpose: string
  reconciliationStatus: ReconciliationStatus
  matchedPaymentId?: number
  matchedPaymentNo?: string
  matchMethod?: string
  matchMessage?: string
  matchedAt?: string
}

export interface ReconciliationSummary {
  totalCount: number
  matchedCount: number
  unmatchedCount: number
  exceptionCount: number
  matchedAmount: number
  unmatchedAmount: number
}

export interface ExceptionCase {
  id: number
  caseNo: string
  category: ExceptionCategory
  type: ExceptionCaseType
  severity: ExceptionSeverity
  status: ExceptionStatus
  title: string
  description: string
  sourceType: string
  sourceId: string
  sourceReference: string
  assignee?: string
  resolution?: string
  detectedAt: string
  claimedAt?: string
  resolvedAt?: string
}

export interface ExceptionSummary {
  totalCount: number
  businessCount: number
  systemCount: number
  openCount: number
  processingCount: number
  resolvedCount: number
  highPriorityCount: number
}

export interface DashboardOverview {
  totalBalance: number
  availableBalance: number
  accountCount: number
  todayOutflow: number
  pendingPaymentCount: number
  nextSevenDaysNet: number
}

export interface DashboardData {
  overview: DashboardOverview
  balanceByBank: Array<{ name: string; value: number }>
  cashFlow: Array<{ date: string; inflow: number; outflow: number }>
  recentPayments: Payment[]
  alerts: Array<{ level: 'warning' | 'danger' | 'info'; title: string; description: string }>
}

export interface ApiErrorBody {
  message: string
  fields?: Record<string, string>
}
