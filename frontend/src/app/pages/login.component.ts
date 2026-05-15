import { Component, inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { PRIME_IMPORTS } from '../shared/prime-imports';
import { AuthService } from '../core/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [...PRIME_IMPORTS],
  template: `
    <div class="min-h-screen bg-[#F5F5F5] px-4 py-10 text-[#111111]">
      <div class="mx-auto flex min-h-[calc(100vh-5rem)] max-w-6xl items-center justify-center">
        <div class="grid w-full overflow-hidden rounded-xl border border-[#E5E5E5] bg-white shadow-soft md:grid-cols-[1.05fr_0.95fr]">
          <section class="hidden bg-[#111111] p-10 text-white md:flex md:flex-col md:justify-between">
            <div>
              <div class="flex h-12 w-12 items-center justify-center rounded-lg bg-white text-xl font-semibold text-[#DCAB46]">L</div>
              <h1 class="mt-8 text-4xl font-semibold leading-tight">Partycentrum Lux</h1>
              <p class="mt-4 max-w-md text-base leading-7 text-white/70">
                Beheer boekingen, klanten, offertes en betalingen vanuit een rustige, snelle werkruimte.
              </p>
            </div>
            <div class="grid grid-cols-3 gap-3 text-sm text-white/70">
              <div class="rounded-lg border border-white/10 p-3">Bruiloften</div>
              <div class="rounded-lg border border-white/10 p-3">Verjaardagen</div>
              <div class="rounded-lg border border-white/10 p-3">Zakelijk</div>
            </div>
          </section>

          <section class="p-6 text-[#111111] sm:p-10">
            <div class="mb-8 md:hidden">
              <div class="flex h-12 w-12 items-center justify-center rounded-lg bg-[#111111] text-xl font-semibold text-[#DCAB46]">L</div>
              <h1 class="mt-4 text-3xl font-semibold">Partycentrum Lux</h1>
            </div>
            <h2 class="text-2xl font-semibold">Inloggen</h2>
            <p class="muted mt-2">Gebruik uw medewerkeraccount om verder te gaan.</p>

            <form class="mt-8 space-y-5" [formGroup]="form" (ngSubmit)="submit()">
              <label class="block">
                <span class="mb-2 block text-sm font-semibold">E-mailadres</span>
                <input pInputText class="w-full" type="text" inputmode="email" formControlName="email" autocomplete="email">
              </label>
              <label class="block">
                <span class="mb-2 block text-sm font-semibold">Wachtwoord</span>
                <p-password formControlName="password" [feedback]="false" [toggleMask]="true" styleClass="w-full" inputStyleClass="w-full"></p-password>
              </label>

              @if (error) {
                <div class="rounded-md border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700">{{ error }}</div>
              }

              <button pButton type="submit" class="w-full" [loading]="loading" label="Inloggen"></button>
            </form>
          </section>
        </div>
      </div>
    </div>
  `
})
export class LoginComponent {
  private readonly fb = inject(FormBuilder);

  loading = false;
  error = '';

  readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', Validators.required]
  });

  constructor(
    private readonly auth: AuthService,
    private readonly router: Router
  ) {}

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.loading = true;
    this.error = '';
    this.auth.login(this.form.controls.email.value, this.form.controls.password.value).subscribe({
      next: () => void this.router.navigate(['/dashboard']),
      error: () => {
        this.error = 'Inloggen mislukt. Controleer uw e-mailadres en wachtwoord.';
        this.loading = false;
      }
    });
  }
}
