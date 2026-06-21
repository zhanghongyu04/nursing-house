// 数字补零，统一输出两位时间片段。
const padNumber = (value: number) => String(value).padStart(2, '0');

// 按系统统一格式拼接日期时间文本。
const buildDateTimeText = (date: Date, withSeconds = true) => {
  const secondText = withSeconds ? `:${padNumber(date.getSeconds())}` : '';
  return `${date.getFullYear()}-${padNumber(date.getMonth() + 1)}-${padNumber(date.getDate())} ${padNumber(date.getHours())}:${padNumber(date.getMinutes())}${secondText}`;
};

// 兼容 Date、ISO 字符串和普通文本时间，统一转为 YYYY-MM-DD HH:mm:ss。
export const normalizeDateTimeText = (value?: string | Date | null, withSeconds = true) => {
  if (value == null || value === '') return '';
  if (value instanceof Date) {
    return Number.isNaN(value.getTime()) ? '' : buildDateTimeText(value, withSeconds);
  }

  const raw = String(value).trim();
  if (!raw) return '';

  const normalized = raw
    .replace('T', ' ')
    .replace(/\.\d+$/, '')
    .replace(/Z$/, '')
    .trim();

  const matched = normalized.match(/^(\d{4}-\d{2}-\d{2}) (\d{2}):(\d{2})(?::(\d{2}))?$/);
  if (matched) {
    const [, datePart, hourPart, minutePart, secondPart] = matched;
    return `${datePart} ${hourPart}:${minutePart}${withSeconds ? `:${secondPart || '00'}` : ''}`;
  }

  const parsed = new Date(raw);
  if (Number.isNaN(parsed.getTime())) {
    return normalized;
  }
  return buildDateTimeText(parsed, withSeconds);
};

// 供页面展示使用，转换失败时回退到占位符。
export const formatDisplayDateTime = (
  value?: string | Date | null,
  fallback = '-',
  withSeconds = true
) => {
  const normalized = normalizeDateTimeText(value, withSeconds);
  return normalized || fallback;
};
