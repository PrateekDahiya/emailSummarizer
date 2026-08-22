'use client';

import { Mail, AlertCircle, Clock, ExternalLink } from 'lucide-react';
import { cn, getCategoryColor, getCategoryIcon, formatRelativeTime, truncate } from '@/lib/utils';
import type { RecentEmail, EmailCategory } from '@/types';

interface RecentImportantEmailsProps {
  emails: RecentEmail[];
}

const categoryIcons: Record<EmailCategory, React.ComponentType<{ className?: string }>> = {
  JOB: Mail,
  INTERVIEW: AlertCircle,
  TRAVEL: Clock,
  FINANCE: Mail,
  PURCHASE: Mail,
  MEETING: Clock,
  DEADLINE: AlertCircle,
  DOCUMENT: Mail,
  PERSONAL: Mail,
  NEWSLETTER: Mail,
  PROMOTION: Mail,
  OTHER: Mail,
};

export function RecentImportantEmails({ emails }: RecentImportantEmailsProps) {
  if (emails.length === 0) return null;

  return (
    <section className="space-y-4">
      <h2 className="text-lg font-semibold text-gray-900 dark:text-white flex items-center gap-2">
        <Mail className="w-5 h-5 text-primary-600 dark:text-primary-400" />
        Recent Important Emails
      </h2>

      <div className="space-y-2">
        {emails.map((email) => {
          const Icon = categoryIcons[email.category];
          return (
            <article
              key={email.id}
              className="group flex items-start gap-3 p-4 rounded-xl bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 hover:border-primary-200 dark:hover:border-primary-800 transition-colors"
            >
              <div className={cn('w-10 h-10 rounded-lg flex items-center justify-center flex-shrink-0', getCategoryColor(email.category))}>
                <Icon className="w-5 h-5" />
              </div>
              <div className="flex-1 min-w-0">
                <div className="flex items-start justify-between gap-2">
                  <div className="flex-1 min-w-0">
                    <p className="font-medium text-gray-900 dark:text-white truncate">{email.sender}</p>
                    <p className="text-sm text-gray-600 dark:text-gray-400 truncate mt-0.5">{email.subject}</p>
                  </div>
                  {email.actionRequired && (
                    <span className="flex-shrink-0 px-2 py-0.5 text-xs font-medium bg-red-100 dark:bg-red-900/30 text-red-700 dark:text-red-300 rounded-full">
                      Action Required
                    </span>
                  )}
                </div>
                <p className="mt-1 text-sm text-gray-500 dark:text-gray-400 line-clamp-2">{truncate(email.summary, 120)}</p>
                <div className="mt-2 flex items-center gap-3 text-xs text-gray-400 dark:text-gray-500">
                  <span className="flex items-center gap-1">
                    <Clock className="w-3.5 h-3.5" />
                    {formatRelativeTime(email.receivedAt)}
                  </span>
                  <span className={cn('px-2 py-0.5 rounded-full text-xs font-medium', getCategoryColor(email.category))}>
                    {email.category}
                  </span>
                  <span className="font-medium text-gray-600 dark:text-gray-400">
                    {email.importanceScore}/100
                  </span>
                </div>
              </div>
              <button className="p-1.5 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors opacity-0 group-hover:opacity-100" aria-label="Open email">
                <ExternalLink className="w-4 h-4 text-gray-400" />
              </button>
            </article>
          );
        })}
      </div>
    </section>
  );
}