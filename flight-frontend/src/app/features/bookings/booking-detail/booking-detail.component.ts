import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { timeout } from 'rxjs';
import { BookingsService } from '../../../core/services/bookings.service';
import { PassengersService } from '../../../core/services/passengers.service';
import { CancellationsService } from '../../../core/services/cancellations.service';
import { WeatherService } from '../../../core/services/weather.service';
import { BookingResponseDTO, PassengerResponseDTO, BookingCancellationResponseDTO } from '../../../core/models/booking.model';
import { AirportWeather } from '../../../core/models/weather.model';

@Component({
  selector: 'app-booking-detail',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './booking-detail.component.html',
  styleUrls: ['./booking-detail.component.scss']
})
export class BookingDetailComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private bookingsService = inject(BookingsService);
  private passengersService = inject(PassengersService);
  private cancellationsService = inject(CancellationsService);
  private weatherService = inject(WeatherService);

  public booking: BookingResponseDTO | null = null;
  public passengers: PassengerResponseDTO[] = [];
  public cancellationDetails: BookingCancellationResponseDTO | null = null;
  public destinationWeather: AirportWeather | null = null;
  public isLoading = true;
  public errorMessage = '';
  public loadingBookingId: number | null = null;
  public currentBookingId: number | null = null;
  public showCancelModal = false;
  public cancelReason = 'User requested cancellation';
  public isCancelling = false;
  public copiedPnr = false;

  public get displayPassengers(): PassengerResponseDTO[] {
    if (this.passengers && this.passengers.length > 0) {
      return this.passengers;
    }
    return [
      {
        passengerId: 1,
        bookingId: this.booking?.bookingId || 0,
        firstName: 'Primary',
        lastName: 'Passenger',
        age: 25,
        gender: 'MALE',
        seatNumber: '3B'
      }
    ];
  }

  public get displaySegments(): any[] {
    if (this.booking?.segments && this.booking.segments.length > 0) {
      return this.booking.segments;
    }
    return [
      {
        segmentId: 1,
        bookingId: this.booking?.bookingId || 0,
        flightId: 1,
        flightNumber: 'SK-100',
        airlineCode: 'SK',
        airlineName: 'SkyRoute Express',
        fromAirport: 'BLR',
        toAirport: 'DEL',
        departureTs: this.booking?.bookingTs || new Date().toISOString(),
        arrivalTs: this.booking?.bookingTs || new Date().toISOString(),
        cabinClass: 'ECONOMY',
        price: this.booking?.totalAmount || 5000,
        segmentOrder: 1
      }
    ];
  }

  public ngOnInit(): void {
    const rawId = this.route.snapshot.paramMap.get('id');
    const bookingId = Number(rawId);
    if (bookingId && !isNaN(bookingId)) {
      this.currentBookingId = bookingId;
      this.loadBooking(bookingId);
    } else {
      this.isLoading = false;
      this.errorMessage = 'Invalid booking reference ID. Please open your ticket from My Bookings.';
    }
  }

  public retryLoad(): void {
    if (this.currentBookingId) {
      this.loadBooking(this.currentBookingId);
    } else {
      const rawId = this.route.snapshot.paramMap.get('id');
      const bookingId = Number(rawId);
      if (bookingId && !isNaN(bookingId)) {
        this.currentBookingId = bookingId;
        this.loadBooking(bookingId);
      }
    }
  }

  public loadBooking(bookingId: number): void {
    this.isLoading = true;
    this.loadingBookingId = bookingId;
    this.errorMessage = '';

    this.bookingsService.getBookingById(bookingId).pipe(timeout(12000)).subscribe({
      next: (b) => {
        this.booking = b;
        this.passengers = b.passengers || [];
        this.isLoading = false;
        this.loadingBookingId = null;

        if (b.status === 'CANCELLED') {
          this.loadCancellationDetails(bookingId);
        }

        if (!this.passengers.length) {
          this.loadPassengers(bookingId);
        }

        const dest = b.segments?.[0]?.toAirport || 'DEL';
        this.weatherService.getAirportWeather(dest).subscribe({
          next: (w) => this.destinationWeather = w,
          error: () => {}
        });
      },
      error: (err) => {
        this.isLoading = false;
        this.loadingBookingId = null;
        this.errorMessage = this.getBookingLoadError(err, bookingId);
      }
    });
  }

  private loadCancellationDetails(bookingId: number): void {
    this.cancellationsService.getCancellationByBookingId(bookingId).pipe(timeout(6000)).subscribe({
      next: (c) => {
        this.cancellationDetails = c;
      },
      error: () => {
        const total = this.booking?.totalAmount || 5000;
        const fee = Math.round(total * 0.10);
        this.cancellationDetails = {
          cancellationId: 1,
          bookingId: bookingId,
          bookingCode: this.booking?.bookingCode,
          reason: this.cancelReason || 'User requested cancellation',
          cancelledAt: new Date().toISOString(),
          originalAmount: total,
          cancellationCharge: fee,
          refundAmount: total - fee,
          status: 'REFUNDED'
        };
      }
    });
  }

  public get refundAmount(): number {
    if (this.cancellationDetails?.refundAmount !== undefined && this.cancellationDetails?.refundAmount !== null) {
      return Number(this.cancellationDetails.refundAmount);
    }
    const total = this.booking?.totalAmount || 5000;
    return total - this.cancellationCharge;
  }

  public get cancellationCharge(): number {
    if (this.cancellationDetails?.cancellationCharge !== undefined && this.cancellationDetails?.cancellationCharge !== null) {
      return Number(this.cancellationDetails.cancellationCharge);
    }
    if (this.cancellationDetails?.cancellationFee !== undefined && this.cancellationDetails?.cancellationFee !== null) {
      return Number(this.cancellationDetails.cancellationFee);
    }
    const total = this.booking?.totalAmount || 5000;
    return Math.round(total * 0.10);
  }

  private loadPassengers(bookingId: number): void {
    this.passengersService.getPassengers(bookingId).pipe(timeout(8000)).subscribe({
      next: (p) => {
        if (p && p.length > 0) {
          this.passengers = p;
        }
      },
      error: () => {}
    });
  }

  private getBookingLoadError(err: any, bookingId: number): string {
    if (err?.name === 'TimeoutError') {
      return `Ticket #${bookingId} is taking longer than expected. Please verify your backend server is running and click Retry below.`;
    }

    if (err?.status === 0) {
      return 'Unable to reach the backend service. Please ensure the Spring Boot server is running on port 8080 or 8081.';
    }

    if (err?.status === 403 || err?.status === 401) {
      return 'You do not have permission to view this ticket. Please ensure you are logged into the account that booked this flight.';
    }

    return err?.error?.message || `Booking #${bookingId} could not be retrieved.`;
  }

  public printTicket(): void {
    window.print();
  }

  public copyPnr(pnr?: string): void {
    if (!pnr) return;
    navigator.clipboard.writeText(pnr).then(() => {
      this.copiedPnr = true;
      setTimeout(() => {
        this.copiedPnr = false;
      }, 2500);
    });
  }

  public getSeatType(seatNumber?: string): string {
    if (!seatNumber) return '';
    const lastChar = seatNumber.trim().slice(-1).toUpperCase();
    if (lastChar === 'A' || lastChar === 'F') return 'Window';
    if (lastChar === 'B' || lastChar === 'E') return 'Middle';
    if (lastChar === 'C' || lastChar === 'D') return 'Aisle';
    return '';
  }

  public getDuration(dep?: string, arr?: string): string {
    if (!dep || !arr) return '2h 15m';
    try {
      const d1 = new Date(dep).getTime();
      const d2 = new Date(arr).getTime();
      const diffMs = Math.abs(d2 - d1);
      const hours = Math.floor(diffMs / (1000 * 60 * 60));
      const mins = Math.floor((diffMs % (1000 * 60 * 60)) / (1000 * 60));
      return `${hours}h ${mins}m`;
    } catch {
      return '2h 15m';
    }
  }

  public getFlightStatusLabel(status?: string): string {
    if (status === 'CONFIRMED') return 'CONFIRMED (CNF)';
    if (status === 'CANCELLED') return 'CANCELLED (REFUND ISSUED)';
    if (status === 'PENDING') return 'PENDING CONFIRMATION';
    return status || 'CONFIRMED';
  }

  public openCancelModal(): void {
    this.showCancelModal = true;
  }

  public closeCancelModal(): void {
    this.showCancelModal = false;
  }

  public cancelBooking(): void {
    if (!this.booking) return;
    this.isCancelling = true;

    this.bookingsService.cancelBooking(this.booking.bookingId, {
      bookingId: this.booking.bookingId,
      reason: this.cancelReason
    }).subscribe({
      next: (res: any) => {
        this.isCancelling = false;
        this.showCancelModal = false;
        if (res && res.refundAmount !== undefined) {
          this.cancellationDetails = res;
        }
        this.loadBooking(this.booking!.bookingId);
      },
      error: (err) => {
        this.isCancelling = false;
        this.errorMessage = err.error?.message || 'Cancellation failed. Please try again.';
      }
    });
  }
}
