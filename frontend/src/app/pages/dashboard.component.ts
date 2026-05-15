import { Component, OnInit } from '@angular/core';
import { ApiService } from '../core/api.service';
import { AuthService } from '../core/auth.service';
import { Bezichtiging, Booking, DashboardStats, Invoice } from '../core/models';
import { PRIME_IMPORTS } from '../shared/prime-imports';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [...PRIME_IMPORTS],
  template: `
    <section class="space-y-6">
      <div class="flex flex-col justify-between gap-4 sm:flex-row sm:items-end">
        <div>
          <h1 class="page-title">Dashboard</h1>
          <p class="muted mt-1">Overzicht van omzet, planning en opvolging.</p>
        </div>
        <span class="lux-status lux-status-info dashboard-role-badge">{{ auth.role === 'OWNER' ? 'Eigenaar' : 'Medewerker' }}</span>
      </div>

      <div class="grid gap-4 md:grid-cols-3">
        <a routerLink="/boekingen" class="lux-card flex min-h-24 items-center gap-4 p-5">
          <span class="flex h-11 w-11 items-center justify-center rounded-lg border border-[#E5E5E5] bg-white text-[#111111]">
            <lucide-icon name="plus" [size]="20"></lucide-icon>
          </span>
          <span class="section-title">Nieuwe boeking</span>
        </a>
        <a routerLink="/bezichtigingen" class="lux-card flex min-h-24 items-center gap-4 p-5">
          <span class="flex h-11 w-11 items-center justify-center rounded-lg border border-[#E5E5E5] bg-white text-[#111111]">
            <lucide-icon name="calendar-check" [size]="20"></lucide-icon>
          </span>
          <span class="section-title">Nieuwe bezichtiging</span>
        </a>
        <a routerLink="/klanten" class="lux-card flex min-h-24 items-center gap-4 p-5">
          <span class="flex h-11 w-11 items-center justify-center rounded-lg border border-[#E5E5E5] bg-white text-[#111111]">
            <lucide-icon name="users" [size]="20"></lucide-icon>
          </span>
          <span class="section-title">Voeg klant toe</span>
        </a>
      </div>

      @if (auth.isOwner && stats) {
        <div class="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
          <article class="surface-panel p-5">
            <p class="label-text">Omzet deze maand</p>
            <p class="mt-3 text-2xl font-semibold text-[#111111]">{{ stats.revenueThisMonth | currency:'EUR' }}</p>
          </article>
          <article class="surface-panel p-5">
            <p class="label-text">Boekingen deze maand</p>
            <p class="mt-3 text-2xl font-semibold text-[#111111]">{{ stats.bookingsThisMonth }}</p>
          </article>
          <article class="surface-panel p-5">
            <p class="label-text">Openstaande betalingen</p>
            <p class="mt-3 text-2xl font-semibold" [ngClass]="stats.outstandingPayments > 0 ? 'text-[#F59E0B]' : 'text-[#22C55E]'">
              {{ stats.outstandingPayments | currency:'EUR' }}
            </p>
          </article>
          <article class="surface-panel p-5">
            <p class="label-text">Bezettingsgraad</p>
            <p class="mt-3 text-2xl font-semibold text-[#111111]">{{ stats.occupancyRate | number:'1.0-1' }}%</p>
          </article>
        </div>
      } @else if (!auth.isOwner) {
        <div class="surface-panel p-5">
          <h2 class="section-title">Uw werkoverzicht</h2>
          <p class="muted mt-1">Financiele cijfers zijn alleen zichtbaar voor de eigenaar.</p>
        </div>
      }

      <div class="grid gap-4 xl:grid-cols-2">
        <section class="surface-panel p-5">
          <h2 class="section-title">Aankomende boekingen</h2>
          <div class="mt-4 space-y-3">
            @for (booking of upcoming; track booking.id) {
              <article class="rounded-lg border border-[#E5E5E5] p-4 transition hover:bg-[#F9F9F9]">
                <div class="flex items-center justify-between gap-3">
                  <div class="min-w-0">
                    <p class="font-semibold">{{ booking.customerName }}</p>
                    <p class="muted truncate">{{ (booking.eventDate || booking.date) | date:'dd MMM yyyy' }} - {{ booking.eventType }} - {{ booking.guestCount }} gasten</p>
                  </div>
                  <span class="lux-status shrink-0" [ngClass]="statusClass(booking.status)">{{ statusLabel(booking.status) }}</span>
                </div>
              </article>
            } @empty {
              <p class="muted">Geen aankomende boekingen gevonden.</p>
            }
          </div>
        </section>

        <section class="surface-panel p-5">
          <h2 class="section-title">Aankomende bezichtigingen</h2>
          <div class="mt-4 space-y-3">
            @for (bezichtiging of upcomingBezichtigingen; track bezichtiging.id) {
              <article class="rounded-lg border border-[#E5E5E5] p-4 transition hover:bg-[#F9F9F9]">
                <div class="flex items-center justify-between gap-3">
                  <div class="min-w-0">
                    <p class="font-semibold">{{ bezichtiging.klantNaam }}</p>
                    <p class="muted truncate">{{ bezichtiging.datum | date:'dd MMM yyyy' }} - {{ bezichtiging.startTijd }} tot {{ bezichtiging.eindTijd }}</p>
                  </div>
                  <span class="lux-status lux-status-gepland shrink-0">{{ bezichtiging.status }}</span>
                </div>
              </article>
            } @empty {
              <p class="muted">Geen aankomende bezichtigingen gevonden.</p>
            }
          </div>
        </section>
      </div>

      @if (auth.isOwner) {
        <section class="surface-panel p-5">
          <div class="flex flex-col justify-between gap-2 sm:flex-row sm:items-end">
            <div>
              <h2 class="section-title">Recente betalingen</h2>
              <p class="muted mt-1">Laatste factuur- en betaalmomenten.</p>
            </div>
            <a routerLink="/betalingen" class="text-sm font-semibold text-[#111111] hover:text-[#DCAB46]">Bekijk alles</a>
          </div>
          <div class="mt-4 overflow-x-auto">
            <table class="w-full min-w-[680px] text-sm">
              <thead>
                <tr class="border-b border-[#E5E5E5] text-left">
                  <th class="label-text py-3">Factuur</th>
                  <th class="label-text py-3">Klant</th>
                  <th class="label-text py-3 text-right">Bedrag</th>
                  <th class="label-text py-3">Vervaldatum</th>
                  <th class="label-text py-3">Status</th>
                </tr>
              </thead>
              <tbody>
                @for (invoice of recentInvoices; track invoice.id) {
                  <tr class="border-b border-[#E5E5E5] hover:bg-[#F9F9F9]">
                    <td class="py-3 font-semibold">{{ invoice.invoiceNumber }}</td>
                    <td class="py-3">{{ invoice.customerName }}</td>
                    <td class="py-3 text-right font-semibold">{{ invoice.totalAmount | currency:'EUR' }}</td>
                    <td class="py-3 text-[#666666]">{{ invoice.dueDate | date:'dd MMM yyyy' }}</td>
                    <td class="py-3"><span class="lux-status" [ngClass]="invoiceStatusClass(invoice.status)">{{ invoice.status }}</span></td>
                  </tr>
                } @empty {
                  <tr>
                    <td colspan="5" class="py-6 text-center text-sm text-[#666666]">Geen recente betalingen gevonden.</td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
        </section>
      }
    </section>
  `
})
export class DashboardComponent implements OnInit {
  stats: DashboardStats | null = null;
  upcoming: Booking[] = [];
  upcomingBezichtigingen: Bezichtiging[] = [];
  recentInvoices: Invoice[] = [];
  revenueData: object | null = null;
  bookingData: object | null = null;
  chartOptions: object = {};

