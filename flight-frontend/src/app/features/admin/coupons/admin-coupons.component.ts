import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule, FormsModule } from '@angular/forms';
import { CouponsService } from '../../../core/services/coupons.service';
import { CouponResponseDTO } from '../../../core/models/pricing.model';
import { AdminSidebarComponent } from '../components/admin-sidebar/admin-sidebar.component';

@Component({
  selector: 'app-admin-coupons',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule, AdminSidebarComponent],
  templateUrl: './admin-coupons.component.html',
  styleUrls: ['./admin-coupons.component.scss']
})
export class AdminCouponsComponent implements OnInit {
  private fb = inject(FormBuilder);
  private couponsService = inject(CouponsService);

  public couponList: CouponResponseDTO[] = [];
  public search = '';
  public isLoading = false;
  public errorMessage = '';
  public successMessage = '';

  public showModal = false;
  public editingId: number | null = null;

  public confirmModal = {
    show: false,
    title: '',
    message: '',
    onConfirm: () => {}
  };

  public couponForm: FormGroup = this.fb.group({
    couponCode: ['', [Validators.required, Validators.maxLength(50)]],
    discountType: ['PERCENTAGE', [Validators.required]],
    discountValue: [10, [Validators.required, Validators.min(0.01)]],
    minimumBookingAmount: [0, [Validators.min(0)]],
    maximumDiscount: [100, [Validators.min(0)]],
    validFrom: [''],
    validTo: [''],
    usageLimit: [100, [Validators.min(1)]],
    active: [true]
  });

  public ngOnInit(): void {
    this.loadCoupons();
  }

  public loadCoupons(preserveSuccessMessage = false): void {
    this.isLoading = true;
    this.errorMessage = '';
    if (!preserveSuccessMessage) {
      this.successMessage = '';
    }

    this.couponsService.getAllCoupons().subscribe({
      next: (data) => {
        this.couponList = data || [];
        this.isLoading = false;
      },
      error: (err) => this.handleError(err)
    });
  }

  public get filteredCoupons(): CouponResponseDTO[] {
    return this.couponList.filter((c) => {
      if (!this.search) return true;
      const term = this.search.toLowerCase();
      return (
        c.couponCode.toLowerCase().includes(term) ||
        c.discountType.toLowerCase().includes(term)
      );
    });
  }

  public openAddModal(): void {
    this.editingId = null;
    this.couponForm.reset({
      discountType: 'PERCENTAGE',
      discountValue: 10,
      minimumBookingAmount: 0,
      maximumDiscount: 100,
      usageLimit: 100,
      active: true
    });
    this.showModal = true;
  }

  public openEditModal(coupon: CouponResponseDTO): void {
    this.editingId = coupon.couponId;
    this.couponForm.patchValue({
      couponCode: coupon.couponCode,
      discountType: coupon.discountType,
      discountValue: coupon.discountValue,
      minimumBookingAmount: coupon.minimumBookingAmount || 0,
      maximumDiscount: coupon.maximumDiscount || 0,
      validFrom: coupon.validFrom ? coupon.validFrom.split('T')[0] : '',
      validTo: coupon.validTo ? coupon.validTo.split('T')[0] : '',
      usageLimit: coupon.usageLimit || 100,
      active: coupon.active !== false
    });
    this.showModal = true;
  }

  public closeModal(): void {
    this.showModal = false;
    this.editingId = null;
  }

  public submitCoupon(): void {
    if (this.couponForm.invalid) {
      this.couponForm.markAllAsTouched();
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';
    this.successMessage = '';

    const raw = this.couponForm.value;
    const dto: any = {
      couponCode: raw.couponCode ? raw.couponCode.trim().toUpperCase() : '',
      discountType: raw.discountType,
      discountValue: Number(raw.discountValue),
      minimumBookingAmount: Number(raw.minimumBookingAmount || 0),
      maximumDiscount: Number(raw.maximumDiscount || 0),
      usageLimit: Number(raw.usageLimit || 100),
      active: raw.active !== false
    };

    if (raw.validFrom && raw.validFrom.trim()) {
      dto.validFrom = raw.validFrom.includes('T') ? raw.validFrom : `${raw.validFrom}T00:00:00`;
    } else {
      dto.validFrom = null;
    }

    if (raw.validTo && raw.validTo.trim()) {
      dto.validTo = raw.validTo.includes('T') ? raw.validTo : `${raw.validTo}T23:59:59`;
    } else {
      dto.validTo = null;
    }

    if (this.editingId) {
      this.couponsService.updateCoupon(this.editingId, dto).subscribe({
        next: () => {
          this.isLoading = false;
          this.closeModal();
          this.successMessage = 'Coupon updated successfully!';
          this.autoDismissToast();
          this.loadCoupons(true);
        },
        error: (err) => this.handleError(err)
      });
    } else {
      this.couponsService.createCoupon(dto).subscribe({
        next: () => {
          this.isLoading = false;
          this.closeModal();
          this.successMessage = 'Coupon created successfully!';
          this.autoDismissToast();
          this.loadCoupons(true);
        },
        error: (err) => this.handleError(err)
      });
    }
  }

  public confirmDelete(coupon: CouponResponseDTO): void {
    this.confirmModal = {
      show: true,
      title: 'Delete Coupon',
      message: `Are you sure you want to delete coupon "${coupon.couponCode}"?`,
      onConfirm: () => {
        this.isLoading = true;
        this.couponsService.deleteCoupon(coupon.couponId).subscribe({
          next: () => {
            this.isLoading = false;
            this.confirmModal.show = false;
            this.successMessage = 'Coupon deleted successfully!';
            this.autoDismissToast();
            this.loadCoupons(true);
          },
          error: (err) => this.handleError(err)
        });
      }
    };
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
    } else if (err.status === 409) {
      this.errorMessage = 'Coupon code already exists.';
    } else {
      this.errorMessage = err.error?.message || err.error || 'Failed to process coupon request.';
    }
  }
}
