import type { Config } from 'tailwindcss'

export default {
  content: ['./index.html', './src/**/*.{vue,ts}'],
  theme: {
    extend: {
      colors: {
        'mk-brand': 'rgb(var(--mk-brand) / <alpha-value>)',
        'mk-brand-deep': 'rgb(var(--mk-brand-deep) / <alpha-value>)',
        'mk-brand-ink': 'rgb(var(--mk-brand-ink) / <alpha-value>)',
        'mk-primary': 'rgb(var(--mk-primary) / <alpha-value>)',
        'mk-primary-ink': 'rgb(var(--mk-primary-ink) / <alpha-value>)',
        'mk-accent': 'rgb(var(--mk-accent) / <alpha-value>)',
        'mk-accent-ink': 'rgb(var(--mk-accent-ink) / <alpha-value>)',
        'mk-bg': 'rgb(var(--mk-bg) / <alpha-value>)',
        'mk-surface': 'rgb(var(--mk-surface) / <alpha-value>)',
        'mk-surface-2': 'rgb(var(--mk-surface-2) / <alpha-value>)',
        'mk-text': 'rgb(var(--mk-text) / <alpha-value>)',
        'mk-text-muted': 'rgb(var(--mk-text-muted) / <alpha-value>)',
        'mk-border': 'rgb(var(--mk-border) / <alpha-value>)',
        'mk-success': 'rgb(var(--mk-success) / <alpha-value>)',
        'mk-pending': 'rgb(var(--mk-pending) / <alpha-value>)',
        'mk-overdue': 'rgb(var(--mk-overdue) / <alpha-value>)',
        'mk-danger': 'rgb(var(--mk-danger) / <alpha-value>)',
        'mk-info': 'rgb(var(--mk-info) / <alpha-value>)',
      },
      fontFamily: {
        sans: ['"Plus Jakarta Sans"', 'ui-sans-serif', 'system-ui', 'sans-serif'],
      },
      borderRadius: {
        mk: '10px',
      },
      boxShadow: {
        mk: '0 1px 2px rgba(15, 23, 42, 0.04), 0 1px 1px rgba(15, 23, 42, 0.03)',
      },
    },
  },
  plugins: [],
} satisfies Config
