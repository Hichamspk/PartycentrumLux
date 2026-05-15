/** @type {import('tailwindcss').Config} */
module.exports = {
  darkMode: 'class',
  content: ['./src/**/*.{html,ts}'],
  theme: {
    extend: {
      colors: {
        lux: {
          ink: '#111111',
          muted: '#666666',
          subtle: '#999999',
          line: '#E5E5E5',
          page: '#F5F5F5',
          sidebar: '#F7F7F7',
          gold: '#DCAB46',
          success: '#22C55E',
          warning: '#F59E0B',
          danger: '#EF4444'
        }
      },
      boxShadow: {
        soft: '0 1px 3px rgba(0, 0, 0, 0.06)',
        hover: '0 4px 12px rgba(0, 0, 0, 0.08)'
      }
    }
  },
  plugins: []
};
