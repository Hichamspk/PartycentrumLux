import { Component, DestroyRef, OnInit, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { FormBuilder, Validators } from '@angular/forms';
import { finalize } from 'rxjs';
import { MessageService } from 'primeng/api';
import { ApiService } from '../core/api.service';
import { ThemeService } from '../core/theme.service';
import { CompanySettings, MailLog, MailLogStatus, MailLogType, SelectOption } from '../core/models';
import { PRIME_IMPORTS } from '../shared/prime-imports';

type PreviewVariables = Record<string, string>;

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [...PRIME_IMPORTS],
  template: `
    <section class="space-y-6">
      <div>
        <h1 class="page-title">Instellingen</h1>
        <p class="muted">Bedrijfsgegevens, offertestijl, DocuSeal, mail en weergave.</p>
      </div>

      <p-tabView>
        <p-tabPanel header="Instellingen">
      <div class="grid gap-5 2xl:grid-cols-[minmax(0,1fr)_620px]">
        <form class="surface-panel grid gap-6 rounded-md p-5" [formGroup]="form" (ngSubmit)="save()">
          <section class="grid gap-4">
            <div>
              <h2 class="text-lg font-bold text-slate-950 dark:text-white">Offerte instellingen</h2>
              <p class="muted">Deze gegevens verschijnen op offertes en in de live preview.</p>
            </div>

            <div class="grid gap-4 lg:grid-cols-2">
              <label class="lg:col-span-2">
                <span class="mb-2 block text-sm font-semibold">Logo upload</span>
                <input
                  class="block w-full rounded-md border border-slate-200 bg-white px-3 py-2 text-sm file:mr-4 file:rounded-md file:border-0 file:bg-slate-950 file:px-4 file:py-2 file:text-sm file:font-semibold file:text-white dark:border-slate-700 dark:bg-slate-950 dark:file:bg-white dark:file:text-slate-950"
                  type="file"
                  accept="image/png,image/jpeg,image/svg+xml"
                  (change)="handleLogoInput($event)"
                >
                @if (uploadingLogo) {
                  <p class="muted mt-2">Logo wordt opgeslagen...</p>
                }
                @if (previewLogoUrl(); as logoUrl) {
                  <div class="mt-3 flex items-center gap-3 rounded-md border border-slate-200 bg-slate-50 p-3 dark:border-slate-800 dark:bg-slate-950">
                    <img [src]="logoUrl" alt="Huidig logo preview" class="h-16 max-w-44 object-contain">
                    <div>
                      <p class="text-sm font-semibold text-slate-950 dark:text-white">Huidig logo</p>
                      <p class="muted">Wordt gebruikt in de offertepreview en PDF.</p>
                    </div>
                  </div>
                }
              </label>

              <label>
                <span class="mb-2 block text-sm font-semibold">Huisstijl kleur</span>
                <div class="flex h-11 items-center gap-3 rounded-md border border-slate-200 bg-white px-3 dark:border-slate-700 dark:bg-slate-950">
                  <input
                    class="h-8 w-12 cursor-pointer rounded border-0 bg-transparent p-0"
                    type="color"
                    formControlName="brandColor"
                    aria-label="Huisstijl kleur"
                  >
                  <span class="text-sm font-semibold uppercase text-slate-700 dark:text-slate-200">{{ brandColorValue }}</span>
                </div>
              </label>

              <label>
                <span class="mb-2 block text-sm font-semibold">Bedrijfsnaam</span>
                <input pInputText class="w-full" formControlName="companyName">
              </label>

              <label>
                <span class="mb-2 block text-sm font-semibold">Adres</span>
                <input pInputText class="w-full" formControlName="address">
              </label>

              <label>
                <span class="mb-2 block text-sm font-semibold">Postcode</span>
                <input pInputText class="w-full" formControlName="postalCode">
              </label>

              <label>
                <span class="mb-2 block text-sm font-semibold">Stad</span>
                <input pInputText class="w-full" formControlName="city">
              </label>

              <label>
                <span class="mb-2 block text-sm font-semibold">KvK</span>
                <input pInputText class="w-full" formControlName="kvk">
              </label>

              <label>
                <span class="mb-2 block text-sm font-semibold">BTW nummer</span>
                <input pInputText class="w-full" formControlName="vatNumber">
              </label>

              <label>
                <span class="mb-2 block text-sm font-semibold">IBAN</span>
                <input pInputText class="w-full" formControlName="iban">
              </label>

              <label>
                <span class="mb-2 block text-sm font-semibold">Telefoon</span>
                <input pInputText class="w-full" formControlName="phone">
              </label>

              <label>
                <span class="mb-2 block text-sm font-semibold">E-mail</span>
                <input pInputText class="w-full" type="text" inputmode="email" formControlName="email">
              </label>

              <label>
                <span class="mb-2 block text-sm font-semibold">Website</span>
                <input pInputText class="w-full" formControlName="website">
              </label>
            </div>
          </section>

          <section class="grid gap-4 border-t border-slate-200 pt-5 dark:border-slate-800">
            <div>
              <h2 class="text-lg font-bold text-slate-950 dark:text-white">Mail instellingen</h2>
            <p class="muted">SMTP gegevens voor bevestigingen, herinneringen en reviews.</p>
            </div>
            <div class="grid gap-4 lg:grid-cols-2">
            <label>
              <span class="mb-2 block text-sm font-semibold">Mail afzender</span>
              <input pInputText class="w-full" type="text" inputmode="email" formControlName="mailFrom">
            </label>
            <label>
              <span class="mb-2 block text-sm font-semibold">SMTP host</span>
              <input pInputText class="w-full" formControlName="smtpHost">
            </label>
            <label>
              <span class="mb-2 block text-sm font-semibold">SMTP poort</span>
              <p-inputNumber formControlName="smtpPort" [min]="1" styleClass="w-full"></p-inputNumber>
            </label>
            <label>
              <span class="mb-2 block text-sm font-semibold">SMTP gebruikersnaam</span>
              <input pInputText class="w-full" formControlName="smtpUsername">
            </label>
            <label>
              <span class="mb-2 block text-sm font-semibold">SMTP wachtwoord</span>
              <input pInputText class="w-full" type="password" autocomplete="off" formControlName="smtpPassword">
            </label>
            <label>
              <span class="mb-2 block text-sm font-semibold">SMTP from</span>
              <input pInputText class="w-full" formControlName="smtpFrom">
            </label>
            </div>
          </section>

          <section class="grid gap-4 border-t border-slate-200 pt-5 dark:border-slate-800">
            <div class="flex flex-wrap items-start justify-between gap-3">
              <div>
                <h2 class="text-lg font-bold text-slate-950 dark:text-white">Contract instellingen</h2>
                <p class="muted">DocuSeal-koppeling en algemene voorwaarden voor huurovereenkomsten.</p>
              </div>
              <button
                pButton
                type="button"
                label="Test verbinding"
                icon="pi pi-wifi"
                severity="secondary"
                [loading]="testingDocuseal"
                (click)="testDocusealConnection()"
              ></button>
            </div>

            <div class="grid gap-4 lg:grid-cols-2">
              <label>
                <span class="mb-2 block text-sm font-semibold">DocuSeal API key</span>
                <input pInputText class="w-full" type="password" autocomplete="off" formControlName="docusealApiKey">
              </label>

              <label>
                <span class="mb-2 block text-sm font-semibold">DocuSeal basis URL</span>
                <input pInputText class="w-full" formControlName="docusealBaseUrl" placeholder="http://docuseal:3000">
              </label>

              <label>
                <span class="mb-2 block text-sm font-semibold">Hussain e-mail</span>
                <input pInputText class="w-full" type="email" formControlName="docusealHussainEmail">
              </label>

              <label>
                <span class="mb-2 block text-sm font-semibold">Hussain signature token</span>
                <input pInputText class="w-full" type="password" autocomplete="off" formControlName="docusealHussainSignatureToken">
              </label>

              <label class="lg:col-span-2">
                <span class="mb-2 block text-sm font-semibold">Google review URL</span>
                <input pInputText class="w-full" formControlName="googleReviewUrl">
              </label>

              <label class="lg:col-span-2">
                <span class="mb-2 block text-sm font-semibold">Algemene voorwaarden</span>
                <textarea pInputTextarea class="w-full" rows="8" formControlName="generalTerms"></textarea>
              </label>
            </div>
          </section>

          <section class="grid gap-4 border-t border-slate-200 pt-5 dark:border-slate-800">
            <div class="flex items-center justify-between gap-4">
              <div>
                <h2 class="text-lg font-bold text-slate-950 dark:text-white">Weergave</h2>
                <p class="muted">Wissel de dashboardweergave.</p>
              </div>
              <p-inputSwitch [ngModel]="theme.darkMode()" [ngModelOptions]="{ standalone: true }" (onChange)="theme.setDarkMode($event.checked)"></p-inputSwitch>
            </div>
          </section>

          <div class="flex justify-end">
            <button pButton type="submit" label="Instellingen opslaan" icon="pi pi-save" [disabled]="form.invalid || uploadingLogo"></button>
          </div>
        </form>

        <aside class="surface-panel rounded-md p-5">
          <div class="flex items-center justify-between gap-4">
            <div>
              <h2 class="text-lg font-bold text-slate-950 dark:text-white">Live voorbeeld</h2>
              <p class="muted">Live offertevoorbeeld met dummydata.</p>
            </div>
            <span class="h-8 w-8 rounded-md border border-slate-200 dark:border-slate-700" [style.background-color]="brandColorValue"></span>
          </div>

          <div class="mt-4 overflow-hidden rounded-md bg-slate-100 p-3 dark:bg-slate-950">
            @if (previewHtml) {
              <iframe
                title="Live offertevoorbeeld"
                class="h-[760px] w-full border-0 bg-white"
                sandbox=""
                [srcdoc]="previewHtml"
              ></iframe>
            } @else {
              <div class="grid h-[640px] place-items-center text-sm text-slate-500">Voorbeeld wordt geladen...</div>
            }
          </div>
        </aside>
      </div>
        </p-tabPanel>
        <p-tabPanel header="Mail overzicht">
          <section class="surface-panel rounded-md p-5">
            <div class="flex flex-col justify-between gap-4 lg:flex-row lg:items-end">
              <div>
                <h2 class="text-lg font-bold text-slate-950 dark:text-white">Mail overzicht</h2>
                <p class="muted">Alle verzonden en mislukte mails over alle boekingen.</p>
              </div>
              <div class="grid gap-3 sm:grid-cols-3">
                <p-dropdown [options]="mailTypeOptions" [(ngModel)]="mailFilters.type" optionLabel="label" optionValue="value" placeholder="Type" styleClass="w-full" (onChange)="loadMailLogs()"></p-dropdown>
                <p-dropdown [options]="mailStatusOptions" [(ngModel)]="mailFilters.status" optionLabel="label" optionValue="value" placeholder="Status" styleClass="w-full" (onChange)="loadMailLogs()"></p-dropdown>
                <span class="p-input-icon-left">
                  <i class="pi pi-search"></i>
                  <input pInputText class="w-full" [(ngModel)]="mailFilters.search" placeholder="Zoek e-mail" (input)="loadMailLogs()">
                </span>
              </div>
            </div>

            <div class="mt-4 overflow-x-auto">
              <p-table [value]="mailLogs" [loading]="mailLogsLoading" responsiveLayout="scroll" [paginator]="true" [rows]="10">
                <ng-template pTemplate="header">
                  <tr>
                    <th>Type</th>
                    <th>Klant naam</th>
                    <th>Ontvanger</th>
                    <th>Verzonden op</th>
                    <th>Status</th>
                  </tr>
                </ng-template>
                <ng-template pTemplate="body" let-log>
                  <tr>
                    <td>{{ label(log.type) }}</td>
                    <td>{{ log.klantNaam || '-' }}</td>
                    <td>{{ log.ontvangerEmail }}</td>
                    <td>{{ log.verzondenOp | date:'dd MMM yyyy HH:mm' }}</td>
                    <td><p-tag [value]="log.status" [severity]="mailSeverity(log.status)"></p-tag></td>
                  </tr>
                </ng-template>
                <ng-template pTemplate="emptymessage">
                  <tr>
                    <td colspan="5" class="py-6 text-center text-sm text-slate-500">Geen mails gevonden.</td>
                  </tr>
                </ng-template>
              </p-table>
            </div>
          </section>
        </p-tabPanel>
      </p-tabView>
    </section>
  `
})
export class SettingsComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);

  settings: CompanySettings | null = null;
  uploadingLogo = false;
  testingDocuseal = false;
  previewHtml: SafeHtml | null = null;
  mailLogs: MailLog[] = [];
  mailLogsLoading = false;
  mailFilters: { type: MailLogType | null; status: MailLogStatus | null; search: string } = {
    type: null,
    status: null,
    search: ''
  };
  private invoiceTemplate = '';

  readonly mailTypeOptions: SelectOption<MailLogType | null>[] = [
    { label: 'Alle types', value: null },
    { label: 'Offerte verzonden', value: 'OFFERTE_VERZONDEN' },
    { label: 'Bevestigingsmail', value: 'BEVESTIGINGSMAIL' },
    { label: 'Aanbetaling herinnering', value: 'BETALING_HERINNERING_AANBETALING' },
    { label: 'Restant herinnering', value: 'BETALING_HERINNERING_RESTANT' },
    { label: 'Evenement herinnering', value: 'EVENEMENT_HERINNERING' },
    { label: 'Review verzoek', value: 'REVIEW_VERZOEK' },
    { label: 'Annulering', value: 'ANNULERING' }
  ];
  readonly mailStatusOptions: SelectOption<MailLogStatus | null>[] = [
    { label: 'Alle statussen', value: null },
    { label: 'Verzonden', value: 'VERZONDEN' },
    { label: 'Mislukt', value: 'MISLUKT' }
  ];

  readonly form = this.fb.nonNullable.group({
    companyName: ['', Validators.required],
    logoPath: [''],
    logoBase64: [''],
    brandColor: ['#C9A84C', [Validators.required, Validators.pattern(/^#[0-9A-Fa-f]{6}$/)]],
    address: ['', Validators.required],
    postalCode: ['', Validators.required],
    city: ['', Validators.required],
    kvk: ['', Validators.required],
    vatNumber: ['', Validators.required],
    iban: ['', Validators.required],
    phone: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
    website: ['', Validators.required],
    mailFrom: ['', [Validators.required, Validators.email]],
    docusealApiKey: [''],
    docusealBaseUrl: ['http://lux-docuseal:3000', Validators.required],
    docusealContractTemplateId: [''],
    docusealHussainEmail: [''],
    docusealHussainSignatureToken: [''],
    googleReviewUrl: [''],
    smtpHost: ['smtp.gmail.com'],
    smtpPort: [587],
    smtpUsername: [''],
    smtpPassword: [''],
    smtpFrom: [''],
    generalTerms: ['', Validators.required]
  });

  constructor(
    public readonly theme: ThemeService,
    private readonly api: ApiService,
    private readonly messages: MessageService,
    private readonly sanitizer: DomSanitizer
  ) {}

  ngOnInit(): void {
    this.invoiceTemplate = this.defaultOfferteTemplate();
    this.api.settings().subscribe((settings) => this.patch(settings));
    this.loadMailLogs();
    this.form.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.refreshPreview());
  }

  loadMailLogs(): void {
    this.mailLogsLoading = true;
    this.api.mailLogs(this.mailFilters).subscribe({
      next: (logs) => this.mailLogs = logs,
      complete: () => this.mailLogsLoading = false
    });
  }

  mailSeverity(status: MailLogStatus): 'success' | 'danger' {
    return status === 'VERZONDEN' ? 'success' : 'danger';
  }

  label(value: string): string {
    return value.replaceAll('_', ' ').toLowerCase();
  }

  get brandColorValue(): string {
    return this.form.controls.brandColor.value || '#C9A84C';
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.api.saveSettings(this.form.getRawValue()).subscribe((settings) => {
      this.patch(settings);
      this.messages.add({ severity: 'success', summary: 'Opgeslagen', detail: 'Instellingen zijn bijgewerkt.' });
    });
  }

  testDocusealConnection(): void {
    this.testingDocuseal = true;
    this.api.testDocusealConnection()
      .pipe(finalize(() => this.testingDocuseal = false))
      .subscribe({
        next: (result) => {
          this.messages.add({
            severity: result.ok ? 'success' : 'warn',
            summary: result.ok ? 'DocuSeal verbonden' : 'DocuSeal niet bereikbaar',
            detail: result.message
          });
        },
        error: (error) => this.messages.add({
          severity: 'error',
          summary: 'DocuSeal test mislukt',
          detail: error.error?.message ?? 'Controleer de URL en API key.'
        })
      });
  }

  handleLogoInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) {
      return;
    }

    if (!this.isAllowedLogo(file)) {
      this.messages.add({ severity: 'error', summary: 'Ongeldig bestand', detail: 'Gebruik een PNG, JPG of SVG logo.' });
      input.value = '';
      return;
    }

    const previousLogo = this.form.controls.logoBase64.value;
    const reader = new FileReader();
    reader.onload = () => this.form.controls.logoBase64.setValue(String(reader.result ?? ''));
    reader.onerror = () => this.messages.add({ severity: 'error', summary: 'Logo niet gelezen', detail: 'Het logo kon niet worden ingelezen.' });
    reader.readAsDataURL(file);

    this.uploadingLogo = true;
    this.api.uploadLogo(file).subscribe({
      next: (settings) => {
        this.patch(settings);
        this.messages.add({ severity: 'success', summary: 'Logo opgeslagen', detail: 'Het logo is opgeslagen als offerte-instelling.' });
      },
      error: (error) => {
        this.uploadingLogo = false;
        this.form.controls.logoBase64.setValue(previousLogo);
        this.messages.add({ severity: 'error', summary: 'Logo niet opgeslagen', detail: error.error?.message ?? 'Probeer een ander logo.' });
      },
      complete: () => this.uploadingLogo = false
    });
  }

  previewLogoUrl(): string | null {
    const logo = this.form.controls.logoBase64.value;
    return logo.startsWith('data:image/') ? logo : null;
  }

  private patch(settings: CompanySettings): void {
    this.settings = settings;
    this.form.patchValue({
      companyName: settings.companyName ?? '',
      logoPath: settings.logoPath ?? '',
      logoBase64: settings.logoBase64 ?? '',
      brandColor: settings.brandColor || '#C9A84C',
      address: settings.address ?? '',
      postalCode: settings.postalCode ?? '',
      city: settings.city ?? '',
      kvk: settings.kvk ?? '',
      vatNumber: settings.vatNumber ?? '',
      iban: settings.iban ?? '',
      phone: settings.phone ?? '',
      email: settings.email ?? '',
      website: settings.website ?? '',
      mailFrom: settings.mailFrom || settings.email || '',
      docusealApiKey: settings.docusealApiKey ?? '',
      docusealBaseUrl: settings.docusealBaseUrl || 'http://lux-docuseal:3000',
      docusealContractTemplateId: settings.docusealContractTemplateId ?? '',
      docusealHussainEmail: settings.docusealHussainEmail ?? '',
      docusealHussainSignatureToken: settings.docusealHussainSignatureToken ?? '',
      googleReviewUrl: settings.googleReviewUrl ?? '',
      smtpHost: settings.smtpHost || 'smtp.gmail.com',
      smtpPort: settings.smtpPort || 587,
      smtpUsername: settings.smtpUsername ?? '',
      smtpPassword: settings.smtpPassword ?? '',
      smtpFrom: settings.smtpFrom || settings.mailFrom || settings.email || '',
      generalTerms: settings.generalTerms || 'Alle genoemde afspraken zijn onder voorbehoud van beschikbaarheid en schriftelijke bevestiging.'
    });
    this.refreshPreview();
  }

  private refreshPreview(): void {
    if (!this.invoiceTemplate) {
      return;
    }
    this.invoiceTemplate = this.defaultOfferteTemplate();
    const logoSrc = this.form.controls.logoBase64.value;
    const variables: PreviewVariables = {
      brandColor: this.brandColorValue,
      brandColorSoft: this.lighten(this.brandColorValue, 0.88),
      logoSrc,
      logoImageDisplay: logoSrc ? 'block' : 'none',
      logoFallbackDisplay: logoSrc ? 'none' : 'block',
      companyName: this.value('companyName', 'Partycentrum Lux'),
      companyAddress: this.value('address', 'Voorbeeldstraat 1'),
      companyPostalCode: this.value('postalCode', '1000 AA'),
      companyCity: this.value('city', 'Amsterdam'),
      companyKvk: this.value('kvk', '00000000'),
      companyVatNumber: this.value('vatNumber', 'NL000000000B01'),
      companyIban: this.value('iban', 'NL00 BANK 0000 0000 00'),
      companyPhone: this.value('phone', '+31 20 000 0000'),
      companyEmail: this.value('email', 'info@partycentrumlux.nl'),
      companyWebsite: this.value('website', 'www.partycentrumlux.nl'),
      KLANT_NAAM: 'Familie De Vries',
      EVENEMENT_DATUM: '14-06-2026',
      EVENEMENT_SOORT: 'bruiloft',
      SUBPRIJZEN_RIJEN: '<tr><td>Zaalhuur</td><td>EUR 2.500,00</td></tr>',
      SUBTOTAAL: 'EUR 2.500,00',
      KORTING: 'EUR 0,00',
      TOTAAL: 'EUR 2.500,00',
      AANBETALING_BEDRAG: 'EUR 750,00',
      AANBETALING_DAGEN: '7',
      RESTANT_BEDRAG: 'EUR 1.750,00',
      GELDIG_TOT: '28-06-2026',
      AANBETALING_DATUM: '21-06-2026',
      RESTANT_DATUM: '31-05-2026',
      AANTAL_GASTEN: '120',
      EXTRA_EIGENSCHAPPEN: '<li>Catering inbegrepen</li><li>Parkeren gratis</li>',
      BETAAL_OMSCHRIJVING: '2026-14-06 - LUX-OFFERTE-2026-0001',
      ONDERTEKENING_DATUM: '',
      DOCUMENT_REF: 'LUX-OFFERTE-2026-0001'
    };
    const html = this.invoiceTemplate.replace(/\{\{\s*([A-Z0-9_]+)\s*}}/g, (_match, key: string) =>
      variables[key] ?? ''
    );
    this.previewHtml = this.sanitizer.bypassSecurityTrustHtml(this.fitPreviewHtml(html));
  }

  private value(controlName: keyof typeof this.form.controls, fallback: string): string {
    const value = this.form.controls[controlName].value;
    return typeof value === 'string' && value.trim() ? value : fallback;
  }

  private isAllowedLogo(file: File): boolean {
    const allowedMimeTypes = ['image/png', 'image/jpeg', 'image/svg+xml'];
    const extension = file.name.toLowerCase().split('.').pop();
    return allowedMimeTypes.includes(file.type) || extension === 'png' || extension === 'jpg' || extension === 'jpeg' || extension === 'svg';
  }

  private escapeHtml(value: string): string {
    return value
      .replaceAll('&', '&amp;')
      .replaceAll('<', '&lt;')
      .replaceAll('>', '&gt;')
      .replaceAll('"', '&quot;')
      .replaceAll("'", '&#39;');
  }

  private lighten(hex: string, factor: number): string {
    try {
      const red = Number.parseInt(hex.slice(1, 3), 16);
      const green = Number.parseInt(hex.slice(3, 5), 16);
      const blue = Number.parseInt(hex.slice(5, 7), 16);
      return `#${this.channel(red, factor)}${this.channel(green, factor)}${this.channel(blue, factor)}`;
    } catch {
      return '#F7F1DF';
    }
  }

  private channel(value: number, factor: number): string {
    const mixed = Math.round(value + (255 - value) * factor);
    return Math.min(255, Math.max(0, mixed)).toString(16).padStart(2, '0').toUpperCase();
  }

  private fitPreviewHtml(html: string): string {
    return html.replace('</style>', `
    @media screen {
      html, body { overflow: hidden; }
      .doc { transform: scale(.82); transform-origin: top left; width: 122%; }
    }
  </style>`);
  }

  private defaultOfferteTemplate(): string {
    return `
      <style>
        body { margin:0; background:#f8fafc; font-family:Arial,sans-serif; color:#231F20; }
        .doc { background:#fff; min-height:900px; padding:42px; box-sizing:border-box; border-top:10px solid ${this.brandColorValue}; }
        .brand { color:${this.brandColorValue}; font-weight:900; letter-spacing:.04em; }
        table { width:100%; border-collapse:collapse; margin:18px 0; }
        th, td { border-bottom:1px solid #e5e7eb; padding:10px; text-align:left; }
        .grid { display:grid; grid-template-columns:1fr 1fr; gap:18px; }
        .box { border:1px solid #e5e7eb; border-radius:8px; padding:16px; }
      </style>
      <div class="doc">
        <h1 class="brand">Partycentrum Lux</h1>
        <h2>Offerte / Huurovereenkomst</h2>
        <p><strong>Klant:</strong> {{KLANT_NAAM}}<br><strong>Evenement:</strong> {{EVENEMENT_SOORT}} op {{EVENEMENT_DATUM}}<br><strong>Document:</strong> {{DOCUMENT_REF}}</p>
        <table><thead><tr><th>Dienst</th><th>Bedrag</th></tr></thead><tbody>{{SUBPRIJZEN_RIJEN}}</tbody></table>
        <div class="grid">
          <div class="box"><strong>Totaal</strong><br>{{TOTAAL}}<br>Gasten: {{AANTAL_GASTEN}}</div>
          <div class="box"><strong>Betaling</strong><br>Aanbetaling: {{AANBETALING_BEDRAG}} binnen {{AANBETALING_DAGEN}} dagen<br>Restant: {{RESTANT_BEDRAG}} uiterlijk {{RESTANT_DATUM}}</div>
        </div>
        <h3>Inbegrepen</h3><ul>{{EXTRA_EIGENSCHAPPEN}}</ul>
        <h3>Ondertekening</h3><p>Huurder: {{KLANT_NAAM}}<br>Datum: {{ONDERTEKENING_DATUM}}</p>
      </div>`;
  }
}
