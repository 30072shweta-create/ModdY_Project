import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule, FormsModule } from '@angular/forms';
import { PricingService } from '../../../core/services/pricing.service';
import { PricingRuleResponseDTO } from '../../../core/models/pricing.model';
import { AdminSidebarComponent } from '../components/admin-sidebar/admin-sidebar.component';

@Component({
  selector: 'app-admin-pricing-rules',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule, AdminSidebarComponent],
  templateUrl: './admin-pricing-rules.component.html',
  styleUrls: ['./admin-pricing-rules.component.scss']
})
export class AdminPricingRulesComponent implements OnInit {
  private fb = inject(FormBuilder);
  private pricingService = inject(PricingService);

  public rulesList: PricingRuleResponseDTO[] = [];
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

  public ruleForm: FormGroup = this.fb.group({
    name: ['', [Validators.required, Validators.maxLength(100)]],
    ruleType: ['WEEKEND', [Validators.required]],
    adjustmentType: ['PERCENTAGE', [Validators.required]],
    adjustmentValue: [10, [Validators.required]],
    effectiveFrom: [''],
    effectiveTo: [''],
    priority: [1, [Validators.min(1)]],
    active: [true]
  });

  public ngOnInit(): void {
    this.loadRules();
  }

  public loadRules(preserveSuccessMessage = false): void {
    this.isLoading = true;
    this.errorMessage = '';
    if (!preserveSuccessMessage) {
      this.successMessage = '';
    }

    this.pricingService.getAllRules().subscribe({
      next: (data) => {
        this.rulesList = data || [];
        this.isLoading = false;
      },
      error: (err) => this.handleError(err)
    });
  }

  public get filteredRules(): PricingRuleResponseDTO[] {
    return this.rulesList.filter((r) => {
      if (!this.search) return true;
      const term = this.search.toLowerCase();
      return (
        r.name.toLowerCase().includes(term) ||
        r.ruleType.toLowerCase().includes(term) ||
        r.adjustmentType.toLowerCase().includes(term)
      );
    });
  }

  public openAddModal(): void {
    this.editingId = null;
    this.ruleForm.reset({
      name: '',
      ruleType: 'WEEKEND',
      adjustmentType: 'PERCENTAGE',
      adjustmentValue: 10,
      priority: 1,
      active: true
    });
    this.showModal = true;
  }

  public openEditModal(rule: PricingRuleResponseDTO): void {
    this.editingId = rule.ruleId;
    this.ruleForm.patchValue({
      name: rule.name,
      ruleType: rule.ruleType,
      adjustmentType: rule.adjustmentType,
      adjustmentValue: rule.adjustmentValue,
      effectiveFrom: rule.effectiveFrom ? rule.effectiveFrom.split('T')[0] : '',
      effectiveTo: rule.effectiveTo ? rule.effectiveTo.split('T')[0] : '',
      priority: rule.priority || 1,
      active: rule.active !== false
    });
    this.showModal = true;
  }

  public closeModal(): void {
    this.showModal = false;
    this.editingId = null;
  }

  public submitRule(): void {
    if (this.ruleForm.invalid) {
      this.ruleForm.markAllAsTouched();
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';
    this.successMessage = '';
    const dto = this.ruleForm.value;

    if (this.editingId) {
      this.pricingService.updateRule(this.editingId, dto).subscribe({
        next: () => {
          this.isLoading = false;
          this.closeModal();
          this.successMessage = 'Pricing rule updated successfully!';
          this.loadRules(true);
        },
        error: (err) => this.handleError(err)
      });
    } else {
      this.pricingService.createRule(dto).subscribe({
        next: () => {
          this.isLoading = false;
          this.closeModal();
          this.successMessage = 'Pricing rule created successfully!';
          this.loadRules(true);
        },
        error: (err) => this.handleError(err)
      });
    }
  }

  public confirmDelete(rule: PricingRuleResponseDTO): void {
    this.confirmModal = {
      show: true,
      title: 'Delete Pricing Rule',
      message: `Are you sure you want to delete pricing rule "${rule.name}"?`,
      onConfirm: () => {
        this.isLoading = true;
        this.pricingService.deleteRule(rule.ruleId).subscribe({
          next: () => {
            this.isLoading = false;
            this.confirmModal.show = false;
            this.successMessage = 'Pricing rule deleted successfully!';
            this.loadRules(true);
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
      this.errorMessage = err.error?.message || err.error || 'Failed to process pricing rule request.';
    }
  }
}
