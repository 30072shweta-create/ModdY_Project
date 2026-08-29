import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink, ActivatedRoute } from '@angular/router';
import { AuthService } from '../../../core/auth/auth.service';

@Component({
  selector: 'app-verify-email',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  template: `
    <div class="min-h-screen bg-background font-body-md text-on-surface flex w-full">
      <!-- Left Side: Photography Canvas -->
      <div class="hidden lg:flex lg:flex-1 relative overflow-hidden bg-primary-container">
        <div class="absolute inset-0 bg-cover bg-center" style="background-image: url('https://lh3.googleusercontent.com/aida-public/AB6AXuC7uk-PDJGZWI9sLOHyuuqxcmpaUap089GZYkJYMuTK_xF1CcjUzVxWlb277B4NulH5ZaGj2R9rG-of8ZsGWykkX2qx5j0AZFChXbnQXRIKE1-BGoTYLthaSPeFYLRzQHCoPkNNQs7PJepxnxCRqKkjSa8QJtvYdusr1Amz00TjNclcR_c0uJ5SdLYV7iCJZwQCRdg1RRTwQyak7sl9j4gmt5k8WFpZsduHZGwIub1mOK9PbnpA6FNl')"></div>
        <div class="absolute inset-0 bg-gradient-to-t from-primary/80 via-primary/40 to-transparent"></div>
        <div class="absolute bottom-12 left-12 text-on-primary max-w-md z-10">
          <h1 class="font-display-lg text-display-lg font-bold mb-4 drop-shadow-md">Elevate your travel experience.</h1>
          <p class="font-body-lg text-body-lg opacity-90 drop-shadow">Seamless booking, premium service, and clear skies ahead.</p>
        </div>
      </div>

      <!-- Right Side: OTP Verification Form Canvas -->
      <div class="flex-1 flex flex-col justify-center px-6 lg:px-20 xl:px-28 bg-surface py-2xl">
        <div class="w-full max-w-md mx-auto">
          <!-- Logo Header -->
          <div class="mb-8 flex items-center gap-2">
            <span class="material-symbols-outlined text-secondary text-3xl">flight_takeoff</span>
            <span class="font-headline-md text-headline-md font-bold text-primary">SkyRoute</span>
          </div>

          <h2 class="font-headline-lg text-headline-lg font-bold text-primary mb-2">Email Verification</h2>
          <p class="font-body-md text-body-md text-on-surface-variant mb-8">Please enter the OTP sent to your registered email.</p>

          @if (errorMessage) {
            <div class="p-md rounded-lg bg-error-container text-on-error-container text-body-sm font-medium mb-md border border-error/20 flex items-center gap-2">
              <span class="material-symbols-outlined text-[20px]">error</span>
              <span>{{ errorMessage }}</span>
            </div>
          }

          @if (successMessage) {
            <div class="p-md rounded-lg bg-[#DCFCE7] text-[#166534] text-body-sm font-medium mb-md border border-[#166534]/20 flex items-center gap-2">
              <span class="material-symbols-outlined text-[20px]">check_circle</span>
              <span>{{ successMessage }}</span>
            </div>
          }

          <form [formGroup]="form" (ngSubmit)="onSubmit()" class="space-y-6">
            <div>
              <label class="block font-label-md text-label-md font-semibold text-on-surface mb-1" for="email">Email Address</label>
              <input type="email" id="email" formControlName="email" class="w-full rounded-lg border border-outline-variant bg-surface-container-lowest shadow-sm focus:border-secondary focus:ring-1 focus:ring-secondary font-body-md text-body-md py-3 px-4 outline-none transition-all" />
            </div>

            <div>
              <label class="block font-label-md text-label-md font-semibold text-on-surface mb-1" for="otp">Verification OTP</label>
              <input type="text" id="otp" formControlName="otp" class="w-full rounded-lg border border-outline-variant bg-surface-container-lowest shadow-sm focus:border-secondary focus:ring-1 focus:ring-secondary font-body-md text-body-md py-3 px-4 outline-none transition-all tracking-widest text-center text-headline-md font-bold" placeholder="123456" />
            </div>

            <button type="submit" class="w-full flex justify-center items-center gap-2 py-3 px-4 rounded-lg shadow-sm font-label-md text-label-md text-white bg-secondary hover:bg-secondary/90 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-secondary transition-all transform active:scale-[0.98] font-semibold" [disabled]="isLoading">
              @if (isLoading) {
                <span class="material-symbols-outlined animate-spin text-[20px]">progress_activity</span>
                <span>Verifying...</span>
              } @else {
                <span>Verify Email</span>
              }
            </button>
          </form>

          <div class="mt-8 text-center space-y-3">
            <button type="button" class="font-label-md text-label-md px-md py-sm rounded-lg border border-outline-variant text-on-surface-variant hover:bg-surface-container-low transition-colors" (click)="resendOtp()" [disabled]="isResending">
              {{ isResending ? 'Resending...' : 'Resend OTP' }}
            </button>
            <p class="font-body-md text-body-md text-on-surface-variant"><a routerLink="/login" class="font-label-md text-label-md text-secondary hover:text-on-secondary-fixed-variant font-semibold">Back to Sign In</a></p>
          </div>
        </div>
      </div>
    </div>
  `,
  styleUrls: ['../login/login.component.scss']
})
export class VerifyEmailComponent {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  public form: FormGroup = this.fb.group({
    email: [this.route.snapshot.queryParams['email'] || '', [Validators.required, Validators.email]],
    otp: ['', [Validators.required]]
  });

  public isLoading = false;
  public isResending = false;
  public errorMessage = '';
  public successMessage = '';

  public onSubmit(): void {
    if (this.form.invalid) return;
    this.isLoading = true;
    this.errorMessage = '';
    this.successMessage = '';

    this.authService.verifyEmail(this.form.value).subscribe({
      next: (res) => {
        this.isLoading = false;
        this.successMessage = res || 'Email verified successfully! You can now sign in.';
        setTimeout(() => this.router.navigate(['/login']), 1500);
      },
      error: (err) => {
        this.isLoading = false;
        this.errorMessage = err.error?.message || err.error || 'Verification failed.';
      }
    });
  }

  public resendOtp(): void {
    const email = this.form.value.email;
    if (!email) return;
    this.isResending = true;
    this.authService.resendVerification(email).subscribe({
      next: (res) => {
        this.isResending = false;
        this.successMessage = res || 'New verification OTP sent to your email.';
      },
      error: (err) => {
        this.isResending = false;
        this.errorMessage = err.error?.message || 'Failed to resend OTP.';
      }
    });
  }
}
