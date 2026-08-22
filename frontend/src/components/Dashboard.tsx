'use client';

import { useEffect, useState } from 'react';
import { api } from '@/lib/api';
import { DashboardHeader } from './DashboardHeader';
import { NeedsAttention } from './NeedsAttention';
import { UpcomingSection } from './UpcomingSection';
import { RecentImportantEmails } from './RecentImportantEmails';
import { JobApplicationsSection } from './JobApplicationsSection';
import { UpcomingTripsSection } from './UpcomingTripsSection';
import { LoadingCard } from './LoadingCard';
import type { DashboardData } from '@/types';

export function Dashboard() {
  const [data, setData] = useState<DashboardData | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchDashboard = async () => {
      try {
        setIsLoading(true);
        const dashboardData = await api.getDashboard();
        setData(dashboardData);
      } catch (err: any) {
        setError(err.message || 'Failed to load dashboard');
      } finally {
        setIsLoading(false);
      }
    };

    fetchDashboard();
  }, []);

  if (isLoading) {
    return (
      <div className="min-h-screen bg-gray-50 dark:bg-gray-900">
        <DashboardHeader userName="User" />
        <main className="p-4 md:p-6 lg:p-8 max-w-6xl mx-auto">
          <div className="space-y-6">
            <LoadingCard />
            <LoadingCard />
            <LoadingCard />
            <LoadingCard />
          </div>
        </main>
      </div>
    );
  }

  if (error) {
    return (
      <div className="min-h-screen bg-gray-50 dark:bg-gray-900 flex items-center justify-center">
        <div className="text-center p-8">
          <p className="text-red-600 dark:text-red-400 mb-4">{error}</p>
          <button
            onClick={() => window.location.reload()}
            className="px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700"
          >
            Retry
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900">
      <DashboardHeader userName={data?.needsAttention[0] ? 'Prateek' : 'User'} />
      <main className="p-4 md:p-6 lg:p-8 max-w-6xl mx-auto">
        <div className="space-y-6">
          <NeedsAttention items={data?.needsAttention || []} />
          <UpcomingSection items={data?.upcoming || []} />
          <RecentImportantEmails emails={data?.recentImportant || []} />
          <JobApplicationsSection applications={data?.jobApplications || []} />
          <UpcomingTripsSection trips={data?.upcomingTrips || []} />
        </div>
      </main>
    </div>
  );
}