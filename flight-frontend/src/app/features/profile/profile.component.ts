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
      <div class="container">
        <div class="card profile-card">
          <h2>User Profile & Settings</h2>

          @if (errorMessage) { <div class="alert alert-danger">{{ errorMessage }}</div> }
          @if (successMessage) { <div class="alert alert-success">{{ successMessage }}</div> }

          <form [formGroup]="profileForm" (ngSubmit)="updateProfile()">
            <h3>Personal Information</h3>
            <div class="form-grid">
              <div class="form-group">
                <label>First Name</label>
                <input type="text" formControlName="firstName" class="form-control" />
              </div>
              <div class="form-group">
                <label>Last Name</label>
                <input type="text" formControlName="lastName" class="form-control" />
              </div>
            </div>
            <button type="submit" class="btn btn-primary btn-sm mb-4" [disabled]="isUpdatingProfile">Update Profile</button>
          </form>

          <form [formGroup]="passwordForm" (ngSubmit)="changePassword()" class="mt-4 pt-4 border-top">
            <h3>Change Password</h3>
            <div class="form-group">
              <label>Current Password</label>
              <input type="password" formControlName="oldPassword" class="form-control" />
            </div>
            <div class="form-group">
              <label>New Password</label>
              <input type="password" formControlName="newPassword" class="form-control" />
            </div>
            <button type="submit" class="btn btn-secondary btn-sm" [disabled]="isChangingPassword">Change Password</button>
          </form>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .profile-page { padding: 3rem 0; }
    .profile-card { max-width: 600px; margin: 0 auto; padding: 2.25rem; }
    .form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 1rem; }
    .border-top { border-top: 1px solid var(--border-color); }
    .mb-4 { margin-bottom: 1.5rem; }
    .mt-4 { margin-top: 1.5rem; }
    .pt-4 { padding-top: 1.5rem; }
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

  public updateProfile(): void {
    if (this.profileForm.invalid) return;
    this.isUpdatingProfile = true;
    this.errorMessage = '';
    this.successMessage = '';

    this.usersService.updateProfile(this.profileForm.value).subscribe({
      next: (updatedUser) => {
        this.isUpdatingProfile = false;
        this.authService.setCurrentUser(updatedUser);
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
