import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ToastModule } from 'primeng/toast';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, ConfirmDialogModule, ToastModule],
  template: `
    <p-toast></p-toast>
    <p-confirmDialog></p-confirmDialog>
    <router-outlet></router-outlet>
  `
})
export class AppComponent {}
