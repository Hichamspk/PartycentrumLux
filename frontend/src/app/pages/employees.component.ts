import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { ConfirmationService, MessageService } from 'primeng/api';
import { ApiService } from '../core/api.service';
import { Role, SelectOption, User } from '../core/models';
import { PRIME_IMPORTS } from '../shared/prime-imports';

@Component({
  selector: 'app-employees',
  standalone: true,
  imports: [...PRIME_IMPORTS],
  template: `
    <section class="space-y-6">
      <div class="flex flex-col justify-between gap-4 sm:flex-row sm:items-end">
        <div>
          <h1 class="page-title">Medewerkers</h1>
          <p class="muted">Beheer toegang en rollen voor het team.</p>
        </div>
        <button pButton type="button" label="Nieuwe medewerker" icon="pi pi-plus" (click)="openCreate()"></button>
      </div>

      <div class="surface-panel rounded-md p-2">
        <p-table [value]="users" [loading]="loading" [paginator]="true" [rows]="10">
          <ng-template pTemplate="header">
            <tr>
              <th>Naam</th>
              <th>E-mail</th>
              <th>Rol</th>
              <th>Aangemaakt</th>
              <th class="w-32">Acties</th>
            </tr>
          </ng-template>
          <ng-template pTemplate="body" let-user>
            <tr>
              <td class="font-semibold">{{ user.name }}</td>
              <td>{{ user.email }}</td>
              <td><p-tag [value]="user.role === 'OWNER' ? 'Eigenaar' : 'Medewerker'" [severity]="user.role === 'OWNER' ? 'success' : 'info'"></p-tag></td>
              <td>{{ user.createdAt | date:'dd MMM yyyy' }}</td>
              <td>
                <div class="flex gap-2">
                  <button class="icon-btn" type="button" (click)="openEdit(user)" aria-label="Medewerker bewerken">
                    <lucide-icon name="edit" [size]="16"></lucide-icon>
                  </button>
                  <button class="icon-btn text-rose-600" type="button" (click)="confirmDelete(user)" aria-label="Medewerker verwijderen">
                    <lucide-icon name="trash-2" [size]="16"></lucide-icon>
                  </button>
                </div>
              </td>
            </tr>
          </ng-template>
        </p-table>
      </div>

      <p-dialog [header]="form.controls.id.value ? 'Medewerker bewerken' : 'Nieuwe medewerker'" [(visible)]="dialogOpen" [modal]="true" [style]="{ width: 'min(620px, 94vw)' }">
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
            <span class="mb-2 block text-sm font-semibold">Wachtwoord</span>
            <p-password formControlName="password" [feedback]="true" [toggleMask]="true" styleClass="w-full" inputStyleClass="w-full"></p-password>
            @if (form.controls.id.value) {
              <p class="muted mt-2">Laat leeg om het huidige wachtwoord te behouden.</p>
            }
          </label>
          <label>
            <span class="mb-2 block text-sm font-semibold">Rol</span>
            <p-dropdown [options]="roleOptions" formControlName="role" optionLabel="label" optionValue="value" styleClass="w-full"></p-dropdown>
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
export class EmployeesComponent implements OnInit {
  private readonly fb = inject(FormBuilder);

  users: User[] = [];
  loading = false;
  dialogOpen = false;

  readonly roleOptions: SelectOption<Role>[] = [
    { label: 'Eigenaar', value: 'OWNER' },
    { label: 'Medewerker', value: 'EMPLOYEE' }
  ];

  readonly form = this.fb.group({
    id: [null as number | null],
    name: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
    password: [''],
    role: ['EMPLOYEE' as Role, Validators.required]
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
    this.api.users().subscribe({
      next: (users) => this.users = users,
      complete: () => this.loading = false
    });
  }

  openCreate(): void {
    this.form.reset({ id: null, name: '', email: '', password: '', role: 'EMPLOYEE' });
    this.form.controls.password.setValidators([Validators.required, Validators.minLength(8)]);
    this.form.controls.password.updateValueAndValidity();
    this.dialogOpen = true;
  }

  openEdit(user: User): void {
    this.form.reset({ id: user.id, name: user.name, email: user.email, password: '', role: user.role });
    this.form.controls.password.clearValidators();
    this.form.controls.password.updateValueAndValidity();
    this.dialogOpen = true;
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const raw = this.form.getRawValue();
    const payload: Record<string, unknown> = {
      id: raw.id,
      name: raw.name,
      email: raw.email,
      role: raw.role
    };
    if (!raw.id || raw.password) {
      payload['password'] = raw.password;
    }

    this.api.saveUser(payload).subscribe({
      next: () => {
        this.messages.add({ severity: 'success', summary: 'Opgeslagen', detail: 'Medewerker is opgeslagen.' });
        this.dialogOpen = false;
        this.load();
      }
    });
  }

  confirmDelete(user: User): void {
    this.confirmations.confirm({
      message: `Weet u zeker dat u ${user.name} wilt verwijderen?`,
      header: 'Medewerker verwijderen',
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: 'Verwijderen',
      rejectLabel: 'Annuleren',
      acceptButtonStyleClass: 'p-button-danger',
      accept: () => this.api.deleteUser(user.id).subscribe(() => this.load())
    });
  }
}
