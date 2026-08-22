'use client';

import { Calendar, Clock, Plane, MapPin, UserCheck, AlertTriangle, FileText, Briefcase } from 'lucide-react';
import { cn, formatDate, formatTime, formatRelativeTime } from '@/lib/utils';
import type { UpcomingItem } from '@/types';

interface UpcomingSectionProps {
  items: UpcomingItem[];
}

const typeIcons: Record<UpcomingItem['type'], React.ComponentType<{ className?: string }>> = {
  interview: UserCheck,
  flight: Plane,
  hotel: MapPin,
  meeting: Calendar,
  deadline: AlertTriangle,
  event: Calendar,
};

const typeColors: Record<UpcomingItem['type'], string> = {
  interview: 'bg-purple-100 dark:bg-purple-900/30 text-purple-600 dark:text-purple-400',
  flight: 'bg-blue-100 dark:bg-blue-900/30 text-blue-600 dark:text-blue-400',
  hotel: 'bg-green-100 dark:bg-green-900/30 text-green-600 dark:text-green-400',
  meeting: 'bg-indigo-100 dark:bg-indigo-900/30 text-indigo-600 dark:text-indigo-400',
  deadline: 'bg-red-100 dark:bg-red-900/30 text-red-600 dark:text-red-400',
  event: 'bg-pink-100 dark:bg-pink-900/30 text-pink-600 dark:text-pink-400',
};

export function UpcomingSection({ items }: UpcomingSectionProps) {
  if (items.length === 0) return null;

  const sortedItems = [...items].sort((a, b) => new Date(a.date).getTime() - new Date(b.date).getTime());

  return (
    <section className="space-y-4">
      <h2 className="text-lg font-semibold text-gray-900 dark:text-white flex items-center gap-2">
        <Calendar className="w-5 h-5 text-primary-600 dark:text-primary-400" />
        Upcoming
      </h2>

      <div className="space-y-2">
        {sortedItems.map((item) => {
          const Icon = typeIcons[item.type];
          return (
            <article
              key={item.id}
              className="group flex items-center gap-4 p-4 rounded-xl bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 hover:border-primary-200 dark:hover:border-primary-800 transition-colors"
            >
              <div className={cn('w-12 h-12 rounded-xl flex items-center justify-center flex-shrink-0', typeColors[item.type])}>
                <Icon className="w-6 h-6" />
              </div>
              <div className="flex-1 min-w-0">
                <h3 className="font-medium text-gray-900 dark:text-white truncate group-hover:text-primary-600 dark:group-hover:text-primary-400 transition-colors">
                  {item.title}
                </h3>
                <div className="mt-1 flex flex-wrap items-center gap-3 text-sm text-gray-500 dark:text-gray-400">
                  <span className="flex items-center gap-1">
                    <Calendar className="w-3.5 h-3.5" />
                    {formatDate(item.date, { weekday: 'short' })}
                  </span>
                  {item.time && (
                    <span className="flex items-center gap-1">
                      <Clock className="w-3.5 h-3.5" />
                      {formatTime(item.time)}
                    </span>
                  )}
                  {item.location && (
                    <span className="flex items-center gap-1">
                      <MapPin className="w-3.5 h-3.5" />
                      {item.location}
                    </span>
                  )}
                </div>
              </div>
              <div className="text-right text-sm text-gray-400 dark:text-gray-500">
                {formatRelativeTime(item.date)}
              </div>
            </article>
          );
        })}
      </div>
    </section>
  );
}