import { titleCase } from '../utils/format';

const STATUS_TONE = {
  SUCCESS: 'ok',
  FAILED: 'bad',
  PENDING: 'warn',
  PROCESSING: 'info',
  CANCELLED: 'muted',
  REVERSED: 'muted',
};
const RISK_TONE = { LOW: 'ok', MEDIUM: 'warn', HIGH: 'bad', BLOCKED: 'blocked' };

export const StatusBadge = ({ status }) => (
  <span className={`badge badge-${STATUS_TONE[status] || 'muted'}`}>{titleCase(status)}</span>
);

export const RiskBadge = ({ level }) =>
  level ? (
    <span className={`badge badge-${RISK_TONE[level] || 'muted'}`}>{titleCase(level)}</span>
  ) : (
    <span className="muted-text" title="Risk checks did not run for this transaction">Not checked</span>
  );
