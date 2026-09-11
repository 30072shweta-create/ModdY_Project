import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule, FormsModule } from '@angular/forms';
import { AirlinesService } from '../../../core/services/airlines.service';
import { AuthService } from '../../../core/auth/auth.service';
import { AirlineResponseDTO } from '../../../core/models/flight.model';
import { AdminSidebarComponent } from '../components/admin-sidebar/admin-sidebar.component';

@Component({
  selector: 'app-admin-airlines',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule, AdminSidebarComponent],
  templateUrl: './admin-airlines.component.html',
  styleUrls: ['./admin-airlines.component.scss']
})
export class AdminAirlinesComponent implements OnInit {
  private fb = inject(FormBuilder);
  public authService = inject(AuthService);
  private airlinesService = inject(AirlinesService);

  public airlines: AirlineResponseDTO[] = [];
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
    onConfirm: () => {}
  };

  public airlineForm: FormGroup = this.fb.group({
    airlineCode: ['', [Validators.required, Validators.maxLength(3)]],
    airlineName: ['', [Validators.required, Validators.maxLength(200)]]
  });

  public ngOnInit(): void {
    this.loadAirlines();
  }

  public loadAirlines(preserveSuccessMessage = false): void {
    this.isLoading = true;
    this.errorMessage = '';
    if (!preserveSuccessMessage) {
      this.successMessage = '';
    }

    this.airlinesService.getAllAirlines().subscribe({
      next: (data) => {
        this.airlines = data || [];
        this.isLoading = false;
      },
      error: (err) => this.handleError(err)
    });
  }

  public get filteredAirlines(): AirlineResponseDTO[] {
    return this.airlines.filter(
      (a) =>
        !this.search ||
        a.airlineCode.toLowerCase().includes(this.search.toLowerCase()) ||
        a.airlineName.toLowerCase().includes(this.search.toLowerCase())
    );
  }

  public openAddModal(): void {
    this.editingCode = null;
    this.airlineForm.reset();
    this.showModal = true;
  }

  public openEditModal(airline: AirlineResponseDTO): void {
    this.editingCode = airline.airlineCode;
    this.airlineForm.patchValue({
      airlineCode: airline.airlineCode,
      airlineName: airline.airlineName
    });
    this.showModal = true;
  }

  public closeModal(): void {
    this.showModal = false;
    this.editingCode = null;
  }

  public submitAirline(): void {
    if (this.airlineForm.invalid) {
      this.airlineForm.markAllAsTouched();
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';
    this.successMessage = '';

    const dto = {
      airlineCode: this.airlineForm.value.airlineCode.trim().toUpperCase(),
      airlineName: this.airlineForm.value.airlineName.trim()
    };

    if (this.editingCode) {
      this.airlinesService.updateAirline(this.editingCode, dto).subscribe({
        next: () => {
          this.isLoading = false;
          this.showModal = false;
          this.editingCode = null;
          this.successMessage = 'Airline updated successfully!';
          this.autoDismissToast();
          this.loadAirlines(true);
        },
        error: (err) => this.handleError(err)
      });
    } else {
      this.airlinesService.addAirline(dto).subscribe({
        next: () => {
          this.isLoading = false;
          this.showModal = false;
          this.editingCode = null;
          this.airlineForm.reset();
          this.successMessage = 'Airline added successfully!';
          this.autoDismissToast();
          this.loadAirlines(true);
        },
        error: (err) => this.handleError(err)
      });
    }
  }

  public confirmDelete(airline: AirlineResponseDTO): void {
    this.confirmModal = {
      show: true,
      title: 'Delete Airline',
      message: `Are you sure you want to delete airline "${airline.airlineName}" (${airline.airlineCode})?`,
      onConfirm: () => {
        this.isLoading = true;
        this.airlinesService.deleteAirline(airline.airlineCode).subscribe({
          next: () => {
            this.isLoading = false;
            this.confirmModal.show = false;
            this.successMessage = 'Airline deleted successfully!';
            this.autoDismissToast();
            this.loadAirlines(true);
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
      this.errorMessage = err.error?.message || 'Airline code already exists.';
    } else if (err.status === 0) {
      this.errorMessage = 'Unable to connect to the server.';
    } else {
      this.errorMessage = err.error?.message || err.error || 'Failed to process airline request.';
    }
  }
}
