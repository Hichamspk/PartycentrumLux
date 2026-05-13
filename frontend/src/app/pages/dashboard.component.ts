import { Component, OnInit } from '@angular/core';
import { ApiService } from '../core/api.service';
import { AuthService } from '../core/auth.service';
import { Booking, DashboardStats, Invoice } from '../core/models';
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
          <p class="muted">Overzicht van omzet, bezetting en aankomende evenementen.</p>
        </div>
        <p-tag [value]="auth.role === 'OWNER' ? 'Eigenaar' : 'Medewerker'" severity="info"></p-tag>
      </div>

      @if (auth.isOwner && stats) {
        <div class="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
          <article class="surface-panel rounded-md p-5">
            <p class="muted">Omzet deze maand</p>
            <p class="mt-2 text-3xl font-black">{{ stats.revenueThisMonth | currency:'EUR' }}</p>
          </article>
          <article class="surface-panel rounded-md p-5">
            <p class="muted">Boekingen deze maand</p>
            <p class="mt-2 text-3xl font-black">{{ stats.bookingsThisMonth }}</p>
          </article>
          <article class="surface-panel rounded-md p-5">
            <p class="muted">Openstaande betalingen</p>
            <p class="mt-2 text-3xl font-black text-rose-600">{{ stats.outstandingPayments | currency:'EUR' }}</p>
          </article>
          <article class="surface-panel rounded-md p-5">
            <p class="muted">Bezettingsgraad</p>
            <p class="mt-2 text-3xl font-black">{{ stats.occupancyRate | number:'1.0-1' }}%</p>
          </article>
        </div>

        <div class="grid gap-4 xl:grid-cols-2">
          <section class="surface-panel rounded-md p-5">
            <h2 class="text-lg font-bold">Omzet per maand</h2>
            <p-chart type="line" [data]="revenueData" [options]="chartOptions" height="280px"></p-chart>
          </section>
          <section class="surface-panel rounded-md p-5">
            <h2 class="text-lg font-bold">Boekingen per maand</h2>
            <p-chart type="bar" [data]="bookingData" [options]="chartOptions" height="280px"></p-chart>
          </section>
        </div>
      } @else if (!auth.isOwner) {
        <div class="surface-panel rounded-md p-5">
          <h2 class="text-lg font-bold">Uw werkoverzicht</h2>
          <p class="muted mt-1">Financiele cijfers zijn alleen zichtbaar voor de eigenaar.</p>
        </div>
      }

      <div class="grid gap-4 xl:grid-cols-2">
        <section class="surface-panel rounded-md p-5">
          <h2 class="text-lg font-bold">Aankomende boekingen</h2>
          <div class="mt-4 space-y-3">
            @for (booking of upcoming; track booking.id) {
              <article class="rounded-md border border-slate-200 p-4 dark:border-slate-800">
                <div class="flex items-center justify-between gap-3">
                  <div>
                    <p class="font-semibold">{{ booking.customerName }}</p>
                    <p class="muted">{{ (booking.eventDate || booking.date) | date:'dd MMM yyyy' }} · {{ booking.eventType }} · {{ booking.guestCount }} gasten</p>
                  </div>
                  <p-tag [value]="booking.status" [severity]="bookingSeverity(booking.status)"></p-tag>
                </div>
              </article>
            } @empty {
              <p class="muted">Geen aankomende boekingen gevonden.</p>
            }
          </div>
        </section>

        @if (auth.isOwner) {
          <section class="surface-panel rounded-md p-5">
            <h2 class="text-lg font-bold">Recente facturen</h2>
            <div class="mt-4 space-y-3">
              @for (invoice of recentInvoices; track invoice.id) {
                <article class="rounded-md border border-slate-200 p-4 dark:border-slate-800">
                  <div class="flex items-center justify-between gap-3">
                    <div>
                      <p class="font-semibold">{{ invoice.invoiceNumber }}</p>
                      <p class="muted">{{ invoice.customerName }} · {{ invoice.totalAmount | currency:'EUR' }}</p>
                    </div>
                    <p-tag [value]="invoice.status" [severity]="invoice.status === 'BETAALD' ? 'success' : invoice.status === 'VERLOPEN' ? 'danger' : 'warn'"></p-tag>
                  </div>
                </article>
              } @empty {
                <p class="muted">Nog geen facturen.</p>
              }
            </div>
          </section>
        }
      </div>
    </section>
  `
})
export class DashboardComponent implements OnInit {
  stats: DashboardStats | null = null;
  upcoming: Booking[] = [];
  recentInvoices: Invoice[] = [];
  revenueData: object | null = null;
  bookingData: object | null = null;
  chartOptions: object = {
    maintainAspectRatio: false,
    plugins: { legend: { display: false } },
    scales: {
      y: { beginAtZero: true, grid: { color: 'rgba(148, 163, 184, .18)' } },
      x: { grid: { display: false } }
    }
  };

  constructor(public readonly auth: AuthService, private readonly api: ApiService) {}

  ngOnInit(): void {
    if (this.auth.isOwner) {
      this.api.dashboardStats().subscribe((stats) => {
        this.stats = stats;
        this.upcoming = stats.upcomingBookings;
        this.recentInvoices = stats.recentInvoices;
        this.revenueData = {
          labels: stats.revenuePerMonth.map((point) => point.label),
          datasets: [{ data: stats.revenuePerMonth.map((point) => point.amount), borderColor: '#2563eb', backgroundColor: 'rgba(37,99,235,.12)', tension: .35, fill: true }]
        };
        this.bookingData = {
          labels: stats.bookingsPerMonth.map((point) => point.label),
          datasets: [{ data: stats.bookingsPerMonth.map((point) => point.count), backgroundColor: '#0f9f6e', borderRadius: 6 }]
        };
      });
    } else {
      this.api.upcomingBookings().subscribe((bookings) => this.upcoming = bookings);
    }
  }

  bookingSeverity(status: string): 'success' | 'info' | 'warn' | 'danger' | 'secondary' {
    return {
      CONCEPT: 'secondary',
      CONTRACT_VERZONDEN: 'info',
      CONTRACT_ONDERTEKEND: 'info',
      FACTUUR_VERZONDEN: 'warn',
      AANBETALING_BETAALD: 'warn',
      VOLLEDIG_BETAALD: 'success',
      AFGEROND: 'success',
      GEANNULEERD: 'danger'
    }[status] as 'success' | 'info' | 'warn' | 'danger' | 'secondary';
  }
}
