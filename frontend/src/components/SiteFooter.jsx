import { Link } from 'react-router-dom'

export default function SiteFooter() {
  return (
    <footer className="border-t border-white/10 bg-[#050b14] px-6 py-8 text-slate-400">
      <div className="mx-auto flex max-w-7xl flex-col gap-3 text-sm sm:flex-row sm:items-center sm:justify-between">
        <p className="font-semibold text-white">◆ CourseFlow learning platform</p>
        <div className="flex gap-4">
          <Link to="/" className="hover:text-teal-700">Home</Link>
          <Link to="/courses" className="hover:text-teal-700">Courses</Link>
        </div>
      </div>
    </footer>
  )
}
