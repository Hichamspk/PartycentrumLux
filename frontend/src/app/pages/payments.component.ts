import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { ConfirmationService, MessageService } from 'primeng/api';
import { ApiService } from '../core/api.service';
import { PaymentPart, PaymentSchedule, PaymentState, SelectOption } from '../core/models';
import { PRIME_IMPORTS } from '../shared/prime-imports';

@Component({
  selector: 'app-payments',
  standalone: true,
  imports: [...PRIME_IMPORTS],
  template: `
    <section class="space-y-6">
      <div class="flex flex-col justify-between gap-4 sm:flex-row sm:items-end">
        <div>
          <h1 class="page-title">Betalingen</h1>
          <p class="muted">Alle aanbetalingen en restantbetalingen over alle boekingen.</p>
        </div>
      </div>

      <div class="surface-panel rounded-md p-4">
        <div class="grid gap-3 md:grid-cols-[220px_minmax(0,1fr)]">
          <p-dropdown [options]="filterOptions" [(ngModel)]="statusFilter" optionLabel="label" optionValue="value" styleClass="w-full" (onChange)="load()"></p-dropdown>
          <span class="p-input-icon-left">
            <i class="pi pi-search"></i>
            <input pInputText class="w-full" [(ngModel)]="search" placeholder="Zoek klant" (input)="load()">
          </span>
        </div>
      </div>

      <div class="surface-panel rounded-md p-2">
        <p-table [value]="payments" [loading]="loading" [paginator]="true" [rows]="12" responsiveLayout="scroll">
          <ng-template pTemplate="header">
            <tr>
              <th>Klant</th>
              <th>Evenement</th>
              <th>Type</th>
              <th>Bedrag</th>
              <th>Deadline</th>
              <th>Status</th>
              <th>Actie</th>
            </tr>
          </ng-template>
          <ng-template pTemplate="body" let-payment>
            <tr>
              <td>
                <p class="font-semibold">{{ payment.klantNaam }}</p>
                <p class="muted">{{ payment.customerEmail }}</p>
              </td>
              <td>
                <p>{{ payment.evenementDatum | date:'dd MMM yyyy' }}</p>
                <p class="muted">{{ payment.evenementType }}</p>
              </td>
              <td>{{ payment.type === 'AANBETALING' ? 'Aanbetaling' : 'Restant' }}</td>
              <td class="font-semibold">{{ payment.bedrag | currency:'EUR' }}</td>
              <td>{{ payment.deadline ? (payment.deadline | date:'dd MMM yyyy') : '-' }}</td>
              <td><p-tag [value]="payment.locked ? 'VERGRENDELD' : payment.status" [severity]="severity(payment)"></p-tag></td>
              <td>
                <div class="flex gap-2">
                  @if (payment.status !== 'BETAALD' && !payment.locked) {
                    <button class="icon-btn text-emerald-700" type="button" (click)="confirmPaid(payment)" aria-label="Markeer als betaald">
                      <lucide-icon name="check-circle-2" [size]="16"></lucide-icon>
                    </button>
                  }
                  <button class="icon-btn" type="button" (click)="openBooking(payment.bookingId)" aria-label="Boeking openen">
                    <lucide-icon name="eye" [size]="16"></lucide-icon>
                  </button>
                </div>
              </td>
            </tr>
          </ng-template>
        </p-table>
      </div>
    </section>
  `
})
export class PaymentsComponent implements OnInit {
  payments: PaymentSchedule[] = [];
  loading = false;
  search = '';
  statusFilter: PaymentState | null = null;

  readonly filterOptions: SelectOption<PaymentState | null>[] = [
    { label: 'Alle', value: null },
    { label: 'Openstaand', value: 'OPENSTAAND' },
    { label: 'Betaald', value: 'BETAALD' },
    { label: 'Verlopen', value: 'VERLOPEN' }
  ];

  constructor(
    private readonly api: ApiService,
    private readonly confirmations: ConfirmationService,
    private readonly messages: MessageService,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.api.payments(this.statusFilter, this.search).subscribe({
      next: (payments) => this.payments = payments,
      complete: () => this.loading = false
    });
  }

  confirmPaid(payment: PaymentSchedule): void {
    this.confirmations.confirm({
      header: 'Betaling markeren',
      message: `Markeer ${payment.type.toLowerCase()} van ${payment.klantNaam} als betaald?`,
      icon: 'pi pi-check-circle',
      acceptLabel: 'Markeer betaald',
      rejectLabel: 'Annuleren',
      accept: () => this.markPaid(payment.bookingId, payment.type)
    });
  }

  markPaid(bookingId: number, type: PaymentPart): void {
    this.api.markBookingPaymentPaid(bookingId, type).subscribe({
      next: () => {
        this.messages.add({ severity: 'success', summary: 'Betaald', detail: 'Betaling is bijgewerkt.' });
        this.load();
      },
      error: (error) => this.messages.add({ severity: 'error', summary: 'Niet bijgewerkt', detail: error.error?.message ?? 'Controleer de betaling.' })
    });
  }

  openBooking(id: number): void {
    void this.router.navigate(['/boekingen', id]);
  }

  severity(payment: PaymentSchedule): 'success' | 'warn' | 'danger' | 'secondary' {
    if (payment.locked) {
      return 'secondary';
    }
    return ({
      OPENSTAAND: 'warn',
      BETAALD: 'success',
      VERLOPEN: 'danger'
    } as const)[payment.status];
  }
}
