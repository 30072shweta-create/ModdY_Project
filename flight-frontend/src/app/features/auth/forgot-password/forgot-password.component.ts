import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/auth/auth.service';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  template: `
    <div class="auth-wrapper">
      <div class="card auth-card">
        <div class="auth-header">
          <h2>Forgot Password</h2>
          <p>Enter your account email to receive a password reset OTP</p>
        </div>

        @if (errorMessage) { <div class="alert alert-danger">{{ errorMessage }}</div> }
        @if (successMessage) { <div class="alert alert-success">{{ successMessage }}</div> }

        <form [formGroup]="form" (ngSubmit)="onSubmit()">
          <div class="form-group">
            <label for="email">Email Address</label>
            <input type="email" id="email" formControlName="email" class="form-control" placeholder="name@example.com" />
          </div>

          <button type="submit" class="btn btn-primary w-100" [disabled]="isLoading">
            {{ isLoading ? 'Sending OTP...' : 'Send Reset Code' }}
          </button>
        </form>

        <div class="auth-footer">
          <p>Remembered password? <a routerLink="/login">Sign In</a></p>
        </div>
      </div>
    </div>
  `,
  styleUrls: ['../login/login.component.scss']
})
export class ForgotPasswordComponent {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private router = inject(Router);

  public form: FormGroup = this.fb.group({
    email: ['', [Validators.required, Validators.email]]
  });

  public isLoading = false;
  public errorMessage = '';
  public successMessage = '';

  public onSubmit(): void {
    if (this.form.invalid) return;
    this.isLoading = true;
    this.errorMessage = '';
    this.successMessage = '';

    this.authService.forgotPassword(this.form.value).subscribe({
      next: (res) => {
        this.isLoading = false;
        this.successMessage = res || 'Password reset OTP sent to your email!';
        setTimeout(() => {
          this.router.navigate(['/reset-password'], { queryParams: { email: this.form.value.email } });
        }, 1500);
      },
      error: (err) => {
        this.isLoading = false;
        this.errorMessage = err.error?.message || err.error || 'Failed to process request.';
      }
    });
  }
}
