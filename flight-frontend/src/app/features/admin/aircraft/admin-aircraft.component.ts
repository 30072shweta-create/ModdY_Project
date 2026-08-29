import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule, FormsModule } from '@angular/forms';
import { AircraftService } from '../../../core/services/aircraft.service';
import { AuthService } from '../../../core/auth/auth.service';
import { AircraftResponseDTO } from '../../../core/models/flight.model';
import { AdminSidebarComponent } from '../components/admin-sidebar/admin-sidebar.component';

@Component({
  selector: 'app-admin-aircraft',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule, AdminSidebarComponent],
  templateUrl: './admin-aircraft.component.html',
  styleUrls: ['./admin-aircraft.component.scss']
})
export class AdminAircraftComponent implements OnInit {
  private fb = inject(FormBuilder);
  public authService = inject(AuthService);
  private aircraftService = inject(AircraftService);

  public aircraftList: AircraftResponseDTO[] = [];
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
    onConfirm: () => { }
  };

  public aircraftForm: FormGroup = this.fb.group({
    aircraftCode: ['', [Validators.required, Validators.maxLength(50)]],
    model: ['', [Validators.required, Validators.maxLength(100)]],
    manufacturer: ['', [Validators.maxLength(100)]],
    totalSeatCapacity: [180, [Validators.required, Validators.min(1)]],
    active: [true]
  });

  public ngOnInit(): void {
    this.loadAircraft();
  }

  public loadAircraft(preserveSuccessMessage = false): void {
    this.isLoading = true;
    this.errorMessage = '';
    if (!preserveSuccessMessage) {
      this.successMessage = '';
    }

    this.aircraftService.getAllAircraft().subscribe({
      next: (data) => {
        this.aircraftList = data || [];
        this.isLoading = false;
      },
      error: (err) => this.handleError(err)
    });
  }

  public get filteredAircraft(): AircraftResponseDTO[] {
    return this.aircraftList.filter(
      (ac) =>
        !this.search ||
        ac.aircraftCode.toLowerCase().includes(this.search.toLowerCase()) ||
        ac.model.toLowerCase().includes(this.search.toLowerCase()) ||
        (ac.manufacturer && ac.manufacturer.toLowerCase().includes(this.search.toLowerCase()))
    );
  }

  public openAddModal(): void {
    this.editingId = null;
    this.aircraftForm.reset({ active: true, totalSeatCapacity: 180 });
    this.showModal = true;
  }

  public openEditModal(aircraft: AircraftResponseDTO): void {
    this.editingId = aircraft.aircraftId;
    this.aircraftForm.patchValue({
      aircraftCode: aircraft.aircraftCode,
      model: aircraft.model,
      manufacturer: aircraft.manufacturer || '',
      totalSeatCapacity: aircraft.totalSeatCapacity || 180,
      active: aircraft.active !== false
    });
    this.showModal = true;
  }

  public closeModal(): void {
    this.showModal = false;
    this.editingId = null;
  }

  public submitAircraft(): void {
    if (this.aircraftForm.invalid) {
      this.aircraftForm.markAllAsTouched();
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';
    this.successMessage = '';
    const dto = this.aircraftForm.value;

    if (this.editingId) {
      this.aircraftService.updateAircraft(this.editingId, dto).subscribe({
        next: () => {
          this.isLoading = false;
          //this.showModal = false;
          //this.editingId = null;
          this.closeModal();
          this.successMessage = 'Aircraft updated successfully!';
          this.loadAircraft(true);
        },
        error: (err) => this.handleError(err)
      });
    } else {
      this.aircraftService.createAircraft(dto).subscribe({
        next: (response) => {
          console.log('AirCraft added successfully:', response);
          this.isLoading = false;
          this.closeModal();
          this.successMessage = 'Aircraft created successfully!';
          this.loadAircraft(true);
        },
        error: (err) => this.handleError(err)
      });
    }
  }

  public confirmDeactivate(aircraft: AircraftResponseDTO): void {
    this.confirmModal = {
      show: true,
      title: 'Deactivate Aircraft',
      message: `Are you sure you want to deactivate aircraft "${aircraft.aircraftCode}" (${aircraft.model})?`,
      onConfirm: () => {
        this.isLoading = true;
        this.aircraftService.deleteAircraft(aircraft.aircraftId).subscribe({
          next: () => {
            this.isLoading = false;
            this.confirmModal.show = false;
            this.successMessage = 'Aircraft deactivated successfully!';
            this.loadAircraft(true);
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
      this.errorMessage = err.error?.message || 'Aircraft code already exists.';
    } else if (err.status === 0) {
      this.errorMessage = 'Unable to connect to the server.';
    } else {
      this.errorMessage = err.error?.message || err.error || 'Failed to process aircraft request.';
    }
  }
}
