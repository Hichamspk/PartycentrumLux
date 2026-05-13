import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { MessageService } from 'primeng/api';
import { QuillModule } from 'ngx-quill';
import { ApiService } from '../core/api.service';
import { Booking, Contract } from '../core/models';
import { PRIME_IMPORTS } from '../shared/prime-imports';

@Component({
  selector: 'app-contract-editor',
  standalone: true,
  imports: [...PRIME_IMPORTS, QuillModule],
  template: `
    <section class="space-y-6">
      <div class="flex flex-col justify-between gap-4 sm:flex-row sm:items-end">
        <div>
          <button class="mb-3 text-sm font-semibold text-slate-500 hover:text-slate-950 dark:text-slate-400 dark:hover:text-white" type="button" (click)="back()">Terug naar boeking</button>
          <h1 class="page-title">Contract editor</h1>
          <p class="muted">{{ booking?.customerName }} · {{ booking?.eventDate | date:'dd MMM yyyy' }}</p>
        </div>
        <p-tag [value]="contract?.status || 'CONCEPT'" [severity]="contract?.status === 'VERZONDEN' ? 'info' : 'warn'"></p-tag>
      </div>

      <div class="grid gap-5 xl:grid-cols-[minmax(0,520px)_minmax(0,1fr)]">
        <form class="surface-panel grid gap-5 rounded-md p-4" [formGroup]="form">
          <section class="rounded-md border border-slate-200 p-4 dark:border-slate-800">
            <h2 class="font-bold">Vaste gegevens</h2>
            <div class="mt-3 grid gap-3 text-sm sm:grid-cols-2">
              <div>
                <p class="muted">Datum</p>
                <p class="font-semibold">{{ booking?.eventDate | date:'dd MMM yyyy' }}</p>
              </div>
              <div>
                <p class="muted">Tijd</p>
                <p class="font-semibold">{{ booking?.startTime }} - {{ booking?.endTime }}</p>
              </div>
              <div>
                <p class="muted">Prijs excl. BTW</p>
                <p class="font-semibold">{{ booking?.price | currency:'EUR' }}</p>
              </div>
              <div>
                <p class="muted">Gasten</p>
                <p class="font-semibold">{{ booking?.guestCount }}</p>
              </div>
            </div>
          </section>

          <label>
            <span class="mb-2 block text-sm font-semibold">Extra contracttekst</span>
            <quill-editor
              formControlName="introHtml"
              theme="snow"
              [styles]="{ height: '140px' }"
              [modules]="quillModules"
              (onContentChanged)="refreshPreview()"
            ></quill-editor>
          </label>

          <label>
            <span class="mb-2 block text-sm font-semibold">Eigenschappen</span>
            <textarea
              pInputTextarea
              rows="6"
              class="w-full"
              formControlName="propertiesText"
              placeholder="Een eigenschap per regel"
              (input)="refreshPreview()"
            ></textarea>
          </label>

          <label>
            <span class="mb-2 block text-sm font-semibold">Algemene voorwaarden</span>
            <quill-editor
              formControlName="generalTermsHtml"
              theme="snow"
              [styles]="{ height: '180px' }"
              [modules]="quillModules"
              (onContentChanged)="refreshPreview()"
            ></quill-editor>
          </label>

          <label>
            <span class="mb-2 block text-sm font-semibold">Aanvullende voorwaarden</span>
            <quill-editor
              formControlName="extraTermsHtml"
              theme="snow"
              [styles]="{ height: '160px' }"
              [modules]="quillModules"
              (onContentChanged)="refreshPreview()"
            ></quill-editor>
          </label>

          <div class="flex flex-wrap justify-end gap-2">
            <button pButton type="button" label="Opslaan als concept" icon="pi pi-save" class="p-button-secondary" [disabled]="form.invalid" (click)="saveConcept()"></button>
            <button pButton type="button" label="Verstuur naar klant" icon="pi pi-send" [disabled]="form.invalid" (click)="send()"></button>
          </div>
        </form>

        <aside class="surface-panel rounded-md p-3">
          <div class="mb-3 flex items-center justify-between gap-3 px-2">
            <div>
              <h2 class="font-bold">Live contractpreview</h2>
              <p class="muted">Prijs, datum en tijden zijn alleen-lezen.</p>
            </div>
          </div>
          <iframe title="Contract preview" class="h-[860px] w-full border-0 bg-white" [srcdoc]="previewHtml"></iframe>
        </aside>
      </div>
    </section>
  `
})
export class ContractEditorComponent implements OnInit {
  private readonly fb = inject(FormBuilder);

  booking: Booking | null = null;
  contract: Contract | null = null;
  bookingId = 0;
  previewHtml = '';
  private baseHtml = '';

