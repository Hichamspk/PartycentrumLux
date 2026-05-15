import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { ApiService } from '../core/api.service';
import { Bezichtiging, Booking } from '../core/models';
import { PRIME_IMPORTS } from '../shared/prime-imports';

interface CalendarDay {
  date: Date;
  iso: string;
  currentMonth: boolean;
}

@Component({
  selector: 'app-calendar',
  standalone: true,
  imports: [...PRIME_IMPORTS],
  template: `
    <section class="space-y-6">
      <div class="flex flex-col justify-between gap-4 sm:flex-row sm:items-end">
        <div>
          <h1 class="page-title">Kalender</h1>
          <p class="muted">Boekingen en bezichtigingen in een overzicht.</p>
        </div>
        <p-calendar [(ngModel)]="monthPicker" view="month" dateFormat="MM yy" [readonlyInput]="true" (onSelect)="changeMonth()"></p-calendar>
      </div>

      <div class="flex flex-wrap gap-3 text-sm">
        <span class="inline-flex items-center gap-2"><span class="h-3 w-3 rounded-full bg-blue-600"></span>Bezichtigingen</span>
        <span class="inline-flex items-center gap-2"><span class="h-3 w-3 rounded-full bg-amber-500"></span>Boekingen gepland</span>
        <span class="inline-flex items-center gap-2"><span class="h-3 w-3 rounded-full bg-emerald-600"></span>Boekingen bevestigd/betaald</span>
      </div>

      <div class="surface-panel overflow-hidden rounded-md">
        <div class="grid grid-cols-7 border-b border-slate-200 bg-slate-50 text-center text-xs font-bold uppercase text-slate-500 dark:border-slate-800 dark:bg-slate-900">
          @for (day of weekDays; track day) {
            <div class="px-2 py-3">{{ day }}</div>
          }
        </div>
        <div class="grid grid-cols-7">
          @for (day of days; track day.iso) {
            <button
              type="button"
              class="min-h-28 border-b border-r border-slate-200 p-2 text-left transition hover:bg-slate-50 dark:border-slate-800 dark:hover:bg-slate-900"
              [ngClass]="!day.currentMonth ? 'bg-slate-50 dark:bg-slate-950' : ''"
              (click)="select(day)"
            >
              <span class="text-sm font-semibold" [class.text-slate-400]="!day.currentMonth">{{ day.date.getDate() }}</span>
              <div class="mt-2 space-y-1">
                @for (booking of eventsFor(day.iso); track booking.id) {
                  <span class="block truncate rounded px-2 py-1 text-xs font-semibold text-white" [class]="eventClass(booking)">
                    {{ booking.customerName }}
                  </span>
                }
                @for (bezichtiging of bezichtigingenFor(day.iso); track bezichtiging.id) {
                  <span class="block truncate rounded bg-blue-600 px-2 py-1 text-xs font-semibold text-white">
                    {{ bezichtiging.klantNaam }}
                  </span>
                }
              </div>
            </button>
          }
        </div>
      </div>

      <p-dialog header="Planning op datum" [(visible)]="detailsOpen" [modal]="true" [style]="{ width: 'min(640px, 92vw)' }">
        <div class="space-y-3">
          @for (bezichtiging of selectedBezichtigingen; track bezichtiging.id) {
            <article class="rounded-md border border-blue-200 bg-blue-50 p-4 text-blue-950">
              <div class="flex items-center justify-between gap-3">
                <div>
                  <p class="font-bold">{{ bezichtiging.klantNaam }}</p>
                  <p class="text-sm text-blue-700">Bezichtiging - {{ bezichtiging.startTijd }} tot {{ bezichtiging.eindTijd }}</p>
                </div>
                <button pButton type="button" label="Open" size="small" class="p-button-secondary" (click)="openBezichtiging(bezichtiging)"></button>
              </div>
            </article>
          }
          @for (booking of selectedBookings; track booking.id) {
            <article class="rounded-md border border-slate-200 p-4 dark:border-slate-800">
              <div class="flex items-center justify-between">
                <div>
                  <p class="font-bold">{{ booking.customerName }}</p>
                  <p class="muted">{{ booking.eventType }} · {{ booking.guestCount }} gasten</p>
                </div>
                <p-tag [value]="booking.status" [severity]="bookingSeverity(booking.status)"></p-tag>
              </div>
              @if (booking.notes) {
                <p class="mt-3 text-sm text-slate-600 dark:text-slate-300">{{ booking.notes }}</p>
              }
            </article>
          }
          @if (!selectedBookings.length && !selectedBezichtigingen.length) {
            <p class="muted">Geen planning op deze datum.</p>
          }
        </div>
      </p-dialog>
    </section>
  `
})
export class CalendarComponent implements OnInit {
  readonly weekDays = ['Ma', 'Di', 'Wo', 'Do', 'Vr', 'Za', 'Zo'];
  monthPicker = new Date();
  days: CalendarDay[] = [];
  bookings: Booking[] = [];
  bezichtigingen: Bezichtiging[] = [];
  selectedBookings: Booking[] = [];
  selectedBezichtigingen: Bezichtiging[] = [];
  detailsOpen = false;

