import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { ConfirmationService, MessageService } from 'primeng/api';
import { ApiService } from '../core/api.service';
import { AuthService } from '../core/auth.service';
import { Booking, Contract, Invoice, SelectOption } from '../core/models';
import { PRIME_IMPORTS } from '../shared/prime-imports';

type InvoiceMode = 'A' | 'B';

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

        <p-tabView>
          <p-tabPanel header="Boeking">
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
                <h2 class="font-bold">Evenement</h2>
                <div class="mt-3 space-y-1 text-sm">
                  <p>{{ booking.eventDate | date:'dd MMM yyyy' }}</p>
                  <p class="muted">{{ booking.startTime }} - {{ booking.endTime }}</p>
                  <p class="muted">{{ booking.guestCount }} gasten</p>
                  <p class="muted">{{ booking.price | currency:'EUR' }} excl. BTW</p>
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

            <section class="surface-panel mt-4 rounded-md p-5">
              <h2 class="font-bold">Subprijzen</h2>
              <div class="mt-4 overflow-x-auto">
                <table class="w-full min-w-[520px] text-sm">
                  <thead>
                    <tr class="border-b border-slate-200 text-left dark:border-slate-800">
                      <th class="py-2">Naam</th>
                      <th class="py-2 text-right">Prijs excl. BTW</th>
                    </tr>
                  </thead>
                  <tbody>
                    @for (subPrijs of booking.subPrijzen; track subPrijs.id || subPrijs.naam) {
                      <tr class="border-b border-slate-100 dark:border-slate-800">
                        <td class="py-3">{{ subPrijs.naam }}</td>
                        <td class="py-3 text-right font-semibold">{{ subPrijs.prijs | currency:'EUR' }}</td>
                      </tr>
                    } @empty {
                      <tr><td colspan="2" class="py-3 muted">Geen subprijzen vastgelegd.</td></tr>
                    }
                  </tbody>
                  <tfoot>
                    <tr>
                      <td class="py-3 font-bold">Totaal</td>
                      <td class="py-3 text-right text-lg font-black">{{ booking.price | currency:'EUR' }}</td>
                    </tr>
                  </tfoot>
                </table>
              </div>
            </section>

            @if (booking.annuleringsReden) {
              <section class="surface-panel mt-4 rounded-md border-rose-200 bg-rose-50 p-5 text-rose-900 dark:border-rose-900 dark:bg-rose-950 dark:text-rose-100">
                <h2 class="font-bold">Annuleringsreden</h2>
                <p class="mt-2 text-sm">{{ booking.annuleringsReden }}</p>
              </section>
            }
          </p-tabPanel>

          <p-tabPanel header="Contract">
            <section class="space-y-4">
              <div class="surface-panel rounded-md p-5">
                <div class="flex flex-col justify-between gap-4 md:flex-row md:items-center">
                  <div>
                    <h2 class="text-lg font-bold">Contract</h2>
                    <p class="muted">Status en digitaal ondertekenen via DocuSeal.</p>
                  </div>
                  <p-tag [value]="contract?.status || booking.contractStatus" [severity]="contractSeverity(contract?.status || booking.contractStatus)"></p-tag>
                </div>

                <div class="mt-5 flex flex-wrap gap-2">
                  @if ((contract?.status || booking.contractStatus) === 'GEEN') {
                    <button pButton type="button" label="Contract genereren" icon="pi pi-file-edit" (click)="openEditor()"></button>
                  } @else if ((contract?.status || booking.contractStatus) === 'ONDERTEKEND') {
                    <button pButton type="button" label="Download getekend contract" icon="pi pi-download" (click)="downloadSignedContract()"></button>
                    <p class="muted self-center">Ondertekend op {{ contract?.signedDate || booking.contractSignedDate | date:'dd MMM yyyy' }}</p>
                  } @else {
                    <button pButton type="button" label="Open editor" icon="pi pi-pencil" (click)="openEditor()"></button>
                    <button pButton type="button" label="Opnieuw versturen" icon="pi pi-send" class="p-button-secondary" (click)="resendContract()"></button>
                  }
                </div>
              </div>

              @if (contract?.html) {
                <div class="surface-panel rounded-md p-3">
                  <iframe title="Contract preview" class="h-[780px] w-full border-0 bg-white" [srcdoc]="contract?.html"></iframe>
                </div>
              }
            </section>
          </p-tabPanel>

          <p-tabPanel header="Facturen">
            <section class="space-y-4">
              <div class="surface-panel rounded-md p-5">
                <div class="flex flex-col justify-between gap-4 lg:flex-row lg:items-end">
                  <div>
                    <h2 class="text-lg font-bold">Factuurflow</h2>
                    <p class="muted">Maak een volledige factuur of splits deze in aanbetaling en restant.</p>
                  </div>
                  @if (auth.isOwner) {
                    <div class="flex flex-col gap-2 sm:flex-row">
                      <p-dropdown [options]="invoiceModeOptions" [(ngModel)]="invoiceMode" optionLabel="label" optionValue="value" styleClass="w-full sm:w-72"></p-dropdown>
                      <button pButton type="button" label="Concept maken" icon="pi pi-plus" (click)="createInvoices()"></button>
                      <button pButton type="button" label="Verstuur factuur" icon="pi pi-send" class="p-button-secondary" [disabled]="!invoices.length" (click)="sendInvoices()"></button>
                    </div>
                  }
                </div>

                @if (booking.contractStatus !== 'ONDERTEKEND') {
                  <div class="mt-4 rounded-md border border-amber-200 bg-amber-50 p-3 text-sm text-amber-900 dark:border-amber-900 dark:bg-amber-950 dark:text-amber-100">
                    Het contract is nog niet ondertekend. Bij factuur aanmaken tonen we eerst een waarschuwing.
                  </div>
                }
              </div>

              <div class="surface-panel rounded-md p-2">
                <p-table [value]="invoices" [loading]="loadingInvoices" responsiveLayout="scroll">
                  <ng-template pTemplate="header">
                    <tr>
                      <th>Factuur</th>
                      <th>Type</th>
                      <th>Bedrag</th>
                      <th>Vervaldatum</th>
                      <th>Status</th>
                      <th>Acties</th>
                    </tr>
                  </ng-template>
                  <ng-template pTemplate="body" let-invoice>
                    <tr>
                      <td class="font-semibold">{{ invoice.invoiceNumber }}</td>
                      <td>{{ invoice.invoiceType }}</td>
                      <td>
                        <p>{{ invoice.totalAmount | currency:'EUR' }}</p>
                        <p class="muted">Excl. {{ invoice.amount | currency:'EUR' }} · BTW {{ invoice.vatAmount | currency:'EUR' }}</p>
                      </td>
                      <td>{{ invoice.dueDate | date:'dd MMM yyyy' }}</td>
                      <td><p-tag [value]="invoice.status" [severity]="invoiceSeverity(invoice.status)"></p-tag></td>
                      <td>
                        <div class="flex flex-wrap gap-2">
                          <button class="icon-btn" type="button" (click)="downloadPdf(invoice)" [disabled]="!invoice.pdfPath" aria-label="PDF downloaden">
                            <lucide-icon name="download" [size]="16"></lucide-icon>
                          </button>
                          @if (auth.isOwner && invoice.status !== 'BETAALD') {
                            <button class="icon-btn text-emerald-700" type="button" (click)="markPaid(invoice)" aria-label="Markeer als betaald">
                              <lucide-icon name="check-circle-2" [size]="16"></lucide-icon>
                            </button>
                          }
                        </div>
                      </td>
                    </tr>
                  </ng-template>
                </p-table>
              </div>
            </section>
          </p-tabPanel>
        </p-tabView>

        <p-dialog header="Boeking annuleren" [(visible)]="cancelDialogOpen" [modal]="true" [style]="{ width: 'min(560px, 94vw)' }">
          <div class="space-y-4">
            <p class="muted">Geef de reden mee voor de annulering. Onbetaalde facturen behalve aanbetalingen worden verwijderd.</p>
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
  contract: Contract | null = null;
  invoices: Invoice[] = [];
  loadingInvoices = false;
  invoiceMode: InvoiceMode = 'A';
  cancelDialogOpen = false;
  cancelReason = '';
  private bookingId = 0;

  readonly invoiceModeOptions: SelectOption<InvoiceMode>[] = [
    { label: 'Optie A: volledige factuur', value: 'A' },
    { label: 'Optie B: 30% aanbetaling + 70% restant', value: 'B' }
  ];

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
    this.api.bookingContract(this.bookingId).subscribe((contract) => this.contract = contract);
    this.loadInvoices();
  }

  loadInvoices(): void {
    this.loadingInvoices = true;
    this.api.bookingInvoices(this.bookingId).subscribe({
      next: (invoices) => this.invoices = invoices,
      complete: () => this.loadingInvoices = false
    });
  }

  openEditor(): void {
    void this.router.navigate(['/boekingen', this.bookingId, 'contract-editor']);
  }

  resendContract(): void {
    this.api.sendBookingContract(this.bookingId, this.contract?.html ?? '').subscribe({
      next: (contract) => {
        this.contract = contract;
        this.messages.add({ severity: 'success', summary: 'Verzonden', detail: 'Contract is naar de klant verstuurd.' });
        this.load();
      },
      error: (error) => this.messages.add({ severity: 'error', summary: 'Niet verzonden', detail: error.error?.message ?? 'Controleer DocuSeal instellingen.' })
    });
  }

  createInvoices(): void {
    if (this.booking?.contractStatus !== 'ONDERTEKEND') {
      this.confirmations.confirm({
        header: 'Contract nog niet ondertekend',
        message: 'Maak pas facturen aan nadat het contract is ondertekend. Wilt u de contracteditor openen?',
        icon: 'pi pi-exclamation-triangle',
        acceptLabel: 'Open contract',
        rejectLabel: 'Sluiten',
        accept: () => this.openEditor()
      });
      return;
    }

    const request = this.invoiceMode === 'A'
      ? this.api.createBookingInvoiceOptionA(this.bookingId)
      : this.api.createBookingInvoiceOptionB(this.bookingId);
    request.subscribe({
      next: (invoices) => {
        this.invoices = invoices;
        this.messages.add({ severity: 'success', summary: 'Concept gemaakt', detail: 'Factuurconcept is aangemaakt.' });
        this.load();
      },
      error: (error) => this.messages.add({ severity: 'error', summary: 'Factuur niet gemaakt', detail: error.error?.message ?? 'Controleer de boeking.' })
    });
  }

  sendInvoices(): void {
    this.api.sendBookingInvoices(this.bookingId).subscribe({
      next: (invoices) => {
        this.invoices = invoices;
        this.messages.add({ severity: 'success', summary: 'Verzonden', detail: 'Factuurmail is naar de klant verzonden.' });
        this.load();
      },
      error: (error) => this.messages.add({ severity: 'error', summary: 'Niet verzonden', detail: error.error?.message ?? 'Controleer de facturen.' })
    });
  }

  markPaid(invoice: Invoice): void {
    this.api.markInvoicePaid(invoice.id).subscribe({
      next: () => {
        this.messages.add({ severity: 'success', summary: 'Betaald', detail: 'Factuur is als betaald gemarkeerd.' });
        this.load();
      },
      error: (error) => this.messages.add({ severity: 'error', summary: 'Niet bijgewerkt', detail: error.error?.message ?? 'Controleer de factuur.' })
    });
  }

  downloadPdf(invoice: Invoice): void {
    this.api.downloadInvoicePdf(invoice.id).subscribe((blob) => {
      const url = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `${invoice.invoiceNumber}.pdf`;
      link.click();
      URL.revokeObjectURL(url);
    });
  }

  downloadSignedContract(): void {
    this.api.downloadSignedContract(this.bookingId).subscribe((blob) => {
      const url = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `getekend-contract-${this.bookingId}.pdf`;
      link.click();
      URL.revokeObjectURL(url);
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
      CONTRACT_VERZONDEN: 'info',
      CONTRACT_ONDERTEKEND: 'info',
      FACTUUR_VERZONDEN: 'warn',
      AANBETALING_BETAALD: 'warn',
      VOLLEDIG_BETAALD: 'success',
      AFGEROND: 'success',
      GEANNULEERD: 'danger'
    }[status] as 'success' | 'info' | 'warn' | 'danger' | 'secondary';
  }

  invoiceSeverity(status: string): 'success' | 'warn' | 'danger' | 'secondary' {
    return {
      CONCEPT: 'secondary',
      ONBETAALD: 'warn',
      BETAALD: 'success',
      VERLOPEN: 'danger'
    }[status] as 'success' | 'warn' | 'danger' | 'secondary';
  }
}
