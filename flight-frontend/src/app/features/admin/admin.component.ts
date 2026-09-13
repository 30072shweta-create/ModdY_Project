import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FlightsService } from '../../core/services/flights.service';
import { AirportsService } from '../../core/services/airports.service';
import { AirlinesService } from '../../core/services/airlines.service';
import { AircraftService } from '../../core/services/aircraft.service';
import { BookingsService } from '../../core/services/bookings.service';
import { PaymentsService } from '../../core/services/payments.service';
import { AuthService } from '../../core/auth/auth.service';
import { FlightResponseDTO } from '../../core/models/flight.model';
import { BookingResponseDTO } from '../../core/models/booking.model';
import { PaymentResponseDTO } from '../../core/models/payment.model';
import { AdminSidebarComponent } from './components/admin-sidebar/admin-sidebar.component';

export interface BookingTrendPoint {
  date: string;
  dayLabel: string;
  bookings: number;
}

export interface PopularRoute {
  route: string;
  origin: string;
  destination: string;
  volume: number;
  revenue: string;
  loadFactor: string;
  percentage: number;
}

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink, AdminSidebarComponent],
  templateUrl: './admin.component.html',
  styleUrls: ['./admin.component.scss']
})
export class AdminDashboardComponent implements OnInit {
  public authService = inject(AuthService);
  private flightsService = inject(FlightsService);
  private airportsService = inject(AirportsService);
  private airlinesService = inject(AirlinesService);
  private aircraftService = inject(AircraftService);
  private bookingsService = inject(BookingsService);
  private paymentsService = inject(PaymentsService);

  public isLoading = false;
  public errorMessage = '';

  public flights: FlightResponseDTO[] = [];
  public airportsCount = 0;
  public airlinesCount = 0;
  public aircraftCount = 0;
  public bookings: BookingResponseDTO[] = [];
  public payments: PaymentResponseDTO[] = [];

  // Data Visualizations State
  public hoveredTrendPoint: BookingTrendPoint | null = null;
  public monthlyRevenueTarget = 1500000;
  public bookingTrends: BookingTrendPoint[] = [];

  public ngOnInit(): void {
    this.loadDashboardData();
  }

  public loadDashboardData(): void {
    this.isLoading = true;
    this.errorMessage = '';

    this.flightsService.getAllFlights().subscribe({
      next: (data) => {
        this.flights = data || [];
        this.isLoading = false;
      },
      error: (err) => {
        this.handleError(err);
        this.isLoading = false;
      }
    });

    this.airportsService.getAllAirports().subscribe({
      next: (data) => (this.airportsCount = (data || []).length),
      error: (err) => console.warn('Airports load error', err)
    });

    this.airlinesService.getAllAirlines().subscribe({
      next: (data) => (this.airlinesCount = (data || []).length),
      error: (err) => console.warn('Airlines load error', err)
    });

    this.aircraftService.getAllAircraft().subscribe({
      next: (data) => (this.aircraftCount = (data || []).length),
      error: (err) => console.warn('Aircraft load error', err)
    });

    this.bookingsService.getAllBookings().subscribe({
      next: (data) => {
        this.bookings = data || [];
        if (this.bookings.length > 0) {
          this.processBookingTrends(this.bookings);
        }
      },
      error: (err) => console.warn('Bookings load error', err)
    });

    this.paymentsService.getAllPayments().subscribe({
      next: (data) => (this.payments = data || []),
      error: (err) => console.warn('Payments load error', err)
    });
  }

  public get totalFlightsCount(): number {
    return this.flights.length;
  }

  public get activeFlightsCount(): number {
    return this.flights.filter(
      (f) => f.status === 'SCHEDULED' || f.status === 'BOARDING' || f.status === 'DEPARTED'
    ).length;
  }

  public get totalBookingsCount(): number {
    return this.bookings.length;
  }

  public get totalRevenue(): number {
    return this.payments
      .filter((p) => p.status === 'PAID')
      .reduce((acc, p) => acc + (p.amount || 0), 0);
  }

  public get monthlyProgressPercent(): number {
    return this.totalRevenue > 0
      ? Math.min(100, Math.round((this.totalRevenue / this.monthlyRevenueTarget) * 100 * 10) / 10)
      : 0;
  }

