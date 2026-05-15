import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { MessageService } from 'primeng/api';
import { ApiService } from '../core/api.service';
import { Bezichtiging, BezichtigingStatus, SelectOption } from '../core/models';
import { PRIME_IMPORTS } from '../shared/prime-imports';

@Component({
  selector: 'app-bezichtigingen',
  standalone: true,
  imports: [...PRIME_IMPORTS],
  template: `
    <section class="space-y-6">
      <div class="flex flex-col justify-between gap-4 sm:flex-row sm:items-end">
        <div>
          <h1 class="page-title">Bezichtigingen</h1>
          <p class="muted">Plan rondleidingen, verstuur bevestigingen en volg de opvolging.</p>
        </div>
        <button pButton type="button" label="Nieuwe bezichtiging" icon="pi pi-plus" (click)="openCreate()"></button>
      </div>

      <div class="surface-panel rounded-md p-4">
        <div class="grid gap-3 md:grid-cols-[240px_minmax(0,1fr)]">
          <p-dropdown [options]="statusOptionsWithAll" [(ngModel)]="statusFilter" optionLabel="label" optionValue="value" placeholder="Status" styleClass="w-full" (onChange)="load()"></p-dropdown>
          <span class="p-input-icon-left">
            <i class="pi pi-search"></i>
            <input pInputText class="w-full" [(ngModel)]="search" placeholder="Zoek klantnaam" (input)="applySearch()">
          </span>
        </div>
      </div>

      <div class="surface-panel rounded-md p-2">
        <p-table [value]="filtered" [loading]="loading" [paginator]="true" [rows]="10" responsiveLayout="scroll">
          <ng-template pTemplate="header">
            <tr>
              <th>Klant</th>
              <th>Datum en tijden</th>
              <th>Status</th>
              <th>Notities</th>
              <th class="w-40">Acties</th>
            </tr>
          </ng-template>
          <ng-template pTemplate="body" let-item>
            <tr>
              <td>
                <p class="font-semibold">{{ item.klantNaam }}</p>
                <p class="muted">{{ item.klantEmail }}</p>
              </td>
              <td>
                <p>{{ item.datum | date:'dd MMM yyyy' }}</p>
                <p class="muted">{{ item.startTijd }} - {{ item.eindTijd }}</p>
              </td>
              <td><p-tag [value]="item.status" [severity]="statusSeverity(item.status)"></p-tag></td>
              <td class="max-w-sm truncate">{{ item.notities || '-' }}</td>
              <td>
                <div class="flex gap-2">
                  <button class="icon-btn" type="button" aria-label="Details" (click)="openDetail(item)">
                    <lucide-icon name="eye" [size]="16"></lucide-icon>
                  </button>
                  <button class="icon-btn" type="button" aria-label="Bewerken" (click)="openEdit(item)">
                    <lucide-icon name="edit" [size]="16"></lucide-icon>
                  </button>
                </div>
              </td>
            </tr>
          </ng-template>
        </p-table>
      </div>

      <p-dialog [header]="form.controls.id.value ? 'Bezichtiging bewerken' : 'Nieuwe bezichtiging'" [(visible)]="dialogOpen" [modal]="true" [style]="{ width: 'min(720px, 94vw)' }">
        <form class="grid gap-4 md:grid-cols-2" [formGroup]="form" (ngSubmit)="save()">
          <label>
            <span class="mb-2 block text-sm font-semibold">Klant naam</span>
            <input pInputText class="w-full" formControlName="klantNaam">
          </label>
          <label>
            <span class="mb-2 block text-sm font-semibold">E-mail</span>
            <input pInputText class="w-full" type="email" formControlName="klantEmail">
          </label>
          <label>
            <span class="mb-2 block text-sm font-semibold">Telefoon</span>
            <input pInputText class="w-full" formControlName="klantTelefoon">
          </label>
          <label>
            <span class="mb-2 block text-sm font-semibold">Datum</span>
            <p-calendar formControlName="datum" dateFormat="dd-mm-yy" [showIcon]="true" styleClass="w-full"></p-calendar>
          </label>
          <label>
            <span class="mb-2 block text-sm font-semibold">Start tijd</span>
            <input pInputText class="w-full" type="time" formControlName="startTijd">
          </label>
          <label>
            <span class="mb-2 block text-sm font-semibold">Eind tijd</span>
            <input pInputText class="w-full" type="time" formControlName="eindTijd">
          </label>
          <label class="md:col-span-2">
            <span class="mb-2 block text-sm font-semibold">Notities</span>
            <textarea pInputTextarea rows="4" class="w-full" formControlName="notities"></textarea>
          </label>
          <div class="flex justify-end gap-2 md:col-span-2">
            <button pButton type="button" label="Annuleren" class="p-button-text" (click)="dialogOpen = false"></button>
            <button pButton type="submit" label="Opslaan" icon="pi pi-save" [disabled]="form.invalid"></button>
          </div>
        </form>
      </p-dialog>
    </section>
  `
})
export class BezichtigingenComponent implements OnInit {
  private readonly fb = inject(FormBuilder);

