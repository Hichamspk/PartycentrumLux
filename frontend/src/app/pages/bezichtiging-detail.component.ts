import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { MessageService } from 'primeng/api';
import { ApiService } from '../core/api.service';
import { Bezichtiging, BezichtigingStatus, MailLog } from '../core/models';
import { PRIME_IMPORTS } from '../shared/prime-imports';

@Component({
  selector: 'app-bezichtiging-detail',
  standalone: true,
  imports: [...PRIME_IMPORTS],
  template: `
    @if (bezichtiging) {
      <section class="space-y-6">
        <div class="flex flex-col justify-between gap-4 sm:flex-row sm:items-end">
          <div>
            <button class="mb-3 text-sm font-semibold text-slate-500 hover:text-slate-950" type="button" (click)="back()">Terug naar bezichtigingen</button>
            <h1 class="page-title">{{ bezichtiging.klantNaam }}</h1>
            <p class="muted">{{ bezichtiging.datum | date:'dd MMM yyyy' }} van {{ bezichtiging.startTijd }} tot {{ bezichtiging.eindTijd }}</p>
          </div>
          <div class="flex flex-wrap gap-2">
            <p-tag [value]="bezichtiging.status" [severity]="statusSeverity(bezichtiging.status)"></p-tag>
            @if (bezichtiging.status === 'GEPLAND') {
              <button pButton type="button" label="Markeer als geweest" icon="pi pi-check" (click)="updateStatus('GEWEEST')"></button>
              <button pButton type="button" label="Annuleer bezichtiging" severity="danger" icon="pi pi-ban" (click)="updateStatus('GEANNULEERD')"></button>
            }
            @if (bezichtiging.status === 'GEWEEST') {
              <button pButton type="button" label="Maak boeking aan" icon="pi pi-plus" (click)="createBooking()"></button>
            }
          </div>
        </div>

        <div class="grid gap-4 lg:grid-cols-3">
          <section class="surface-panel rounded-md p-5">
            <h2 class="font-bold">Klant</h2>
            <div class="mt-3 space-y-1 text-sm">
              <p>{{ bezichtiging.klantNaam }}</p>
              <p class="muted">{{ bezichtiging.klantEmail }}</p>
              <p class="muted">{{ bezichtiging.klantTelefoon }}</p>
            </div>
          </section>
          <section class="surface-panel rounded-md p-5">
            <h2 class="font-bold">Afspraak</h2>
            <div class="mt-3 space-y-1 text-sm">
              <p>{{ bezichtiging.datum | date:'dd MMM yyyy' }}</p>
              <p class="muted">{{ bezichtiging.startTijd }} - {{ bezichtiging.eindTijd }}</p>
              <p class="muted">Partycentrum Lux, Bennebroekerweg 530</p>
            </div>
          </section>
          <section class="surface-panel rounded-md p-5">
            <h2 class="font-bold">Boeking</h2>
            @if (bezichtiging.bookingId) {
              <p class="mt-3 text-sm">Gekoppeld aan boeking #{{ bezichtiging.bookingId }}</p>
              <button pButton type="button" label="Open boeking" class="mt-3 p-button-secondary" (click)="openBooking()"></button>
            } @else {
              <p class="muted mt-3">Nog niet gekoppeld aan een boeking.</p>
            }
          </section>
        </div>

        <section class="surface-panel rounded-md p-5">
          <h2 class="text-lg font-bold">Notities</h2>
          <textarea pInputTextarea rows="4" class="mt-3 w-full" [(ngModel)]="notities"></textarea>
          <div class="mt-3 flex justify-end">
            <button pButton type="button" label="Notities opslaan" icon="pi pi-save" class="p-button-secondary" (click)="saveNotes()"></button>
          </div>
        </section>

        <section class="surface-panel rounded-md p-5">
          <h2 class="text-lg font-bold">Mail log</h2>
          <div class="mt-4 overflow-x-auto">
            <p-table [value]="mailLogs" responsiveLayout="scroll">
              <ng-template pTemplate="header">
                <tr>
                  <th>Type</th>
                  <th>Ontvanger</th>
                  <th>Verzonden op</th>
                  <th>Status</th>
                  <th class="w-32">Actie</th>
                </tr>
              </ng-template>
              <ng-template pTemplate="body" let-log>
                <tr>
                  <td>{{ label(log.type) }}</td>
                  <td>{{ log.ontvangerEmail }}</td>
                  <td>{{ log.verzondenOp | date:'dd MMM yyyy HH:mm' }}</td>
                  <td><p-tag [value]="log.status" [severity]="log.status === 'VERZONDEN' ? 'success' : 'danger'"></p-tag></td>
                  <td>
                    @if (log.status === 'MISLUKT') {
                      <button pButton type="button" label="Opnieuw" size="small" class="p-button-secondary" (click)="resend(log)"></button>
                    }
                  </td>
                </tr>
              </ng-template>
            </p-table>
          </div>
        </section>
      </section>
    }
  `
})
export class BezichtigingDetailComponent implements OnInit {
  bezichtiging: Bezichtiging | null = null;
  mailLogs: MailLog[] = [];
  notities = '';
  private id = 0;

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly api: ApiService,
    private readonly messages: MessageService
  ) {}

  ngOnInit(): void {
    this.id = Number(this.route.snapshot.paramMap.get('id'));
    this.load();
  }

  load(): void {
    this.api.bezichtiging(this.id).subscribe((bezichtiging) => {
      this.bezichtiging = bezichtiging;
      this.notities = bezichtiging.notities ?? '';
    });
    this.api.bezichtigingMailLogs(this.id).subscribe((logs) => this.mailLogs = logs);
  }

  updateStatus(status: BezichtigingStatus): void {
    this.api.updateBezichtigingStatus(this.id, status).subscribe({
      next: () => {
        this.messages.add({ severity: 'success', summary: 'Status bijgewerkt', detail: 'De bezichtiging is bijgewerkt.' });
        this.load();
      },
      error: (error) => this.messages.add({ severity: 'error', summary: 'Niet bijgewerkt', detail: error.error?.message ?? 'Controleer de status.' })
    });
  }

  saveNotes(): void {
    if (!this.bezichtiging) {
      return;
    }
    this.api.saveBezichtiging({ ...this.bezichtiging, notities: this.notities }).subscribe({
      next: () => {
        this.messages.add({ severity: 'success', summary: 'Notities opgeslagen', detail: 'De notities zijn bijgewerkt.' });
        this.load();
      },
      error: (error) => this.messages.add({ severity: 'error', summary: 'Niet opgeslagen', detail: error.error?.message ?? 'Controleer de invoer.' })
    });
  }

  createBooking(): void {
    if (!this.bezichtiging) {
      return;
    }
    void this.router.navigate(['/boekingen'], {
      queryParams: {
        bezichtigingId: this.bezichtiging.id,
        klantNaam: this.bezichtiging.klantNaam,
        klantEmail: this.bezichtiging.klantEmail,
        klantTelefoon: this.bezichtiging.klantTelefoon
      }
    });
  }

  openBooking(): void {
    if (this.bezichtiging?.bookingId) {
      void this.router.navigate(['/boekingen', this.bezichtiging.bookingId]);
    }
  }

  resend(log: MailLog): void {
    this.api.resendMailLog(log.id).subscribe({
      next: () => {
        this.messages.add({ severity: 'success', summary: 'Opnieuw verzonden', detail: 'De mail is opnieuw verwerkt.' });
        this.load();
      },
      error: (error) => this.messages.add({ severity: 'error', summary: 'Niet verzonden', detail: error.error?.message ?? 'Controleer de instellingen.' })
    });
  }

  back(): void {
    void this.router.navigate(['/bezichtigingen']);
  }

  statusSeverity(status: BezichtigingStatus): 'info' | 'success' | 'danger' {
    return ({ GEPLAND: 'info', GEWEEST: 'success', GEANNULEERD: 'danger' } as const)[status];
  }

  label(value: string): string {
    return value.replaceAll('_', ' ').toLowerCase();
  }
}
