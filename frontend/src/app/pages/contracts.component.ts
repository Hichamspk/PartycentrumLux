import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { ApiService } from '../core/api.service';
import { AuthService } from '../core/auth.service';
import { Booking, ContractStatus, SelectOption } from '../core/models';
import { PRIME_IMPORTS } from '../shared/prime-imports';

@Component({
  selector: 'app-contracts',
  standalone: true,
  imports: [...PRIME_IMPORTS],
  template: `
    <section class="space-y-6">
      <div class="flex flex-col justify-between gap-4 sm:flex-row sm:items-end">
        <div>
          <h1 class="page-title">Contracten</h1>
          <p class="muted">Volg concepten, verzonden contracten en ondertekende overeenkomsten.</p>
        </div>
      </div>

      <div class="surface-panel rounded-md p-4">
        <div class="grid gap-3 md:grid-cols-[minmax(0,1fr)_260px]">
          <span class="p-input-icon-left">
            <i class="pi pi-search"></i>
            <input pInputText class="w-full" [(ngModel)]="filters.customer" placeholder="Zoek op klant" (input)="load()">
          </span>
          <p-dropdown
            [options]="statusOptionsWithAll"
            [(ngModel)]="filters.contractStatus"
            optionLabel="label"
            optionValue="value"
            placeholder="Contractstatus"
            styleClass="w-full"
            (onChange)="load()"
          ></p-dropdown>
        </div>
      </div>

      <div class="surface-panel rounded-md p-2">
        <p-table [value]="contracts" [loading]="loading" [paginator]="true" [rows]="10" responsiveLayout="scroll">
          <ng-template pTemplate="header">
            <tr>
              <th>Klant</th>
              <th>Evenement</th>
              <th>Boekingstatus</th>
              <th>Contract</th>
              <th>Ondertekend</th>
              <th class="min-w-40">Acties</th>
            </tr>
          </ng-template>
          <ng-template pTemplate="body" let-booking>
            <tr>
              <td>
                <p class="font-semibold">{{ booking.customerName }}</p>
                <p class="muted">{{ booking.customerEmail }}</p>
              </td>
              <td>
                <p>{{ booking.eventDate | date:'dd MMM yyyy' }}</p>
                <p class="muted">{{ booking.eventType }} · {{ booking.startTime }} - {{ booking.endTime }}</p>
              </td>
              <td><p-tag [value]="booking.status" [severity]="bookingSeverity(booking.status)"></p-tag></td>
              <td><p-tag [value]="booking.contractStatus" [severity]="contractSeverity(booking.contractStatus)"></p-tag></td>
              <td>{{ booking.contractSignedDate ? (booking.contractSignedDate | date:'dd MMM yyyy') : '-' }}</td>
              <td>
                <div class="flex flex-wrap gap-2">
                  <button class="icon-btn" type="button" (click)="openBooking(booking)" aria-label="Boeking openen">
                    <lucide-icon name="eye" [size]="16"></lucide-icon>
                  </button>
                  @if (auth.isOwner && booking.contractStatus !== 'ONDERTEKEND') {
                    <button class="icon-btn" type="button" (click)="openEditor(booking)" aria-label="Contract bewerken">
                      <lucide-icon name="edit" [size]="16"></lucide-icon>
                    </button>
                  }
                </div>
              </td>
            </tr>
          </ng-template>
        </p-table>
      </div>
    </section>
  `
})
export class ContractsComponent implements OnInit {
  contracts: Booking[] = [];
  loading = false;
  filters: { contractStatus: ContractStatus | null; customer: string } = {
    contractStatus: null,
    customer: ''
  };

  readonly statusOptions: SelectOption<ContractStatus>[] = [
    { label: 'Geen', value: 'GEEN' },
    { label: 'Concept', value: 'CONCEPT' },
    { label: 'Verzonden', value: 'VERZONDEN' },
    { label: 'Ondertekend', value: 'ONDERTEKEND' }
  ];
  readonly statusOptionsWithAll = [{ label: 'Alle contracten', value: null }, ...this.statusOptions];

  constructor(
    public readonly auth: AuthService,
    private readonly api: ApiService,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.api.contracts({
      contractStatus: this.filters.contractStatus,
      customer: this.filters.customer
    }).subscribe({
      next: (contracts) => this.contracts = contracts,
      complete: () => this.loading = false
    });
  }

  openBooking(booking: Booking): void {
    void this.router.navigate(['/boekingen', booking.id]);
  }

  openEditor(booking: Booking): void {
    void this.router.navigate(['/boekingen', booking.id, 'contract-editor']);
  }

  contractSeverity(status: string): 'success' | 'info' | 'warn' | 'secondary' {
    return {
      GEEN: 'secondary',
      CONCEPT: 'warn',
      VERZONDEN: 'info',
      ONDERTEKEND: 'success'
    }[status] as 'success' | 'info' | 'warn' | 'secondary';
  }

  bookingSeverity(status: string): 'success' | 'info' | 'warn' | 'danger' | 'secondary' {
    return {
      CONCEPT: 'secondary',
      OFFERTE_VERZONDEN: 'info',
      BEVESTIGD: 'success',
      AANBETALING_BETAALD: 'success',
      VOLLEDIG_BETAALD: 'success',
      AFGEROND: 'secondary',
      GEANNULEERD: 'danger'
    }[status] as 'success' | 'info' | 'warn' | 'danger' | 'secondary';
  }
}
