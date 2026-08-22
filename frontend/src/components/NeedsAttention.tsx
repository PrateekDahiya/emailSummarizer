'use client';

import { AlertTriangle, FileText, Calendar, Clock, Plane, UserCheck, ExternalLink } from 'lucide-react';
import { cn, getPriorityColor, formatRelativeTime, formatTime } from '@/lib/utils';
import type { AttentionItem } from '@/types';

interface NeedsAttentionProps {
  items: AttentionItem[];
}

const typeIcons: Record<AttentionItem['type'], React.ComponentType<{ className?: string }>> = {
  interview: UserCheck,
  deadline: AlertTriangle,
  document: FileText,
  meeting: Calendar,
  payment: AlertTriangle,
  travel: Plane,
};

const typeLabels: Record<AttentionItem['type'], string> = {
  interview: 'Interview',
  deadline: 'Deadline',
  document: 'Document',
  meeting: 'Meeting',
  payment: 'Payment',
  travel: 'Travel',
};

export function NeedsAttention({ items }: NeedsAttentionProps) {
  if (items.length === 0) return null;

  return (
    <section className="space-y-4">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <div className="w-2 h-8 bg-red-500 rounded-full" />
          <h2 className="text-lg font-semibold text-gray-900 dark:text-white">
            {items.length} Thing{items.length !== 1 ? 's' : ''} Need Attention
          </h2>
        </div>
      </div>

      <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
        {items.map((item) => {
          const Icon = typeIcons[item.type];
          return (
            <article
              key={item.id}
              className={cn(
                'relative p-4 rounded-xl border transition-all hover:shadow-md',
                getPriorityColor(item.priority)
              )}
            >
              <div className="flex items-start gap-3">
                <div className={cn(
                  'w-10 h-10 rounded-lg flex items-center justify-center flex-shrink-0',
                  item.priority === 'high' && 'bg-red-100 dark:bg-red-900/30 text-red-600 dark:text-red-400',
                  item.priority === 'medium' && 'bg-yellow-100 dark:bg-yellow-900/30 text-yellow-600 dark:text-yellow-400',
                  item.priority === 'low' && 'bg-green-100 dark:bg-green-900/30 text-green-600 dark:text-green-400'
                )}>
                  <Icon className="w-5 h-5" />
                </div>
                <div className="flex-1 min-w-0">
                  <div className="flex items-center justify-between gap-2">
                    <h3 className="font-medium text-gray-900 dark:text-white truncate">{item.title}</h3>
                    <span className="text-xs font-medium px-2 py-0.5 rounded-full bg-white/50 dark:bg-gray-700/50">
                      {typeLabels[item.type]}
                    </span>
                  </div>
                  <p className="mt-1 text-sm text-gray-600 dark:text-gray-400 line-clamp-2">{item.description}</p>
                  <div className="mt-3 flex items-center gap-3 text-xs text-gray-500 dark:text-gray-400">
                    <span className="flex items-center gap-1">
                      <Calendar className="w-3.5 h-3.5" />
                      {formatRelativeTime(item.date)}
                    </span>
                    {item.time && (
                      <span className="flex items-center gap-1">
                        <Clock className="w-3.5 h-3.5" />
                        {formatTime(item.time)}
                      </span>
                    )}
                  </div>
                </div>
              </div>
              {item.actionUrl && (
                <a
                  href={item.actionUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="absolute bottom-3 right-3 p-1.5 rounded-lg hover:bg-white/50 dark:hover:bg-gray-700/50 transition-colors"
                  aria-label="Open in Gmail"
                >
                  <ExternalLink className="w-4 h-4 text-gray-400 hover:text-gray-600 dark:hover:text-gray-300" />
                </a>
              )}
            </article>
          );
        })}
      </div>
    </section>
  );
}