  bezichtigingen: Bezichtiging[] = [];
  filtered: Bezichtiging[] = [];
  loading = false;
  dialogOpen = false;
  search = '';
  statusFilter: BezichtigingStatus | null = null;

  readonly statusOptions: SelectOption<BezichtigingStatus>[] = [
    { label: 'Gepland', value: 'GEPLAND' },
    { label: 'Geweest', value: 'GEWEEST' },
    { label: 'Geannuleerd', value: 'GEANNULEERD' }
  ];
  readonly statusOptionsWithAll = [{ label: 'Alle statussen', value: null }, ...this.statusOptions];

  readonly form = this.fb.group({
    id: [null as number | null],
    klantNaam: ['', Validators.required],
    klantEmail: ['', [Validators.required, Validators.email]],
    klantTelefoon: ['', Validators.required],
    datum: [null as Date | null, Validators.required],
    startTijd: ['10:00', Validators.required],
    eindTijd: ['11:00', Validators.required],
    notities: ['']
  });

  constructor(
    private readonly api: ApiService,
    private readonly router: Router,
    private readonly messages: MessageService
  ) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.api.bezichtigingen(this.statusFilter).subscribe({
      next: (items) => {
        this.bezichtigingen = items;
        this.applySearch();
      },
      complete: () => this.loading = false
    });
  }

  applySearch(): void {
    const term = this.search.trim().toLowerCase();
    this.filtered = this.bezichtigingen.filter((item) => !term || item.klantNaam.toLowerCase().includes(term));
  }

  openCreate(): void {
    this.form.reset({
      id: null,
      klantNaam: '',
      klantEmail: '',
      klantTelefoon: '',
      datum: new Date(),
      startTijd: '10:00',
      eindTijd: '11:00',
      notities: ''
    });
    this.dialogOpen = true;
  }

  openEdit(item: Bezichtiging): void {
    this.form.reset({
      id: item.id,
      klantNaam: item.klantNaam,
      klantEmail: item.klantEmail,
      klantTelefoon: item.klantTelefoon,
      datum: this.parseIso(item.datum),
      startTijd: item.startTijd?.slice(0, 5) ?? '10:00',
      eindTijd: item.eindTijd?.slice(0, 5) ?? '11:00',
      notities: item.notities ?? ''
    });
    this.dialogOpen = true;
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const raw = this.form.getRawValue();
    const payload = {
      id: raw.id,
      klantNaam: raw.klantNaam,
      klantEmail: raw.klantEmail,
      klantTelefoon: raw.klantTelefoon,
      datum: this.toIso(raw.datum as Date),
      startTijd: raw.startTijd,
      eindTijd: raw.eindTijd,
      notities: raw.notities
    };
    this.api.saveBezichtiging(payload).subscribe({
      next: () => {
        this.messages.add({ severity: 'success', summary: 'Bezichtiging opgeslagen', detail: 'De bevestiging met agenda-uitnodiging is verwerkt.' });
        this.dialogOpen = false;
        this.load();
      },
      error: (error) => this.messages.add({ severity: 'error', summary: 'Niet opgeslagen', detail: error.error?.message ?? 'Controleer de invoer.' })
    });
  }

  openDetail(item: Bezichtiging): void {
    void this.router.navigate(['/bezichtigingen', item.id]);
  }

  statusSeverity(status: BezichtigingStatus): 'info' | 'success' | 'danger' {
    return ({ GEPLAND: 'info', GEWEEST: 'success', GEANNULEERD: 'danger' } as const)[status];
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
