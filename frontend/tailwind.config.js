/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,jsx}'],
  theme: {
    extend: {
      colors: {
        brand: {
          DEFAULT: '#2563eb',
          hover: '#1d4ed8',
          navy: '#0f172a',
        },
        student: {
          DEFAULT: '#10b981',
        },
        instructor: {
          DEFAULT: '#8b5cf6',
        },
        admin: {
          DEFAULT: '#f43f5e',
        },
        surface: {
          DEFAULT: '#f8fafc',
        },
      },
      boxShadow: {
        card: '0 1px 3px 0 rgb(15 23 42 / 0.08), 0 8px 24px -12px rgb(15 23 42 / 0.18)',
      },
    },
  },
  plugins: [],
}