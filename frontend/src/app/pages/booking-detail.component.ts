import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { ConfirmationService, MessageService } from 'primeng/api';
import { ApiService } from '../core/api.service';
import { AuthService } from '../core/auth.service';
import { Booking, BookingStatus, MailLog, Offerte, PaymentPart, PaymentSchedule, PaymentState } from '../core/models';
import { PRIME_IMPORTS } from '../shared/prime-imports';

@Component({
  selector: 'app-booking-detail',
  standalone: true,
  imports: [...PRIME_IMPORTS],
  template: `
    @if (booking) {
      <section class="space-y-6">
        <div class="flex flex-col justify-between gap-4 sm:flex-row sm:items-end">
          <div>
            <button class="mb-3 text-sm font-semibold text-slate-500 hover:text-slate-950 dark:text-slate-400 dark:hover:text-white" type="button" (click)="back()">Terug naar boekingen</button>
            <h1 class="page-title">{{ booking.customerName }}</h1>
            <p class="muted">{{ booking.eventType }} op {{ booking.eventDate | date:'dd MMM yyyy' }} van {{ booking.startTime }} tot {{ booking.endTime }}</p>
          </div>
          <div class="flex flex-wrap gap-2">
            <p-tag [value]="booking.status" [severity]="bookingSeverity(booking.status)"></p-tag>
            @if (auth.isOwner && booking.status !== 'GEANNULEERD') {
              <button pButton type="button" label="Annuleren" severity="danger" icon="pi pi-ban" (click)="openCancelDialog()"></button>
            }
          </div>
        </div>

        <div class="grid gap-4 lg:grid-cols-3">
          <section class="surface-panel rounded-md p-5">
            <h2 class="font-bold">Klant</h2>
            <div class="mt-3 space-y-1 text-sm">
              <p>{{ booking.customerName }}</p>
              <p class="muted">{{ booking.customerEmail }}</p>
              <p class="muted">{{ booking.customerPhone }}</p>
              <p class="muted">{{ booking.customerAddress || '-' }}</p>
            </div>
          </section>
          <section class="surface-panel rounded-md p-5">
            <h2 class="font-bold">Boeking</h2>
            <div class="mt-3 space-y-1 text-sm">
              <p>{{ booking.eventDate | date:'dd MMM yyyy' }}</p>
              <p class="muted">{{ booking.startTime }} - {{ booking.endTime }}</p>
              <p class="muted">{{ booking.guestCount }} gasten</p>
              <p class="muted">{{ booking.totaal | currency:'EUR' }}</p>
            </div>
          </section>
          <section class="surface-panel rounded-md p-5">
            <h2 class="font-bold">Eigenschappen</h2>
            <div class="mt-3 flex flex-wrap gap-2">
              @for (property of booking.properties; track property) {
                <span class="rounded-md bg-slate-100 px-2 py-1 text-xs font-semibold text-slate-700 dark:bg-slate-800 dark:text-slate-200">{{ property }}</span>
              } @empty {
                <p class="muted">Geen eigenschappen vastgelegd.</p>
              }
            </div>
          </section>
        </div>

        <section class="surface-panel rounded-md p-5">
          <h2 class="font-bold">Subprijzen</h2>
          <div class="mt-4 overflow-x-auto">
            <table class="w-full min-w-[520px] text-sm">
              <thead>
                <tr class="border-b border-slate-200 text-left dark:border-slate-800">
                  <th class="py-2">Naam</th>
                  <th class="py-2 text-right">Bedrag</th>
                </tr>
              </thead>
              <tbody>
                @for (subPrijs of booking.subPrijzen; track subPrijs.id || subPrijs.naam) {
                  <tr class="border-b border-slate-100 dark:border-slate-800">
                    <td class="py-3">{{ subPrijs.naam }}</td>
                    <td class="py-3 text-right font-semibold">{{ subPrijs.bedrag | currency:'EUR' }}</td>
                  </tr>
                }
              </tbody>
              <tfoot>
                <tr><td class="py-2">Subtotaal</td><td class="py-2 text-right">{{ booking.subtotaal | currency:'EUR' }}</td></tr>
                <tr><td class="py-2">Korting</td><td class="py-2 text-right">-{{ booking.korting | currency:'EUR' }}</td></tr>
                <tr><td class="py-3 font-bold">Totaal</td><td class="py-3 text-right text-lg font-black">{{ booking.totaal | currency:'EUR' }}</td></tr>
              </tfoot>
            </table>
          </div>
        </section>

        <section class="surface-panel rounded-md p-5">
          <div class="flex flex-col justify-between gap-4 lg:flex-row lg:items-center">
            <div>
              <h2 class="text-lg font-bold">Offerte / contract</h2>
              <p class="muted">De getekende offerte is de bevestiging en factuur.</p>
            </div>
            <div class="flex flex-wrap gap-2">
              <button pButton type="button" label="Genereer offerte" icon="pi pi-file-pdf" (click)="openOfferteEditor()"></button>
              @if (offerte?.pdfPath) {
                <button pButton type="button" label="Download" icon="pi pi-download" class="p-button-secondary" (click)="downloadOfferte()"></button>
              }
            </div>
          </div>

          <div class="mt-4 grid gap-3 md:grid-cols-3">
            <div class="rounded-md bg-slate-50 p-3 dark:bg-slate-950">
              <p class="muted">Document</p>
              <p class="font-semibold">{{ offerte?.documentRef || '-' }}</p>
            </div>
            <div class="rounded-md bg-slate-50 p-3 dark:bg-slate-950">
              <p class="muted">Verzonden</p>
              <p class="font-semibold">{{ offerte?.offerteSentDate ? (offerte?.offerteSentDate | date:'dd MMM yyyy') : '-' }}</p>
            </div>
            <div class="rounded-md bg-slate-50 p-3 dark:bg-slate-950">
              <p class="muted">Ondertekend</p>
              <p class="font-semibold">{{ booking.ondertekeningDatum ? (booking.ondertekeningDatum | date:'dd MMM yyyy') : '-' }}</p>
            </div>
          </div>

        </section>

        <section class="surface-panel rounded-md p-5">
          <h2 class="text-lg font-bold">Betalingen</h2>
          <div class="mt-4 grid gap-4 lg:grid-cols-2">
            @for (payment of payments; track payment.type) {
              <div class="rounded-md border border-slate-200 p-4 dark:border-slate-800">
                <div class="flex items-start justify-between gap-3">
                  <div>
                    <h3 class="font-bold">{{ payment.type === 'AANBETALING' ? 'Aanbetaling' : 'Restant' }}</h3>
                    <p class="muted">{{ payment.deadline ? ('Deadline ' + (payment.deadline | date:'dd MMM yyyy')) : 'Deadline na ondertekening' }}</p>
                  </div>
                  <p-tag [value]="payment.locked ? 'VERGRENDELD' : payment.status" [severity]="paymentSeverity(payment.status, payment.locked)"></p-tag>
                </div>
                <p class="mt-4 text-2xl font-black">{{ payment.bedrag | currency:'EUR' }}</p>
                @if (payment.locked) {
                  <p class="muted mt-2">Beschikbaar na ontvangst aanbetaling.</p>
                } @else if (payment.status === 'BETAALD') {
                  <p class="mt-2 text-sm font-semibold text-emerald-700">Betaald op {{ payment.betaaldDatum | date:'dd MMM yyyy' }}</p>
                } @else if (auth.isOwner) {
                  <button pButton type="button" label="Markeer als betaald" icon="pi pi-check" class="mt-4" (click)="confirmMarkPaid(payment.type)"></button>
                }
              </div>
            }
          </div>
        </section>

        <section class="surface-panel rounded-md p-5">
          <div class="flex items-center justify-between gap-3">
            <div>
              <h2 class="text-lg font-bold">Mail log</h2>
              <p class="muted">Alle mails die voor deze boeking zijn geregistreerd.</p>
            </div>
          </div>
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
                  <td><p-tag [value]="log.status" [severity]="mailSeverity(log.status)"></p-tag></td>
                  <td>
                    @if (log.status === 'MISLUKT') {
                      <button pButton type="button" label="Opnieuw" size="small" class="p-button-secondary" (click)="resendMail(log)"></button>
                    }
                  </td>
                </tr>
              </ng-template>
              <ng-template pTemplate="emptymessage">
                <tr>
                  <td colspan="5" class="py-6 text-center text-sm text-slate-500">Nog geen mails geregistreerd.</td>
                </tr>
              </ng-template>
            </p-table>
          </div>
        </section>

        @if (booking.annuleringsReden) {
          <section class="surface-panel rounded-md border-rose-200 bg-rose-50 p-5 text-rose-900 dark:border-rose-900 dark:bg-rose-950 dark:text-rose-100">
            <h2 class="font-bold">Annuleringsreden</h2>
            <p class="mt-2 text-sm">{{ booking.annuleringsReden }}</p>
          </section>
        }

        <p-dialog header="Boeking annuleren" [(visible)]="cancelDialogOpen" [modal]="true" [style]="{ width: 'min(560px, 94vw)' }">
          <div class="space-y-4">
            <p class="muted">Geef de reden mee voor de annulering. De klant ontvangt deze tekst per mail.</p>
            <textarea pInputTextarea class="w-full" rows="5" [(ngModel)]="cancelReason" placeholder="Reden van annulering"></textarea>
            <div class="flex justify-end gap-2">
              <button pButton type="button" label="Sluiten" class="p-button-text" (click)="cancelDialogOpen = false"></button>
              <button pButton type="button" label="Annuleren bevestigen" severity="danger" [disabled]="!cancelReason.trim()" (click)="cancelBooking()"></button>
            </div>
          </div>
        </p-dialog>
      </section>
    }
  `
})
export class BookingDetailComponent implements OnInit {
  booking: Booking | null = null;
  offerte: Offerte | null = null;
  payments: PaymentSchedule[] = [];
  mailLogs: MailLog[] = [];
  cancelDialogOpen = false;
  cancelReason = '';
  private bookingId = 0;

