import { Component, OnInit, inject } from '@angular/core';
import { FormArray, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { Router } from '@angular/router';
import { ConfirmationService, MessageService } from 'primeng/api';
import { switchMap } from 'rxjs';
import { ApiService } from '../core/api.service';
import { AuthService } from '../core/auth.service';
import { Booking, BookingStatus, Customer, EventType, SelectOption, SubPrijs } from '../core/models';
import { PRIME_IMPORTS } from '../shared/prime-imports';

@Component({
  selector: 'app-bookings',
  standalone: true,
  imports: [...PRIME_IMPORTS],
  template: `
    <section class="space-y-6">
      <div class="flex flex-col justify-between gap-4 sm:flex-row sm:items-end">
        <div>
          <h1 class="page-title">Boekingen</h1>
          <p class="muted">Maak, filter en beheer alle zaalreserveringen.</p>
        </div>
        <button pButton type="button" label="Nieuwe boeking" icon="pi pi-plus" (click)="openCreate()"></button>
      </div>

      <div class="surface-panel rounded-md p-4">
        <div class="grid gap-3 md:grid-cols-5">
          <span class="p-input-icon-left">
            <i class="pi pi-search"></i>
            <input pInputText class="w-full" [(ngModel)]="filters.customer" placeholder="Zoek klant" (input)="load()">
          </span>
          <p-dropdown [options]="statusOptionsWithAll" [(ngModel)]="filters.status" optionLabel="label" optionValue="value" placeholder="Status" styleClass="w-full" (onChange)="load()"></p-dropdown>
          <p-dropdown [options]="eventOptionsWithAll" [(ngModel)]="filters.eventType" optionLabel="label" optionValue="value" placeholder="Type" styleClass="w-full" (onChange)="load()"></p-dropdown>
          <p-calendar [(ngModel)]="filters.startDate" dateFormat="dd-mm-yy" placeholder="Vanaf" styleClass="w-full" (onSelect)="load()" [showIcon]="true"></p-calendar>
          <p-calendar [(ngModel)]="filters.endDate" dateFormat="dd-mm-yy" placeholder="Tot" styleClass="w-full" (onSelect)="load()" [showIcon]="true"></p-calendar>
        </div>
      </div>

      <div class="surface-panel rounded-md p-2">
        <p-table [value]="bookings" [loading]="loading" [paginator]="true" [rows]="10" responsiveLayout="scroll">
          <ng-template pTemplate="header">
            <tr>
              <th>Klant</th>
              <th>Datum en tijd</th>
              <th>Type</th>
              <th>Gasten</th>
              <th>Prijs</th>
              <th>Status</th>
              <th class="min-w-52 text-right">Acties</th>
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
                <p class="muted">{{ booking.startTime }} - {{ booking.endTime }}</p>
              </td>
              <td>{{ booking.eventType }}</td>
              <td>{{ booking.guestCount }}</td>
              <td>{{ booking.price | currency:'EUR' }}</td>
              <td>
                <p-dropdown
                  [options]="statusOptions"
                  [ngModel]="booking.status"
                  optionLabel="label"
                  optionValue="value"
                  styleClass="min-w-56"
                  (onChange)="changeStatus(booking, $event.value)"
                ></p-dropdown>
              </td>
              <td class="min-w-52">
                <div class="flex flex-nowrap justify-end gap-2">
                  <button class="icon-btn" type="button" (click)="openDetail(booking)" aria-label="Boeking details">
                    <lucide-icon name="eye" [size]="16"></lucide-icon>
                  </button>
                  <button class="icon-btn" type="button" (click)="openEdit(booking)" aria-label="Boeking bewerken">
                    <lucide-icon name="edit" [size]="16"></lucide-icon>
                  </button>
                  @if (auth.isOwner && booking.status !== 'GEANNULEERD') {
                    <button class="icon-btn text-rose-600" type="button" (click)="openCancelDialog(booking)" aria-label="Boeking annuleren">
                      <lucide-icon name="x" [size]="16"></lucide-icon>
                    </button>
                  }
                  @if (auth.isOwner) {
                    <button class="icon-btn text-rose-600" type="button" (click)="confirmDelete(booking)" aria-label="Boeking verwijderen">
                      <lucide-icon name="trash-2" [size]="16"></lucide-icon>
                    </button>
                  }
                </div>
              </td>
            </tr>
          </ng-template>
        </p-table>
      </div>

      <p-dialog [header]="form.controls.id.value ? 'Boeking bewerken' : 'Nieuwe boeking'" [(visible)]="dialogOpen" [modal]="true" [style]="{ width: 'min(980px, 94vw)' }">
        <form class="grid gap-4 md:grid-cols-2" [formGroup]="form" (ngSubmit)="save()">
          <section class="md:col-span-2 rounded-md border border-slate-200 p-4 dark:border-slate-800">
            <div class="flex flex-wrap items-center justify-between gap-3">
              <div>
                <h2 class="font-bold">Klant</h2>
                <p class="muted">Kies een bestaande klant of maak direct een nieuwe klant aan.</p>
              </div>
              <button pButton type="button" class="p-button-secondary" [label]="creatingCustomer ? 'Bestaande klant' : 'Nieuwe klant'" (click)="toggleCustomerMode()"></button>
            </div>

            @if (!creatingCustomer) {
              <label class="mt-4 block">
                <span class="mb-2 block text-sm font-semibold">Bestaande klant</span>
                <p-dropdown [options]="customers" formControlName="customerId" optionLabel="name" optionValue="id" placeholder="Selecteer klant" styleClass="w-full"></p-dropdown>
              </label>
            } @else {
              <div class="mt-4 grid gap-4 md:grid-cols-2" formGroupName="newCustomer">
                <label>
                  <span class="mb-2 block text-sm font-semibold">Naam</span>
                  <input pInputText class="w-full" formControlName="name">
                </label>
                <label>
                  <span class="mb-2 block text-sm font-semibold">E-mail</span>
                  <input pInputText class="w-full" type="email" formControlName="email">
                </label>
                <label>
                  <span class="mb-2 block text-sm font-semibold">Telefoon</span>
                  <input pInputText class="w-full" formControlName="phone">
                </label>
                <label>
                  <span class="mb-2 block text-sm font-semibold">Adres</span>
                  <input pInputText class="w-full" formControlName="address">
                </label>
              </div>
            }
          </section>

          <label class="block">
            <span class="mb-2 block text-sm font-semibold">Evenementdatum</span>
            <p-calendar formControlName="eventDate" dateFormat="dd-mm-yy" [showIcon]="true" styleClass="w-full"></p-calendar>
          </label>
          <div class="grid grid-cols-2 gap-3">
            <label class="block">
              <span class="mb-2 block text-sm font-semibold">Begintijd</span>
              <input pInputText class="w-full" type="time" formControlName="startTime">
            </label>
            <label class="block">
              <span class="mb-2 block text-sm font-semibold">Eindtijd</span>
              <input pInputText class="w-full" type="time" formControlName="endTime">
            </label>
          </div>
          <label class="block">
            <span class="mb-2 block text-sm font-semibold">Type evenement</span>
            <p-dropdown [options]="eventOptions" formControlName="eventType" optionLabel="label" optionValue="value" styleClass="w-full"></p-dropdown>
          </label>
          <label class="block">
            <span class="mb-2 block text-sm font-semibold">Status</span>
            <p-dropdown [options]="statusOptions" formControlName="status" optionLabel="label" optionValue="value" styleClass="w-full"></p-dropdown>
          </label>
          <label class="block">
            <span class="mb-2 block text-sm font-semibold">Aantal gasten</span>
            <p-inputNumber formControlName="guestCount" [min]="1" styleClass="w-full"></p-inputNumber>
          </label>
          <label class="block">
            <span class="mb-2 block text-sm font-semibold">Korting</span>
            <p-inputNumber formControlName="korting" mode="currency" currency="EUR" locale="nl-NL" [min]="0" styleClass="w-full"></p-inputNumber>
          </label>
          <label class="block">
            <span class="mb-2 block text-sm font-semibold">Aanbetaling %</span>
            <p-inputNumber formControlName="aanbetalingPercentage" [min]="0" [max]="100" suffix="%" styleClass="w-full"></p-inputNumber>
          </label>

          <section class="md:col-span-2 rounded-md border border-slate-200 p-4 dark:border-slate-800" formArrayName="subPrijzen">
            <div class="flex flex-wrap items-center justify-between gap-3">
              <div>
                <h2 class="font-bold">Subprijzen</h2>
                <p class="muted">Het totaal wordt automatisch berekend uit deze regels.</p>
              </div>
              <button pButton type="button" label="Regel toevoegen" icon="pi pi-plus" class="p-button-secondary" (click)="addSubPrijs()"></button>
            </div>

            <div class="mt-4 space-y-3">
              @for (group of subPrijzen.controls; track $index; let index = $index) {
                <div class="grid gap-3 rounded-md bg-slate-50 p-3 dark:bg-slate-950 md:grid-cols-[minmax(0,1fr)_180px_44px]" [formGroupName]="index">
                  <label>
                    <span class="mb-2 block text-sm font-semibold">Naam</span>
                    <input pInputText class="w-full" formControlName="naam" placeholder="Bijv. zaalhuur">
                  </label>
                  <label>
                    <span class="mb-2 block text-sm font-semibold">Prijs</span>
                    <p-inputNumber formControlName="prijs" mode="currency" currency="EUR" locale="nl-NL" [min]="0" styleClass="w-full"></p-inputNumber>
                  </label>
                  <button class="icon-btn self-end text-rose-600" type="button" (click)="removeSubPrijs(index)" [disabled]="subPrijzen.length === 1" aria-label="Subprijs verwijderen">
                    <lucide-icon name="trash-2" [size]="16"></lucide-icon>
                  </button>
                </div>
              }
            </div>

            <div class="mt-4 flex justify-end">
              <div class="w-full max-w-sm rounded-md bg-slate-950 px-4 py-3 text-white dark:bg-white dark:text-slate-950">
                <div class="flex justify-between text-sm"><span>Subtotaal</span><strong>{{ subtotal | currency:'EUR' }}</strong></div>
                <div class="flex justify-between text-sm"><span>Korting</span><strong>-{{ discount | currency:'EUR' }}</strong></div>
                <div class="mt-2 flex justify-between border-t border-white/20 pt-2 text-lg font-black"><span>Totaal</span><span>{{ totalPrice | currency:'EUR' }}</span></div>
                <div class="mt-2 grid grid-cols-2 gap-2 text-xs">
                  <span>Aanbetaling {{ depositPercentage }}%<br><strong>{{ depositAmount | currency:'EUR' }}</strong></span>
                  <span>Restant<br><strong>{{ remainderAmount | currency:'EUR' }}</strong></span>
                </div>
              </div>
            </div>
          </section>

          <section class="md:col-span-2">
            <span class="mb-2 block text-sm font-semibold">Eigenschappen</span>
            <div class="grid gap-2 md:grid-cols-2">
              @for (property of allProperties; track property) {
                <label class="flex items-center gap-2 rounded-md border border-slate-200 px-3 py-2 text-sm dark:border-slate-800">
                  <input type="checkbox" [checked]="selectedProperties.has(property)" (change)="toggleProperty(property, $event)">
                  <span>{{ property }}</span>
                </label>
              }
            </div>
            <div class="mt-3 flex flex-col gap-2 sm:flex-row">
              <input pInputText class="w-full" [(ngModel)]="customProperty" [ngModelOptions]="{ standalone: true }" placeholder="Eigen eigenschap toevoegen">
              <button pButton type="button" label="Toevoegen" class="p-button-secondary" (click)="addCustomProperty()"></button>
            </div>
          </section>

          <label class="block md:col-span-2">
            <span class="mb-2 block text-sm font-semibold">Extra voorwaarden</span>
            <textarea pInputTextarea rows="4" class="w-full" formControlName="conditions"></textarea>
          </label>
          <label class="block md:col-span-2">
            <span class="mb-2 block text-sm font-semibold">Notities</span>
            <textarea pInputTextarea rows="3" class="w-full" formControlName="notes"></textarea>
          </label>
          <div class="flex justify-end gap-2 md:col-span-2">
            <button pButton type="button" label="Annuleren" class="p-button-text" (click)="dialogOpen = false"></button>
            <button pButton type="submit" label="Opslaan" icon="pi pi-save" [disabled]="!canSave"></button>
          </div>
        </form>
      </p-dialog>

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
  `
})
export class BookingsComponent implements OnInit {
  private readonly fb = inject(FormBuilder);

  bookings: Booking[] = [];
  customers: Customer[] = [];
  loading = false;
  dialogOpen = false;
  creatingCustomer = false;
  cancelDialogOpen = false;
  cancelReason = '';
  bookingToCancel: Booking | null = null;
  prefillBezichtigingId: number | null = null;
  customProperty = '';
  selectedProperties = new Set<string>();
  readonly defaultProperties = [
    'Catering inbegrepen',
    'Decoratie inbegrepen',
    'DJ niet inbegrepen',
    'Parkeren gratis',
    'Bruidskamer beschikbaar'
  ];
  allProperties = [...this.defaultProperties];

  filters: {
    status: BookingStatus | null;
    eventType: EventType | null;
    startDate: Date | null;
    endDate: Date | null;
    customer: string;
  } = { status: null, eventType: null, startDate: null, endDate: null, customer: '' };

  readonly statusOptions: SelectOption<BookingStatus>[] = [
    { label: 'Concept', value: 'CONCEPT' },
    { label: 'Offerte verzonden', value: 'OFFERTE_VERZONDEN' },
    { label: 'Bevestigd', value: 'BEVESTIGD' },
    { label: 'Aanbetaling betaald', value: 'AANBETALING_BETAALD' },
    { label: 'Volledig betaald', value: 'VOLLEDIG_BETAALD' },
    { label: 'Afgerond', value: 'AFGEROND' },
    { label: 'Geannuleerd', value: 'GEANNULEERD' }
  ];
  readonly statusOptionsWithAll = [{ label: 'Alle statussen', value: null }, ...this.statusOptions];
  readonly eventOptions: SelectOption<EventType>[] = [
    { label: 'Bruiloft', value: 'BRUILOFT' },
    { label: 'Verjaardag', value: 'VERJAARDAG' },
    { label: 'Congres', value: 'CONGRES' },
    { label: 'Overig', value: 'OVERIG' }
  ];
  readonly eventOptionsWithAll = [{ label: 'Alle types', value: null }, ...this.eventOptions];

  readonly form = this.fb.group({
    id: [null as number | null],
    customerId: [null as number | null],
    newCustomer: this.fb.group({
      name: [''],
      email: [''],
      phone: [''],
      address: ['']
    }),
    eventDate: [null as Date | null, Validators.required],
    startTime: ['18:00', Validators.required],
    endTime: ['23:00', Validators.required],
    eventType: ['BRUILOFT' as EventType, Validators.required],
    guestCount: [50, [Validators.required, Validators.min(1)]],
    korting: [0, [Validators.required, Validators.min(0)]],
    aanbetalingPercentage: [30, [Validators.required, Validators.min(0), Validators.max(100)]],
    status: ['CONCEPT' as BookingStatus, Validators.required],
    subPrijzen: this.fb.array<FormGroup>([]),
    conditions: [''],
    notes: ['']
  });

  constructor(
    public readonly auth: AuthService,
    private readonly api: ApiService,
    private readonly confirmations: ConfirmationService,
    private readonly messages: MessageService,
    private readonly router: Router,
    private readonly route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    this.load();
    this.loadCustomers();
    this.route.queryParamMap.subscribe((params) => {
      if (params.has('klantNaam') || params.has('bezichtigingId')) {
        this.prefillBezichtigingId = Number(params.get('bezichtigingId')) || null;
        this.openCreate();
        this.creatingCustomer = true;
        this.form.controls.newCustomer.patchValue({
          name: params.get('klantNaam') ?? '',
          email: params.get('klantEmail') ?? '',
          phone: params.get('klantTelefoon') ?? '',
          address: ''
        });
      }
    });
  }

  get subPrijzen(): FormArray<FormGroup> {
    return this.form.controls.subPrijzen;
  }

  get totalPrice(): number {
    return Math.max(0, this.subtotal - this.discount);
  }

  get subtotal(): number {
    return this.subPrijzen.controls.reduce((sum, group) => sum + Number(group.get('prijs')?.value ?? 0), 0);
  }

  get discount(): number {
    return Number(this.form.controls.korting.value ?? 0);
  }

  get depositPercentage(): number {
    return Number(this.form.controls.aanbetalingPercentage.value ?? 30);
  }

  get depositAmount(): number {
    return this.totalPrice * this.depositPercentage / 100;
  }

  get remainderAmount(): number {
    return this.totalPrice - this.depositAmount;
  }

  get canSave(): boolean {
    if (this.form.invalid || this.subPrijzen.length === 0) {
      return false;
    }
    if (this.creatingCustomer) {
      const customer = this.form.controls.newCustomer.getRawValue();
      return Boolean(customer.name?.trim() && customer.email?.trim() && customer.phone?.trim());
    }
    return Boolean(this.form.controls.customerId.value);
  }

  load(): void {
    this.loading = true;
    this.api.bookings({
      status: this.filters.status,
      eventType: this.filters.eventType,
      startDate: this.filters.startDate ? this.toIso(this.filters.startDate) : null,
      endDate: this.filters.endDate ? this.toIso(this.filters.endDate) : null,
      customer: this.filters.customer
    }).subscribe({
      next: (bookings) => this.bookings = bookings,
      complete: () => this.loading = false
    });
  }

  openCreate(): void {
    this.creatingCustomer = false;
    this.selectedProperties = new Set<string>();
    this.allProperties = [...this.defaultProperties];
    this.subPrijzen.clear();
    this.subPrijzen.push(this.createSubPrijsGroup({ naam: 'Huur evenementenlocatie', prijs: 0, position: 0 }));
    this.form.reset({
      id: null,
      customerId: null,
      newCustomer: { name: '', email: '', phone: '', address: '' },
      eventDate: new Date(),
      startTime: '18:00',
      endTime: '23:00',
      eventType: 'BRUILOFT',
      guestCount: 50,
      korting: 0,
      aanbetalingPercentage: 30,
      status: 'CONCEPT',
      conditions: '',
      notes: ''
    });
    this.dialogOpen = true;
  }

  openEdit(booking: Booking): void {
    this.creatingCustomer = false;
    this.selectedProperties = new Set(booking.properties ?? []);
    this.allProperties = Array.from(new Set([...this.defaultProperties, ...this.selectedProperties]));
    this.subPrijzen.clear();
    const subPrijzen = booking.subPrijzen?.length
      ? booking.subPrijzen
      : [{ naam: 'Huur evenementenlocatie', prijs: booking.price, position: 0 }];
    subPrijzen.forEach((subPrijs, index) => this.subPrijzen.push(this.createSubPrijsGroup({ ...subPrijs, position: subPrijs.position ?? index })));
    this.form.reset({
      id: booking.id,
      customerId: booking.customerId,
      newCustomer: { name: '', email: '', phone: '', address: '' },
      eventDate: this.parseIso(booking.eventDate),
      startTime: booking.startTime?.slice(0, 5) ?? '18:00',
      endTime: booking.endTime?.slice(0, 5) ?? '23:00',
      eventType: booking.eventType,
      guestCount: booking.guestCount,
      korting: booking.korting ?? 0,
      aanbetalingPercentage: booking.aanbetalingPercentage ?? 30,
      status: booking.status,
      conditions: booking.conditions ?? '',
      notes: booking.notes ?? ''
    });
    this.dialogOpen = true;
  }

  save(): void {
    if (!this.canSave) {
      this.form.markAllAsTouched();
      return;
    }

    if (this.creatingCustomer) {
      this.api.saveCustomer(this.form.controls.newCustomer.getRawValue()).pipe(
        switchMap((customer) => this.api.saveBooking(this.buildBookingPayload(customer.id)))
      ).subscribe({
        next: (booking) => this.afterBookingSaved(booking),
        error: (error) => this.saveError(error)
      });
      return;
    }

    this.api.saveBooking(this.buildBookingPayload(this.form.controls.customerId.value as number)).subscribe({
      next: (booking) => this.afterBookingSaved(booking),
      error: (error) => this.saveError(error)
    });
  }

  changeStatus(booking: Booking, status: BookingStatus): void {
    this.api.updateBookingStatus(booking.id, status).subscribe({
      next: () => {
        this.messages.add({ severity: 'success', summary: 'Status gewijzigd', detail: 'Boeking is bijgewerkt.' });
        this.load();
      },
      error: (error) => this.messages.add({ severity: 'error', summary: 'Status niet gewijzigd', detail: error.error?.message ?? 'Controleer de status.' })
    });
  }

  toggleCustomerMode(): void {
    this.creatingCustomer = !this.creatingCustomer;
    if (this.creatingCustomer) {
      this.form.controls.customerId.setValue(null);
    }
  }

  addSubPrijs(): void {
    this.subPrijzen.push(this.createSubPrijsGroup({ naam: '', prijs: 0, position: this.subPrijzen.length }));
  }

  removeSubPrijs(index: number): void {
    if (this.subPrijzen.length > 1) {
      this.subPrijzen.removeAt(index);
    }
  }

  toggleProperty(property: string, event: Event): void {
    const checked = (event.target as HTMLInputElement).checked;
    if (checked) {
      this.selectedProperties.add(property);
    } else {
      this.selectedProperties.delete(property);
    }
  }

  addCustomProperty(): void {
    const value = this.customProperty.trim();
    if (!value) {
      return;
    }
    this.allProperties = Array.from(new Set([...this.allProperties, value]));
    this.selectedProperties.add(value);
    this.customProperty = '';
  }

  openDetail(booking: Booking): void {
    void this.router.navigate(['/boekingen', booking.id]);
  }

  openCancelDialog(booking: Booking): void {
    this.bookingToCancel = booking;
    this.cancelReason = '';
    this.cancelDialogOpen = true;
  }

  cancelBooking(): void {
    if (!this.bookingToCancel || !this.cancelReason.trim()) {
      return;
    }
    this.api.cancelBooking(this.bookingToCancel.id, this.cancelReason.trim()).subscribe({
      next: () => {
        this.messages.add({ severity: 'success', summary: 'Geannuleerd', detail: 'Boeking is geannuleerd en de klant is gemaild.' });
        this.cancelDialogOpen = false;
        this.load();
      },
      error: (error) => this.messages.add({ severity: 'error', summary: 'Niet geannuleerd', detail: error.error?.message ?? 'Controleer de reden.' })
    });
  }

  confirmDelete(booking: Booking): void {
    this.confirmations.confirm({
      message: `Weet u zeker dat u de boeking van ${booking.customerName} wilt verwijderen?`,
      header: 'Boeking verwijderen',
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: 'Verwijderen',
      rejectLabel: 'Annuleren',
      acceptButtonStyleClass: 'p-button-danger',
      accept: () => this.api.deleteBooking(booking.id).subscribe(() => this.load())
    });
  }

  contractSeverity(status: string): 'success' | 'info' | 'warn' | 'secondary' {
    return {
      GEEN: 'secondary',
      CONCEPT: 'warn',
      VERZONDEN: 'info',
      ONDERTEKEND: 'success'
    }[status] as 'success' | 'info' | 'warn' | 'secondary';
  }

  private loadCustomers(): void {
    this.api.customers().subscribe((customers) => this.customers = customers);
  }

  private createSubPrijsGroup(subPrijs: Partial<SubPrijs>): FormGroup {
    return this.fb.group({
      id: [subPrijs.id ?? null],
      naam: [subPrijs.naam ?? '', Validators.required],
      prijs: [subPrijs.prijs ?? 0, [Validators.required, Validators.min(0)]],
      position: [subPrijs.position ?? 0]
    });
  }

  private buildBookingPayload(customerId: number): Record<string, unknown> {
    const raw = this.form.getRawValue();
    const eventDate = this.toIso(raw.eventDate as Date);
    const subPrijzen = raw.subPrijzen.map((subPrijs, index) => ({
      id: subPrijs['id'],
      naam: subPrijs['naam'],
      bedrag: subPrijs['prijs'],
      prijs: subPrijs['prijs'],
      position: index
    }));
    return {
      id: raw.id,
      customerId,
      evenementDatum: eventDate,
      eventDate,
      date: eventDate,
      endDate: eventDate,
      startTijd: raw.startTime,
      startTime: raw.startTime,
      eindTijd: raw.endTime,
      endTime: raw.endTime,
      evenementType: raw.eventType,
      eventType: raw.eventType,
      aantalGasten: raw.guestCount,
      guestCount: raw.guestCount,
      price: this.subtotal,
      korting: raw.korting,
      aanbetalingPercentage: raw.aanbetalingPercentage,
      status: raw.status,
      subPrijzen,
      properties: Array.from(this.selectedProperties),
      conditions: raw.conditions,
      notes: raw.notes
    };
  }

  private afterSave(): void {
    this.messages.add({ severity: 'success', summary: 'Opgeslagen', detail: 'Boeking is opgeslagen.' });
    this.dialogOpen = false;
    this.loadCustomers();
    this.load();
  }

  private afterBookingSaved(booking: Booking): void {
    if (this.prefillBezichtigingId) {
      this.api.linkBezichtigingToBoeking(this.prefillBezichtigingId, booking.id).subscribe();
      this.prefillBezichtigingId = null;
    }
    this.afterSave();
  }

  private saveError(error: { error?: { message?: string } }): void {
    this.messages.add({ severity: 'error', summary: 'Niet opgeslagen', detail: error.error?.message ?? 'Controleer de invoer.' });
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
