import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink, ActivatedRoute } from '@angular/router';
import { AuthService } from '../../../core/auth/auth.service';

@Component({
  selector: 'app-reset-password',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  template: `
    <div class="auth-wrapper">
      <div class="card auth-card">
        <div class="auth-header">
          <h2>Reset Password</h2>
          <p>Enter the OTP received on email and your new password</p>
        </div>

        @if (errorMessage) { <div class="alert alert-danger">{{ errorMessage }}</div> }
        @if (successMessage) { <div class="alert alert-success">{{ successMessage }}</div> }

        <form [formGroup]="form" (ngSubmit)="onSubmit()">
          <div class="form-group">
            <label for="email">Email Address</label>
            <input type="email" id="email" formControlName="email" class="form-control" />
          </div>

          <div class="form-group">
            <label for="otp">OTP / Token</label>
            <input type="text" id="otp" formControlName="otp" class="form-control" placeholder="123456" />
          </div>

          <div class="form-group">
            <label for="newPassword">New Password</label>
            <input type="password" id="newPassword" formControlName="newPassword" class="form-control" placeholder="New secure password" />
          </div>

          <button type="submit" class="btn btn-primary w-100" [disabled]="isLoading">
            {{ isLoading ? 'Resetting Password...' : 'Reset Password' }}
          </button>
        </form>

        <div class="auth-footer">
          <p><a routerLink="/login">Back to Sign In</a></p>
        </div>
      </div>
    </div>
  `,
  styleUrls: ['../login/login.component.scss']
})
export class ResetPasswordComponent {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  public form: FormGroup = this.fb.group({
    email: [this.route.snapshot.queryParams['email'] || '', [Validators.required, Validators.email]],
    otp: ['', [Validators.required]],
    newPassword: ['', [Validators.required, Validators.minLength(6)]]
  });

  public isLoading = false;
  public errorMessage = '';
  public successMessage = '';

  public onSubmit(): void {
    if (this.form.invalid) return;
    this.isLoading = true;
    this.errorMessage = '';
    this.successMessage = '';

    this.authService.resetPassword(this.form.value).subscribe({
      next: (res) => {
        this.isLoading = false;
        this.successMessage = res || 'Password reset successfully! Redirecting to login...';
        setTimeout(() => this.router.navigate(['/login']), 1500);
      },
      error: (err) => {
        this.isLoading = false;
        this.errorMessage = err.error?.message || err.error || 'Failed to reset password.';
      }
    });
  }
}
