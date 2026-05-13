import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { ConfirmationService, MessageService } from 'primeng/api';
import { ApiService } from '../core/api.service';
import { AuthService } from '../core/auth.service';
import { Booking, Invoice, InvoiceStatus, InvoiceType, SelectOption } from '../core/models';
import { PRIME_IMPORTS } from '../shared/prime-imports';

@Component({
  selector: 'app-invoices',
  standalone: true,
  imports: [...PRIME_IMPORTS],
  template: `
    <section class="space-y-6">
      <div class="flex flex-col justify-between gap-4 sm:flex-row sm:items-end">
        <div>
          <h1 class="page-title">Facturen</h1>
          <p class="muted">Genereer PDF's, bewaak vervaldata en stuur herinneringen.</p>
        </div>
        @if (auth.isOwner) {
          <button pButton type="button" label="Nieuwe factuur" icon="pi pi-plus" (click)="openCreate()"></button>
        }
      </div>

      <div class="surface-panel rounded-md p-4">
        <p-dropdown [options]="statusOptionsWithAll" [(ngModel)]="statusFilter" optionLabel="label" optionValue="value" placeholder="Filter status" (onChange)="load()"></p-dropdown>
      </div>

      <div class="surface-panel rounded-md p-2">
        <p-table [value]="invoices" [loading]="loading" [paginator]="true" [rows]="10" responsiveLayout="scroll">
          <ng-template pTemplate="header">
            <tr>
              <th>Factuur</th>
              <th>Type</th>
              <th>Klant</th>
              <th>Bedrag</th>
              <th>Vervaldatum</th>
              <th>Status</th>
              <th class="min-w-56">Acties</th>
            </tr>
          </ng-template>
          <ng-template pTemplate="body" let-invoice>
            <tr>
              <td class="font-semibold">{{ invoice.invoiceNumber }}</td>
              <td>{{ invoice.invoiceType }}</td>
              <td>{{ invoice.customerName }}</td>
              <td>
                <p>{{ invoice.totalAmount | currency:'EUR' }}</p>
                <p class="muted">BTW {{ invoice.vatAmount | currency:'EUR' }}</p>
              </td>
              <td>{{ invoice.dueDate | date:'dd MMM yyyy' }}</td>
              <td>
                <p-tag [value]="invoice.status" [severity]="invoice.status === 'BETAALD' ? 'success' : invoice.status === 'VERLOPEN' ? 'danger' : 'warn'"></p-tag>
              </td>
              <td>
                <div class="flex flex-wrap gap-2">
                  <button class="icon-btn" type="button" (click)="downloadPdf(invoice)" [disabled]="!invoice.pdfPath" aria-label="PDF downloaden">
                    <lucide-icon name="download" [size]="16"></lucide-icon>
                  </button>
                  @if (auth.isOwner) {
                    <button class="icon-btn" type="button" (click)="openEdit(invoice)" aria-label="Factuur bewerken">
                      <lucide-icon name="edit" [size]="16"></lucide-icon>
                    </button>
                    <button class="icon-btn" type="button" (click)="generatePdf(invoice)" aria-label="PDF genereren">
                      <lucide-icon name="file-text" [size]="16"></lucide-icon>
                    </button>
                    @if (invoice.status !== 'BETAALD') {
                      <button class="icon-btn text-emerald-700" type="button" (click)="markPaid(invoice)" aria-label="Markeer betaald">
                        <lucide-icon name="check-circle-2" [size]="16"></lucide-icon>
                      </button>
                    }
                    <button class="icon-btn" type="button" (click)="sendReminder(invoice)" aria-label="Herinnering sturen">
                      <lucide-icon name="send" [size]="16"></lucide-icon>
                    </button>
                    <button class="icon-btn text-rose-600" type="button" (click)="confirmDelete(invoice)" aria-label="Factuur verwijderen">
                      <lucide-icon name="trash-2" [size]="16"></lucide-icon>
                    </button>
                  }
                </div>
              </td>
            </tr>
          </ng-template>
        </p-table>
      </div>

      <p-dialog [header]="form.controls.id.value ? 'Factuur bewerken' : 'Nieuwe factuur'" [(visible)]="dialogOpen" [modal]="true" [style]="{ width: 'min(640px, 94vw)' }">
        <form class="grid gap-4" [formGroup]="form" (ngSubmit)="save()">
          <label>
            <span class="mb-2 block text-sm font-semibold">Boeking</span>
            <p-dropdown [options]="bookings" formControlName="bookingId" optionLabel="customerName" optionValue="id" placeholder="Selecteer boeking" styleClass="w-full"></p-dropdown>
          </label>
          <label>
            <span class="mb-2 block text-sm font-semibold">Bedrag exclusief BTW</span>
            <p-inputNumber formControlName="amount" mode="currency" currency="EUR" locale="nl-NL" [min]="0" styleClass="w-full"></p-inputNumber>
          </label>
          <label>
            <span class="mb-2 block text-sm font-semibold">Factuurtype</span>
            <p-dropdown [options]="typeOptions" formControlName="invoiceType" optionLabel="label" optionValue="value" styleClass="w-full"></p-dropdown>
          </label>
          <label>
            <span class="mb-2 block text-sm font-semibold">Vervaldatum</span>
            <p-calendar formControlName="dueDate" dateFormat="dd-mm-yy" [showIcon]="true" styleClass="w-full"></p-calendar>
          </label>
          <div class="flex justify-end gap-2">
            <button pButton type="button" label="Annuleren" class="p-button-text" (click)="dialogOpen = false"></button>
            <button pButton type="submit" label="Opslaan" icon="pi pi-save" [disabled]="form.invalid"></button>
          </div>
        </form>
      </p-dialog>
    </section>
  `
})
export class InvoicesComponent implements OnInit {
  private readonly fb = inject(FormBuilder);

