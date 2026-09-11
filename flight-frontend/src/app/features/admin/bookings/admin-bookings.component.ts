import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { BookingsService } from '../../../core/services/bookings.service';
import { BookingResponseDTO } from '../../../core/models/booking.model';
import { AdminSidebarComponent } from '../components/admin-sidebar/admin-sidebar.component';

@Component({
  selector: 'app-admin-bookings',
  standalone: true,
  imports: [CommonModule, FormsModule, AdminSidebarComponent],
  templateUrl: './admin-bookings.component.html',
  styleUrls: ['./admin-bookings.component.scss']
})
export class AdminBookingsComponent implements OnInit {
  private bookingsService = inject(BookingsService);

  public bookingList: BookingResponseDTO[] = [];
  public search = '';
  public statusFilter = 'ALL';
  public isLoading = false;
  public errorMessage = '';
  public successMessage = '';

  public selectedBooking: BookingResponseDTO | null = null;
  public showDetailModal = false;

  public confirmCancelModal = {
    show: false,
    booking: null as BookingResponseDTO | null,
    reason: 'Cancelled by administrator'
  };

  public ngOnInit(): void {
    this.loadBookings();
  }

  public loadBookings(preserveSuccessMessage = false): void {
    this.isLoading = true;
    this.errorMessage = '';
    if (!preserveSuccessMessage) {
      this.successMessage = '';
    }

    this.bookingsService.getAllBookings().subscribe({
      next: (data) => {
        this.bookingList = data || [];
        this.isLoading = false;
      },
      error: (err) => this.handleError(err)
    });
  }

  public get filteredBookings(): BookingResponseDTO[] {
    return this.bookingList.filter((b) => {
      const matchesStatus = this.statusFilter === 'ALL' || b.status === this.statusFilter;
      if (!matchesStatus) return false;

      if (!this.search) return true;
      const term = this.search.toLowerCase();
      return (
        b.bookingCode.toLowerCase().includes(term) ||
        b.userId.toString().includes(term) ||
        b.status.toLowerCase().includes(term) ||
        b.paymentStatus.toLowerCase().includes(term)
      );
    });
  }

  public viewBookingDetails(booking: BookingResponseDTO): void {
    this.selectedBooking = booking;
    this.showDetailModal = true;
  }

  public closeDetailModal(): void {
    this.showDetailModal = false;
    this.selectedBooking = null;
  }

  public openCancelModal(booking: BookingResponseDTO): void {
    this.confirmCancelModal = {
      show: true,
      booking: booking,
      reason: 'Cancelled by administrator'
    };
  }

  public executeCancellation(): void {
    if (!this.confirmCancelModal.booking) return;

    const bookingId = this.confirmCancelModal.booking.bookingId;
    this.isLoading = true;
    this.errorMessage = '';
    this.successMessage = '';

    this.bookingsService.cancelBooking(bookingId, { bookingId, reason: this.confirmCancelModal.reason }).subscribe({
      next: () => {
        this.isLoading = false;
        this.confirmCancelModal.show = false;
        this.successMessage = `Booking #${bookingId} cancelled successfully!`;
        this.autoDismissToast();
        this.loadBookings(true);
      },
      error: (err) => this.handleError(err)
    });
  }

  private autoDismissToast(): void {
    setTimeout(() => {
      this.successMessage = '';
    }, 4000);
  }

  private handleError(err: any): void {
    this.isLoading = false;
    if (err.status === 403) {
      this.errorMessage = 'You do not have permission to perform this action.';
    } else {
      this.errorMessage = err.error?.message || err.error || 'Failed to process booking request.';
    }
  }
}
