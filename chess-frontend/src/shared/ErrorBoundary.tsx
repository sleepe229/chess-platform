import { Component, type ErrorInfo, type ReactNode } from 'react'

type Props = { children: ReactNode }
type State = { hasError: boolean; error: Error | null }

export class ErrorBoundary extends Component<Props, State> {
  state: State = { hasError: false, error: null }

  static getDerivedStateFromError(error: Error): State {
    return { hasError: true, error }
  }

  componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    console.error('ErrorBoundary caught:', error, errorInfo)
  }

  render() {
    if (this.state.hasError && this.state.error) {
      return (
        <div className="min-h-[40vh] flex items-center justify-center p-6">
          <div className="rounded-lg border border-red-800 bg-red-950/30 px-6 py-5 max-w-md">
            <div className="font-semibold text-red-300">Something went wrong</div>
            <div className="mt-2 text-sm text-slate-400 break-all">
              {this.state.error.message}
            </div>
            <button
              type="button"
              className="mt-4 text-sm text-sky-400 hover:underline"
              onClick={() => this.setState({ hasError: false, error: null })}
            >
              Try again
            </button>
          </div>
        </div>
      )
    }
    return this.props.children
  }
}