  readonly form = this.fb.group({
    introHtml: [''],
    propertiesText: ['', Validators.required],
    generalTermsHtml: ['', Validators.required],
    extraTermsHtml: ['', Validators.required]
  });
  readonly quillModules = {
    toolbar: [
      ['bold', 'italic', 'underline'],
      [{ list: 'ordered' }, { list: 'bullet' }],
      ['link'],
      ['clean']
    ]
  };

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly api: ApiService,
    private readonly messages: MessageService
  ) {}

  ngOnInit(): void {
    this.bookingId = Number(this.route.snapshot.paramMap.get('id'));
    this.api.booking(this.bookingId).subscribe((booking) => this.booking = booking);
    this.api.bookingContract(this.bookingId).subscribe((contract) => {
      if (contract.html) {
        this.patch(contract);
      } else {
        this.api.generateBookingContract(this.bookingId).subscribe((generated) => this.patch(generated));
      }
    });
  }

  saveConcept(): void {
    this.api.saveBookingContract(this.bookingId, this.composeHtml()).subscribe({
      next: (contract) => {
        this.patch(contract);
        this.messages.add({ severity: 'success', summary: 'Concept opgeslagen', detail: 'Contract is opgeslagen.' });
      },
      error: (error) => this.messages.add({ severity: 'error', summary: 'Niet opgeslagen', detail: error.error?.message ?? 'Controleer het contract.' })
    });
  }

  send(): void {
    this.api.sendBookingContract(this.bookingId, this.composeHtml()).subscribe({
      next: (contract) => {
        this.patch(contract);
        this.messages.add({ severity: 'success', summary: 'Verzonden', detail: 'Contract is naar de klant verstuurd.' });
        void this.router.navigate(['/boekingen', this.bookingId]);
      },
      error: (error) => this.messages.add({ severity: 'error', summary: 'Niet verzonden', detail: error.error?.message ?? 'Controleer DocuSeal instellingen.' })
    });
  }

  refreshPreview(): void {
    this.previewHtml = this.composeHtml();
  }

  back(): void {
    void this.router.navigate(['/boekingen', this.bookingId]);
  }

  private patch(contract: Contract): void {
    this.contract = contract;
    this.baseHtml = contract.html ?? '';
    this.form.patchValue({
      introHtml: this.extractIntro(this.baseHtml),
      propertiesText: this.extractProperties(this.baseHtml),
      generalTermsHtml: this.extractBox(this.baseHtml, 'Algemene voorwaarden'),
      extraTermsHtml: this.extractBox(this.baseHtml, 'Aanvullende voorwaarden')
    });
    this.refreshPreview();
  }

  private composeHtml(): string {
    let html = this.baseHtml;
    if (!html) {
      return '';
    }

    html = this.replaceIntro(html, this.form.controls.introHtml.value ?? '');
    html = html.replace(
      /(<h2>Eigenschappen<\/h2>)([\s\S]*?)(<h2>Financieel<\/h2>)/,
      `$1\n  ${this.propertiesHtml()}\n\n  $3`
    );
    html = this.replaceBox(html, 'Algemene voorwaarden', this.form.controls.generalTermsHtml.value ?? '');
    html = this.replaceBox(html, 'Aanvullende voorwaarden', this.form.controls.extraTermsHtml.value ?? '');
    return html;
  }

  private propertiesHtml(): string {
    const lines = (this.form.controls.propertiesText.value ?? '')
      .split('\n')
      .map((line) => line.trim())
      .filter(Boolean);
    if (!lines.length) {
      return '<p>Geen specifieke eigenschappen vastgelegd.</p>';
    }
    return `<ul>${lines.map((line) => `<li>${this.escapeHtml(line)}</li>`).join('')}</ul>`;
  }

  private extractProperties(html: string): string {
    const match = html.match(/<h2>Eigenschappen<\/h2>([\s\S]*?)<h2>Financieel<\/h2>/);
    return this.listItemsToText(match?.[1] ?? '');
  }

  private extractBox(html: string, title: string): string {
    const match = html.match(new RegExp(`<h2>${title}<\\/h2>\\s*<div class="box">([\\s\\S]*?)<\\/div>`));
    return match?.[1]?.trim() || '<p></p>';
  }

  private extractIntro(html: string): string {
    const match = html.match(/<!-- lux-editor-text-start -->([\s\S]*?)<!-- lux-editor-text-end -->/);
    return match?.[1]?.trim() || '';
  }

  private replaceIntro(html: string, introHtml: string): string {
    const block = introHtml?.trim()
      ? `\n  <!-- lux-editor-text-start -->\n  <div class="box">${introHtml}</div>\n  <!-- lux-editor-text-end -->\n`
      : '\n  <!-- lux-editor-text-start --><!-- lux-editor-text-end -->\n';
    if (html.includes('<!-- lux-editor-text-start -->')) {
      return html.replace(/<!-- lux-editor-text-start -->([\s\S]*?)<!-- lux-editor-text-end -->/, block.trim());
    }
    return html.replace('<h2>Partijen</h2>', `${block}\n  <h2>Partijen</h2>`);
  }

  private replaceBox(html: string, title: string, content: string): string {
    return html.replace(
      new RegExp(`(<h2>${title}<\\/h2>\\s*<div class="box">)([\\s\\S]*?)(<\\/div>)`),
      `$1${content || '<p></p>'}$3`
    );
  }

  private listItemsToText(html: string): string {
    const matches = Array.from(html.matchAll(/<li>([\s\S]*?)<\/li>/g)).map((match) => this.decodeHtml(this.stripTags(match[1])).trim());
    if (matches.length) {
      return matches.join('\n');
    }
    return this.decodeHtml(this.stripTags(html)).trim();
  }

  private stripTags(value: string): string {
    return value.replace(/<br\s*\/?>/g, '\n').replace(/<[^>]+>/g, '');
  }

  private decodeHtml(value: string): string {
    const text = document.createElement('textarea');
    text.innerHTML = value;
    return text.value;
  }

  private escapeHtml(value: string): string {
    return value
      .replaceAll('&', '&amp;')
      .replaceAll('<', '&lt;')
      .replaceAll('>', '&gt;')
      .replaceAll('"', '&quot;')
      .replaceAll("'", '&#39;');
  }
}
