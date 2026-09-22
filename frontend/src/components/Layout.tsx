import { NavLink, Outlet } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

export function Layout() {
  const { user, logout } = useAuth();

  return (
    <div className="app-shell">
      <header className="topbar">
        <div className="brand">CreatorOS</div>
        <nav className="nav-links">
          <NavLink to="/" end>
            Dashboard
          </NavLink>
          <NavLink to="/content">Content</NavLink>
          <NavLink to="/ai">AI Analyst</NavLink>
        </nav>
        <div className="topbar-right">
          <span className="user-email">{user?.email}</span>
          <button className="btn btn-ghost" onClick={logout}>
            Sign out
          </button>
        </div>
      </header>
      <main className="content-area">
        <Outlet />
      </main>
    </div>
  );
}
