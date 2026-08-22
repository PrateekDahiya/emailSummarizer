export interface Email {
  id: string;
  gmailMessageId: string;
  threadId: string;
  sender: string;
  senderEmail: string;
  subject: string;
  snippet: string;
  body: string;
  receivedAt: string;
  labels: string[];
  hasAttachments: boolean;
  createdAt: string;
}

export interface EmailClassification {
  emailId: string;
  category: EmailCategory;
  importanceScore: number;
  summary: string;
  actionRequired: boolean;
  action: string | null;
  deadline: string | null;
  confidence: number;
  entities: ExtractedEntities;
}

export type EmailCategory = 
  | 'JOB'
  | 'INTERVIEW'
  | 'TRAVEL'
  | 'FINANCE'
  | 'PURCHASE'
  | 'MEETING'
  | 'DEADLINE'
  | 'DOCUMENT'
  | 'PERSONAL'
  | 'NEWSLETTER'
  | 'PROMOTION'
  | 'OTHER';

export interface ExtractedEntities {
  // Job-related
  company?: string;
  role?: string;
  applicationStatus?: ApplicationStatus;
  interviewStage?: string;
  interviewDate?: string;
  recruiterContact?: string;

  // Travel-related
  airline?: string;
  flightNumber?: string;
  departure?: string;
  arrival?: string;
  hotel?: string;
  bookingNumber?: string;
  travelDates?: string[];

  // Deadline-related
  deadlineType?: string;
  deadlineDate?: string;

  // Event-related
  eventTitle?: string;
  eventDate?: string;
  eventTime?: string;
  eventLocation?: string;
  eventType?: string;

  // Money-related
  amount?: number;
  currency?: string;
  transactionType?: string;

  // People
  people?: string[];
}

export type ApplicationStatus = 
  | 'APPLIED'
  | 'INTERVIEW'
  | 'OFFER'
  | 'REJECTED'
  | 'WAITING';

export interface JobApplication {
  id: string;
  company: string;
  role: string;
  status: ApplicationStatus;
  appliedAt: string;
  updatedAt: string;
  interviewDate?: string;
  recruiterName?: string;
  recruiterEmail?: string;
  sourceEmailIds: string[];
}

export interface TravelTrip {
  id: string;
  name: string;
  destination: string;
  startDate: string;
  endDate: string;
  flights: Flight[];
  hotels: Hotel[];
  events: TravelEvent[];
  totalCost?: number;
}

export interface Flight {
  id: string;
  airline: string;
  flightNumber: string;
  departure: string;
  arrival: string;
  departureTime: string;
  arrivalTime: string;
  bookingNumber: string;
}

export interface Hotel {
  id: string;
  name: string;
  checkIn: string;
  checkOut: string;
  bookingNumber: string;
  cost?: number;
}

export interface TravelEvent {
  id: string;
  title: string;
  date: string;
  time: string;
  location: string;
}

export interface DashboardData {
  needsAttention: AttentionItem[];
  upcoming: UpcomingItem[];
  recentImportant: RecentEmail[];
  jobApplications: JobApplication[];
  upcomingTrips: TravelTrip[];
}

export interface AttentionItem {
  id: string;
  type: 'interview' | 'deadline' | 'document' | 'meeting' | 'payment' | 'travel';
  title: string;
  description: string;
  date: string;
  time?: string;
  priority: 'high' | 'medium' | 'low';
  sourceEmailId: string;
  actionUrl?: string;
}

export interface UpcomingItem {
  id: string;
  type: 'interview' | 'flight' | 'hotel' | 'meeting' | 'deadline' | 'event';
  title: string;
  date: string;
  time?: string;
  location?: string;
  sourceEmailId: string;
}

export interface RecentEmail {
  id: string;
  sender: string;
  subject: string;
  category: EmailCategory;
  importanceScore: number;
  receivedAt: string;
  summary: string;
  actionRequired: boolean;
}

export interface User {
  id: string;
  email: string;
  name: string;
  picture: string;
  gmailConnected: boolean;
  lastSyncAt?: string;
}

export interface AuthState {
  user: User | null;
  isLoading: boolean;
  isAuthenticated: boolean;
}

export interface SyncStatus {
  isSyncing: boolean;
  lastSyncedAt: string | null;
  totalEmails: number;
  processedEmails: number;
  error: string | null;
}