  constructor(
    public readonly auth: AuthService,
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly api: ApiService,
    private readonly confirmations: ConfirmationService,
    private readonly messages: MessageService
  ) {}

  ngOnInit(): void {
    this.bookingId = Number(this.route.snapshot.paramMap.get('id'));
    this.load();
  }

  load(): void {
    this.api.booking(this.bookingId).subscribe((booking) => this.booking = booking);
    this.api.offerte(this.bookingId).subscribe((offerte) => this.offerte = offerte);
    this.api.bookingPayments(this.bookingId).subscribe((payments) => this.payments = payments);
    this.api.bookingMailLogs(this.bookingId).subscribe((mailLogs) => this.mailLogs = mailLogs);
  }

  openOfferteEditor(): void {
    void this.router.navigate(['/boekingen', this.bookingId, 'offerte']);
  }

  sendOfferte(): void {
    this.api.sendOfferte(this.bookingId).subscribe({
      next: (offerte) => {
        this.offerte = offerte;
        this.messages.add({ severity: 'success', summary: 'Verzonden', detail: 'Offerte is naar DocuSeal gestuurd.' });
        this.load();
      },
      error: (error) => this.messages.add({ severity: 'error', summary: 'Niet verzonden', detail: error.error?.message ?? 'Controleer DocuSeal instellingen.' })
    });
  }

