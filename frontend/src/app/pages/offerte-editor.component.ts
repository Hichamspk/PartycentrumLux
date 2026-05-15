import { Component, OnDestroy, OnInit } from '@angular/core';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { ActivatedRoute, Router } from '@angular/router';
import { MessageService } from 'primeng/api';
import { finalize } from 'rxjs';
import { ApiService } from '../core/api.service';
import { Booking, OfferteDraft } from '../core/models';
import { PRIME_IMPORTS } from '../shared/prime-imports';

@Component({
  selector: 'app-offerte-editor',
  standalone: true,
  imports: [...PRIME_IMPORTS],
  template: `
    @if (booking) {
      <section class="space-y-4 pb-24">
        <div class="flex flex-col justify-between gap-3 lg:flex-row lg:items-end">
          <div>
            <button class="mb-3 text-sm font-semibold text-slate-500 hover:text-slate-950" type="button" (click)="back()">Terug naar boeking</button>
            <h1 class="page-title">Offerte preview</h1>
            <p class="muted">{{ booking.customerName }} - {{ booking.eventType }} op {{ booking.eventDate | date:'dd MMM yyyy' }}</p>
          </div>
          <p-tag [value]="booking.status" [severity]="booking.status === 'CONCEPT' ? 'secondary' : 'info'"></p-tag>
        </div>

        <div class="grid gap-4 xl:grid-cols-[440px_minmax(0,1fr)]">
          <aside class="surface-panel rounded-md p-5">
            <section>
              <h2 class="text-lg font-bold">Eigenschappen</h2>
              <p class="muted mt-1">Deze regels verschijnen onder Inbegrepen in de PDF.</p>
              <div class="mt-4 space-y-2">
                @for (property of eigenschappen; track property; let index = $index) {
                  <div class="flex items-center gap-2 rounded-md border border-slate-200 px-3 py-2 dark:border-slate-800">
                    <span class="min-w-0 flex-1 text-sm">{{ property }}</span>
                    <button class="icon-btn h-8 w-8 text-rose-600" type="button" (click)="removeProperty(index)" aria-label="Eigenschap verwijderen">
                      <lucide-icon name="x" [size]="15"></lucide-icon>
                    </button>
                  </div>
                } @empty {
                  <p class="muted">Nog geen eigenschappen gekozen.</p>
                }
              </div>
              <div class="mt-3 flex gap-2">
                <input pInputText class="w-full" [(ngModel)]="newProperty" placeholder="Extra eigenschap">
                <button pButton type="button" label="Toevoegen" class="p-button-secondary" (click)="addProperty()"></button>
              </div>
            </section>

            <section class="mt-6 border-t border-slate-200 pt-5 dark:border-slate-800">
              <label class="block">
                <span class="mb-2 block text-sm font-semibold">Extra voorwaarden</span>
                <textarea pInputTextarea rows="5" class="w-full" [(ngModel)]="extraVoorwaarden" (ngModelChange)="schedulePreview()"></textarea>
              </label>
            </section>

            <section class="mt-6 border-t border-slate-200 pt-5 dark:border-slate-800">
              <label class="block">
                <span class="mb-2 block text-sm font-semibold">Notities voor klant</span>
                <textarea pInputTextarea rows="4" class="w-full" [(ngModel)]="klantNotities" (ngModelChange)="schedulePreview()"></textarea>
              </label>
            </section>

            <section class="mt-6 border-t border-slate-200 pt-5 dark:border-slate-800">
              <h2 class="text-lg font-bold">Boeking</h2>
              <div class="mt-3 grid gap-3 text-sm">
                <div class="rounded-md bg-slate-50 p-3 dark:bg-slate-950">
                  <p class="muted">Klant</p>
                  <p class="font-semibold">{{ booking.customerName }}</p>
                </div>
                <div class="rounded-md bg-slate-50 p-3 dark:bg-slate-950">
                  <p class="muted">Datum en tijden</p>
                  <p class="font-semibold">{{ booking.eventDate | date:'dd MMM yyyy' }} - {{ booking.startTime }} tot {{ booking.endTime }}</p>
                </div>
                <div class="rounded-md bg-slate-50 p-3 dark:bg-slate-950">
                  <p class="muted">Prijzen</p>
                  <p>Subtotaal: <strong>{{ booking.subtotaal | currency:'EUR' }}</strong></p>
                  <p>Korting: <strong>-{{ booking.korting | currency:'EUR' }}</strong></p>
                  <p>Totaal: <strong>{{ booking.totaal | currency:'EUR' }}</strong></p>
                </div>
                <div class="rounded-md bg-slate-50 p-3 dark:bg-slate-950">
                  <p class="muted">Vaste algemene voorwaarden</p>
                  <p class="text-xs leading-5 text-slate-600 dark:text-slate-300">Worden automatisch uit het offerte template gebruikt en kunnen hier niet aangepast worden.</p>
                </div>
              </div>
            </section>
          </aside>

          <section class="surface-panel min-h-[820px] overflow-hidden rounded-md">
            <div class="flex items-center justify-between border-b border-slate-200 px-4 py-3 dark:border-slate-800">
              <div>
                <h2 class="font-bold">Live PDF preview</h2>
                <p class="muted">De preview wordt bijgewerkt wanneer u links iets wijzigt.</p>
              </div>
              @if (previewLoading) {
                <p-progressSpinner styleClass="h-7 w-7" strokeWidth="4"></p-progressSpinner>
              }
            </div>
            @if (previewUrl) {
              <iframe title="Offerte PDF preview" class="h-[calc(100vh-210px)] min-h-[760px] w-full border-0 bg-white" [src]="previewUrl"></iframe>
            } @else {
              <div class="grid h-[760px] place-items-center text-sm text-slate-500">Preview wordt geladen...</div>
            }
          </section>
        </div>

        <div class="fixed inset-x-0 bottom-0 z-30 border-t border-slate-200 bg-white/95 px-4 py-3 shadow-lg backdrop-blur dark:border-slate-800 dark:bg-slate-950/95 md:left-[240px]">
          <div class="mx-auto flex max-w-7xl flex-col justify-end gap-2 sm:flex-row">
            <button pButton type="button" label="Opslaan als concept" icon="pi pi-save" class="p-button-secondary" [loading]="saving" (click)="saveConcept()"></button>
            <button pButton type="button" label="Verstuur naar klant" icon="pi pi-send" [loading]="sending" (click)="send()"></button>
          </div>
        </div>
      </section>
    }
  `
})
export class OfferteEditorComponent implements OnInit, OnDestroy {
  booking: Booking | null = null;
  eigenschappen: string[] = [];
  newProperty = '';
  extraVoorwaarden = '';
  klantNotities = '';
  previewUrl: SafeResourceUrl | null = null;
  previewLoading = false;
  saving = false;
  sending = false;
  private bookingId = 0;
  private previewObjectUrl: string | null = null;
  private previewTimer: number | null = null;

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly api: ApiService,
    private readonly messages: MessageService,
    private readonly sanitizer: DomSanitizer
  ) {}

  ngOnInit(): void {
    this.bookingId = Number(this.route.snapshot.paramMap.get('id'));
    this.api.booking(this.bookingId).subscribe((booking) => {
      this.booking = booking;
      this.eigenschappen = [...(booking.properties ?? [])];
      this.extraVoorwaarden = booking.conditions ?? '';
      this.klantNotities = booking.offerteCustomerMessage ?? '';
      this.updatePreview();
    });
  }

  ngOnDestroy(): void {
    if (this.previewTimer !== null) {
      window.clearTimeout(this.previewTimer);
    }
    this.revokePreview();
  }

  addProperty(): void {
    const value = this.newProperty.trim();
    if (!value) {
      return;
    }
    if (!this.eigenschappen.includes(value)) {
      this.eigenschappen = [...this.eigenschappen, value];
      this.schedulePreview();
    }
    this.newProperty = '';
  }

  removeProperty(index: number): void {
    this.eigenschappen = this.eigenschappen.filter((_item, itemIndex) => itemIndex !== index);
    this.schedulePreview();
  }

  saveConcept(): void {
    this.saving = true;
    this.api.saveOfferteConcept(this.bookingId, this.draft())
      .pipe(finalize(() => this.saving = false))
      .subscribe({
        next: () => {
          this.messages.add({ severity: 'success', summary: 'Concept opgeslagen', detail: 'De offertewijzigingen zijn bewaard.' });
          this.api.booking(this.bookingId).subscribe((booking) => this.booking = booking);
        },
        error: (error) => this.messages.add({ severity: 'error', summary: 'Niet opgeslagen', detail: error.error?.message ?? 'Controleer de invoer.' })
      });
  }

  send(): void {
    this.sending = true;
    this.api.sendOfferte(this.bookingId, this.draft())
      .pipe(finalize(() => this.sending = false))
      .subscribe({
        next: () => {
          const email = this.booking?.customerEmail ?? 'de klant';
          this.messages.add({ severity: 'success', summary: 'Offerte verzonden', detail: `De offerte is verstuurd naar ${email}.` });
          void this.router.navigate(['/boekingen', this.bookingId]);
        },
        error: (error) => this.messages.add({ severity: 'error', summary: 'Niet verzonden', detail: error.error?.message ?? 'Controleer DocuSeal en mail instellingen.' })
      });
  }

  back(): void {
    void this.router.navigate(['/boekingen', this.bookingId]);
  }

  schedulePreview(): void {
    if (this.previewTimer !== null) {
      window.clearTimeout(this.previewTimer);
    }
    this.previewTimer = window.setTimeout(() => this.updatePreview(), 350);
  }

  private updatePreview(): void {
    this.previewLoading = true;
    this.api.offertePreviewPdf(this.bookingId, this.draft())
      .pipe(finalize(() => this.previewLoading = false))
      .subscribe({
        next: (blob) => {
          this.revokePreview();
          this.previewObjectUrl = URL.createObjectURL(blob);
          this.previewUrl = this.sanitizer.bypassSecurityTrustResourceUrl(this.previewObjectUrl);
        },
        error: (error) => this.messages.add({ severity: 'error', summary: 'Preview mislukt', detail: error.error?.message ?? 'De PDF kon niet worden opgebouwd.' })
      });
  }

  private draft(): OfferteDraft {
    return {
      eigenschappen: this.eigenschappen,
      extraVoorwaarden: this.extraVoorwaarden,
      klantNotities: this.klantNotities
    };
  }

  private revokePreview(): void {
    if (this.previewObjectUrl) {
      URL.revokeObjectURL(this.previewObjectUrl);
      this.previewObjectUrl = null;
    }
  }
}
