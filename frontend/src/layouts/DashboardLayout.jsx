import { useEffect, useState } from 'react'
import { Link, NavLink, useLocation, useNavigate } from 'react-router-dom'
import {
  Award,
  Bell,
  BookOpen,
  ChevronDown,
  CreditCard,
  FolderTree,
  GraduationCap,
  KeyRound,
  LayoutDashboard,
  LogOut,
  Menu,
  Receipt,
  Settings,
  TrendingUp,
  UserCog,
  Users,
  X,
} from 'lucide-react'
import { useAuth } from '../context/AuthContext'
import SiteFooter from '../components/SiteFooter'
import NotificationBell from '../components/NotificationBell'
import { useIsDesktop } from '../hooks/useMediaQuery'
import { initialsOf } from '../utils/format'

const navItems = {
  STUDENT: [
    { label: 'Overview', to: '/student', icon: LayoutDashboard },
    { label: 'My Courses', to: '/student/courses', icon: BookOpen },
    { label: 'Progress', to: '/student/progress', icon: TrendingUp },
    { label: 'Certificates', to: '/student/certificates', icon: Award },
    { label: 'Notifications', to: '/student/notifications', icon: Bell },
    { label: 'Payments', to: '/student/payments', icon: Receipt },
    { label: 'Profile', to: '/profile', icon: UserCog },
  ],
  INSTRUCTOR: [
    { label: 'Overview', to: '/instructor', icon: LayoutDashboard },
    { label: 'Courses', to: '/instructor/courses', icon: BookOpen },
    { label: 'Assignments', to: '/instructor/assignments', icon: GraduationCap },
    { label: 'Notifications', to: '/instructor/notifications', icon: Bell },
    { label: 'Profile', to: '/profile', icon: UserCog },
  ],
  ADMIN: [
    { label: 'Overview', to: '/admin', icon: LayoutDashboard },
    { label: 'Users', to: '/admin/users', icon: Users },
    { label: 'Courses', to: '/admin/courses', icon: BookOpen },
    { label: 'Categories', to: '/admin/categories', icon: FolderTree },
    { label: 'Payments', to: '/admin/payments', icon: CreditCard },
    { label: 'Notifications', to: '/admin/notifications', icon: Bell },
    { label: 'Profile', to: '/profile', icon: UserCog },
  ],
}

