'use client';

import { Briefcase, Building2, UserCheck, Clock, CheckCircle, XCircle, Loader2, AlertCircle } from 'lucide-react';
import { cn, formatRelativeTime } from '@/lib/utils';
import type { JobApplication, ApplicationStatus } from '@/types';

interface JobApplicationsSectionProps {
  applications: JobApplication[];
}

const statusConfig: Record<ApplicationStatus, { label: string; color: string; icon: React.ComponentType<{ className?: string }> }> = {
  APPLIED: { label: 'Applied', color: 'bg-blue-100 dark:bg-blue-900/30 text-blue-700 dark:text-blue-300', icon: Briefcase },
  INTERVIEW: { label: 'Interview', color: 'bg-purple-100 dark:bg-purple-900/30 text-purple-700 dark:text-purple-300', icon: UserCheck },
  OFFER: { label: 'Offer', color: 'bg-green-100 dark:bg-green-900/30 text-green-700 dark:text-green-300', icon: CheckCircle },
  REJECTED: { label: 'Rejected', color: 'bg-red-100 dark:bg-red-900/30 text-red-700 dark:text-red-300', icon: XCircle },
  WAITING: { label: 'Waiting', color: 'bg-yellow-100 dark:bg-yellow-900/30 text-yellow-700 dark:text-yellow-300', icon: Loader2 },
};

const statusOrder: ApplicationStatus[] = ['INTERVIEW', 'OFFER', 'APPLIED', 'WAITING', 'REJECTED'];

export function JobApplicationsSection({ applications }: JobApplicationsSectionProps) {
  if (applications.length === 0) return null;

  const sortedApps = [...applications].sort((a, b) => {
    const orderA = statusOrder.indexOf(a.status);
    const orderB = statusOrder.indexOf(b.status);
    if (orderA !== orderB) return orderA - orderB;
    return new Date(b.updatedAt).getTime() - new Date(a.updatedAt).getTime();
  });

  return (
    <section className="space-y-4">
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-semibold text-gray-900 dark:text-white flex items-center gap-2">
          <Briefcase className="w-5 h-5 text-primary-600 dark:text-primary-400" />
          Job Applications
        </h2>
        <span className="text-sm text-gray-500 dark:text-gray-400">{applications.length} application{applications.length !== 1 ? 's' : ''}</span>
      </div>

      <div className="space-y-3">
        {sortedApps.map((app) => {
          const { color, icon: StatusIcon, label } = statusConfig[app.status];
          return (
            <article
              key={app.id}
              className="group flex items-center gap-4 p-4 rounded-xl bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 hover:border-primary-200 dark:hover:border-primary-800 transition-colors"
            >
              <div className="w-12 h-12 rounded-xl bg-primary-100 dark:bg-primary-900/30 flex items-center justify-center flex-shrink-0">
                <Building2 className="w-6 h-6 text-primary-600 dark:text-primary-400" />
              </div>
              <div className="flex-1 min-w-0">
                <div className="flex items-center justify-between gap-2">
                  <div className="flex-1 min-w-0">
                    <h3 className="font-medium text-gray-900 dark:text-white truncate">{app.company}</h3>
                    <p className="text-sm text-gray-600 dark:text-gray-400 truncate mt-0.5">{app.role}</p>
                  </div>
                  <span className={cn('flex-shrink-0 px-3 py-1 rounded-full text-xs font-medium flex items-center gap-1', color)}>
                    <StatusIcon className="w-3.5 h-3.5" />
                    {label}
                  </span>
                </div>
                <div className="mt-2 flex flex-wrap items-center gap-3 text-xs text-gray-500 dark:text-gray-400">
                  {app.interviewDate && (
                    <span className="flex items-center gap-1 text-purple-600 dark:text-purple-400 font-medium">
                      <Clock className="w-3.5 h-3.5" />
                      Interview: {formatRelativeTime(app.interviewDate)}
                    </span>
                  )}
                  {app.recruiterName && (
                    <span className="flex items-center gap-1">
                      <UserCheck className="w-3.5 h-3.5" />
                      {app.recruiterName}
                    </span>
                  )}
                </div>
              </div>
              <div className="text-right text-sm text-gray-400 dark:text-gray-500">
                {formatRelativeTime(app.updatedAt)}
              </div>
            </article>
          );
        })}
      </div>
    </section>
  );
}