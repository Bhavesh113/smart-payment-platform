import { useAuth } from '../context/AuthContext';
import { dateTime, titleCase } from '../utils/format';

export default function Profile() {
  const { user } = useAuth();
  return (
    <>
      <header className="page-head"><h1>Profile</h1></header>
      <section className="card narrow">
        <dl className="detail-list">
          <div><dt>Name</dt><dd>{user.name}</dd></div>
          <div><dt>Email</dt><dd>{user.email}</dd></div>
          <div><dt>Phone</dt><dd>{user.phone}</dd></div>
          <div><dt>Role</dt><dd>{titleCase(user.role)}</dd></div>
          <div><dt>Account status</dt><dd>{titleCase(user.status)}</dd></div>
          <div><dt>Member since</dt><dd>{dateTime(user.createdAt)}</dd></div>
        </dl>
      </section>
    </>
  );
}
