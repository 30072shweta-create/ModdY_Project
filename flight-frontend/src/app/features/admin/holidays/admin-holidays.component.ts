import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule, FormsModule } from '@angular/forms';
import { HolidaysService } from '../../../core/services/holidays.service';
import { HolidayResponseDTO } from '../../../core/models/pricing.model';
import { AdminSidebarComponent } from '../components/admin-sidebar/admin-sidebar.component';

@Component({
  selector: 'app-admin-holidays',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule, AdminSidebarComponent],
  templateUrl: './admin-holidays.component.html',
  styleUrls: ['./admin-holidays.component.scss']
})
export class AdminHolidaysComponent implements OnInit {
  private fb = inject(FormBuilder);
  private holidaysService = inject(HolidaysService);

  public holidayList: HolidayResponseDTO[] = [];
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

  public holidayForm: FormGroup = this.fb.group({
    name: ['', [Validators.required, Validators.maxLength(100)]],
    holidayDate: ['', [Validators.required]],
    active: [true]
  });

  public ngOnInit(): void {
    this.loadHolidays();
  }

  public loadHolidays(preserveSuccessMessage = false): void {
    this.isLoading = true;
    this.errorMessage = '';
    if (!preserveSuccessMessage) {
      this.successMessage = '';
    }

    this.holidaysService.getAllHolidays().subscribe({
      next: (data) => {
        this.holidayList = data || [];
        this.isLoading = false;
      },
      error: (err) => this.handleError(err)
    });
  }

  public get filteredHolidays(): HolidayResponseDTO[] {
    return this.holidayList.filter((h) => {
      if (!this.search) return true;
      const term = this.search.toLowerCase();
      return (
        h.name.toLowerCase().includes(term) ||
        (h.holidayDate && h.holidayDate.includes(term))
      );
    });
  }

  public openAddModal(): void {
    this.editingId = null;
    this.holidayForm.reset({
      name: '',
      holidayDate: '',
      active: true
    });
    this.showModal = true;
  }

  public openEditModal(holiday: HolidayResponseDTO): void {
    this.editingId = holiday.holidayId;
    this.holidayForm.patchValue({
      name: holiday.name,
      holidayDate: holiday.holidayDate ? holiday.holidayDate.split('T')[0] : '',
      active: holiday.active !== false
    });
    this.showModal = true;
  }

  public closeModal(): void {
    this.showModal = false;
    this.editingId = null;
  }

  public submitHoliday(): void {
    if (this.holidayForm.invalid) {
      this.holidayForm.markAllAsTouched();
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';
    this.successMessage = '';

    const raw = this.holidayForm.value;
    const dto: any = {
      name: raw.name ? raw.name.trim() : '',
      holidayDate: raw.holidayDate ? raw.holidayDate.split('T')[0] : '',
      active: raw.active !== false
    };

    if (this.editingId) {
      this.holidaysService.updateHoliday(this.editingId, dto).subscribe({
        next: () => {
          this.isLoading = false;
          this.closeModal();
          this.successMessage = 'Holiday updated successfully!';
          this.autoDismissToast();
          this.loadHolidays(true);
        },
        error: (err) => this.handleError(err)
      });
    } else {
      this.holidaysService.createHoliday(dto).subscribe({
        next: () => {
          this.isLoading = false;
          this.closeModal();
          this.successMessage = 'Holiday created successfully!';
          this.autoDismissToast();
          this.loadHolidays(true);
        },
        error: (err) => this.handleError(err)
      });
    }
  }

  public confirmDelete(holiday: HolidayResponseDTO): void {
    this.confirmModal = {
      show: true,
      title: 'Delete Holiday',
      message: `Are you sure you want to delete holiday "${holiday.name}" (${holiday.holidayDate})?`,
      onConfirm: () => {
        this.isLoading = true;
        this.holidaysService.deleteHoliday(holiday.holidayId).subscribe({
          next: () => {
            this.isLoading = false;
            this.confirmModal.show = false;
            this.successMessage = 'Holiday deleted successfully!';
            this.autoDismissToast();
            this.loadHolidays(true);
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
    } else {
      this.errorMessage = err.error?.message || err.error || 'Failed to process holiday request.';
    }
  }
}
