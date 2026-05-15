import { Injectable, signal } from '@angular/core';

const THEME_KEY = 'lux_theme';

@Injectable({ providedIn: 'root' })
export class ThemeService {
  readonly darkMode = signal(false);

  constructor() {
    this.apply();
  }

  toggle(): void {
    this.darkMode.set(false);
    localStorage.setItem(THEME_KEY, 'light');
    this.apply();
  }

  setDarkMode(_value: boolean): void {
    this.darkMode.set(false);
    localStorage.setItem(THEME_KEY, 'light');
    this.apply();
  }

  private apply(): void {
    document.documentElement.classList.remove('dark');
  }
}
