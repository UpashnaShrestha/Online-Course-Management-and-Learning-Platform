import { BrowserRouter, Link, Navigate, Route, Routes } from 'react-router-dom'
import { useAuth } from './context/AuthContext'
import NotificationToasts from './components/NotificationToasts'
import Home from './pages/Home'
import CoursesPage from './pages/CoursesPage'
import CourseDetailPage from './pages/CourseDetailPage'
import AboutPage from './pages/AboutPage'
import ContactPage from './pages/ContactPage'
import PaymentPage from './pages/PaymentPage'
import PaymentSuccessPage from './pages/PaymentSuccessPage'
import ProfilePage from './pages/ProfilePage'
import { dashboardPathFor } from './utils/format'
import Login from './pages/auth/Login'
import Register from './pages/auth/Register'
import ForgotPassword from './pages/auth/ForgotPassword'
import ResetPassword from './pages/auth/ResetPassword'
import StudentDashboard from './pages/student/StudentDashboard'
import StudentCourses from './pages/student/StudentCourses'
import StudentProgress from './pages/student/StudentProgress'
import StudentLearning from './pages/student/StudentLearning'
import StudentCertificates from './pages/student/StudentCertificates'
import StudentNotifications from './pages/student/StudentNotifications'
import StudentPayments from './pages/student/StudentPayments'
import InstructorDashboard from './pages/instructor/InstructorDashboard'
import InstructorCourses from './pages/instructor/InstructorCourses'
import InstructorAssignments from './pages/instructor/InstructorAssignments'
import AdminDashboard from './pages/admin/AdminDashboard'
import AdminUsers from './pages/admin/AdminUsers'
import AdminCourses from './pages/admin/AdminCourses'
import AdminCategories from './pages/admin/AdminCategories'
import ScrollToTopButton from './components/ScrollToTopButton'
import AdminPayments from './pages/admin/AdminPayments'
import InstructorPublicProfile from './pages/InstructorPublicProfile'

function ProtectedRoute({ children, allowedRoles, redirectTo = '/login' }) {
  const { session, isAuthenticated } = useAuth()

  if (!isAuthenticated) {
    return <Navigate to={redirectTo} replace />
  }

  if (allowedRoles && !allowedRoles.includes(session?.role)) {
    return <Navigate to={dashboardPathFor(session?.role)} replace />
  }

  return children
}

/** Passes the signed-in role through so the profile page renders the right portal chrome. */
function RoleAwarePage({ render }) {
  const { session } = useAuth()
  return render(session?.role || 'STUDENT')
}

function NotFound() {
  return (
    <main className="flex min-h-screen items-center justify-center bg-slate-50 px-6">
      <section className="text-center">
        <h1 className="text-4xl font-bold text-slate-900">Page not found</h1>
        <Link to="/" className="mt-5 inline-block text-teal-700 underline">
          Return home
        </Link>
      </section>
    </main>
  )
}

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/courses" element={<CoursesPage />} />
        <Route path="/courses/:id" element={<CourseDetailPage />} />
        <Route path="/instructors/:id" element={<InstructorPublicProfile />} />
        <Route path="/about" element={<AboutPage />} />
        <Route path="/contact" element={<ContactPage />} />
        <Route path="/payment/success/:paymentId" element={<ProtectedRoute allowedRoles={['STUDENT']}><PaymentSuccessPage /></ProtectedRoute>} />
        <Route path="/payment/:paymentId" element={<ProtectedRoute allowedRoles={['STUDENT']}><PaymentPage /></ProtectedRoute>} />
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />
        <Route path="/forgot-password" element={<ForgotPassword />} />
        <Route path="/reset-password" element={<ResetPassword />} />

        {/* Shared across every role: the dashboard layout reads the signed-in role. */}
        <Route
          path="/profile"
          element={
            <ProtectedRoute>
              <RoleAwarePage render={(role) => <ProfilePage role={role} />} />
            </ProtectedRoute>
          }
        />

        <Route
          path="/student"
          element={
            <ProtectedRoute allowedRoles={['STUDENT']}>
              <StudentDashboard />
            </ProtectedRoute>
          }
        />
        <Route
          path="/student/courses"
          element={
            <ProtectedRoute allowedRoles={['STUDENT']}>
              <StudentCourses />
            </ProtectedRoute>
          }
        />
        <Route
          path="/student/progress"
          element={
            <ProtectedRoute allowedRoles={['STUDENT']}>
              <StudentProgress />
            </ProtectedRoute>
          }
        />
        <Route
          path="/student/courses/:courseId/learn"
          element={
            <ProtectedRoute allowedRoles={['STUDENT']}>
              <StudentLearning />
            </ProtectedRoute>
          }
        />
        <Route
          path="/student/certificates"
          element={<ProtectedRoute allowedRoles={['STUDENT']}><StudentCertificates /></ProtectedRoute>}
        />
        <Route
          path="/student/notifications"
          element={<ProtectedRoute allowedRoles={['STUDENT']}><StudentNotifications role="STUDENT" /></ProtectedRoute>}
        />
        <Route
          path="/student/payments"
          element={<ProtectedRoute allowedRoles={['STUDENT']}><StudentPayments /></ProtectedRoute>}
        />

        <Route
          path="/instructor"
          element={
            <ProtectedRoute allowedRoles={['INSTRUCTOR']}>
              <InstructorDashboard />
            </ProtectedRoute>
          }
        />
        <Route
          path="/instructor/courses"
          element={
            <ProtectedRoute allowedRoles={['INSTRUCTOR']}>
              <InstructorCourses />
            </ProtectedRoute>
          }
        />
        <Route
          path="/instructor/assignments"
          element={
            <ProtectedRoute allowedRoles={['INSTRUCTOR']}>
              <InstructorAssignments />
            </ProtectedRoute>
          }
        />
        <Route
          path="/instructor/notifications"
          element={
            <ProtectedRoute allowedRoles={['INSTRUCTOR']}>
              <StudentNotifications role="INSTRUCTOR" />
            </ProtectedRoute>
          }
        />

        <Route
          path="/admin"
          element={
            <ProtectedRoute allowedRoles={['ADMIN']}>
              <AdminDashboard />
            </ProtectedRoute>
          }
        />
        <Route
          path="/admin/users"
          element={
            <ProtectedRoute allowedRoles={['ADMIN']}>
              <AdminUsers />
            </ProtectedRoute>
          }
        />
        <Route
          path="/admin/courses"
          element={
            <ProtectedRoute allowedRoles={['ADMIN']}>
              <AdminCourses />
            </ProtectedRoute>
          }
        />
        <Route path="/admin/categories" element={<ProtectedRoute allowedRoles={['ADMIN']}><AdminCategories /></ProtectedRoute>} />
        <Route path="/admin/payments" element={<ProtectedRoute allowedRoles={['ADMIN']}><AdminPayments /></ProtectedRoute>} />
        <Route path="/admin/notifications" element={<ProtectedRoute allowedRoles={['ADMIN']}><StudentNotifications role="ADMIN" /></ProtectedRoute>} />

        <Route path="*" element={<NotFound />} />
      </Routes>

      {/* Red pop-ups for notifications that arrive while the user is browsing. */}
      <NotificationToasts />

      {/* Long pages (catalog, course detail, course builder) scroll a long way. Mounted here
          so every route gets it without each page having to add it. */}
      <ScrollToTopButton />
    </BrowserRouter>
  )
}
