import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { AuthService } from '../core/auth.service';
import { ThemeService } from '../core/theme.service';
import { PRIME_IMPORTS } from '../shared/prime-imports';

interface NavItem {
  label: string;
  route: string;
  icon: string;
  ownerOnly?: boolean;
}

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [RouterOutlet, ...PRIME_IMPORTS],
  template: `
    <div class="min-h-screen bg-slate-50 text-slate-950 dark:bg-slate-950 dark:text-slate-100">
      <aside
        class="fixed inset-y-0 left-0 z-40 w-72 border-r border-slate-200 bg-white px-4 py-5 transition-transform dark:border-slate-800 dark:bg-slate-900 lg:translate-x-0"
        [class.-translate-x-full]="!sidebarOpen"
      >
        <div class="flex items-center justify-between">
          <a routerLink="/dashboard" class="flex items-center gap-3">
            <span class="flex h-10 w-10 items-center justify-center rounded-md bg-slate-950 text-lg font-black text-white dark:bg-white dark:text-slate-950">L</span>
            <span>
              <span class="block text-sm font-bold uppercase tracking-wide text-slate-950 dark:text-white">Partycentrum Lux</span>
              <span class="muted">Boekingsbeheer</span>
            </span>
          </a>
          <button class="icon-btn lg:hidden" (click)="sidebarOpen = false" type="button" aria-label="Menu sluiten">
            <lucide-icon name="x" [size]="18"></lucide-icon>
          </button>
        </div>

        <nav class="mt-8 space-y-1">
          @for (item of navItems; track item.route) {
            @if (!item.ownerOnly || auth.isOwner) {
              <a
                [routerLink]="item.route"
                routerLinkActive="bg-slate-950 text-white dark:bg-white dark:text-slate-950"
                class="flex items-center gap-3 rounded-md px-3 py-2.5 text-sm font-semibold text-slate-600 transition hover:bg-slate-100 hover:text-slate-950 dark:text-slate-300 dark:hover:bg-slate-800 dark:hover:text-white"
                (click)="sidebarOpen = false"
              >
                <lucide-icon [name]="item.icon" [size]="18"></lucide-icon>
                <span>{{ item.label }}</span>
              </a>
            }
          }
        </nav>

        <div class="absolute inset-x-4 bottom-5 rounded-md border border-slate-200 p-3 dark:border-slate-800">
          <div class="flex items-center justify-between gap-3">
            <div class="min-w-0">
              <p class="truncate text-sm font-semibold">{{ auth.session()?.name }}</p>
              <p class="muted">{{ auth.role === 'OWNER' ? 'Eigenaar' : 'Medewerker' }}</p>
            </div>
            <button class="icon-btn" (click)="auth.logout()" type="button" aria-label="Uitloggen">
              <lucide-icon name="log-out" [size]="18"></lucide-icon>
            </button>
          </div>
        </div>
      </aside>

      <div class="lg:pl-72">
        <header class="sticky top-0 z-30 border-b border-slate-200 bg-white/90 px-4 py-3 backdrop-blur dark:border-slate-800 dark:bg-slate-950/90">
          <div class="flex items-center justify-between gap-3">
            <button class="icon-btn lg:hidden" type="button" (click)="sidebarOpen = true" aria-label="Menu openen">
              <lucide-icon name="menu" [size]="18"></lucide-icon>
            </button>
            <div>
              <p class="text-sm font-semibold text-slate-950 dark:text-white">Dashboard</p>
              <p class="muted">Vandaag klaarzetten, morgen soepel draaien.</p>
            </div>
            <button class="icon-btn" (click)="theme.toggle()" type="button" aria-label="Thema wisselen">
              <lucide-icon [name]="theme.darkMode() ? 'sun' : 'moon'" [size]="18"></lucide-icon>
            </button>
          </div>
        </header>

        <main class="px-4 py-6 sm:px-6 lg:px-8">
          <router-outlet></router-outlet>
        </main>
      </div>
    </div>
  `
})
export class ShellComponent {
  sidebarOpen = false;

  readonly navItems: NavItem[] = [
    { label: 'Dashboard', route: '/dashboard', icon: 'layout-dashboard' },
    { label: 'Kalender', route: '/kalender', icon: 'calendar-days' },
    { label: 'Boekingen', route: '/boekingen', icon: 'book-open' },
    { label: 'Klanten', route: '/klanten', icon: 'users' },
    { label: 'Betalingen', route: '/betalingen', icon: 'credit-card' },
    { label: 'Medewerkers', route: '/medewerkers', icon: 'user-cog', ownerOnly: true },
    { label: 'Instellingen', route: '/instellingen', icon: 'settings', ownerOnly: true }
  ];

  constructor(public readonly auth: AuthService, public readonly theme: ThemeService) {}
}
