/** @type {import('tailwindcss').Config} */
module.exports = {
  darkMode: 'class',
  content: ['./src/**/*.{html,ts}'],
  theme: {
    extend: {
      colors: {
        lux: {
          ink: '#111827',
          muted: '#64748b',
          line: '#e5e7eb',
          blue: '#2563eb',
          mint: '#0f9f6e',
          rose: '#e11d48'
        }
      },
      boxShadow: {
        soft: '0 16px 50px rgba(15, 23, 42, 0.08)'
      }
    }
  },
  plugins: []
};
