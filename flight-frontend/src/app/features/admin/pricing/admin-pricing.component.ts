import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule, FormsModule } from '@angular/forms';
import { PricingService } from '../../../core/services/pricing.service';
import { FlightsService } from '../../../core/services/flights.service';
import { FlightPricingResponseDTO } from '../../../core/models/pricing.model';
import { FlightResponseDTO } from '../../../core/models/flight.model';
import { AdminSidebarComponent } from '../components/admin-sidebar/admin-sidebar.component';

@Component({
  selector: 'app-admin-pricing',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule, AdminSidebarComponent],
  templateUrl: './admin-pricing.component.html',
  styleUrls: ['./admin-pricing.component.scss']
})
export class AdminPricingComponent implements OnInit {
  private fb = inject(FormBuilder);
  private pricingService = inject(PricingService);
  private flightsService = inject(FlightsService);

  public pricingList: FlightPricingResponseDTO[] = [];
  public flightsList: FlightResponseDTO[] = [];
  public search = '';
  public isLoading = false;
  public isSaving = false;
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

  public pricingForm: FormGroup = this.fb.group({
    flightId: ['', [Validators.required]],
    cabinClass: ['ECONOMY', [Validators.required]],
    baseFare: [1000, [Validators.required, Validators.min(0)]],
    tax: [150, [Validators.min(0)]],
    airportFee: [100, [Validators.min(0)]],
    convenienceFee: [50, [Validators.min(0)]],
    baggageFee: [0, [Validators.min(0)]],
    discount: [0, [Validators.min(0)]],
    currency: ['INR', [Validators.required]]
  });

  public ngOnInit(): void {
    this.loadPricing();
    this.loadFlights();
  }

  public loadPricing(preserveSuccessMessage = false): void {
    this.isLoading = true;
    this.errorMessage = '';
    if (!preserveSuccessMessage) {
      this.successMessage = '';
    }

    this.pricingService.getAllPricing().subscribe({
      next: (data) => {
        this.pricingList = data || [];
        this.isLoading = false;
      },
      error: (err) => this.handleError(err)
    });
  }

  public loadFlights(): void {
    this.flightsService.getAllFlights().subscribe({
      next: (data) => {
        this.flightsList = data || [];
      },
      error: (err) => console.error('Failed to load flights', err)
    });
  }

  public get filteredPricing(): FlightPricingResponseDTO[] {
    return this.pricingList.filter((p) => {
      if (!this.search) return true;
      const term = this.search.toLowerCase();
      const flightNum = this.getFlightNumber(p.flightId).toLowerCase();
      return (
        p.pricingId?.toString().includes(term) ||
        p.flightId?.toString().includes(term) ||
        flightNum.includes(term) ||
        p.cabinClass?.toLowerCase().includes(term)
      );
    });
  }

  public getFlightNumber(flightId: number): string {
    const flight = this.flightsList.find((f) => f.flightId === flightId);
    return flight ? flight.flightNumber : `Flight #${flightId}`;
  }

  public openAddModal(): void {
    this.editingId = null;
    this.pricingForm.reset({
      cabinClass: 'ECONOMY',
      baseFare: 1000,
      tax: 150,
      airportFee: 100,
      convenienceFee: 50,
      baggageFee: 0,
      discount: 0,
      currency: 'INR'
    });
    this.showModal = true;
  }

  public openEditModal(pricing: FlightPricingResponseDTO): void {
    this.editingId = pricing.pricingId;
    this.pricingForm.patchValue({
      flightId: pricing.flightId,
      cabinClass: pricing.cabinClass,
      baseFare: pricing.baseFare,
      tax: pricing.tax || 0,
      airportFee: pricing.airportFee || 0,
      convenienceFee: pricing.convenienceFee || 0,
      baggageFee: pricing.baggageFee || 0,
      discount: pricing.discount || 0,
      currency: pricing.currency || 'INR'
    });
    this.showModal = true;
  }

  public closeModal(): void {
    this.showModal = false;
    this.editingId = null;
  }

  public submitPricing(): void {
    if (this.pricingForm.invalid) {
      this.pricingForm.markAllAsTouched();
      return;
    }

    this.isSaving = true;
    this.errorMessage = '';
    this.successMessage = '';
    const raw = this.pricingForm.value;
    const taxVal = raw.tax !== undefined && raw.tax !== null ? Number(raw.tax) : (raw.taxes !== undefined && raw.taxes !== null ? Number(raw.taxes) : 0);
    const dto = {
      ...raw,
      flightId: Number(raw.flightId),
      baseFare: Number(raw.baseFare),
      tax: taxVal,
      taxes: taxVal,
      airportFee: Number(raw.airportFee || 0),
      convenienceFee: Number(raw.convenienceFee || 0),
      baggageFee: Number(raw.baggageFee || 0),
      discount: Number(raw.discount || 0)
    };

    if (this.editingId) {
      this.pricingService.updatePricing(this.editingId, dto).subscribe({
        next: () => this.handlePricingSaveSuccess('Pricing record updated successfully!'),
        error: (err) => this.handlePricingSaveError(err)
      });
    } else {
      this.pricingService.addPricing(dto).subscribe({
        next: () => this.handlePricingSaveSuccess('Pricing record created successfully!'),
        error: (err) => this.handlePricingSaveError(err)
      });
    }
  }

  public confirmDelete(pricing: FlightPricingResponseDTO): void {
    this.confirmModal = {
      show: true,
      title: 'Delete Pricing',
      message: `Are you sure you want to delete pricing record #${pricing.pricingId} (${pricing.cabinClass})?`,
      onConfirm: () => {
        this.isLoading = true;
        this.pricingService.deletePricing(pricing.pricingId).subscribe({
          next: () => {
            this.isLoading = false;
            this.confirmModal.show = false;
            this.successMessage = 'Pricing record deleted successfully!';
            this.loadPricing(true);
          },
          error: (err) => this.handleError(err)
        });
      }
    };
  }

  private handleError(err: any): void {
    this.isLoading = false;
    if (err.status === 403) {
      this.errorMessage = 'You do not have permission to perform this action.';
    } else {
      this.errorMessage = this.getErrorMessage(err, 'Failed to process pricing request.');
    }
  }

  private handlePricingSaveSuccess(message: string): void {
    this.isSaving = false;
    this.closeModal();
    this.pricingForm.reset({
      cabinClass: 'ECONOMY',
      baseFare: 100,
      tax: 15,
      airportFee: 10,
      convenienceFee: 5,
      baggageFee: 0,
      discount: 0,
      currency: 'USD'
    });
    this.successMessage = message;
    this.autoDismissToast();
    this.loadPricing(true);
  }

  private autoDismissToast(): void {
    setTimeout(() => {
      this.successMessage = '';
    }, 4000);
  }

  private handlePricingSaveError(err: any): void {
    if (err?.status >= 200 && err?.status < 300) {
      this.handlePricingSaveSuccess(this.editingId ? 'Pricing record updated successfully!' : 'Pricing record created successfully!');
      return;
    }

    this.isSaving = false;
    this.handleError(err);
  }

  private getErrorMessage(err: any, fallback: string): string {
    if (typeof err?.error === 'string') {
      return err.error;
    }

    if (typeof err?.error?.message === 'string') {
      return err.error.message;
    }

    if (typeof err?.message === 'string') {
      return err.message;
    }

    return fallback;
  }
}