export function DashboardLayout({ title, subtitle, children, role = 'STUDENT' }) {
  const { session, logout } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()

  // Desktop: rail vs full sidebar. Mobile: off-canvas drawer.
  const [collapsed, setCollapsed] = useState(false)
  const [mobileOpen, setMobileOpen] = useState(false)
  const [menuOpen, setMenuOpen] = useState(true)
  const [settingsOpen, setSettingsOpen] = useState(false)

  const links = navItems[role] || navItems.STUDENT
  // Reactive: resizing across the lg breakpoint switches between the icon rail
  // and the off-canvas drawer without needing an unrelated re-render.
  const isDesktop = useIsDesktop()

  const navigationExpanded = isDesktop ? !collapsed : mobileOpen

  // Any navigation closes the mobile drawer.
  useEffect(() => {
    setMobileOpen(false)
  }, [location.pathname])

  // Lock body scroll while the mobile drawer covers the page.
  useEffect(() => {
    document.body.style.overflow = mobileOpen ? 'hidden' : ''
    return () => {
      document.body.style.overflow = ''
    }
  }, [mobileOpen])

  const handleLogout = () => {
    logout()
    navigate('/login', { replace: true })
  }

  const sidebarWidth = collapsed ? 'lg:w-20' : 'lg:w-72'

  return (
    <main className="flex min-h-screen flex-col bg-[#050b14] text-slate-800">
      {/* Top bar (mobile + desktop) */}
      <header className="sticky top-0 z-40 border-b border-white/10 bg-[#0b1628] px-4 py-3 text-white">
        <div className="flex items-center gap-3">
          <button
            type="button"
            onClick={() => {
              if (isDesktop) {
                setCollapsed((current) => !current)
              } else {
                setMobileOpen((current) => !current)
              }
            }}
            aria-label={navigationExpanded ? 'Collapse navigation' : 'Expand navigation'}
            aria-expanded={navigationExpanded}
            className="rounded-xl border border-white/15 p-2 transition hover:bg-white/10"
          >
            {mobileOpen ? (
              <X size={18} aria-hidden="true" />
            ) : (
              <Menu size={18} aria-hidden="true" />
            )}
          </button>

          <div className="min-w-0 flex-1">
            <p className="truncate text-xs font-semibold uppercase tracking-[0.25em] text-[#f45135]">
              CourseFlow
            </p>
          </div>

          <NotificationBell role={role} />
        </div>
      </header>

      <div className="mx-auto flex w-full max-w-[100rem] flex-1 gap-6 px-4 py-6 lg:flex-row">
        {/* Mobile drawer scrim */}
        {mobileOpen && (
          <button
            type="button"
            aria-label="Close navigation"
            onClick={() => setMobileOpen(false)}
            className="fixed inset-0 z-40 bg-black/60 lg:hidden"
          />
        )}

        {/* Sidebar */}
        <aside
          className={[
            'fixed inset-y-0 left-0 z-50 flex w-72 flex-col overflow-y-auto border-r border-white/10 bg-[#0b1628] p-5 text-slate-50 transition-transform lg:sticky lg:top-[4.5rem] lg:z-0 lg:h-[calc(100vh-6rem)] lg:translate-x-0 lg:rounded-3xl lg:border',
            sidebarWidth,
            mobileOpen ? 'translate-x-0' : '-translate-x-full',
          ].join(' ')}
        >
          {/* Menu section */}
          <nav className="flex-1">
            <button
              type="button"
              onClick={() => setMenuOpen((current) => !current)}
              aria-expanded={menuOpen}
              className={`flex w-full items-center gap-2 rounded-xl px-3 py-2 text-xs font-bold uppercase tracking-[0.18em] text-slate-400 transition hover:bg-white/5 ${
                collapsed ? 'lg:justify-center' : 'justify-between'
              }`}
            >
              <span className={collapsed ? 'lg:hidden' : ''}>Menu</span>
              <ChevronDown
                size={15}
                aria-hidden="true"
                className={`transition-transform ${menuOpen ? '' : '-rotate-90'} ${
                  collapsed ? 'lg:hidden' : ''
                }`}
              />
            </button>

            {menuOpen && (
              <ul className="mt-2 space-y-1.5">
                {links.map((item) => {
                  const Icon = item.icon

                  return (
                    <li key={item.to}>
                      <NavLink
                        to={item.to}
                        end={item.to === `/${role.toLowerCase()}` || item.to === '/student' || item.to === '/instructor' || item.to === '/admin'}
                        title={collapsed ? item.label : undefined}
                        className={({ isActive }) =>
                          [
                            'flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-medium transition',
                            collapsed ? 'lg:justify-center lg:px-2' : '',
                            isActive
                              ? 'bg-[#263f7a] text-white'
                              : 'text-slate-300 hover:bg-white/10 hover:text-white',
                          ].join(' ')
                        }
                      >
                        <Icon size={18} aria-hidden="true" className="shrink-0" />
                        <span className={collapsed ? 'lg:hidden' : ''}>{item.label}</span>
                      </NavLink>
                    </li>
                  )
                })}
              </ul>
            )}
          </nav>

          {/* Settings / account section */}
          <div className="mt-6 border-t border-white/10 pt-4">
            <button
              type="button"
              onClick={() => setSettingsOpen((current) => !current)}
              aria-expanded={settingsOpen}
              className={`flex w-full items-center gap-2 rounded-xl px-3 py-2 text-xs font-bold uppercase tracking-[0.18em] text-slate-400 transition hover:bg-white/5 ${
                collapsed ? 'lg:justify-center' : 'justify-between'
              }`}
            >
              <span className={`flex items-center gap-2 ${collapsed ? 'lg:hidden' : ''}`}>
                <Settings size={14} aria-hidden="true" />
                Settings
              </span>
              <ChevronDown
                size={15}
                aria-hidden="true"
                className={`transition-transform ${settingsOpen ? '' : '-rotate-90'} ${
                  collapsed ? 'lg:hidden' : ''
                }`}
              />
            </button>

            {settingsOpen && (
              <div className="mt-2 space-y-2">
                {/* The account card doubles as the "Profile" link already present in the
                    Menu section above, so Settings only lists the destinations that are
                    NOT reachable from Menu (deep link to the password tab, the public
                    catalog). */}
                <Link
                  to="/profile"
                  title={collapsed ? 'Account' : undefined}
                  className={`flex items-center gap-3 rounded-2xl bg-[#182b4d] p-3 transition hover:bg-[#1f3866] ${
                    collapsed ? 'lg:justify-center' : ''
                  }`}
                >
                  <span
                    aria-hidden="true"
                    className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-[#fff5d6] text-xs font-extrabold text-[#0f172a]"
                  >
                    {initialsOf(`${session?.firstName || ''} ${session?.lastName || ''}`.trim()
                      || session?.username
                      || 'U')}
                  </span>
                  <div className={`min-w-0 ${collapsed ? 'lg:hidden' : ''}`}>
                    <p className="truncate text-sm font-semibold text-white">
                      {session?.firstName} {session?.lastName}
                    </p>
                    <p className="truncate text-xs text-slate-400">{session?.email}</p>
                  </div>
                </Link>

                <Link
                  to="/profile?tab=password"
                  title={collapsed ? 'Change password' : undefined}
                  className={`flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-medium text-slate-300 transition hover:bg-white/10 hover:text-white ${
                    collapsed ? 'lg:justify-center lg:px-2' : ''
                  }`}
                >
                  <KeyRound size={18} aria-hidden="true" className="shrink-0" />
                  <span className={collapsed ? 'lg:hidden' : ''}>Change password</span>
                </Link>

                <Link
                  to="/courses"
                  title={collapsed ? 'Course catalog' : undefined}
                  className={`flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-medium text-slate-300 transition hover:bg-white/10 hover:text-white ${
                    collapsed ? 'lg:justify-center lg:px-2' : ''
                  }`}
                >
                  <BookOpen size={18} aria-hidden="true" className="shrink-0" />
                  <span className={collapsed ? 'lg:hidden' : ''}>Course catalog</span>
                </Link>
              </div>
            )}
          </div>

          <button
            type="button"
            onClick={handleLogout}
            title={collapsed ? 'Log out' : undefined}
            className={`mt-4 flex items-center gap-3 rounded-xl border border-white/20 px-3 py-2.5 text-sm font-semibold text-slate-100 transition hover:bg-white/10 ${
              collapsed ? 'justify-center lg:px-2' : ''
            }`}
          >
            <LogOut size={18} aria-hidden="true" className="shrink-0" />
            <span className={collapsed ? 'lg:hidden' : ''}>Log out</span>
          </button>
        </aside>

        {/* Content */}
        <section className="surface-cream min-w-0 flex-1 rounded-3xl p-6 shadow-2xl shadow-black/20 md:p-8">
          <div className="mb-6 flex flex-wrap items-start justify-between gap-4">
            <div>
              <p className="text-sm font-semibold uppercase tracking-[0.2em] text-[#f45135]">
                {role} portal
              </p>
              <h1 className="mt-2 text-3xl font-bold text-slate-900">{title}</h1>
              {subtitle && <p className="mt-2 text-slate-600">{subtitle}</p>}
            </div>

            <Link
              to="/"
              className="rounded-lg bg-[#0f172a] px-4 py-2 text-sm font-semibold text-white hover:bg-[#263f7a]"
            >
              Home
            </Link>
          </div>

          {children}
        </section>
      </div>

      <SiteFooter />
    </main>
  )
}