import { Link, NavLink } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { dashboardPathFor } from '../utils/format'
import NotificationBell from './NotificationBell'

export default function SiteNavbar() {
  const { isAuthenticated, session } = useAuth()
  const dashboardPath = dashboardPathFor(session?.role)
  const role = session?.role || 'STUDENT'
  const notificationsPath = `/${role.toLowerCase()}/notifications`
  const displayName = session?.firstName || session?.username || 'Account'

  return (
    <header className="sticky top-0 z-50 border-b border-white/10 bg-[#050b14]/95 px-6 py-4 text-white backdrop-blur">
      <nav className="mx-auto flex max-w-7xl items-center justify-between gap-5">
        <Link to="/" className="flex items-center gap-2 text-xl font-extrabold tracking-tight text-white">
          <span className="text-[#f45135]">◆</span>CourseFlow
        </Link>

        <div className="flex items-center gap-2 text-sm font-semibold">
          <div className="hidden items-center gap-2 md:flex">
            <NavLink to="/" end className={({ isActive }) => `rounded-full px-4 py-2 transition ${isActive ? 'bg-white/10 text-white' : 'text-slate-300 hover:bg-white/10 hover:text-white'}`}>Home</NavLink>
            <NavLink to="/courses" className={({ isActive }) => `rounded-full px-4 py-2 transition ${isActive ? 'bg-white/10 text-white' : 'text-slate-300 hover:bg-white/10 hover:text-white'}`}>Courses</NavLink>
            <NavLink to="/about" className={({ isActive }) => `rounded-full px-4 py-2 transition ${isActive ? 'bg-white/10 text-white' : 'text-slate-300 hover:bg-white/10 hover:text-white'}`}>About Us</NavLink>
            <NavLink to="/contact" className={({ isActive }) => `rounded-full px-4 py-2 transition ${isActive ? 'bg-white/10 text-white' : 'text-slate-300 hover:bg-white/10 hover:text-white'}`}>Contact Us</NavLink>
          </div>

          {isAuthenticated && (
            <Link
              to={notificationsPath}
              className="rounded-lg p-2.5 text-slate-200 transition hover:bg-white/10 md:hidden"
              aria-label="Notifications"
            >
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                <path d="M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9" />
                <path d="M10.3 21a1.94 1.94 0 0 0 3.4 0" />
              </svg>
            </Link>
          )}

          {isAuthenticated && (
            <div className="hidden md:block">
              <NotificationBell role={role} />
            </div>
          )}

          {isAuthenticated ? (
            <Link to={dashboardPath} className="coral-action px-4 py-2">
              {displayName}
            </Link>
          ) : (
            <Link to="/login" className="coral-action px-4 py-2">Login</Link>
          )}
        </div>
      </nav>
    </header>
  )
}