import { Component } from 'react'

/**
 * Catches render-time errors so a single broken page shows a recoverable message
 * instead of permanently blanking the app.
 */
export class ErrorBoundary extends Component {
  constructor(props) {
    super(props)
    this.state = { hasError: false, message: '' }
  }

  static getDerivedStateFromError(error) {
    return { hasError: true, message: error?.message || 'Unexpected error' }
  }

  componentDidCatch(error, info) {
    console.error('Unhandled UI error', error, info)
  }

  render() {
    if (!this.state.hasError) {
      return this.props.children
    }

    return (
      <main className="portal-frame flex min-h-screen flex-col items-center justify-center px-6 text-center">
        <div className="max-w-md rounded-3xl border border-white/10 bg-[#0b1628] p-8">
          <p className="text-sm font-bold uppercase tracking-[0.2em] text-[#f45135]">Something broke</p>
          <h1 className="mt-4 text-2xl font-extrabold text-white">This page could not be displayed</h1>
          <p className="mt-3 text-sm leading-6 text-slate-400">{this.state.message}</p>
          <button
            type="button"
            onClick={() => this.setState({ hasError: false, message: '' })}
            className="mt-6 w-full rounded-xl bg-[#f45135] px-5 py-3 text-sm font-bold text-white transition hover:bg-[#ff694d]"
          >
            Try again
          </button>
          <a
            href="/"
            className="mt-3 block text-xs font-semibold text-slate-400 underline underline-offset-4 hover:text-white"
          >
            Back to home
          </a>
        </div>
      </main>
    )
  }
}

export default ErrorBoundary