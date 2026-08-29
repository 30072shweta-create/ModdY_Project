import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { BookingsService } from '../../../core/services/bookings.service';
import { AuthService } from '../../../core/auth/auth.service';
import { BookingResponseDTO } from '../../../core/models/booking.model';

@Component({
  selector: 'app-my-bookings',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './my-bookings.component.html',
  styleUrls: ['./my-bookings.component.scss']
})
export class MyBookingsComponent implements OnInit {
  private bookingsService = inject(BookingsService);
  private authService = inject(AuthService);
  private router = inject(Router);

  public bookings: BookingResponseDTO[] = [];
  public isLoading = true;
  public errorMessage = '';

  public ngOnInit(): void {
    const user = this.authService.currentUser();
    if (user?.userId) {
      this.loadUserBookings(user.userId);
    } else {
      this.isLoading = false;
    }
  }

  private loadUserBookings(userId: number): void {
    this.isLoading = true;
    this.bookingsService.getUserBookings(userId).subscribe({
      next: (data) => {
        this.bookings = data;
        this.isLoading = false;
      },
      error: (err) => {
        this.isLoading = false;
        this.errorMessage = err.error?.message || 'Failed to load your booking history.';
      }
    });
  }

  public viewBooking(bookingId: number): void {
    this.router.navigate(['/booking', bookingId]);
  }
}
