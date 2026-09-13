import { AfterViewInit, Component, OnDestroy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink, ActivatedRoute } from '@angular/router';
import { AuthService } from '../../../core/auth/auth.service';
import { environment } from '../../../../environments/environment';

declare global {
  interface Window {
    google?: {
      accounts: {
        id: {
          initialize: (config: GoogleIdInitializeConfig) => void;
          renderButton: (parent: HTMLElement, options: GoogleButtonOptions) => void;
          cancel: () => void;
        };
      };
    };
  }
}

interface GoogleCredentialResponse {
  credential?: string;
}

interface GoogleIdInitializeConfig {
  client_id: string;
  callback: (response: GoogleCredentialResponse) => void;
}

interface GoogleButtonOptions {
  theme: 'outline' | 'filled_blue' | 'filled_black';
  size: 'large' | 'medium' | 'small';
  shape: 'rectangular' | 'pill' | 'circle' | 'square';
  text: 'signin_with' | 'signup_with' | 'continue_with' | 'signin';
  width: number;
}

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss']
})
export class LoginComponent implements AfterViewInit, OnDestroy {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  public loginForm: FormGroup = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(6)]]
  });

  public isLoading = false;
  public isGoogleLoading = false;
  public showPassword = false;
  public errorMessage = '';
  public returnUrl: string = this.route.snapshot.queryParams['returnUrl'] || '/';
  private googleButtonRendered = false;

  public ngAfterViewInit(): void {
    this.renderGoogleButton();
  }

  public ngOnDestroy(): void {
    window.google?.accounts.id.cancel();
  }

  public onSubmit(): void {
    if (this.loginForm.invalid) {
      this.loginForm.markAllAsTouched();
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';

    this.authService.login(this.loginForm.value).subscribe({
      next: (res) => {
        this.isLoading = false;
        this.redirectAfterLogin(res.user.role);
      },
      error: (err) => {
        this.isLoading = false;
        this.errorMessage = this.getLoginErrorMessage(err);
      }
    });
  }

  public togglePasswordVisibility(): void {
    this.showPassword = !this.showPassword;
  }

  public renderGoogleButton(): void {
    if (this.googleButtonRendered) {
      return;
    }

    const buttonContainer = document.getElementById('googleSignInButton');
    if (!buttonContainer) {
      return;
    }

    if (!window.google?.accounts?.id) {
      setTimeout(() => this.renderGoogleButton(), 250);
      return;
    }

    window.google.accounts.id.initialize({
      client_id: environment.googleClientId,
      callback: (response) => this.handleGoogleCredential(response)
    });

    window.google.accounts.id.renderButton(buttonContainer, {
      theme: 'outline',
      size: 'large',
      shape: 'rectangular',
      text: 'signin_with',
      width: buttonContainer.offsetWidth || 384
    });

    this.googleButtonRendered = true;
  }

  private handleGoogleCredential(response: GoogleCredentialResponse): void {
    if (!response.credential) {
      this.errorMessage = 'Google sign-in did not return a valid token.';
      return;
    }

    this.isGoogleLoading = true;
    this.errorMessage = '';

    this.authService.googleLogin({ idToken: response.credential }).subscribe({
      next: (res) => {
        this.isGoogleLoading = false;
        this.redirectAfterLogin(res.user.role);
      },
      error: (err) => {
        this.isGoogleLoading = false;
        this.errorMessage = err.error?.message || err.error || 'Google sign-in failed. Please try again.';
      }
    });
  }

  public loginAsMockAdmin(): void {
    this.authService.mockAdminLogin();
  }

  private redirectAfterLogin(role: string): void {
    if (role === 'ADMIN' || role === 'ROLE_ADMIN') {
      this.router.navigate(['/admin']);
    } else {
      this.router.navigateByUrl(this.returnUrl);
    }
  }

  private getLoginErrorMessage(err: any): string {
    if (err?.status === 0) {
      return 'Cannot connect to backend. Please start the Spring Boot server on http://localhost:8081 and try again.';
    }

    if (typeof err?.error === 'string') {
      return err.error;
    }

    return err?.error?.message || 'Login failed. Please check your credentials.';
  }
}
