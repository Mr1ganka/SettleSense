import { Link } from 'react-router-dom';

export function NotFoundPage() {
  return (
    <section className="workspace">
      <div className="panel">
        <h2>Page not found</h2>
        <Link to="/">Return home</Link>
      </div>
    </section>
  );
}
