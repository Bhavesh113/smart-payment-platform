export const money = (value, currency = 'INR') =>
  new Intl.NumberFormat('en-IN', { style: 'currency', currency, maximumFractionDigits: 2 }).format(Number(value ?? 0));

export const compactMoney = (value) =>
  new Intl.NumberFormat('en-IN', { notation: 'compact', maximumFractionDigits: 1 }).format(Number(value ?? 0));

export const dateTime = (iso) =>
  iso
    ? new Intl.DateTimeFormat('en-IN', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(iso))
    : '-';

export const shortDate = (iso) =>
  new Intl.DateTimeFormat('en-IN', { day: '2-digit', month: 'short' }).format(new Date(iso));

export const titleCase = (s = '') => s.charAt(0) + s.slice(1).toLowerCase().replaceAll('_', ' ');

/** crypto.randomUUID needs a secure context (https/localhost); fall back for plain-http deployments. */
export function newIdempotencyKey(prefix = 'TXN') {
  const id =
    globalThis.crypto?.randomUUID?.() ||
    `${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 12)}`;
  return `${prefix}-${id}`;
}

/**
 * Keeps the same Idempotency-Key while the user is retrying the *same* payload (e.g. after a network timeout),
 * and mints a new one as soon as the payload changes or the server has given a definite answer.
 */
export function idempotencyKeeper(prefix) {
  let current = null;
  return {
    keyFor(payload) {
      const fingerprint = JSON.stringify(payload);
      if (!current || current.fingerprint !== fingerprint) current = { key: newIdempotencyKey(prefix), fingerprint };
      return current.key;
    },
    reset() {
      current = null;
    },
  };
}
