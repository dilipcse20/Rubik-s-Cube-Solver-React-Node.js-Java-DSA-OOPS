// Top bar: project title, subtitle and the backend connection light.
function Header({ isOnline }) {
  const statusText = isOnline ? 'Backend connected' : 'Backend offline';
  const statusClass = isOnline ? 'status-dot status-online' : 'status-dot status-offline';

  return (
    <header className="app-header">
      <div>
        <h1>Rubik&apos;s Cube Solver</h1>
        <p className="subtitle">Explore Search Algorithms</p>
      </div>

      <div className="connection-indicator" role="status">
        <span className={statusClass} />
        <span>{statusText}</span>
      </div>
    </header>
  );
}

export default Header;