  constructor(private readonly api: ApiService, private readonly router: Router) {}

  ngOnInit(): void {
    this.buildDays();
    this.load();
  }

  changeMonth(): void {
    this.buildDays();
    this.load();
  }

  select(day: CalendarDay): void {
    this.selectedBookings = this.eventsFor(day.iso);
    this.selectedBezichtigingen = this.bezichtigingenFor(day.iso);
    this.detailsOpen = true;
  }

  eventsFor(iso: string): Booking[] {
    return this.bookings.filter((booking) => {
      const eventDate = booking.eventDate || booking.date;
      return booking.status !== 'GEANNULEERD' && eventDate === iso;
    });
  }

  bezichtigingenFor(iso: string): Bezichtiging[] {
    return this.bezichtigingen.filter((bezichtiging) => bezichtiging.status !== 'GEANNULEERD' && bezichtiging.datum === iso);
  }

  eventClass(booking: Booking): string {
    return ['BEVESTIGD', 'AANBETALING_BETAALD', 'VOLLEDIG_BETAALD', 'AFGEROND'].includes(booking.status)
      ? 'bg-emerald-600'
      : 'bg-amber-500';
  }

  openBezichtiging(bezichtiging: Bezichtiging): void {
    void this.router.navigate(['/bezichtigingen', bezichtiging.id]);
  }

  bookingSeverity(status: string): 'success' | 'info' | 'warn' | 'danger' | 'secondary' {
    return {
      CONCEPT: 'secondary',
      OFFERTE_VERZONDEN: 'info',
      BEVESTIGD: 'info',
      AANBETALING_BETAALD: 'warn',
      VOLLEDIG_BETAALD: 'success',
      AFGEROND: 'success',
      GEANNULEERD: 'danger'
    }[status] as 'success' | 'info' | 'warn' | 'danger' | 'secondary';
  }

  private load(): void {
    const start = this.days[0].iso;
    const end = this.days[this.days.length - 1].iso;
    this.api.calendarBookings(start, end).subscribe((bookings) => this.bookings = bookings);
    this.api.calendarBezichtigingen(start, end).subscribe((bezichtigingen) => this.bezichtigingen = bezichtigingen);
  }

  private buildDays(): void {
    const startOfMonth = new Date(this.monthPicker.getFullYear(), this.monthPicker.getMonth(), 1);
    const firstDay = (startOfMonth.getDay() + 6) % 7;
    const gridStart = new Date(startOfMonth);
    gridStart.setDate(startOfMonth.getDate() - firstDay);

    this.days = Array.from({ length: 42 }).map((_, index) => {
      const date = new Date(gridStart);
      date.setDate(gridStart.getDate() + index);
      return {
        date,
        iso: this.toIso(date),
        currentMonth: date.getMonth() === this.monthPicker.getMonth()
      };
    });
  }

  private toIso(date: Date): string {
    const month = `${date.getMonth() + 1}`.padStart(2, '0');
    const day = `${date.getDate()}`.padStart(2, '0');
    return `${date.getFullYear()}-${month}-${day}`;
  }
}
