'use client';

import { useEffect, useState } from 'react';
import { useAuth } from '@/hooks/useApi';
import { Dashboard } from '@/components/Dashboard';
import { ConnectGmail } from '@/components/ConnectGmail';
import { LoadingScreen } from '@/components/LoadingScreen';

export default function HomePage() {
  const { user, isLoading, isAuthenticated } = useAuth();
  const [showConnect, setShowConnect] = useState(false);

  useEffect(() => {
    if (!isLoading && isAuthenticated && !user?.gmailConnected) {
      setShowConnect(true);
    }
  }, [isLoading, isAuthenticated, user]);

  if (isLoading) {
    return <LoadingScreen />;
  }

  if (!isAuthenticated) {
    return <ConnectGmail onConnect={() => setShowConnect(true)} />;
  }

  if (showConnect || !user?.gmailConnected) {
    return <ConnectGmail onSuccess={() => setShowConnect(false)} />;
  }

  return <Dashboard />;
}