  downloadOfferte(): void {
    this.api.downloadOfferte(this.bookingId).subscribe((blob) => this.downloadBlob(blob, `offerte-${this.bookingId}.pdf`));
  }

  resendMail(log: MailLog): void {
    this.api.resendMailLog(log.id).subscribe({
      next: () => {
        this.messages.add({ severity: 'success', summary: 'Opnieuw verzonden', detail: 'De mail is opnieuw verwerkt.' });
        this.load();
      },
      error: (error) => this.messages.add({ severity: 'error', summary: 'Niet verzonden', detail: error.error?.message ?? 'Controleer de instellingen.' })
    });
  }

  confirmMarkPaid(type: PaymentPart): void {
    this.confirmations.confirm({
      header: 'Betaling markeren',
      message: `Weet u zeker dat u ${type.toLowerCase()} als betaald wilt markeren?`,
      icon: 'pi pi-check-circle',
      acceptLabel: 'Markeer betaald',
      rejectLabel: 'Annuleren',
      accept: () => this.api.markBookingPaymentPaid(this.bookingId, type).subscribe({
        next: () => {
          this.messages.add({ severity: 'success', summary: 'Betaald', detail: 'Betaling is bijgewerkt.' });
          this.load();
        },
        error: (error) => this.messages.add({ severity: 'error', summary: 'Niet bijgewerkt', detail: error.error?.message ?? 'Controleer de betaling.' })
      })
    });
  }

  openCancelDialog(): void {
    this.cancelReason = '';
    this.cancelDialogOpen = true;
  }

  cancelBooking(): void {
    if (!this.cancelReason.trim()) {
      return;
    }
    this.api.cancelBooking(this.bookingId, this.cancelReason.trim()).subscribe({
      next: () => {
        this.messages.add({ severity: 'success', summary: 'Geannuleerd', detail: 'Boeking is geannuleerd.' });
        this.cancelDialogOpen = false;
        this.load();
      },
      error: (error) => this.messages.add({ severity: 'error', summary: 'Niet geannuleerd', detail: error.error?.message ?? 'Controleer de reden.' })
    });
  }

  back(): void {
    void this.router.navigate(['/boekingen']);
  }

  bookingSeverity(status: BookingStatus): 'success' | 'info' | 'warn' | 'danger' | 'secondary' {
    return ({
      CONCEPT: 'secondary',
      OFFERTE_VERZONDEN: 'info',
      BEVESTIGD: 'warn',
      AANBETALING_BETAALD: 'warn',
      VOLLEDIG_BETAALD: 'success',
      AFGEROND: 'success',
      GEANNULEERD: 'danger'
    } as const)[status];
  }

  paymentSeverity(status: PaymentState, locked: boolean): 'success' | 'warn' | 'danger' | 'secondary' {
    if (locked) {
      return 'secondary';
    }
    return ({
      OPENSTAAND: 'warn',
      BETAALD: 'success',
      VERLOPEN: 'danger'
    } as const)[status];
  }

  mailSeverity(status: string): 'success' | 'danger' {
    return status === 'VERZONDEN' ? 'success' : 'danger';
  }

  label(value: string): string {
    return value.replaceAll('_', ' ').toLowerCase();
  }

  private downloadBlob(blob: Blob, filename: string): void {
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = filename;
    link.click();
    URL.revokeObjectURL(url);
  }
}
