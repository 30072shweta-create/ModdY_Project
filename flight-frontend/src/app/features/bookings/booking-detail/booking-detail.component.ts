import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { BookingsService } from '../../../core/services/bookings.service';
import { PassengersService } from '../../../core/services/passengers.service';
import { BookingResponseDTO, PassengerResponseDTO } from '../../../core/models/booking.model';

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

  public booking: BookingResponseDTO | null = null;
  public passengers: PassengerResponseDTO[] = [];
  public isLoading = true;
  public errorMessage = '';
  public showCancelModal = false;
  public cancelReason = 'User requested cancellation';
  public isCancelling = false;

  public ngOnInit(): void {
    const bookingId = Number(this.route.snapshot.paramMap.get('id'));
    if (bookingId) {
      this.loadBooking(bookingId);
    }
  }

  private loadBooking(bookingId: number): void {
    this.isLoading = true;
    this.bookingsService.getBookingById(bookingId).subscribe({
      next: (b) => {
        this.booking = b;
        this.isLoading = false;
        this.loadPassengers(bookingId);
      },
      error: (err) => {
        this.isLoading = false;
        this.errorMessage = err.error?.message || 'Booking not found.';
      }
    });
  }

  private loadPassengers(bookingId: number): void {
    this.passengersService.getPassengers(bookingId).subscribe({
      next: (p) => this.passengers = p
    });
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
      next: () => {
        this.isCancelling = false;
        this.showCancelModal = false;
        this.loadBooking(this.booking!.bookingId);
      },
      error: (err) => {
        this.isCancelling = false;
        this.errorMessage = err.error?.message || 'Cancellation failed.';
      }
    });
  }
}