  invoices: Invoice[] = [];
  bookings: Booking[] = [];
  loading = false;
  dialogOpen = false;
  statusFilter: InvoiceStatus | null = null;

  readonly statusOptions: SelectOption<InvoiceStatus>[] = [
    { label: 'Concept', value: 'CONCEPT' },
    { label: 'Onbetaald', value: 'ONBETAALD' },
    { label: 'Betaald', value: 'BETAALD' },
    { label: 'Verlopen', value: 'VERLOPEN' }
  ];
  readonly statusOptionsWithAll = [{ label: 'Alle statussen', value: null }, ...this.statusOptions];
  readonly typeOptions: SelectOption<InvoiceType>[] = [
    { label: 'Volledig', value: 'VOLLEDIG' },
    { label: 'Aanbetaling', value: 'AANBETALING' },
    { label: 'Restant', value: 'RESTANT' }
  ];

  readonly form = this.fb.group({
    id: [null as number | null],
    bookingId: [null as number | null, Validators.required],
    amount: [0, [Validators.required, Validators.min(0)]],
    invoiceType: ['VOLLEDIG' as InvoiceType, Validators.required],
    dueDate: [null as Date | null, Validators.required]
  });

  constructor(
    public readonly auth: AuthService,
    private readonly api: ApiService,
    private readonly confirmations: ConfirmationService,
    private readonly messages: MessageService
  ) {}

  ngOnInit(): void {
    this.load();
    this.api.bookings().subscribe((bookings) => this.bookings = bookings);
  }

  load(): void {
    this.loading = true;
    this.api.invoices(this.statusFilter).subscribe({
      next: (invoices) => this.invoices = invoices,
      complete: () => this.loading = false
    });
  }

  openCreate(): void {
    this.form.reset({ id: null, bookingId: null, amount: 0, invoiceType: 'VOLLEDIG', dueDate: new Date() });
    this.dialogOpen = true;
  }

  openEdit(invoice: Invoice): void {
    this.form.reset({
      id: invoice.id,
      bookingId: invoice.bookingId,
      amount: invoice.amount,
      invoiceType: invoice.invoiceType ?? 'VOLLEDIG',
      dueDate: this.parseIso(invoice.dueDate)
    });
    this.dialogOpen = true;
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const raw = this.form.getRawValue();
    const payload = { ...raw, dueDate: this.toIso(raw.dueDate as Date) };
    this.api.saveInvoice(payload).subscribe({
      next: () => {
        this.messages.add({ severity: 'success', summary: 'Opgeslagen', detail: 'Factuur is opgeslagen.' });
        this.dialogOpen = false;
        this.load();
      },
      error: (error) => this.messages.add({ severity: 'error', summary: 'Niet opgeslagen', detail: error.error?.message ?? 'Controleer de factuur.' })
    });
  }

  generatePdf(invoice: Invoice): void {
    this.api.generateInvoicePdf(invoice.id).subscribe(() => {
      this.messages.add({ severity: 'success', summary: 'PDF gemaakt', detail: 'Factuur PDF is gegenereerd.' });
      this.load();
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

  markPaid(invoice: Invoice): void {
    this.api.markInvoicePaid(invoice.id).subscribe(() => {
      this.messages.add({ severity: 'success', summary: 'Betaald', detail: 'Factuur is als betaald gemarkeerd.' });
      this.load();
    });
  }

  sendReminder(invoice: Invoice): void {
    this.api.sendInvoiceReminder(invoice.id).subscribe(() => {
      this.messages.add({ severity: 'success', summary: 'Verzonden', detail: 'Betalingsherinnering is verzonden.' });
    });
  }

  confirmDelete(invoice: Invoice): void {
    this.confirmations.confirm({
      message: `Weet u zeker dat u factuur ${invoice.invoiceNumber} wilt verwijderen?`,
      header: 'Factuur verwijderen',
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: 'Verwijderen',
      rejectLabel: 'Annuleren',
      acceptButtonStyleClass: 'p-button-danger',
      accept: () => this.api.deleteInvoice(invoice.id).subscribe(() => this.load())
    });
  }

  private toIso(date: Date): string {
    const month = `${date.getMonth() + 1}`.padStart(2, '0');
    const day = `${date.getDate()}`.padStart(2, '0');
    return `${date.getFullYear()}-${month}-${day}`;
  }

  private parseIso(iso: string): Date {
    const [year, month, day] = iso.split('-').map(Number);
    return new Date(year, month - 1, day);
  }
}
