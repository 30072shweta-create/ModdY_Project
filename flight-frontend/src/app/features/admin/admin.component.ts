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

  public ngOnInit(): void {
    this.loadDashboardData();
  }

  public loadDashboardData(): void {
    this.isLoading = true;
    this.errorMessage = '';

    this.flightsService.getAllFlights().subscribe({
      next: (data) => (this.flights = data || []),
      error: (err) => this.handleError(err)
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
      next: (data) => (this.bookings = data || []),
      error: (err) => console.warn('Bookings load error', err)
    });

    this.paymentsService.getAllPayments().subscribe({
      next: (data) => (this.payments = data || []),
      error: (err) => console.warn('Payments load error', err)
    });

    this.isLoading = false;
  }

  public get totalFlightsCount(): number {
    return this.flights.length;
  }

  public get activeFlightsCount(): number {
    return this.flights.filter((f) => f.status === 'SCHEDULED' || f.status === 'BOARDING' || f.status === 'DEPARTED').length;
  }

  public get totalRevenue(): number {
    return this.payments
      .filter((p) => p.status === 'PAID')
      .reduce((acc, p) => acc + (p.amount || 0), 0);
  }

  private handleError(err: any): void {
    this.isLoading = false;
    if (err.status === 403) {
      this.errorMessage = 'You do not have permission to access the admin dashboard.';
    } else if (err.status === 0) {
      this.errorMessage = 'Unable to connect to the server.';
    } else {
      this.errorMessage = err.error?.message || err.error || 'Failed to load dashboard data.';
    }
  }
}
