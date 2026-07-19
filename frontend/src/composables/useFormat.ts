const numberFormatter = new Intl.NumberFormat('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })

export function useFormat() {
  const number = (value: number) => numberFormatter.format(value)
  const wan = (value: number) => `${(value / 10_000).toLocaleString('zh-CN', { maximumFractionDigits: 1 })} 万`
  const moneyWan = (value: number) => (value / 10_000).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
  const symbol = (currency: string) => currency === 'CNY' ? '¥' : currency === 'USD' ? '$' : currency === 'EUR' ? '€' : currency
  const dateTime = (value?: string) => value
    ? new Intl.DateTimeFormat('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', hour12: false }).format(new Date(value))
    : '—'
  const shortDate = (value: string) => {
    const date = new Date(`${value}T00:00:00`)
    return `${date.getMonth() + 1}/${date.getDate()}`
  }
  const longDate = (value: string) => new Intl.DateTimeFormat('zh-CN', { month: 'long', day: 'numeric', weekday: 'short' }).format(new Date(`${value}T00:00:00`))
  const dateInput = (date: Date) => new Date(date.getTime() - date.getTimezoneOffset() * 60_000).toISOString().slice(0, 10)

  return { number, wan, moneyWan, symbol, dateTime, shortDate, longDate, dateInput }
}
