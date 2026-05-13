import { ApplicationConfig, LOCALE_ID, importProvidersFrom } from '@angular/core';
import { registerLocaleData } from '@angular/common';
import localeNl from '@angular/common/locales/nl';
import { provideAnimations } from '@angular/platform-browser/animations';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { ConfirmationService, MessageService } from 'primeng/api';
import { providePrimeNG } from 'primeng/config';
import Aura from '@primeng/themes/aura';
import {
  BadgeEuro,
  BookOpen,
  CalendarDays,
  CheckCircle2,
  CreditCard,
  Download,
  Edit,
  Eye,
  FileText,
  LayoutDashboard,
  LogOut,
  Menu,
  Moon,
  Plus,
  ReceiptText,
  Save,
  Search,
  Send,
  Settings,
  Shield,
  Sun,
  Trash2,
  UserCog,
  Users,
  X
} from 'lucide-angular';
import { LucideAngularModule } from 'lucide-angular';
import { routes } from './app.routes';
import { authInterceptor } from './core/auth.interceptor';

registerLocaleData(localeNl);

export const appConfig: ApplicationConfig = {
  providers: [
    { provide: LOCALE_ID, useValue: 'nl-NL' },
    provideRouter(routes),
    provideAnimations(),
    provideHttpClient(withInterceptors([authInterceptor])),
    providePrimeNG({
      theme: {
        preset: Aura,
        options: {
          darkModeSelector: '.dark'
        }
      }
    }),
    importProvidersFrom(LucideAngularModule.pick({
      BadgeEuro,
      BookOpen,
      CalendarDays,
      CheckCircle2,
      CreditCard,
      Download,
      Edit,
      Eye,
      FileText,
      LayoutDashboard,
      LogOut,
      Menu,
      Moon,
      Plus,
      ReceiptText,
      Save,
      Search,
      Send,
      Settings,
      Shield,
      Sun,
      Trash2,
      UserCog,
      Users,
      X
    })),
    ConfirmationService,
    MessageService
  ]
};
