import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule, FormsModule } from '@angular/forms';
import { AirportsService } from '../../../core/services/airports.service';
import { AuthService } from '../../../core/auth/auth.service';
import { AirportResponseDTO } from '../../../core/models/flight.model';
import { AdminSidebarComponent } from '../components/admin-sidebar/admin-sidebar.component';

@Component({
  selector: 'app-admin-airports',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule, AdminSidebarComponent],
  templateUrl: './admin-airports.component.html',
  styleUrls: ['./admin-airports.component.scss']
})
export class AdminAirportsComponent implements OnInit {
  private fb = inject(FormBuilder);
  public authService = inject(AuthService);
  private airportsService = inject(AirportsService);

  public airports: AirportResponseDTO[] = [];
  public search = '';
  public isLoading = false;
  public errorMessage = '';
  public successMessage = '';

  public showModal = false;
  public editingCode: string | null = null;

  public confirmModal = {
    show: false,
    title: '',
    message: '',
    onConfirm: () => { }
  };

  public airportForm: FormGroup = this.fb.group({
    airportCode: ['', [Validators.required, Validators.maxLength(10)]],
    name: ['', [Validators.required]],
    city: ['', [Validators.required]],
    country: ['', [Validators.required]]
  });

  public ngOnInit(): void {
    this.loadAirports();
  }

  public loadAirports(preserveSuccessMessage = false): void {
    this.isLoading = true;
    this.errorMessage = '';
    if (!preserveSuccessMessage) {
      this.successMessage = '';
    }

    this.airportsService.getAllAirports().subscribe({
      next: (data) => {
        this.airports = data || [];
        this.isLoading = false;
      },
      error: (err) => this.handleError(err)
    });
  }

  public get filteredAirports(): AirportResponseDTO[] {
    return this.airports.filter((ap) => {
      const apName = ap.name || ap.airportName || '';
      return (
        !this.search ||
        ap.airportCode.toLowerCase().includes(this.search.toLowerCase()) ||
        apName.toLowerCase().includes(this.search.toLowerCase()) ||
        ap.city.toLowerCase().includes(this.search.toLowerCase()) ||
        ap.country.toLowerCase().includes(this.search.toLowerCase())
      );
    });
  }

  public openAddModal(): void {
    this.editingCode = null;
    this.airportForm.reset();
    this.showModal = true;
  }

  public openEditModal(airport: AirportResponseDTO): void {
    this.editingCode = airport.airportCode;
    this.airportForm.patchValue({
      airportCode: airport.airportCode,
      name: airport.name || airport.airportName || '',
      city: airport.city,
      country: airport.country
    });
    this.showModal = true;
  }

  public closeModal(): void {
    this.showModal = false;
    this.editingCode = null;
  }

  public submitAirport(): void {
    if (this.airportForm.invalid) {
      this.airportForm.markAllAsTouched();
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';
    this.successMessage = '';

    const dto = {
      airportCode: this.airportForm.value.airportCode,
      name: this.airportForm.value.name,
      city: this.airportForm.value.city,
      country: this.airportForm.value.country
    };

    if (this.editingCode) {
      this.airportsService.updateAirport(this.editingCode, dto).subscribe({
        next: () => {
          this.isLoading = false;
          this.closeModal();
          this.successMessage = 'Airport updated successfully!';
          this.loadAirports(true);
        },
        error: (err) => this.handleError(err)
      });
    } else {
      this.airportsService.addAirport(dto).subscribe({
        next: () => {
          this.isLoading = false;
          this.closeModal();
          this.successMessage = 'Airport added successfully!';
          this.loadAirports(true);
        },
        error: (err) => this.handleError(err)
      });
    }
  }

  public confirmDelete(airport: AirportResponseDTO): void {
    const nameStr = airport.name || airport.airportName || airport.airportCode;
    this.confirmModal = {
      show: true,
      title: 'Delete Airport',
      message: `Are you sure you want to delete airport "${nameStr}" (${airport.airportCode})?`,
      onConfirm: () => {
        this.isLoading = true;
        this.airportsService.deleteAirport(airport.airportCode).subscribe({
          next: () => {
            this.isLoading = false;
            this.confirmModal.show = false;
            this.successMessage = 'Airport deleted successfully!';
            this.loadAirports(true);
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
    } else if (err.status === 409) {
      this.errorMessage = err.error?.message || 'Airport code already exists.';
    } else if (err.status === 0) {
      this.errorMessage = 'Unable to connect to the server.';
    } else {
      this.errorMessage = err.error?.message || err.error || 'Failed to process airport request.';
    }
  }
}
