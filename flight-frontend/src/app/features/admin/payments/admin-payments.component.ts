import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PaymentsService } from '../../../core/services/payments.service';
import { PaymentResponseDTO } from '../../../core/models/payment.model';
import { AdminSidebarComponent } from '../components/admin-sidebar/admin-sidebar.component';

@Component({
  selector: 'app-admin-payments',
  standalone: true,
  imports: [CommonModule, FormsModule, AdminSidebarComponent],
  templateUrl: './admin-payments.component.html',
  styleUrls: ['./admin-payments.component.scss']
})
export class AdminPaymentsComponent implements OnInit {
  private paymentsService = inject(PaymentsService);

  public paymentList: PaymentResponseDTO[] = [];
  public search = '';
  public statusFilter = 'ALL';
  public isLoading = false;
  public errorMessage = '';
  public successMessage = '';

  public selectedPayment: PaymentResponseDTO | null = null;
  public showDetailModal = false;

  public ngOnInit(): void {
    this.loadPayments();
  }

  public loadPayments(): void {
    this.isLoading = true;
    this.errorMessage = '';

    this.paymentsService.getAllPayments().subscribe({
      next: (data) => {
        this.paymentList = data || [];
        this.isLoading = false;
      },
      error: (err) => this.handleError(err)
    });
  }

  public get filteredPayments(): PaymentResponseDTO[] {
    return this.paymentList.filter((p) => {
      const matchesStatus = this.statusFilter === 'ALL' || p.status === this.statusFilter;
      if (!matchesStatus) return false;

      if (!this.search) return true;
      const term = this.search.toLowerCase();
      return (
        p.paymentId.toString().includes(term) ||
        p.bookingId.toString().includes(term) ||
        p.paymentMethod.toLowerCase().includes(term) ||
        (p.razorpayOrderId && p.razorpayOrderId.toLowerCase().includes(term)) ||
        (p.razorpayPaymentId && p.razorpayPaymentId.toLowerCase().includes(term)) ||
        (p.transactionRef && p.transactionRef.toLowerCase().includes(term))
      );
    });
  }

  public viewPaymentDetails(payment: PaymentResponseDTO): void {
    this.selectedPayment = payment;
    this.showDetailModal = true;
  }

  public closeDetailModal(): void {
    this.showDetailModal = false;
    this.selectedPayment = null;
  }

  private handleError(err: any): void {
    this.isLoading = false;
    if (err.status === 403) {
      this.errorMessage = 'You do not have permission to perform this action.';
    } else {
      this.errorMessage = err.error?.message || err.error || 'Failed to process payment request.';
    }
  }
}
