import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { ConfirmationService, MessageService } from 'primeng/api';
import { ApiService } from '../core/api.service';
import { Invoice, Payment, PaymentMethod, SelectOption } from '../core/models';
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
          <p class="muted">Registreer bank-, contante en pinbetalingen.</p>
        </div>
        <button pButton type="button" label="Nieuwe betaling" icon="pi pi-plus" (click)="openCreate()"></button>
      </div>

      <div class="surface-panel rounded-md p-2">
        <p-table [value]="payments" [loading]="loading" [paginator]="true" [rows]="10" responsiveLayout="scroll">
          <ng-template pTemplate="header">
            <tr>
              <th>Factuur</th>
              <th>Klant</th>
              <th>Datum</th>
              <th>Methode</th>
              <th>Bedrag</th>
              <th class="w-32">Acties</th>
            </tr>
          </ng-template>
          <ng-template pTemplate="body" let-payment>
            <tr>
              <td class="font-semibold">{{ payment.invoiceNumber }}</td>
              <td>{{ payment.customerName }}</td>
              <td>{{ payment.paymentDate | date:'dd MMM yyyy' }}</td>
              <td>{{ payment.paymentMethod }}</td>
              <td>{{ payment.amount | currency:'EUR' }}</td>
              <td>
                <div class="flex gap-2">
                  <button class="icon-btn" type="button" (click)="openEdit(payment)" aria-label="Betaling bewerken">
                    <lucide-icon name="edit" [size]="16"></lucide-icon>
                  </button>
                  <button class="icon-btn text-rose-600" type="button" (click)="confirmDelete(payment)" aria-label="Betaling verwijderen">
                    <lucide-icon name="trash-2" [size]="16"></lucide-icon>
                  </button>
                </div>
              </td>
            </tr>
          </ng-template>
        </p-table>
      </div>

      <p-dialog [header]="form.controls.id.value ? 'Betaling bewerken' : 'Nieuwe betaling'" [(visible)]="dialogOpen" [modal]="true" [style]="{ width: 'min(620px, 94vw)' }">
        <form class="grid gap-4" [formGroup]="form" (ngSubmit)="save()">
          <label>
            <span class="mb-2 block text-sm font-semibold">Factuur</span>
            <p-dropdown [options]="invoices" formControlName="invoiceId" optionLabel="invoiceNumber" optionValue="id" placeholder="Selecteer factuur" styleClass="w-full"></p-dropdown>
          </label>
          <label>
            <span class="mb-2 block text-sm font-semibold">Bedrag</span>
            <p-inputNumber formControlName="amount" mode="currency" currency="EUR" locale="nl-NL" [min]="0.01" styleClass="w-full"></p-inputNumber>
          </label>
          <label>
            <span class="mb-2 block text-sm font-semibold">Betaaldatum</span>
            <p-calendar formControlName="paymentDate" dateFormat="dd-mm-yy" [showIcon]="true" styleClass="w-full"></p-calendar>
          </label>
          <label>
            <span class="mb-2 block text-sm font-semibold">Methode</span>
            <p-dropdown [options]="methodOptions" formControlName="paymentMethod" optionLabel="label" optionValue="value" styleClass="w-full"></p-dropdown>
          </label>
          <label>
            <span class="mb-2 block text-sm font-semibold">Notities</span>
            <textarea pInputTextarea rows="3" class="w-full" formControlName="notes"></textarea>
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
export class PaymentsComponent implements OnInit {
  private readonly fb = inject(FormBuilder);

  payments: Payment[] = [];
  invoices: Invoice[] = [];
  loading = false;
  dialogOpen = false;

  readonly methodOptions: SelectOption<PaymentMethod>[] = [
    { label: 'Bank', value: 'BANK' },
    { label: 'Contant', value: 'CASH' },
    { label: 'Pin', value: 'PIN' }
  ];

  readonly form = this.fb.group({
    id: [null as number | null],
    invoiceId: [null as number | null, Validators.required],
    amount: [0, [Validators.required, Validators.min(0.01)]],
    paymentDate: [new Date() as Date | null, Validators.required],
    paymentMethod: ['BANK' as PaymentMethod, Validators.required],
    notes: ['']
  });

  constructor(
    private readonly api: ApiService,
    private readonly confirmations: ConfirmationService,
    private readonly messages: MessageService
  ) {}

  ngOnInit(): void {
    this.load();
    this.api.invoices().subscribe((invoices) => this.invoices = invoices);
  }

  load(): void {
    this.loading = true;
    this.api.payments().subscribe({
      next: (payments) => this.payments = payments,
      complete: () => this.loading = false
    });
  }

  openCreate(): void {
    this.form.reset({ id: null, invoiceId: null, amount: 0, paymentDate: new Date(), paymentMethod: 'BANK', notes: '' });
    this.dialogOpen = true;
  }

  openEdit(payment: Payment): void {
    this.form.reset({
      id: payment.id,
      invoiceId: payment.invoiceId,
      amount: payment.amount,
      paymentDate: this.parseIso(payment.paymentDate),
      paymentMethod: payment.paymentMethod,
      notes: payment.notes ?? ''
    });
    this.dialogOpen = true;
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const raw = this.form.getRawValue();
    const payload = { ...raw, paymentDate: this.toIso(raw.paymentDate as Date) };
    this.api.savePayment(payload).subscribe({
      next: () => {
        this.messages.add({ severity: 'success', summary: 'Opgeslagen', detail: 'Betaling is opgeslagen.' });
        this.dialogOpen = false;
        this.load();
      }
    });
  }

  confirmDelete(payment: Payment): void {
    this.confirmations.confirm({
      message: `Weet u zeker dat u deze betaling voor ${payment.invoiceNumber} wilt verwijderen?`,
      header: 'Betaling verwijderen',
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: 'Verwijderen',
      rejectLabel: 'Annuleren',
      acceptButtonStyleClass: 'p-button-danger',
      accept: () => this.api.deletePayment(payment.id).subscribe(() => this.load())
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
