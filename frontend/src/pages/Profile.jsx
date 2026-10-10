import { useAuth } from '../context/AuthContext';
import { dateTime, titleCase } from '../utils/format';

function ProfileIcon({ type }) {
  const paths = {
    user: (
      <>
        <circle cx="12" cy="8" r="3.5" />
        <path d="M5 20v-1.5a7 7 0 0 1 14 0V20Z" />
      </>
    ),
    mail: (
      <>
        <rect x="3" y="5" width="18" height="14" rx="2.5" />
        <path d="m4 7 8 6 8-6" />
      </>
    ),
    phone: (
      <path d="M7 3.5h10a1.5 1.5 0 0 1 1.5 1.5v14a1.5 1.5 0 0 1-1.5 1.5H7A1.5 1.5 0 0 1 5.5 19V5A1.5 1.5 0 0 1 7 3.5ZM10 17.5h4" />
    ),
    shield: (
      <>
        <path d="M12 3 20 6v5.5c0 4.7-3.4 8.5-8 9.5-4.6-1-8-4.8-8-9.5V6Z" />
        <path d="m9 12 2 2 4-4" />
      </>
    ),
    calendar: (
      <>
        <rect x="3.5" y="5" width="17" height="15" rx="2" />
        <path d="M7.5 3v4M16.5 3v4M3.5 10h17" />
      </>
    ),
  };

  return (
    <svg
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.7"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      {paths[type]}
    </svg>
  );
}

export default function Profile() {
  const { user } = useAuth();

  const name = user?.name || 'SmartPay User';
  const initial = name.trim().charAt(0).toUpperCase() || 'U';

  const fields = [
    {
      label: 'Full name',
      value: user?.name,
      icon: 'user',
    },
    {
      label: 'Email address',
      value: user?.email,
      icon: 'mail',
    },
    {
      label: 'Phone number',
      value: user?.phone,
      icon: 'phone',
    },
    {
      label: 'Account role',
      value: user?.role ? titleCase(user.role) : null,
      icon: 'shield',
    },
    {
      label: 'Account status',
      value: user?.status ? titleCase(user.status) : null,
      icon: 'shield',
    },
    {
      label: 'Member since',
      value: user?.createdAt ? dateTime(user.createdAt) : null,
      icon: 'calendar',
    },
  ];

  return (
    <div className="profile-page">
      <header className="page-head profile-heading">
        <div>
          <span className="profile-eyebrow">
            <span className="profile-eyebrow-dot" />
            ACCOUNT MANAGEMENT
          </span>
          <h1>My Profile</h1>
          <p>View your personal information and account details.</p>
        </div>
      </header>

      <section className="profile-layout">
        <div className="card profile-summary-card">
          <div className="profile-cover">
            <div className="profile-cover-decoration" />
          </div>

          <div className="profile-avatar" aria-label={`Avatar for ${name}`}>
            {initial}
          </div>

          <div className="profile-summary-content">
            <h2>{name}</h2>
            <p className="profile-summary-email">
              {user?.email || 'Email not available'}
            </p>

            {user?.role && (
              <span className="profile-role-badge">
                <ProfileIcon type="shield" />
                {titleCase(user.role)}
              </span>
            )}

            <div className="profile-summary-divider" />

            <div className="profile-summary-detail">
              <span className="profile-summary-detail-icon">
                <ProfileIcon type="calendar" />
              </span>
              <div>
                <span>Member since</span>
                <strong>
                  {user?.createdAt ? dateTime(user.createdAt) : 'Not available'}
                </strong>
              </div>
            </div>
          </div>
        </div>

        <div className="card profile-information-card">
          <div className="profile-information-heading">
            <div>
              <h2>Personal information</h2>
              <p>Your registered SmartPay account details.</p>
            </div>

            <span className="profile-readonly-badge">Read only</span>
          </div>

          <div className="profile-information-grid">
            {fields.map((field) => (
              <div className="profile-information-item" key={field.label}>
                <span className="profile-information-icon">
                  <ProfileIcon type={field.icon} />
                </span>

                <div className="profile-information-text">
                  <span className="profile-information-label">
                    {field.label}
                  </span>
                  <strong>
                    {field.value || 'Not provided'}
                  </strong>
                </div>
              </div>
            ))}
          </div>

          <div className="profile-security-note">
            <span className="profile-security-icon">
              <ProfileIcon type="shield" />
            </span>

            <div>
              <strong>Keep your account information safe</strong>
              <p>
                Never share your password or sensitive account credentials
                with anyone.
              </p>
            </div>
          </div>
        </div>
      </section>
    </div>
  );
}