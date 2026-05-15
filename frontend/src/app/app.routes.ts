import { Routes } from '@angular/router';
import { authGuard } from './core/auth.guard';
import { roleGuard } from './core/role.guard';
import { ShellComponent } from './layout/shell.component';
import { LoginComponent } from './pages/login.component';
import { DashboardComponent } from './pages/dashboard.component';
import { CalendarComponent } from './pages/calendar.component';
import { BookingsComponent } from './pages/bookings.component';
import { BookingDetailComponent } from './pages/booking-detail.component';
import { OfferteEditorComponent } from './pages/offerte-editor.component';
import { CustomersComponent } from './pages/customers.component';
import { PaymentsComponent } from './pages/payments.component';
import { EmployeesComponent } from './pages/employees.component';
import { SettingsComponent } from './pages/settings.component';

export const routes: Routes = [
  { path: 'login', component: LoginComponent },
  {
    path: '',
    component: ShellComponent,
    canActivate: [authGuard],
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
      { path: 'dashboard', component: DashboardComponent },
      { path: 'kalender', component: CalendarComponent },
      { path: 'boekingen', component: BookingsComponent },
      { path: 'boekingen/:id/offerte', component: OfferteEditorComponent, canActivate: [roleGuard], data: { roles: ['OWNER'] } },
      { path: 'boekingen/:id', component: BookingDetailComponent },
      { path: 'klanten', component: CustomersComponent },
      { path: 'betalingen', component: PaymentsComponent },
      { path: 'medewerkers', component: EmployeesComponent, canActivate: [roleGuard], data: { roles: ['OWNER'] } },
      { path: 'instellingen', component: SettingsComponent, canActivate: [roleGuard], data: { roles: ['OWNER'] } }
    ]
  },
  { path: '**', redirectTo: 'dashboard' }
];
