import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import {
  Booking,
  BookingStatus,
  Bezichtiging,
  BezichtigingStatus,
  CompanySettings,
  Contract,
  ContractStatus,
  Customer,
  DashboardStats,
  DocusealConnection,
  EventType,
  Invoice,
  InvoiceStatus,
  MailLog,
  MailLogStatus,
  MailLogType,
  Offerte,
  OfferteDraft,
  SubPrijs,
  Payment,
  PaymentPart,
  PaymentSchedule,
  PaymentState,
  User
} from './models';

@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly apiUrl = environment.apiUrl;

  constructor(private readonly http: HttpClient) {}

  dashboardStats(): Observable<DashboardStats> {
    return this.http.get<DashboardStats>(`${this.apiUrl}/dashboard/stats`);
  }

  upcomingBookings(): Observable<Booking[]> {
    return this.http.get<Booking[]>(`${this.apiUrl}/dashboard/upcoming-bookings`);
  }

  customers(search?: string): Observable<Customer[]> {
    const params = search ? new HttpParams().set('search', search) : undefined;
    return this.http.get<Customer[]>(`${this.apiUrl}/customers`, { params });
  }

  customer(id: number): Observable<Customer> {
    return this.http.get<Customer>(`${this.apiUrl}/customers/${id}`);
  }

  customerBookings(id: number): Observable<Booking[]> {
    return this.http.get<Booking[]>(`${this.apiUrl}/customers/${id}/bookings`);
  }

  saveCustomer(customer: Record<string, unknown>): Observable<Customer> {
    return customer['id']
      ? this.http.put<Customer>(`${this.apiUrl}/customers/${customer['id']}`, customer)
      : this.http.post<Customer>(`${this.apiUrl}/customers`, customer);
  }

  deleteCustomer(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/customers/${id}`);
  }

  bookings(filters?: {
    status?: BookingStatus | null;
    eventType?: EventType | null;
    startDate?: string | null;
    endDate?: string | null;
    customer?: string | null;
  }): Observable<Booking[]> {
    let params = new HttpParams();
    Object.entries(filters ?? {}).forEach(([key, value]) => {
      if (value) {
        params = params.set(key, value);
      }
    });
    return this.http.get<Booking[]>(`${this.apiUrl}/bookings`, { params });
  }

  calendarBookings(start: string, end: string): Observable<Booking[]> {
    return this.http.get<Booking[]>(`${this.apiUrl}/bookings/calendar`, {
      params: new HttpParams().set('start', start).set('end', end)
    });
  }

  saveBooking(booking: Record<string, unknown>): Observable<Booking> {
    return booking['id']
      ? this.http.put<Booking>(`${this.apiUrl}/bookings/${booking['id']}`, booking)
      : this.http.post<Booking>(`${this.apiUrl}/bookings`, booking);
  }

  booking(id: number): Observable<Booking> {
    return this.http.get<Booking>(`${this.apiUrl}/bookings/${id}`);
  }

  updateBookingStatus(id: number, status: BookingStatus): Observable<Booking> {
    return this.http.patch<Booking>(`${this.apiUrl}/bookings/${id}/status`, { status });
  }

  bezichtigingen(status?: BezichtigingStatus | null): Observable<Bezichtiging[]> {
    const params = status ? new HttpParams().set('status', status) : undefined;
    return this.http.get<Bezichtiging[]>(`${this.apiUrl}/bezichtigingen`, { params });
  }

  upcomingBezichtigingen(): Observable<Bezichtiging[]> {
    return this.http.get<Bezichtiging[]>(`${this.apiUrl}/bezichtigingen/upcoming`);
  }

  calendarBezichtigingen(start: string, end: string): Observable<Bezichtiging[]> {
    return this.http.get<Bezichtiging[]>(`${this.apiUrl}/bezichtigingen/calendar`, {
      params: new HttpParams().set('start', start).set('end', end)
    });
  }

  bezichtiging(id: number): Observable<Bezichtiging> {
    return this.http.get<Bezichtiging>(`${this.apiUrl}/bezichtigingen/${id}`);
  }

  saveBezichtiging(bezichtiging: Record<string, unknown>): Observable<Bezichtiging> {
    return bezichtiging['id']
      ? this.http.put<Bezichtiging>(`${this.apiUrl}/bezichtigingen/${bezichtiging['id']}`, bezichtiging)
      : this.http.post<Bezichtiging>(`${this.apiUrl}/bezichtigingen`, bezichtiging);
  }

  updateBezichtigingStatus(id: number, status: BezichtigingStatus): Observable<Bezichtiging> {
    return this.http.patch<Bezichtiging>(`${this.apiUrl}/bezichtigingen/${id}/status`, { status });
  }

  linkBezichtigingToBoeking(bezichtigingId: number, boekingId: number): Observable<Bezichtiging> {
    return this.http.post<Bezichtiging>(`${this.apiUrl}/bezichtigingen/${bezichtigingId}/link-boeking/${boekingId}`, {});
  }

  downloadBezichtigingIcs(id: number): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/bezichtigingen/${id}/ics`, { responseType: 'blob' });
  }

  cancelBooking(id: number, reason: string): Observable<Booking> {
    return this.http.post<Booking>(`${this.apiUrl}/bookings/${id}/cancel`, { reason });
  }

  deleteBooking(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/bookings/${id}`);
  }

  subPrijzen(bookingId: number): Observable<SubPrijs[]> {
    return this.http.get<SubPrijs[]>(`${this.apiUrl}/bookings/${bookingId}/subprijzen`);
  }

  saveSubPrijs(bookingId: number, subPrijs: SubPrijs): Observable<SubPrijs> {
    return subPrijs.id
      ? this.http.put<SubPrijs>(`${this.apiUrl}/bookings/${bookingId}/subprijzen/${subPrijs.id}`, subPrijs)
      : this.http.post<SubPrijs>(`${this.apiUrl}/bookings/${bookingId}/subprijzen`, subPrijs);
  }

  deleteSubPrijs(bookingId: number, subPrijsId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/bookings/${bookingId}/subprijzen/${subPrijsId}`);
  }

  contracts(filters?: {
    contractStatus?: ContractStatus | null;
    customer?: string | null;
  }): Observable<Booking[]> {
    let params = new HttpParams();
    Object.entries(filters ?? {}).forEach(([key, value]) => {
      if (value) {
        params = params.set(key, value);
      }
    });
    return this.http.get<Booking[]>(`${this.apiUrl}/contracts`, { params });
  }

  bookingContract(id: number): Observable<Contract> {
    return this.http.get<Contract>(`${this.apiUrl}/bookings/${id}/contract`);
  }

  generateBookingContract(id: number): Observable<Contract> {
    return this.http.post<Contract>(`${this.apiUrl}/bookings/${id}/contract/generate`, {});
  }

  saveBookingContract(id: number, html: string): Observable<Contract> {
    return this.http.put<Contract>(`${this.apiUrl}/bookings/${id}/contract/concept`, { html });
  }

  sendBookingContract(id: number, html: string): Observable<Contract> {
    return this.http.post<Contract>(`${this.apiUrl}/bookings/${id}/contract/send`, { html });
  }

  markBookingContractSigned(id: number): Observable<Contract> {
    return this.http.post<Contract>(`${this.apiUrl}/bookings/${id}/contract/mark-signed`, {});
  }

  downloadSignedContract(id: number): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/bookings/${id}/contract/download-signed`, { responseType: 'blob' });
  }

  offerte(bookingId: number): Observable<Offerte> {
    return this.http.get<Offerte>(`${this.apiUrl}/bookings/${bookingId}/offerte`);
  }

  offertePreview(bookingId: number): Observable<string> {
    return this.http.get(`${this.apiUrl}/bookings/${bookingId}/offerte/preview`, { responseType: 'text' });
  }

  offertePreviewHtml(bookingId: number, draft: OfferteDraft): Observable<string> {
    return this.http.post(`${this.apiUrl}/bookings/${bookingId}/offerte/preview`, draft, { responseType: 'text' });
  }

  offertePreviewPdf(bookingId: number, draft: OfferteDraft): Observable<Blob> {
    return this.http.post(`${this.apiUrl}/bookings/${bookingId}/offerte/preview/pdf`, draft, { responseType: 'blob' });
  }

  generateOfferte(bookingId: number): Observable<Offerte> {
    return this.http.post<Offerte>(`${this.apiUrl}/bookings/${bookingId}/offerte/generate`, {});
  }

  saveOfferteConcept(bookingId: number, draft: OfferteDraft): Observable<Offerte> {
    return this.http.post<Offerte>(`${this.apiUrl}/bookings/${bookingId}/offerte/concept`, draft);
  }

  sendOfferte(bookingId: number, draft?: OfferteDraft): Observable<Offerte> {
    return this.http.post<Offerte>(`${this.apiUrl}/bookings/${bookingId}/offerte/send`, draft ?? {});
  }

  downloadOfferte(bookingId: number): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/bookings/${bookingId}/offerte/download`, { responseType: 'blob' });
  }

  invoices(status?: InvoiceStatus | null): Observable<Invoice[]> {
    const params = status ? new HttpParams().set('status', status) : undefined;
    return this.http.get<Invoice[]>(`${this.apiUrl}/invoices`, { params });
  }

  bookingInvoices(bookingId: number): Observable<Invoice[]> {
    return this.http.get<Invoice[]>(`${this.apiUrl}/invoices/booking/${bookingId}`);
  }

  saveInvoice(invoice: Record<string, unknown>): Observable<Invoice> {
    return invoice['id']
      ? this.http.put<Invoice>(`${this.apiUrl}/invoices/${invoice['id']}`, invoice)
      : this.http.post<Invoice>(`${this.apiUrl}/invoices`, invoice);
  }

  createBookingInvoiceOptionA(bookingId: number): Observable<Invoice[]> {
    return this.http.post<Invoice[]>(`${this.apiUrl}/invoices/booking/${bookingId}/option-a`, {});
  }

  createBookingInvoiceOptionB(bookingId: number): Observable<Invoice[]> {
    return this.http.post<Invoice[]>(`${this.apiUrl}/invoices/booking/${bookingId}/option-b`, {});
  }

  sendBookingInvoices(bookingId: number): Observable<Invoice[]> {
    return this.http.post<Invoice[]>(`${this.apiUrl}/invoices/booking/${bookingId}/send`, {});
  }

  generateInvoicePdf(id: number): Observable<Invoice> {
    return this.http.post<Invoice>(`${this.apiUrl}/invoices/${id}/generate-pdf`, {});
  }

  downloadInvoicePdf(id: number): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/invoices/${id}/download`, { responseType: 'blob' });
  }

  markInvoicePaid(id: number): Observable<Invoice> {
    return this.http.post<Invoice>(`${this.apiUrl}/invoices/${id}/mark-paid`, {});
  }

  sendInvoiceReminder(id: number): Observable<Invoice> {
    return this.http.post<Invoice>(`${this.apiUrl}/invoices/${id}/send-reminder`, {});
  }

  deleteInvoice(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/invoices/${id}`);
  }

  payments(status?: PaymentState | null, search?: string | null): Observable<PaymentSchedule[]> {
    let params = new HttpParams();
    if (status) {
      params = params.set('status', status);
    }
    if (search) {
      params = params.set('search', search);
    }
    return this.http.get<PaymentSchedule[]>(`${this.apiUrl}/payments`, { params });
  }

  bookingPayments(bookingId: number): Observable<PaymentSchedule[]> {
    return this.http.get<PaymentSchedule[]>(`${this.apiUrl}/payments/bookings/${bookingId}`);
  }

  markBookingPaymentPaid(bookingId: number, type: PaymentPart): Observable<PaymentSchedule> {
    return this.http.post<PaymentSchedule>(`${this.apiUrl}/payments/bookings/${bookingId}/mark-paid`, { type });
  }

  legacyPayments(): Observable<Payment[]> {
    return this.http.get<Payment[]>(`${this.apiUrl}/payments`);
  }

  savePayment(payment: Record<string, unknown>): Observable<Payment> {
    return payment['id']
      ? this.http.put<Payment>(`${this.apiUrl}/payments/${payment['id']}`, payment)
      : this.http.post<Payment>(`${this.apiUrl}/payments`, payment);
  }

  deletePayment(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/payments/${id}`);
  }

  users(): Observable<User[]> {
    return this.http.get<User[]>(`${this.apiUrl}/users`);
  }

  saveUser(user: Record<string, unknown>): Observable<User> {
    return user['id']
      ? this.http.put<User>(`${this.apiUrl}/users/${user['id']}`, user)
      : this.http.post<User>(`${this.apiUrl}/users`, user);
  }

  deleteUser(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/users/${id}`);
  }

  settings(): Observable<CompanySettings> {
    return this.http.get<CompanySettings>(`${this.apiUrl}/settings`);
  }

  saveSettings(settings: Partial<CompanySettings>): Observable<CompanySettings> {
    return this.http.put<CompanySettings>(`${this.apiUrl}/settings`, settings);
  }

  invoiceTemplate(): Observable<string> {
    return this.http.get(`${this.apiUrl}/settings/invoice-template`, { responseType: 'text' });
  }

  testDocusealConnection(): Observable<DocusealConnection> {
    return this.http.post<DocusealConnection>(`${this.apiUrl}/settings/docuseal/test`, {});
  }

  uploadLogo(file: File): Observable<CompanySettings> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<CompanySettings>(`${this.apiUrl}/settings/logo`, formData);
  }

  mailLogs(filters?: {
    type?: MailLogType | null;
    status?: MailLogStatus | null;
    search?: string | null;
  }): Observable<MailLog[]> {
    let params = new HttpParams();
    Object.entries(filters ?? {}).forEach(([key, value]) => {
      if (value) {
        params = params.set(key, value);
      }
    });
    return this.http.get<MailLog[]>(`${this.apiUrl}/mail-logs`, { params });
  }

  bookingMailLogs(bookingId: number): Observable<MailLog[]> {
    return this.http.get<MailLog[]>(`${this.apiUrl}/mail-logs/booking/${bookingId}`);
  }

  bezichtigingMailLogs(bezichtigingId: number): Observable<MailLog[]> {
    return this.http.get<MailLog[]>(`${this.apiUrl}/mail-logs/bezichtiging/${bezichtigingId}`);
  }

  resendMailLog(id: number): Observable<MailLog> {
    return this.http.post<MailLog>(`${this.apiUrl}/mail-logs/${id}/resend`, {});
  }
}
