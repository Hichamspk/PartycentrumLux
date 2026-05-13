export type Role = 'OWNER' | 'EMPLOYEE';
export type EventType = 'BRUILOFT' | 'VERJAARDAG' | 'CONGRES' | 'OVERIG';
export type BookingStatus =
  | 'CONCEPT'
  | 'CONTRACT_VERZONDEN'
  | 'CONTRACT_ONDERTEKEND'
  | 'FACTUUR_VERZONDEN'
  | 'AANBETALING_BETAALD'
  | 'VOLLEDIG_BETAALD'
  | 'AFGEROND'
  | 'GEANNULEERD';
export type ContractStatus = 'GEEN' | 'CONCEPT' | 'VERZONDEN' | 'ONDERTEKEND';
export type InvoiceStatus = 'CONCEPT' | 'ONBETAALD' | 'BETAALD' | 'VERLOPEN';
export type InvoiceType = 'VOLLEDIG' | 'AANBETALING' | 'RESTANT';
export type PaymentMethod = 'BANK' | 'CASH' | 'PIN';

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  userId: number;
  name: string;
  email: string;
  role: Role;
}

export interface Customer {
  id: number;
  name: string;
  email: string;
  phone: string;
  address?: string;
  createdAt: string;
  updatedAt: string;
}

export interface SubPrijs {
  id?: number;
  bookingId?: number;
  naam: string;
  prijs: number;
  position?: number;
}

export interface Booking {
  id: number;
  customerId: number;
  customerName: string;
  customerEmail: string;
  customerPhone: string;
  customerAddress?: string;
  eventDate: string;
  date: string;
  endDate: string;
  startTime: string;
  endTime: string;
  eventType: EventType;
  guestCount: number;
  price: number;
  subPrijzen: SubPrijs[];
  status: BookingStatus;
  notes?: string;
  properties: string[];
  conditions?: string;
  contractStatus: ContractStatus;
  docusealSubmissionId?: string;
  contractSignedDate?: string;
  annuleringsReden?: string;
  invoiceId?: number;
  createdAt: string;
  updatedAt: string;
}

export interface Contract {
  bookingId: number;
  status: ContractStatus;
  html?: string;
  docusealSubmissionId?: string;
  signedDate?: string;
}

export interface Invoice {
  id: number;
  bookingId: number;
  customerName: string;
  invoiceNumber: string;
  invoiceType: InvoiceType;
  invoiceDate?: string;
  amount: number;
  vatAmount: number;
  totalAmount: number;
  status: InvoiceStatus;
  dueDate: string;
  paidDate?: string;
  pdfPath?: string;
  downloadUrl?: string;
  createdAt: string;
  updatedAt: string;
}

export interface Payment {
  id: number;
  invoiceId: number;
  invoiceNumber: string;
  bookingId: number;
  customerName: string;
  amount: number;
  paymentDate: string;
  paymentMethod: PaymentMethod;
  notes?: string;
  createdAt: string;
  updatedAt: string;
}

export interface User {
  id: number;
  name: string;
  email: string;
  role: Role;
  createdAt: string;
  updatedAt: string;
}

export interface CompanySettings {
  id: number;
  companyName: string;
  logoPath?: string;
  logoBase64?: string;
  brandColor: string;
  address: string;
  postalCode: string;
  city: string;
  kvk: string;
  vatNumber: string;
  iban: string;
  phone: string;
  email: string;
  website: string;
  mailFrom: string;
  docusealApiKey?: string;
  docusealBaseUrl: string;
  docusealContractTemplateId?: string;
  generalTerms: string;
  createdAt: string;
  updatedAt: string;
}

export interface DocusealConnection {
  ok: boolean;
  message: string;
}

export interface ChartPoint {
  label: string;
  amount: number;
  count: number;
}

export interface DashboardStats {
  revenueThisMonth: number;
  bookingsThisMonth: number;
  outstandingPayments: number;
  occupancyRate: number;
  revenuePerMonth: ChartPoint[];
  bookingsPerMonth: ChartPoint[];
  upcomingBookings: Booking[];
  recentInvoices: Invoice[];
}

export interface SelectOption<T = string> {
  label: string;
  value: T;
}