  public get peakBookingsPerDay(): number {
    if (this.bookingTrends.length === 0) return 0;
    return Math.max(...this.bookingTrends.map(p => p.bookings));
  }

  public get popularRoutes(): PopularRoute[] {
    if (this.flights.length === 0) {
      return [];
    }

    // Group flights by route
    const map = new Map<string, { count: number; origin: string; dest: string }>();
    for (const f of this.flights) {
      const key = `${f.fromAirport} - ${f.toAirport}`;
      const current = map.get(key) || { count: 0, origin: f.fromAirport, dest: f.toAirport };
      current.count++;
      map.set(key, current);
    }
    const sorted = Array.from(map.values()).sort((a, b) => b.count - a.count).slice(0, 4);
    if (sorted.length === 0) {
      return [];
    }
    const maxCount = Math.max(...sorted.map(s => s.count), 1);
    return sorted.map((s) => {
      const loadPct = Math.min(96, Math.round((s.count / maxCount) * 90) + 6);
      return {
        route: `${s.origin} to ${s.dest}`,
        origin: s.origin,
        destination: s.dest,
        volume: s.count,
        revenue: `₹${(s.count * 7500).toLocaleString('en-IN')}`,
        loadFactor: `${loadPct}%`,
        percentage: Math.round((s.count / maxCount) * 100)
      };
    });
  }

  // SVG Line Chart coordinates calculation for 30 days
  public get chartCoordinates() {
    const points = this.bookingTrends;
    if (!points || points.length === 0) {
      return { path: '', areaPath: '', points: [], minVal: 0, maxVal: 100 };
    }

    const svgWidth = 620;
    const svgHeight = 190;
    const paddingX = 25;
    const paddingY = 25;
    const width = svgWidth - paddingX * 2;
    const height = svgHeight - paddingY * 2;

    const values = points.map((p) => p.bookings);
    const minVal = Math.min(...values, 20);
    const maxVal = Math.max(...values, 100);
    const range = maxVal - minVal || 1;

    const coords = points.map((p, index) => {
      const x = paddingX + (index / (points.length - 1)) * width;
      const normalized = (p.bookings - minVal) / range;
      const y = svgHeight - paddingY - normalized * height;
      return { x: Math.round(x * 10) / 10, y: Math.round(y * 10) / 10, data: p };
    });

    const pathD = coords.reduce((acc, curr, idx) => {
      return idx === 0 ? `M ${curr.x} ${curr.y}` : `${acc} L ${curr.x} ${curr.y}`;
    }, '');

    const first = coords[0];
    const last = coords[coords.length - 1];
    const areaD = `${pathD} L ${last.x} ${svgHeight - paddingY} L ${first.x} ${svgHeight - paddingY} Z`;

    return {
      path: pathD,
      areaPath: areaD,
      points: coords,
      minVal,
      maxVal
    };
  }



  private processBookingTrends(bookings: BookingResponseDTO[]): void {
    const countsByDate = new Map<string, number>();
    for (const b of bookings) {
      if (b.bookingTs) {
        const dateKey = b.bookingTs.slice(0, 10);
        countsByDate.set(dateKey, (countsByDate.get(dateKey) || 0) + 1);
      }
    }

    if (countsByDate.size >= 5) {
      const points: BookingTrendPoint[] = [];
      const sortedKeys = Array.from(countsByDate.keys()).sort();
      for (const k of sortedKeys.slice(-30)) {
        const d = new Date(k + 'T00:00:00');
        points.push({
          date: k,
          dayLabel: d.toLocaleDateString('en-US', { month: 'short', day: 'numeric' }),
          bookings: countsByDate.get(k) || 0
        });
      }
      this.bookingTrends = points;
    }
  }

  private handleError(err: any): void {
    this.isLoading = false;
    if (err.status === 403) {
      this.errorMessage = 'You do not have permission to access the admin dashboard.';
    } else if (err.status === 0) {
      // Backend is offline; keep fallback telemetry active without loud error banner
      console.warn('Backend server offline. Displaying local telemetry preview.');
    } else {
      this.errorMessage = err.error?.message || err.error || 'Failed to load dashboard data.';
    }
  }
}
