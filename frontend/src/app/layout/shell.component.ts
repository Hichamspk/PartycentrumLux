import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { AuthService } from '../core/auth.service';
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
    <div class="min-h-screen bg-[#F5F5F5] text-[#111111]">
      @if (sidebarOpen) {
        <button
          type="button"
          class="fixed inset-0 z-30 bg-black/25 md:hidden"
          aria-label="Menu sluiten"
          (click)="sidebarOpen = false"
        ></button>
      }

      <aside
        class="fixed inset-y-0 left-0 z-40 flex w-[240px] flex-col border-r border-[#E5E5E5] bg-[#F7F7F7] px-3 py-4 transition-transform md:translate-x-0"
        [class.-translate-x-full]="!sidebarOpen"
      >
        <div class="flex items-center justify-between px-1">
          <a routerLink="/dashboard" class="flex min-w-0 items-center gap-3" (click)="sidebarOpen = false">
            <span class="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-[#111111] text-lg font-semibold text-[#DCAB46]">L</span>
            <span>
              <span class="block truncate text-sm font-semibold text-[#111111]">Partycentrum Lux</span>
              <span class="block truncate text-xs text-[#666666]">Boekingsbeheer</span>
            </span>
          </a>
          <button class="icon-btn md:hidden" (click)="sidebarOpen = false" type="button" aria-label="Menu sluiten">
            <lucide-icon name="x" [size]="18"></lucide-icon>
          </button>
        </div>

        <nav class="mt-8 flex-1 space-y-1">
          @for (item of navItems; track item.route) {
            @if (!item.ownerOnly || auth.isOwner) {
              <a
                [routerLink]="item.route"
                routerLinkActive="bg-white text-[#111111] shadow-soft"
                class="flex h-10 items-center gap-3 rounded-lg px-3 text-sm font-medium text-[#666666] transition hover:bg-[#EFEFEF] hover:text-[#111111]"
                (click)="sidebarOpen = false"
              >
                <lucide-icon class="shrink-0" [name]="item.icon" [size]="18"></lucide-icon>
                <span>{{ item.label }}</span>
              </a>
            }
          }
        </nav>

        <div class="rounded-xl border border-[#E5E5E5] bg-white p-3 shadow-soft">
          <div class="flex items-center gap-3">
            <div class="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-[#111111] text-sm font-semibold text-white">
              {{ initials }}
            </div>
            <div class="min-w-0">
              <p class="truncate text-sm font-semibold text-[#111111]">{{ auth.session()?.name }}</p>
              <p class="text-xs text-[#666666]">{{ auth.role === 'OWNER' ? 'Eigenaar' : 'Medewerker' }}</p>
            </div>
            <button class="icon-btn ml-auto h-9 w-9" (click)="auth.logout()" type="button" aria-label="Uitloggen">
              <lucide-icon name="log-out" [size]="18"></lucide-icon>
            </button>
          </div>
        </div>
      </aside>

      <div class="md:pl-[240px]">
        <header class="sticky top-0 z-20 border-b border-[#E5E5E5] bg-[#F5F5F5]/95 px-4 py-3 backdrop-blur md:hidden">
          <div class="flex items-center justify-between">
            <button class="icon-btn" type="button" (click)="sidebarOpen = true" aria-label="Menu openen">
              <lucide-icon name="menu" [size]="18"></lucide-icon>
            </button>
            <p class="text-sm font-semibold">Partycentrum Lux</p>
            <span class="h-10 w-10"></span>
          </div>
        </header>

        <main class="mx-auto w-full max-w-[1440px] px-4 py-5 sm:px-6 md:py-8 lg:px-8">
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
    { label: 'Bezichtigingen', route: '/bezichtigingen', icon: 'calendar-check' },
    { label: 'Boekingen', route: '/boekingen', icon: 'book-open' },
    { label: 'Klanten', route: '/klanten', icon: 'users' },
    { label: 'Betalingen', route: '/betalingen', icon: 'credit-card' },
    { label: 'Medewerkers', route: '/medewerkers', icon: 'user-cog', ownerOnly: true },
    { label: 'Instellingen', route: '/instellingen', icon: 'settings', ownerOnly: true }
  ];

  constructor(public readonly auth: AuthService) {}

  get initials(): string {
    const name = this.auth.session()?.name?.trim() || 'PL';
    return name
      .split(/\s+/)
      .slice(0, 2)
      .map((part) => part.charAt(0).toUpperCase())
      .join('');
  }
}
