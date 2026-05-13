export type Role = 'OWNER' | 'EMPLOYEE';
export type EventType = 'BRUILOFT' | 'VERJAARDAG' | 'CONGRES' | 'OVERIG';
export type BookingStatus =
  | 'CONCEPT'
  | 'OFFERTE_VERZONDEN'
  | 'BEVESTIGD'
  | 'AANBETALING_BETAALD'
  | 'VOLLEDIG_BETAALD'
  | 'AFGEROND'
  | 'GEANNULEERD';
export type ContractStatus = 'GEEN' | 'CONCEPT' | 'VERZONDEN' | 'ONDERTEKEND';
export type InvoiceStatus = 'CONCEPT' | 'ONBETAALD' | 'BETAALD' | 'VERLOPEN';
export type InvoiceType = 'VOLLEDIG' | 'AANBETALING' | 'RESTANT';
export type PaymentMethod = 'BANK' | 'CASH' | 'PIN';
export type PaymentPart = 'AANBETALING' | 'RESTANT';
export type PaymentState = 'OPENSTAAND' | 'BETAALD' | 'VERLOPEN';

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
  naam: string;
  name: string;
  email: string;
  telefoon: string;
  phone: string;
  adres?: string;
  address?: string;
  createdAt: string;
  updatedAt: string;
}

export interface SubPrijs {
  id?: number;
  bookingId?: number;
  naam: string;
  bedrag: number;
  prijs: number;
  position?: number;
}

export interface Booking {
  id: number;
  customerId: number;
  customerName: string;
  klantNaam: string;
  customerEmail: string;
  customerPhone: string;
  customerAddress?: string;
  evenementDatum: string;
  eventDate: string;
  date: string;
  endDate: string;
  startTijd: string;
  startTime: string;
  eindTijd: string;
  endTime: string;
  evenementType: EventType;
  eventType: EventType;
  aantalGasten: number;
  guestCount: number;
  subtotaal: number;
  korting: number;
  totaal: number;
  price: number;
  aanbetalingPercentage: number;
  aanbetalingBedrag: number;
  restantBedrag: number;
  aanbetalingDeadline?: string;
  restantDeadline?: string;
  subPrijzen: SubPrijs[];
  status: BookingStatus;
  notes?: string;
  eigenschappen: string[];
  properties: string[];
  conditions?: string;
  contractStatus: ContractStatus;
  docusealSubmissionId?: string;
  offerteDatum?: string;
  offerteSentDate?: string;
  ondertekeningDatum?: string;
  contractSignedDate?: string;
  aanbetalingBetaald: boolean;
  aanbetalingBetaaldDatum?: string;
  restantBetaald: boolean;
  restantBetaaldDatum?: string;
  offertePdfPath?: string;
  annuleringsReden?: string;
  invoiceId?: number;
  createdAt: string;
  updatedAt: string;
}

export interface Offerte {
  bookingId: number;
  status: BookingStatus;
  documentRef: string;
  offerteDatum?: string;
  offerteSentDate?: string;
  ondertekeningDatum?: string;
  docusealSubmissionId?: string;
  pdfPath?: string;
  downloadUrl?: string;
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

export interface PaymentSchedule {
  bookingId: number;
  customerId: number;
  klantNaam: string;
  customerName: string;
  customerEmail: string;
  evenementDatum: string;
  evenementType: EventType;
  bookingStatus: BookingStatus;
  type: PaymentPart;
  bedrag: number;
  deadline?: string;
  status: PaymentState;
  betaaldDatum?: string;
  locked: boolean;
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
  docusealHussainEmail?: string;
  docusealHussainSignatureToken?: string;
  googleReviewUrl?: string;
  smtpHost: string;
  smtpPort: number;
  smtpUsername?: string;
  smtpPassword?: string;
  smtpFrom?: string;
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
