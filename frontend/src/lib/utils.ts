import { clsx, type ClassValue } from 'clsx';
import { twMerge } from 'tailwind-merge';

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

export function formatDate(dateString: string, options?: Intl.DateTimeFormatOptions): string {
  const date = new Date(dateString);
  return date.toLocaleDateString('en-US', {
    month: 'short',
    day: 'numeric',
    ...options,
  });
}

export function formatTime(dateString: string): string {
  const date = new Date(dateString);
  return date.toLocaleTimeString('en-US', {
    hour: 'numeric',
    minute: '2-digit',
    hour12: true,
  });
}

export function formatRelativeTime(dateString: string): string {
  const date = new Date(dateString);
  const now = new Date();
  const diffMs = date.getTime() - now.getTime();
  const diffDays = Math.ceil(diffMs / (1000 * 60 * 60 * 24));

  if (diffDays < 0) {
    return `${Math.abs(diffDays)} day${Math.abs(diffDays) > 1 ? 's' : ''} ago`;
  } else if (diffDays === 0) {
    return 'Today';
  } else if (diffDays === 1) {
    return 'Tomorrow';
  } else if (diffDays <= 7) {
    return `In ${diffDays} days`;
  } else {
    return formatDate(dateString);
  }
}

export function getCategoryColor(category: string): string {
  const colors: Record<string, string> = {
    JOB: 'bg-blue-100 text-blue-800',
    INTERVIEW: 'bg-purple-100 text-purple-800',
    TRAVEL: 'bg-green-100 text-green-800',
    FINANCE: 'bg-yellow-100 text-yellow-800',
    PURCHASE: 'bg-orange-100 text-orange-800',
    MEETING: 'bg-indigo-100 text-indigo-800',
    DEADLINE: 'bg-red-100 text-red-800',
    DOCUMENT: 'bg-pink-100 text-pink-800',
    PERSONAL: 'bg-gray-100 text-gray-800',
    NEWSLETTER: 'bg-slate-100 text-slate-800',
    PROMOTION: 'bg-amber-100 text-amber-800',
    OTHER: 'bg-neutral-100 text-neutral-800',
  };
  return colors[category] || colors.OTHER;
}

export function getCategoryIcon(category: string): string {
  const icons: Record<string, string> = {
    JOB: 'briefcase',
    INTERVIEW: 'user-check',
    TRAVEL: 'plane',
    FINANCE: 'dollar-sign',
    PURCHASE: 'shopping-bag',
    MEETING: 'calendar',
    DEADLINE: 'alert-triangle',
    DOCUMENT: 'file-text',
    PERSONAL: 'heart',
    NEWSLETTER: 'mail',
    PROMOTION: 'tag',
    OTHER: 'mail',
  };
  return icons[category] || icons.OTHER;
}

export function getPriorityColor(priority: string): string {
  const colors: Record<string, string> = {
    high: 'bg-red-100 text-red-800 border-red-200',
    medium: 'bg-yellow-100 text-yellow-800 border-yellow-200',
    low: 'bg-green-100 text-green-800 border-green-200',
  };
  return colors[priority] || colors.medium;
}

export function truncate(str: string, length: number): string {
  if (str.length <= length) return str;
  return str.slice(0, length).trim() + '...';
}

export function extractEmailDomain(email: string): string {
  const match = email.match(/@([^>]+)/);
  return match ? match[1] : email;
}