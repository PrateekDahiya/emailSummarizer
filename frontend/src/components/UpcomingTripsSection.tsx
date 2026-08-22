'use client';

import { Plane, MapPin, Calendar, Clock, Building2, MapPin as MapPinIcon } from 'lucide-react';
import { cn, formatDate, formatTime, formatRelativeTime } from '@/lib/utils';
import type { TravelTrip, Flight, Hotel } from '@/types';

interface UpcomingTripsSectionProps {
  trips: TravelTrip[];
}

export function UpcomingTripsSection({ trips }: UpcomingTripsSectionProps) {
  if (trips.length === 0) return null;

  const sortedTrips = [...trips].sort((a, b) => new Date(a.startDate).getTime() - new Date(b.startDate).getTime());

  return (
    <section className="space-y-4">
      <h2 className="text-lg font-semibold text-gray-900 dark:text-white flex items-center gap-2">
        <Plane className="w-5 h-5 text-primary-600 dark:text-primary-400" />
        Upcoming Trips
      </h2>

      <div className="space-y-4">
        {sortedTrips.map((trip) => (
          <article key={trip.id} className="rounded-xl bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 overflow-hidden">
            <div className="p-4 border-b border-gray-200 dark:border-gray-700 bg-gradient-to-r from-blue-50 to-indigo-50 dark:from-blue-900/20 dark:to-indigo-900/20">
              <div className="flex items-center justify-between gap-4 flex-wrap">
                <div>
                  <h3 className="font-semibold text-gray-900 dark:text-white">{trip.name}</h3>
                  <p className="text-sm text-gray-600 dark:text-gray-400 mt-0.5">{trip.destination}</p>
                </div>
                <div className="text-right">
                  <div className="flex items-center gap-1 text-sm font-medium text-gray-900 dark:text-white">
                    <Calendar className="w-4 h-4" />
                    {formatDate(trip.startDate, { month: 'short', day: 'numeric' })} - {formatDate(trip.endDate, { month: 'short', day: 'numeric' })}
                  </div>
                  <div className="text-xs text-gray-500 dark:text-gray-400 mt-0.5">
                    {formatRelativeTime(trip.startDate)}
                  </div>
                </div>
              </div>
            </div>

            <div className="p-4 space-y-4">
              {trip.flights.length > 0 && (
                <div className="space-y-3">
                  <h4 className="text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wider flex items-center gap-1">
                    <Plane className="w-3.5 h-3.5" />
                    Flights
                  </h4>
                  {trip.flights.map((flight: Flight) => (
                    <div key={flight.id} className="flex items-center gap-3 p-3 rounded-lg bg-gray-50 dark:bg-gray-700/50">
                      <div className="w-10 h-10 rounded-lg bg-blue-100 dark:bg-blue-900/30 flex items-center justify-center flex-shrink-0">
                        <Plane className="w-5 h-5 text-blue-600 dark:text-blue-400" />
                      </div>
                      <div className="flex-1 min-w-0">
                        <div className="flex items-center justify-between gap-2">
                          <span className="font-medium text-gray-900 dark:text-white truncate">{flight.airline} {flight.flightNumber}</span>
                          <span className="text-xs text-gray-500 dark:text-gray-400">Booking: {flight.bookingNumber}</span>
                        </div>
                        <div className="mt-1 flex flex-wrap items-center gap-3 text-sm text-gray-600 dark:text-gray-400">
                          <span className="flex items-center gap-1">
                            <MapPinIcon className="w-3.5 h-3.5" />
                            {flight.departure} → {flight.arrival}
                          </span>
                          <span className="flex items-center gap-1">
                            <Clock className="w-3.5 h-3.5" />
                            {formatTime(flight.departureTime)} - {formatTime(flight.arrivalTime)}
                          </span>
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              )}

              {trip.hotels.length > 0 && (
                <div className="space-y-3">
                  <h4 className="text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wider flex items-center gap-1">
                    <Building2 className="w-3.5 h-3.5" />
                    Hotels
                  </h4>
                  {trip.hotels.map((hotel: Hotel) => (
                    <div key={hotel.id} className="flex items-center gap-3 p-3 rounded-lg bg-gray-50 dark:bg-gray-700/50">
                      <div className="w-10 h-10 rounded-lg bg-green-100 dark:bg-green-900/30 flex items-center justify-center flex-shrink-0">
                        <Building2 className="w-5 h-5 text-green-600 dark:text-green-400" />
                      </div>
                      <div className="flex-1 min-w-0">
                        <div className="flex items-center justify-between gap-2">
                          <span className="font-medium text-gray-900 dark:text-white truncate">{hotel.name}</span>
                          {hotel.cost && (
                            <span className="text-sm font-medium text-gray-900 dark:text-white">
                              ₹{hotel.cost.toLocaleString()}
                            </span>
                          )}
                        </div>
                        <div className="mt-1 flex items-center gap-3 text-sm text-gray-600 dark:text-gray-400">
                          <span className="flex items-center gap-1">
                            <Calendar className="w-3.5 h-3.5" />
                            Check-in: {formatDate(hotel.checkIn, { month: 'short', day: 'numeric' })}
                          </span>
                          <span className="flex items-center gap-1">
                            <Calendar className="w-3.5 h-3.5" />
                            Check-out: {formatDate(hotel.checkOut, { month: 'short', day: 'numeric' })}
                          </span>
                          {hotel.bookingNumber && (
                            <span className="text-xs text-gray-500 dark:text-gray-400">Booking: {hotel.bookingNumber}</span>
                          )}
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              )}

              {trip.totalCost && (
                <div className="pt-4 border-t border-gray-200 dark:border-gray-700 flex items-center justify-between">
                  <span className="text-sm font-medium text-gray-700 dark:text-gray-300">Estimated Total</span>
                  <span className="text-lg font-bold text-gray-900 dark:text-white">₹{trip.totalCost.toLocaleString()}</span>
                </div>
              )}
            </div>
          </article>
        ))}
      </div>
    </section>
  );
}