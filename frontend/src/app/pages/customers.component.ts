import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { ConfirmationService, MessageService } from 'primeng/api';
import { ApiService } from '../core/api.service';
import { Booking, Customer } from '../core/models';
import { PRIME_IMPORTS } from '../shared/prime-imports';

@Component({
  selector: 'app-customers',
  standalone: true,
  imports: [...PRIME_IMPORTS],
  template: `
    <section class="space-y-6">
      <div class="flex flex-col justify-between gap-4 sm:flex-row sm:items-end">
        <div>
          <h1 class="page-title">Klanten</h1>
          <p class="muted">Klantgegevens en boekingshistorie op een plek.</p>
        </div>
        <button pButton type="button" label="Nieuwe klant" icon="pi pi-plus" (click)="openCreate()"></button>
      </div>

      <div class="grid gap-4 xl:grid-cols-[1fr_420px]">
        <div class="surface-panel rounded-md p-2">
          <div class="p-3">
            <span class="p-input-icon-left w-full">
              <i class="pi pi-search"></i>
              <input pInputText class="w-full" [(ngModel)]="search" placeholder="Zoek op naam of e-mail" (input)="load()">
            </span>
          </div>
          <p-table [value]="customers" [loading]="loading" [paginator]="true" [rows]="10" selectionMode="single" [(selection)]="selected" (onRowSelect)="loadHistory()">
            <ng-template pTemplate="header">
              <tr>
                <th>Naam</th>
                <th>E-mail</th>
                <th>Telefoon</th>
                <th class="w-32">Acties</th>
              </tr>
            </ng-template>
            <ng-template pTemplate="body" let-customer>
              <tr [pSelectableRow]="customer">
                <td class="font-semibold">{{ customer.name }}</td>
                <td>{{ customer.email }}</td>
                <td>{{ customer.phone }}</td>
                <td>
                  <div class="flex gap-2">
                    <button class="icon-btn" type="button" (click)="openEdit(customer)" aria-label="Klant bewerken">
                      <lucide-icon name="edit" [size]="16"></lucide-icon>
                    </button>
                    <button class="icon-btn text-rose-600" type="button" (click)="confirmDelete(customer)" aria-label="Klant verwijderen">
                      <lucide-icon name="trash-2" [size]="16"></lucide-icon>
                    </button>
                  </div>
                </td>
              </tr>
            </ng-template>
          </p-table>
        </div>

        <aside class="surface-panel rounded-md p-5">
          @if (selected) {
            <p class="muted">Klantdetail</p>
            <h2 class="mt-1 text-xl font-bold">{{ selected.name }}</h2>
            <div class="mt-4 space-y-2 text-sm">
              <p><strong>E-mail:</strong> {{ selected.email }}</p>
              <p><strong>Telefoon:</strong> {{ selected.phone }}</p>
              <p><strong>Adres:</strong> {{ selected.address || 'Niet ingevuld' }}</p>
            </div>
            <h3 class="mt-6 font-bold">Boekingshistorie</h3>
            <div class="mt-3 space-y-3">
              @for (booking of history; track booking.id) {
                <article class="rounded-md border border-slate-200 p-3 dark:border-slate-800">
                  <p class="font-semibold">{{ (booking.eventDate || booking.date) | date:'dd MMM yyyy' }}</p>
                  <p class="muted">{{ booking.eventType }} · {{ booking.status }} · {{ booking.price | currency:'EUR' }}</p>
                </article>
              } @empty {
                <p class="muted">Nog geen boekingen voor deze klant.</p>
              }
            </div>
          } @else {
            <p class="muted">Selecteer een klant om de historie te bekijken.</p>
          }
        </aside>
      </div>

      <p-dialog [header]="form.controls.id.value ? 'Klant bewerken' : 'Nieuwe klant'" [(visible)]="dialogOpen" [modal]="true" [style]="{ width: 'min(620px, 94vw)' }">
        <form class="grid gap-4" [formGroup]="form" (ngSubmit)="save()">
          <label>
            <span class="mb-2 block text-sm font-semibold">Naam</span>
            <input pInputText class="w-full" formControlName="name">
          </label>
          <label>
            <span class="mb-2 block text-sm font-semibold">E-mail</span>
            <input pInputText class="w-full" type="text" inputmode="email" formControlName="email">
          </label>
          <label>
            <span class="mb-2 block text-sm font-semibold">Telefoon</span>
            <input pInputText class="w-full" formControlName="phone">
          </label>
          <label>
            <span class="mb-2 block text-sm font-semibold">Adres</span>
            <input pInputText class="w-full" formControlName="address">
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
export class CustomersComponent implements OnInit {
  private readonly fb = inject(FormBuilder);

  customers: Customer[] = [];
  history: Booking[] = [];
  selected: Customer | null = null;
  search = '';
  loading = false;
  dialogOpen = false;

  readonly form = this.fb.group({
    id: [null as number | null],
    name: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
    phone: ['', Validators.required],
    address: ['']
  });

  constructor(
    private readonly api: ApiService,
    private readonly confirmations: ConfirmationService,
    private readonly messages: MessageService
  ) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.api.customers(this.search).subscribe({
      next: (customers) => this.customers = customers,
      complete: () => this.loading = false
    });
  }

  loadHistory(): void {
    if (!this.selected) {
      return;
    }
    this.api.customerBookings(this.selected.id).subscribe((history) => this.history = history);
  }

  openCreate(): void {
    this.form.reset({ id: null, name: '', email: '', phone: '', address: '' });
    this.dialogOpen = true;
  }

  openEdit(customer: Customer): void {
    this.form.reset({
      id: customer.id,
      name: customer.name,
      email: customer.email,
      phone: customer.phone,
      address: customer.address ?? ''
    });
    this.dialogOpen = true;
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.api.saveCustomer(this.form.getRawValue() as Record<string, unknown>).subscribe({
      next: () => {
        this.messages.add({ severity: 'success', summary: 'Opgeslagen', detail: 'Klant is opgeslagen.' });
        this.dialogOpen = false;
        this.load();
      }
    });
  }

  confirmDelete(customer: Customer): void {
    this.confirmations.confirm({
      message: `Weet u zeker dat u ${customer.name} wilt verwijderen?`,
      header: 'Klant verwijderen',
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: 'Verwijderen',
      rejectLabel: 'Annuleren',
      acceptButtonStyleClass: 'p-button-danger',
      accept: () => this.api.deleteCustomer(customer.id).subscribe(() => this.load())
    });
  }
}
