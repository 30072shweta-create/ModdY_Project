import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { UsersService } from '../../core/services/users.service';
import { AuthService } from '../../core/auth/auth.service';
import { UserResponseDTO } from '../../core/models/auth.model';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <div class="profile-page">
      <div class="profile-container">

        <!-- Page Header -->
        <div class="profile-header">
          <div class="profile-header__avatar">
            <span>{{ getUserInitials() }}</span>
          </div>
          <div class="profile-header__info">
            <h1 class="profile-header__title">{{ user?.firstName || 'User' }} {{ user?.lastName || 'Profile' }}</h1>
            <p class="profile-header__email">{{ user?.email || 'No email associated' }}</p>
          </div>
          <div class="profile-header__badge">
            <span class="role-pill">{{ (user?.role || 'CUSTOMER').replace('ROLE_', '') }}</span>
          </div>
        </div>

        <!-- Feedback Alerts -->
        @if (errorMessage) {
          <div class="alert alert-danger">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="10"></circle>
              <line x1="12" y1="8" x2="12" y2="12"></line>
              <line x1="12" y1="16" x2="12.01" y2="16"></line>
            </svg>
            <span>{{ errorMessage }}</span>
          </div>
        }
        @if (successMessage) {
          <div class="alert alert-success">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path>
              <polyline points="22 4 12 14.01 9 11.01"></polyline>
            </svg>
            <span>{{ successMessage }}</span>
          </div>
        }

        <!-- Card 1: Personal Information -->
        <div class="profile-card">
          <div class="profile-card__header">
            <div class="profile-card__title-group">
              <h2 class="profile-card__title">Personal Information</h2>
              <p class="profile-card__subtitle">Update your personal credentials and public name</p>
            </div>
          </div>

          <form [formGroup]="profileForm" (ngSubmit)="updateProfile()" class="profile-form">
            <div class="form-grid">
              <div class="form-group">
                <label class="form-label" for="firstName">First Name</label>
                <input
                  id="firstName"
                  type="text"
                  formControlName="firstName"
                  class="form-control"
                  placeholder="Enter your first name"
                />
              </div>

              <div class="form-group">
                <label class="form-label" for="lastName">Last Name</label>
                <input
                  id="lastName"
                  type="text"
                  formControlName="lastName"
                  class="form-control"
                  placeholder="Enter your last name"
                />
              </div>

              <div class="form-group form-group--full">
                <label class="form-label">Email Address</label>
                <input
                  type="email"
                  [value]="user?.email"
                  disabled
                  class="form-control form-control--disabled"
                />
                <span class="form-hint">Email address is permanently tied to your account credentials.</span>
              </div>
            </div>

            <div class="form-actions">
              <button type="submit" class="btn btn-primary" [disabled]="isUpdatingProfile || profileForm.invalid">
                <span *ngIf="isUpdatingProfile" class="spinner"></span>
                <span>{{ isUpdatingProfile ? 'Saving Changes...' : 'Update Profile' }}</span>
              </button>
            </div>
          </form>
        </div>

        <!-- Card 2: Change Password -->
        <div class="profile-card">
          <div class="profile-card__header">
            <div class="profile-card__title-group">
              <h2 class="profile-card__title">Security & Password</h2>
              <p class="profile-card__subtitle">Change your password to maintain account security</p>
            </div>
          </div>

          <form [formGroup]="passwordForm" (ngSubmit)="changePassword()" class="profile-form">
            <div class="form-grid">
              <div class="form-group">
                <label class="form-label" for="oldPassword">Current Password</label>
                <input
                  id="oldPassword"
                  type="password"
                  formControlName="oldPassword"
                  class="form-control"
                  placeholder="••••••••••••"
                />
              </div>

              <div class="form-group">
                <label class="form-label" for="newPassword">New Password</label>
                <input
                  id="newPassword"
                  type="password"
                  formControlName="newPassword"
                  class="form-control"
                  placeholder="At least 6 characters"
                />
              </div>
            </div>

            <div class="form-actions">
              <button type="submit" class="btn btn-outline" [disabled]="isChangingPassword || passwordForm.invalid">
                <span *ngIf="isChangingPassword" class="spinner"></span>
                <span>{{ isChangingPassword ? 'Updating Password...' : 'Change Password' }}</span>
              </button>
            </div>
          </form>
        </div>

      </div>
    </div>
  `,
  styles: [`
    .profile-page {
      min-height: 80vh;
      background-color: var(--md-background, #F9F9FF);
      padding: 3rem 1rem 5rem;
      font-family: 'Inter', -apple-system, BlinkMacSystemFont, sans-serif;
    }

    .profile-container {
      max-width: 720px;
      margin: 0 auto;
      display: flex;
      flex-direction: column;
      gap: 1.5rem;
    }

    /* ── Header Banner ── */
    .profile-header {
      background: #FFFFFF;
      border: 1px solid var(--border-color, #E8EBF0);
      border-radius: 12px;
      padding: 1.5rem 2rem;
      display: flex;
      align-items: center;
      gap: 1.25rem;
      box-shadow: 0 2px 8px rgba(16, 26, 43, 0.04);

      @media (max-width: 600px) {
        flex-direction: column;
        text-align: center;
        padding: 1.5rem 1rem;
      }
    }

    .profile-header__avatar {
      width: 58px;
      height: 58px;
      border-radius: 50%;
      background: #101A2B;
      color: #FFFFFF;
      border: 2px solid #B58A4A;
      display: flex;
      align-items: center;
      justify-content: center;
      font-weight: 700;
      font-size: 1.25rem;
      letter-spacing: 0.05em;
      flex-shrink: 0;
      box-shadow: 0 4px 12px rgba(16, 26, 43, 0.15);
    }

    .profile-header__info {
      flex: 1;
    }

    .profile-header__title {
      font-size: 1.35rem;
      font-weight: 700;
      color: #101A2B;
      margin: 0 0 2px 0;
      letter-spacing: -0.01em;
    }

    .profile-header__email {
      font-size: 0.875rem;
      color: #6B7280;
      margin: 0;
    }

    .profile-header__badge {
      flex-shrink: 0;
    }

    .role-pill {
      display: inline-block;
      font-family: monospace;
      font-size: 0.75rem;
      font-weight: 700;
      letter-spacing: 0.08em;
      padding: 4px 10px;
      border-radius: 4px;
      background: #F4F6F9;
      color: #101A2B;
      border: 1px solid #E2E5EC;
    }

    /* ── Alerts ── */
    .alert {
      display: flex;
      align-items: center;
      gap: 10px;
      padding: 12px 16px;
      border-radius: 8px;
      font-size: 0.88rem;
      font-weight: 500;
      line-height: 1.4;
    }

    .alert-danger {
      background: #FEF2F2;
      border: 1px solid #FECACA;
      color: #991B1B;
    }

    .alert-success {
      background: #E6F6ED;
      border: 1px solid #B7E4C7;
      color: #1A6B3C;
    }

    /* ── Profile Cards ── */
    .profile-card {
      background: #FFFFFF;
      border: 1px solid var(--border-color, #E8EBF0);
      border-radius: 12px;
      padding: 2rem 2rem;
      box-shadow: 0 2px 8px rgba(16, 26, 43, 0.04);

      @media (max-width: 600px) {
        padding: 1.5rem 1.25rem;
      }
    }

    .profile-card__header {
      margin-bottom: 1.5rem;
      padding-bottom: 1rem;
      border-bottom: 1px solid var(--border-color, #E8EBF0);
    }

    .profile-card__title {
      font-size: 1.15rem;
      font-weight: 700;
      color: #101A2B;
      margin: 0 0 4px 0;
      letter-spacing: -0.01em;
    }

    .profile-card__subtitle {
      font-size: 0.85rem;
      color: #6B7280;
      margin: 0;
    }

    /* ── Form Styles ── */
    .profile-form {
      display: flex;
      flex-direction: column;
    }

    .form-grid {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 1.25rem;
      margin-bottom: 1.5rem;

      @media (max-width: 600px) {
        grid-template-columns: 1fr;
        gap: 1rem;
      }
    }

    .form-group {
      display: flex;
      flex-direction: column;
      gap: 6px;

      &--full {
        grid-column: 1 / -1;
      }
    }

    .form-label {
      font-size: 0.78rem;
      font-weight: 700;
      color: #101A2B;
      letter-spacing: 0.04em;
      text-transform: uppercase;
      margin: 0;
    }

    .form-control {
      width: 100%;
      box-sizing: border-box;
      padding: 10px 14px;
      font-size: 0.95rem;
      font-weight: 500;
      font-family: inherit;
      color: #101A2B;
      background: #F9FAFC;
      border: 1px solid #D5D9E2;
      border-radius: 8px;
      outline: none;
      transition: border-color 0.2s ease, box-shadow 0.2s ease, background 0.2s ease;

      &:focus {
        border-color: #B58A4A;
        background: #FFFFFF;
        box-shadow: 0 0 0 3px rgba(181, 138, 74, 0.16);
      }

      &--disabled {
        background: #F1F3F7;
        color: #6B7280;
        cursor: not-allowed;
        border-color: #E2E5EC;
      }
    }

    .form-hint {
      font-size: 0.75rem;
      color: #6B7280;
      margin-top: 2px;
    }

    /* ── Buttons ── */
    .form-actions {
      display: flex;
      justify-content: flex-start;
      margin-top: 0.5rem;
    }

    .btn {
      display: inline-flex;
      align-items: center;
      gap: 8px;
      padding: 10px 22px;
      border-radius: 8px;
      font-size: 0.9rem;
      font-weight: 600;
      cursor: pointer;
      transition: all 0.2s ease;
      text-decoration: none;

      &:disabled {
        opacity: 0.55;
        cursor: not-allowed;
      }
    }

    .btn-primary {
      background: #101A2B;
      color: #FFFFFF;
      border: 1px solid #101A2B;

      &:hover:not(:disabled) {
        background: #1B2C47;
        border-color: #1B2C47;
        transform: translateY(-1px);
        box-shadow: 0 4px 12px rgba(16, 26, 43, 0.15);
      }
    }

    .btn-outline {
      background: #FFFFFF;
      color: #101A2B;
      border: 1px solid #D5D9E2;

      &:hover:not(:disabled) {
        border-color: #B58A4A;
        color: #B58A4A;
        background: rgba(181, 138, 74, 0.04);
        transform: translateY(-1px);
        box-shadow: 0 2px 8px rgba(181, 138, 74, 0.12);
      }
    }

    .spinner {
      width: 14px;
      height: 14px;
      border: 2px solid rgba(255, 255, 255, 0.3);
      border-top-color: #FFFFFF;
      border-radius: 50%;
      animation: spin 0.6s linear infinite;
    }

    .btn-outline .spinner {
      border: 2px solid rgba(16, 26, 43, 0.2);
      border-top-color: #101A2B;
    }

    @keyframes spin {
      to { transform: rotate(360deg); }
    }
  `]
})
export class ProfileComponent implements OnInit {
  private fb = inject(FormBuilder);
  private usersService = inject(UsersService);
  private authService = inject(AuthService);

  public user: UserResponseDTO | null = null;
  public errorMessage = '';
  public successMessage = '';
  public isUpdatingProfile = false;
  public isChangingPassword = false;

  public profileForm: FormGroup = this.fb.group({
    firstName: ['', [Validators.required]],
    lastName: ['', [Validators.required]]
  });

  public passwordForm: FormGroup = this.fb.group({
    oldPassword: ['', [Validators.required]],
    newPassword: ['', [Validators.required, Validators.minLength(6)]]
  });

  public ngOnInit(): void {
    this.user = this.authService.currentUser();
    if (this.user) {
      this.profileForm.patchValue({
        firstName: this.user.firstName,
        lastName: this.user.lastName
      });
    }
  }

  public getUserInitials(): string {
    if (!this.user) return 'M';
    const f = this.user.firstName ? this.user.firstName.charAt(0).toUpperCase() : '';
    const l = this.user.lastName ? this.user.lastName.charAt(0).toUpperCase() : '';
    return f + l || 'M';
  }

  public updateProfile(): void {
    if (this.profileForm.invalid) return;
    this.isUpdatingProfile = true;
    this.errorMessage = '';
    this.successMessage = '';

    this.usersService.updateProfile(this.profileForm.value).subscribe({
      next: (updatedUser) => {
        this.isUpdatingProfile = false;
        this.authService.setCurrentUser(updatedUser);
        this.user = updatedUser;
        this.successMessage = 'Profile updated successfully!';
      },
      error: (err) => {
        this.isUpdatingProfile = false;
        this.errorMessage = err.error?.message || 'Failed to update profile.';
      }
    });
  }

  public changePassword(): void {
    if (this.passwordForm.invalid) return;
    this.isChangingPassword = true;
    this.errorMessage = '';
    this.successMessage = '';

    this.usersService.changePassword(this.passwordForm.value).subscribe({
      next: (msg) => {
        this.isChangingPassword = false;
        this.passwordForm.reset();
        this.successMessage = msg || 'Password changed successfully!';
      },
      error: (err) => {
        this.isChangingPassword = false;
        this.errorMessage = err.error?.message || 'Failed to change password.';
      }
    });
  }
}
