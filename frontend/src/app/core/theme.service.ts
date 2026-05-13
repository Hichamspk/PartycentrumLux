import { Injectable, signal } from '@angular/core';

const THEME_KEY = 'lux_theme';

@Injectable({ providedIn: 'root' })
export class ThemeService {
  readonly darkMode = signal(localStorage.getItem(THEME_KEY) === 'dark');

  constructor() {
    this.apply();
  }

  toggle(): void {
    this.darkMode.update((value) => !value);
    localStorage.setItem(THEME_KEY, this.darkMode() ? 'dark' : 'light');
    this.apply();
  }

  setDarkMode(value: boolean): void {
    this.darkMode.set(value);
    localStorage.setItem(THEME_KEY, value ? 'dark' : 'light');
    this.apply();
  }

  private apply(): void {
    document.documentElement.classList.toggle('dark', this.darkMode());
  }
}