  constructor(public readonly auth: AuthService, private readonly api: ApiService) {}

  ngOnInit(): void {
    this.api.upcomingBezichtigingen().subscribe((items) => this.upcomingBezichtigingen = items.slice(0, 5));

    if (this.auth.isOwner) {
      this.api.dashboardStats().subscribe((stats) => {
        this.stats = stats;
        this.upcoming = stats.upcomingBookings;
        this.recentInvoices = stats.recentInvoices;
        this.revenueData = {
          labels: stats.revenuePerMonth.map((point) => point.label),
          datasets: [{ data: stats.revenuePerMonth.map((point) => point.amount), borderColor: '#DCAB46', backgroundColor: 'rgba(220, 171, 70, .14)', tension: .35, fill: true }]
        };
        this.bookingData = {
          labels: stats.bookingsPerMonth.map((point) => point.label),
          datasets: [{ data: stats.bookingsPerMonth.map((point) => point.count), backgroundColor: '#22C55E', borderRadius: 6 }]
        };
      });
    } else {
      this.api.upcomingBookings().subscribe((bookings) => this.upcoming = bookings);
    }
  }

  statusClass(status: string): string {
    return {
      CONCEPT: 'lux-status-concept',
      OFFERTE_VERZONDEN: 'lux-status-offerte',
      BEVESTIGD: 'lux-status-bevestigd',
      AANBETALING_BETAALD: 'lux-status-aanbetaling',
      VOLLEDIG_BETAALD: 'lux-status-volledig',
      AFGEROND: 'lux-status-afgerond',
      GEANNULEERD: 'lux-status-geannuleerd'
    }[status] ?? 'lux-status-secondary';
  }

  invoiceStatusClass(status: string): string {
    return {
      CONCEPT: 'lux-status-concept',
      ONBETAALD: 'lux-status-openstaand',
      BETAALD: 'lux-status-verzonden',
      VERLOPEN: 'lux-status-danger'
    }[status] ?? 'lux-status-secondary';
  }

  statusLabel(status: string): string {
    return status.replaceAll('_', ' ');
  }
}
