import { Component, OnInit } from '@angular/core';
import { ApiService } from '../core/api.service';
import { Booking, EventType } from '../core/models';
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
          <p class="muted">Bekijk bezette dagen en open direct de boekingsdetails.</p>
        </div>
        <p-calendar [(ngModel)]="monthPicker" view="month" dateFormat="MM yy" [readonlyInput]="true" (onSelect)="changeMonth()"></p-calendar>
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
                  <span class="block truncate rounded px-2 py-1 text-xs font-semibold text-white" [class]="eventClass(booking.eventType)">
                    {{ booking.customerName }}
                  </span>
                }
              </div>
            </button>
          }
        </div>
      </div>

      <p-dialog header="Boekingen op datum" [(visible)]="detailsOpen" [modal]="true" [style]="{ width: 'min(560px, 92vw)' }">
        <div class="space-y-3">
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
          } @empty {
            <p class="muted">Geen boekingen op deze datum.</p>
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
  selectedBookings: Booking[] = [];
  detailsOpen = false;

  constructor(private readonly api: ApiService) {}

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
    this.detailsOpen = true;
  }

  eventsFor(iso: string): Booking[] {
    return this.bookings.filter((booking) => {
      const eventDate = booking.eventDate || booking.date;
      return booking.status !== 'GEANNULEERD' && eventDate === iso;
    });
  }

  eventClass(type: EventType): string {
    return {
      BRUILOFT: 'bg-rose-600',
      VERJAARDAG: 'bg-amber-500',
      CONGRES: 'bg-blue-600',
      OVERIG: 'bg-emerald-600'
    }[type];
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

  private load(): void {
    const start = this.days[0].iso;
    const end = this.days[this.days.length - 1].iso;
    this.api.calendarBookings(start, end).subscribe((bookings) => this.bookings = bookings);
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
