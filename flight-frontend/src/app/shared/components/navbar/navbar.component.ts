import { Component, inject, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../../core/auth/auth.service';
import { ImageAnimationDirective } from '../../../core/directives/image-animation.directive';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive, ImageAnimationDirective],
  templateUrl: './navbar.component.html',
  styleUrls: ['./navbar.component.scss']
})
export class NavbarComponent {
  public authService = inject(AuthService);
  private router = inject(Router);

  public isMobileMenuOpen = false;
  public isProfileOpen = false;

  get isAdminPage(): boolean {
    return this.router.url.startsWith('/admin');
  }

  get isAdminUser(): boolean {
    return this.authService.isAdmin();
  }

  public toggleMobileMenu(): void {
    this.isMobileMenuOpen = !this.isMobileMenuOpen;
  }

  public closeMobileMenu(): void {
    this.isMobileMenuOpen = false;
  }

  public toggleProfile(): void {
    this.isProfileOpen = !this.isProfileOpen;
  }

  public logout(): void {
    this.isProfileOpen = false;
    this.isMobileMenuOpen = false;
    this.authService.logout();
  }

  public initials(user: any): string {
    const name = user?.firstName ? `${user.firstName} ${user.lastName || ''}`.trim() : user?.name;
    if (!name) return 'M';
    const parts = name.trim().split(/\s+/);
    const first = parts[0]?.[0] ?? '';
    const second = parts.length > 1 ? parts[parts.length - 1][0] : '';
    return (first + second).toUpperCase();
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    const target = event.target as HTMLElement;
    if (!target.closest('.profile-menu')) {
      this.isProfileOpen = false;
    }
    if (!target.closest('.navbar__mobile-menu') && !target.closest('.navbar__mobile-toggle')) {
      this.isMobileMenuOpen = false;
    }
  }
}
